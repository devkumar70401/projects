use pomodoro_core::state::{PomodoroEvent, PomodoroState, PomodoroStateMachine, StateEffect};
use pomodoro_core::time::{Clock, MockClock};
use std::time::Duration;

#[test]
fn test_exact_boundary_transitions_1500s_300s() {
    let mock_clock = MockClock::new(1_000_000_000_000); // 1000s
    let mut sm = PomodoroStateMachine::new(1500, 300);

    let t0 = mock_clock.now();
    sm.step(
        PomodoroEvent::StartWork {
            duration_secs: None,
        },
        t0,
    );

    // 1499.999s: Must still be WORKING
    let t_1499_999 = t0 + Duration::from_nanos(1_499_999_000_000);
    let eff = sm.step(
        PomodoroEvent::Tick {
            current_time: t_1499_999,
        },
        t_1499_999,
    );
    assert!(eff.is_empty());
    assert!(sm.state().is_working());
    assert_eq!(
        sm.state().remaining_duration(t_1499_999),
        Duration::from_nanos(1_000_000)
    );

    // 1500.000s: Exactly reaches deadline -> Transitions to BREAK
    let t_1500_000 = t0 + Duration::from_secs(1500);
    let eff = sm.step(
        PomodoroEvent::Tick {
            current_time: t_1500_000,
        },
        t_1500_000,
    );
    assert!(sm.state().is_break());
    assert!(eff.contains(&StateEffect::WorkPhaseEnded));
    assert!(eff.contains(&StateEffect::AcquireInputLock {
        deadline: t_1500_000 + Duration::from_secs(300)
    }));

    // 1500.001s: After boundary inside BREAK
    let t_1500_001 = t0 + Duration::from_nanos(1_500_001_000_000);
    let eff = sm.step(
        PomodoroEvent::Tick {
            current_time: t_1500_001,
        },
        t_1500_001,
    );
    assert!(eff.is_empty());
    assert!(sm.state().is_break());

    // Break boundary test: 299.999s into break -> Still BREAK
    let t_break_299_999 = t_1500_000 + Duration::from_nanos(299_999_000_000);
    let eff = sm.step(
        PomodoroEvent::Tick {
            current_time: t_break_299_999,
        },
        t_break_299_999,
    );
    assert!(eff.is_empty());
    assert!(sm.state().is_break());

    // Break boundary test: 300.000s into break -> Transitions to WORKING
    let t_break_300_000 = t_1500_000 + Duration::from_secs(300);
    let eff = sm.step(
        PomodoroEvent::Tick {
            current_time: t_break_300_000,
        },
        t_break_300_000,
    );
    assert!(sm.state().is_working());
    assert!(eff.contains(&StateEffect::ReleaseInputLock));
    assert!(eff.contains(&StateEffect::BreakPhaseEnded));
}

#[test]
fn test_clock_jump_and_suspend_immunity() {
    let mock_clock = MockClock::new(100_000_000_000); // 100s
    let mut sm = PomodoroStateMachine::new(1500, 300);

    let t0 = mock_clock.now();
    sm.step(
        PomodoroEvent::StartWork {
            duration_secs: None,
        },
        t0,
    );

    // Simulate system suspend for 2000 seconds
    mock_clock.advance(Duration::from_secs(2000));
    let t_wake = mock_clock.now();

    // On wake, Tick detects deadline passed and immediately triggers Break
    let eff = sm.step(
        PomodoroEvent::Tick {
            current_time: t_wake,
        },
        t_wake,
    );
    assert!(sm.state().is_break());
    assert!(eff.contains(&StateEffect::AcquireInputLock {
        deadline: t_wake + Duration::from_secs(300)
    }));
}

#[test]
fn test_repeated_start_calls_do_not_recreate_timers() {
    let mock_clock = MockClock::new(50_000_000_000);
    let mut sm = PomodoroStateMachine::new(1500, 300);
    let t0 = mock_clock.now();

    sm.step(
        PomodoroEvent::StartWork {
            duration_secs: None,
        },
        t0,
    );
    let initial_deadline = if let PomodoroState::Working { deadline, .. } = sm.state() {
        *deadline
    } else {
        panic!("Must be in working state");
    };

    // 10 repeated starts at t0 + 10s
    let t_10 = t0 + Duration::from_secs(10);
    for _ in 0..10 {
        let eff = sm.step(
            PomodoroEvent::StartWork {
                duration_secs: None,
            },
            t_10,
        );
        assert!(eff.is_empty());
        if let PomodoroState::Working { deadline, .. } = sm.state() {
            assert_eq!(
                *deadline, initial_deadline,
                "Deadline must not be reset on repeated start"
            );
        }
    }
}
