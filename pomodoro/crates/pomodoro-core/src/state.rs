use crate::time::MonotonicTime;
use serde::{Deserialize, Serialize};
use std::time::Duration;

pub const DEFAULT_WORK_DURATION_SECS: u64 = 30 * 60; // 1800 seconds = 30 minutes
pub const DEFAULT_BREAK_DURATION_SECS: u64 = 10 * 60; // 600 seconds = 10 minutes

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum LockStatus {
    Unchecked,
    Active { device_count: usize },
    Failed { reason: String },
    Released,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(tag = "type", content = "data")]
pub enum PomodoroState {
    Idle,
    Working {
        start_time: MonotonicTime,
        deadline: MonotonicTime,
        duration_secs: u64,
    },
    Break {
        start_time: MonotonicTime,
        deadline: MonotonicTime,
        duration_secs: u64,
        lock_status: LockStatus,
    },
}

impl PomodoroState {
    pub fn is_idle(&self) -> bool {
        matches!(self, PomodoroState::Idle)
    }

    pub fn is_working(&self) -> bool {
        matches!(self, PomodoroState::Working { .. })
    }

    pub fn is_break(&self) -> bool {
        matches!(self, PomodoroState::Break { .. })
    }

    pub fn phase_name(&self) -> &'static str {
        match self {
            PomodoroState::Idle => "IDLE",
            PomodoroState::Working { .. } => "WORKING",
            PomodoroState::Break { .. } => "BREAK",
        }
    }

    pub fn remaining_duration(&self, now: MonotonicTime) -> Duration {
        match self {
            PomodoroState::Idle => Duration::ZERO,
            PomodoroState::Working { deadline, .. } => now.remaining_until(*deadline),
            PomodoroState::Break { deadline, .. } => now.remaining_until(*deadline),
        }
    }

    pub fn formatted_remaining(&self, now: MonotonicTime) -> String {
        match self {
            PomodoroState::Idle => "30:00".to_string(),
            PomodoroState::Working { deadline, .. } => {
                MonotonicTime::format_remaining(now.remaining_until(*deadline))
            }
            PomodoroState::Break { deadline, .. } => {
                MonotonicTime::format_remaining(now.remaining_until(*deadline))
            }
        }
    }
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum PomodoroEvent {
    StartWork { duration_secs: Option<u64> },
    StartBreak { duration_secs: Option<u64> },
    Tick { current_time: MonotonicTime },
    LockEstablished { device_count: usize },
    LockFailed { reason: String },
    Reset,
    EmergencyUnlock,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum StateEffect {
    AcquireInputLock {
        deadline: MonotonicTime,
    },
    ReleaseInputLock,
    WorkPhaseStarted {
        deadline: MonotonicTime,
        duration_secs: u64,
    },
    BreakPhaseStarted {
        deadline: MonotonicTime,
        duration_secs: u64,
    },
    WorkPhaseEnded,
    BreakPhaseEnded,
    LockEstablishmentFailed {
        reason: String,
    },
}

#[derive(Debug, Clone)]
pub struct PomodoroStateMachine {
    state: PomodoroState,
    default_work_secs: u64,
    default_break_secs: u64,
}

impl Default for PomodoroStateMachine {
    fn default() -> Self {
        Self::new(DEFAULT_WORK_DURATION_SECS, DEFAULT_BREAK_DURATION_SECS)
    }
}

impl PomodoroStateMachine {
    pub fn new(work_secs: u64, break_secs: u64) -> Self {
        Self {
            state: PomodoroState::Idle,
            default_work_secs: if work_secs > 0 {
                work_secs
            } else {
                DEFAULT_WORK_DURATION_SECS
            },
            default_break_secs: if break_secs > 0 {
                break_secs
            } else {
                DEFAULT_BREAK_DURATION_SECS
            },
        }
    }

    pub fn state(&self) -> &PomodoroState {
        &self.state
    }

    pub fn set_state(&mut self, state: PomodoroState) {
        self.state = state;
    }

    /// Pure step function: processes event given the current monotonic time.
    /// Returns list of side-effects that should be executed by the runtime.
    pub fn step(&mut self, event: PomodoroEvent, now: MonotonicTime) -> Vec<StateEffect> {
        let mut effects = Vec::new();

        match (&self.state, event) {
            // StartWork when Idle
            (PomodoroState::Idle, PomodoroEvent::StartWork { duration_secs }) => {
                let dur = duration_secs.unwrap_or(self.default_work_secs).max(1);
                let deadline = now + Duration::from_secs(dur);
                self.state = PomodoroState::Working {
                    start_time: now,
                    deadline,
                    duration_secs: dur,
                };
                effects.push(StateEffect::WorkPhaseStarted {
                    deadline,
                    duration_secs: dur,
                });
            }

            // StartWork when already Working: Idempotent or no-op
            (PomodoroState::Working { .. }, PomodoroEvent::StartWork { .. }) => {
                // Idempotent: remain in existing working phase
            }

            // StartWork when in Break: Not allowed during enforced break unless EmergencyUnlock
            (PomodoroState::Break { .. }, PomodoroEvent::StartWork { .. }) => {
                // Break is enforced: ignore manual work start until break deadline arrives
            }

            // StartBreak manually from Working or Idle
            (
                PomodoroState::Idle | PomodoroState::Working { .. },
                PomodoroEvent::StartBreak { duration_secs },
            ) => {
                let dur = duration_secs.unwrap_or(self.default_break_secs).max(1);
                let deadline = now + Duration::from_secs(dur);
                self.state = PomodoroState::Break {
                    start_time: now,
                    deadline,
                    duration_secs: dur,
                    lock_status: LockStatus::Unchecked,
                };
                effects.push(StateEffect::AcquireInputLock { deadline });
                effects.push(StateEffect::BreakPhaseStarted {
                    deadline,
                    duration_secs: dur,
                });
            }

            // StartBreak when already in Break: Idempotent
            (PomodoroState::Break { .. }, PomodoroEvent::StartBreak { .. }) => {
                // Idempotent: already in break
            }

            // Tick in Working phase: Check if work duration expired
            (PomodoroState::Working { deadline, .. }, PomodoroEvent::Tick { current_time }) => {
                if current_time >= *deadline {
                    let dur = self.default_break_secs;
                    let break_deadline = current_time + Duration::from_secs(dur);
                    self.state = PomodoroState::Break {
                        start_time: current_time,
                        deadline: break_deadline,
                        duration_secs: dur,
                        lock_status: LockStatus::Unchecked,
                    };
                    effects.push(StateEffect::WorkPhaseEnded);
                    effects.push(StateEffect::AcquireInputLock {
                        deadline: break_deadline,
                    });
                    effects.push(StateEffect::BreakPhaseStarted {
                        deadline: break_deadline,
                        duration_secs: dur,
                    });
                }
            }

            // Tick in Break phase: Check if break duration expired
            (PomodoroState::Break { deadline, .. }, PomodoroEvent::Tick { current_time }) => {
                if current_time >= *deadline {
                    let dur = self.default_work_secs;
                    let work_deadline = current_time + Duration::from_secs(dur);
                    self.state = PomodoroState::Working {
                        start_time: current_time,
                        deadline: work_deadline,
                        duration_secs: dur,
                    };
                    effects.push(StateEffect::ReleaseInputLock);
                    effects.push(StateEffect::BreakPhaseEnded);
                    effects.push(StateEffect::WorkPhaseStarted {
                        deadline: work_deadline,
                        duration_secs: dur,
                    });
                }
            }

            // Lock feedback during Break
            (
                PomodoroState::Break {
                    start_time,
                    deadline,
                    duration_secs,
                    ..
                },
                PomodoroEvent::LockEstablished { device_count },
            ) => {
                self.state = PomodoroState::Break {
                    start_time: *start_time,
                    deadline: *deadline,
                    duration_secs: *duration_secs,
                    lock_status: LockStatus::Active { device_count },
                };
            }

            (
                PomodoroState::Break {
                    start_time,
                    deadline,
                    duration_secs,
                    ..
                },
                PomodoroEvent::LockFailed { reason },
            ) => {
                effects.push(StateEffect::LockEstablishmentFailed {
                    reason: reason.clone(),
                });
                self.state = PomodoroState::Break {
                    start_time: *start_time,
                    deadline: *deadline,
                    duration_secs: *duration_secs,
                    lock_status: LockStatus::Failed { reason },
                };
            }

            // Reset when Idle or Working
            (PomodoroState::Idle, PomodoroEvent::Reset) => {}
            (PomodoroState::Working { .. }, PomodoroEvent::Reset) => {
                self.state = PomodoroState::Idle;
            }

            // Reset during Break: Enforced break refuses normal reset
            (PomodoroState::Break { .. }, PomodoroEvent::Reset) => {
                // Invariant: Normal Reset cannot bypass active enforced break
            }

            // EmergencyUnlock: Admin/emergency override
            (PomodoroState::Break { .. }, PomodoroEvent::EmergencyUnlock) => {
                self.state = PomodoroState::Idle;
                effects.push(StateEffect::ReleaseInputLock);
                effects.push(StateEffect::BreakPhaseEnded);
            }

            (
                PomodoroState::Idle | PomodoroState::Working { .. },
                PomodoroEvent::EmergencyUnlock,
            ) => {
                self.state = PomodoroState::Idle;
                effects.push(StateEffect::ReleaseInputLock);
            }

            _ => {}
        }

        effects
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_standard_pomodoro_lifecycle() {
        let mut sm = PomodoroStateMachine::new(1500, 300);
        let t0 = MonotonicTime::from_secs(100);

        // 1. Start Work
        let eff = sm.step(
            PomodoroEvent::StartWork {
                duration_secs: None,
            },
            t0,
        );
        assert_eq!(eff.len(), 1);
        assert!(sm.state().is_working());
        assert_eq!(sm.state().remaining_duration(t0), Duration::from_secs(1500));

        // 2. Advance time to 1499s (1s before work deadline)
        let t_work_near = t0 + Duration::from_secs(1499);
        let eff = sm.step(
            PomodoroEvent::Tick {
                current_time: t_work_near,
            },
            t_work_near,
        );
        assert!(eff.is_empty());
        assert!(sm.state().is_working());

        // 3. Reach work deadline (1500s)
        let t_work_end = t0 + Duration::from_secs(1500);
        let eff = sm.step(
            PomodoroEvent::Tick {
                current_time: t_work_end,
            },
            t_work_end,
        );
        assert_eq!(eff.len(), 3);
        assert!(eff.contains(&StateEffect::WorkPhaseEnded));
        assert!(eff.contains(&StateEffect::AcquireInputLock {
            deadline: t_work_end + Duration::from_secs(300)
        }));
        assert!(sm.state().is_break());

        // 4. Lock feedback
        let eff = sm.step(
            PomodoroEvent::LockEstablished { device_count: 4 },
            t_work_end,
        );
        assert!(eff.is_empty());
        if let PomodoroState::Break { lock_status, .. } = sm.state() {
            assert_eq!(*lock_status, LockStatus::Active { device_count: 4 });
        } else {
            panic!("Expected break state");
        }

        // 5. Advance time during break (299s)
        let t_break_near = t_work_end + Duration::from_secs(299);
        let eff = sm.step(
            PomodoroEvent::Tick {
                current_time: t_break_near,
            },
            t_break_near,
        );
        assert!(eff.is_empty());
        assert!(sm.state().is_break());

        // 6. Normal reset during break is IGNORED
        let eff = sm.step(PomodoroEvent::Reset, t_break_near);
        assert!(eff.is_empty());
        assert!(sm.state().is_break());

        // 7. Reach break deadline (300s)
        let t_break_end = t_work_end + Duration::from_secs(300);
        let eff = sm.step(
            PomodoroEvent::Tick {
                current_time: t_break_end,
            },
            t_break_end,
        );
        assert_eq!(eff.len(), 3);
        assert!(eff.contains(&StateEffect::ReleaseInputLock));
        assert!(eff.contains(&StateEffect::BreakPhaseEnded));
        assert!(sm.state().is_working());
    }

    #[test]
    fn test_idempotency() {
        let mut sm = PomodoroStateMachine::new(1500, 300);
        let t0 = MonotonicTime::from_secs(100);

        sm.step(
            PomodoroEvent::StartWork {
                duration_secs: None,
            },
            t0,
        );
        let eff = sm.step(
            PomodoroEvent::StartWork {
                duration_secs: None,
            },
            t0,
        );
        assert!(eff.is_empty());
        assert!(sm.state().is_working());
    }

    #[test]
    fn test_emergency_unlock() {
        let mut sm = PomodoroStateMachine::new(1500, 300);
        let t0 = MonotonicTime::from_secs(100);
        sm.step(
            PomodoroEvent::StartBreak {
                duration_secs: None,
            },
            t0,
        );
        assert!(sm.state().is_break());

        let eff = sm.step(PomodoroEvent::EmergencyUnlock, t0);
        assert!(eff.contains(&StateEffect::ReleaseInputLock));
        assert!(sm.state().is_idle());
    }
}
