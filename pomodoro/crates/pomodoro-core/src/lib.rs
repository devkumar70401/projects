pub mod device;
pub mod error;
pub mod protocol;
pub mod state;
pub mod time;

pub use device::{classify_device, test_bit, DeviceClass, DeviceInfo};
pub use error::{PomodoroError, Result};
pub use protocol::{
    GuardStatusInfo, IpcRequest, IpcResponse, DEFAULT_SOCKET_PATH, FALLBACK_SOCKET_PATH,
};
pub use state::{LockStatus, PomodoroEvent, PomodoroState, PomodoroStateMachine, StateEffect};
pub use time::{Clock, MockClock, MonotonicTime, SystemBootClock};

#[cfg(test)]
mod property_tests {
    use super::*;
    use proptest::prelude::*;
    use std::time::Duration;

    proptest! {
        #[test]
        fn prop_monotonic_remaining_non_negative(
            t0_nanos in 0u64..1_000_000_000_000,
            delta_nanos in 0u64..1_000_000_000_000,
            elapsed_nanos in 0u64..2_000_000_000_000,
        ) {
            let start = MonotonicTime::from_nanos(t0_nanos);
            let deadline = start + Duration::from_nanos(delta_nanos);
            let current = start + Duration::from_nanos(elapsed_nanos);

            let remaining = current.remaining_until(deadline);
            prop_assert!(remaining.as_nanos() <= delta_nanos as u128);

            if current >= deadline {
                prop_assert_eq!(remaining, Duration::ZERO);
                prop_assert!(current.is_expired(deadline));
            } else {
                prop_assert!(remaining > Duration::ZERO);
                prop_assert!(!current.is_expired(deadline));
            }
        }

        #[test]
        fn prop_state_machine_invariants(
            work_secs in 1u64..3600,
            break_secs in 1u64..3600,
            start_offset in 0u64..1_000_000,
            time_advances in prop::collection::vec(1u64..600, 1..50),
        ) {
            let mut sm = PomodoroStateMachine::new(work_secs, break_secs);
            let mut current_time = MonotonicTime::from_secs(start_offset);

            // Initial state is Idle
            prop_assert!(sm.state().is_idle());

            // Start work
            sm.step(PomodoroEvent::StartWork { duration_secs: None }, current_time);
            prop_assert!(sm.state().is_working());

            for step_secs in time_advances {
                current_time = current_time + Duration::from_secs(step_secs);
                let _effects = sm.step(PomodoroEvent::Tick { current_time }, current_time);

                match sm.state() {
                    PomodoroState::Idle => {},
                    PomodoroState::Working { start_time, deadline, .. } => {
                        prop_assert!(current_time < *deadline || current_time == *start_time);
                    },
                    PomodoroState::Break { start_time, deadline, .. } => {
                        prop_assert!(current_time < *deadline || current_time == *start_time);
                    }
                }
            }
        }

        #[test]
        fn prop_break_never_finishes_before_deadline(
            break_secs in 1u64..3600,
            t_advance in 0u64..3599,
        ) {
            let mut sm = PomodoroStateMachine::new(1500, break_secs);
            let t0 = MonotonicTime::from_secs(1000);

            // Trigger break
            sm.step(PomodoroEvent::StartBreak { duration_secs: Some(break_secs) }, t0);
            prop_assert!(sm.state().is_break());

            // Step time less than break_secs
            let step_dur = t_advance % break_secs;
            let t_mid = t0 + Duration::from_secs(step_dur);
            let effects = sm.step(PomodoroEvent::Tick { current_time: t_mid }, t_mid);

            if step_dur < break_secs {
                prop_assert!(sm.state().is_break());
                prop_assert!(!effects.contains(&StateEffect::BreakPhaseEnded));
            }
        }
    }
}
