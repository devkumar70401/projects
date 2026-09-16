use pomodoro_core::device::{
    classify_device, DeviceClass, EV_ABS, EV_KEY, EV_REL, INPUT_PROP_BUTTONPAD,
    INPUT_PROP_POINTING_STICK, KEY_A, KEY_ENTER, KEY_Q, KEY_Z, REL_X, REL_Y,
};

#[test]
fn test_complex_device_classification_matrix() {
    // 1. Internal AT Keyboard
    let mut ev = [0u8; 8];
    ev[0] |= 1 << EV_KEY;
    let mut key = [0u8; 96];
    for k in KEY_Q..=KEY_ENTER {
        key[k / 8] |= 1 << (k % 8);
    }
    for k in KEY_A..=KEY_Z {
        key[k / 8] |= 1 << (k % 8);
    }
    let cls = classify_device("AT Translated Set 2 keyboard", &ev, &key, &[], &[], &[]);
    assert_eq!(cls, DeviceClass::Keyboard);
    assert!(cls.is_enforcement_target());

    // 2. PointingStick (TrackPoint)
    let mut prop = [0u8; 8];
    prop[0] |= 1 << INPUT_PROP_POINTING_STICK;
    let cls = classify_device("DualPoint Stick", &ev, &key, &[], &[], &prop);
    assert_eq!(cls, DeviceClass::PointingStick);
    assert!(cls.is_enforcement_target());

    // 3. Precision Touchpad
    let mut prop_pad = [0u8; 8];
    prop_pad[0] |= 1 << INPUT_PROP_BUTTONPAD;
    let mut ev_abs = [0u8; 8];
    ev_abs[0] |= (1 << EV_KEY) | (1 << EV_ABS);
    let cls = classify_device("DLL0B48:01 Touchpad", &ev_abs, &key, &[], &[], &prop_pad);
    assert_eq!(cls, DeviceClass::Touchpad);
    assert!(cls.is_enforcement_target());

    // 4. USB Optical Mouse
    let mut ev_rel = [0u8; 8];
    ev_rel[0] |= (1 << EV_KEY) | (1 << EV_REL);
    let mut rel = [0u8; 8];
    rel[0] |= (1 << REL_X) | (1 << REL_Y);
    let cls = classify_device("Logitech Optical USB Mouse", &ev_rel, &key, &rel, &[], &[]);
    assert_eq!(cls, DeviceClass::Mouse);
    assert!(cls.is_enforcement_target());

    // 5. Ignored: Video Bus
    let cls = classify_device("Video Bus", &ev, &key, &[], &[], &[]);
    assert_eq!(cls, DeviceClass::Ignored);
    assert!(!cls.is_enforcement_target());

    // 6. Ignored: Lid Switch
    let cls = classify_device("Lid Switch", &ev, &key, &[], &[], &[]);
    assert_eq!(cls, DeviceClass::Ignored);
    assert!(!cls.is_enforcement_target());
}

#[test]
fn test_hotplug_simulation_lifecycle() {
    use pomodoro_core::state::{LockStatus, PomodoroEvent, PomodoroStateMachine, StateEffect};
    use pomodoro_core::time::MonotonicTime;
    use std::time::Duration;

    let mut sm = PomodoroStateMachine::new(1500, 300);
    let t0 = MonotonicTime::from_secs(500);

    // Enter break
    let eff = sm.step(
        PomodoroEvent::StartBreak {
            duration_secs: None,
        },
        t0,
    );
    assert!(eff.contains(&StateEffect::AcquireInputLock {
        deadline: t0 + Duration::from_secs(300)
    }));

    // Initial lock with 3 devices
    sm.step(PomodoroEvent::LockEstablished { device_count: 3 }, t0);
    if let pomodoro_core::state::PomodoroState::Break { lock_status, .. } = sm.state() {
        assert_eq!(*lock_status, LockStatus::Active { device_count: 3 });
    }

    // Simulate newly hotplugged keyboard mid-break (+100s)
    let t_mid = t0 + Duration::from_secs(100);
    sm.step(PomodoroEvent::LockEstablished { device_count: 4 }, t_mid);
    if let pomodoro_core::state::PomodoroState::Break { lock_status, .. } = sm.state() {
        assert_eq!(*lock_status, LockStatus::Active { device_count: 4 });
    }

    // At deadline (+300s): Break finishes and input is released automatically
    let t_end = t0 + Duration::from_secs(300);
    let eff = sm.step(
        PomodoroEvent::Tick {
            current_time: t_end,
        },
        t_end,
    );
    assert!(eff.contains(&StateEffect::ReleaseInputLock));
    assert!(sm.state().is_working());
}
