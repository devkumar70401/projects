use pomodoro_core::device::{DeviceClass, DeviceInfo};
use pomodoro_core::protocol::{GuardStatusInfo, IpcRequest, IpcResponse, PROTOCOL_VERSION};
use pomodoro_core::state::{LockStatus, PomodoroState};
use pomodoro_core::time::MonotonicTime;
use std::path::PathBuf;

#[test]
fn test_ipc_request_roundtrips() {
    let requests = vec![
        IpcRequest::Ping,
        IpcRequest::GetStatus,
        IpcRequest::StartWork {
            duration_secs: Some(1500),
        },
        IpcRequest::StartBreak {
            duration_secs: Some(300),
        },
        IpcRequest::AcquireLock {
            deadline: MonotonicTime::from_secs(9999),
        },
        IpcRequest::ReleaseLock,
        IpcRequest::ListDevices,
        IpcRequest::EmergencyUnlock,
    ];

    for req in requests {
        let json_line = req.to_json_line().expect("Serialization must succeed");
        assert!(json_line.ends_with('\n'));
        let parsed = IpcRequest::from_json_str(&json_line).expect("Deserialization must succeed");
        let roundtrip_line = parsed.to_json_line().expect("Roundtrip serialize");
        assert_eq!(json_line, roundtrip_line);
    }
}

#[test]
fn test_ipc_response_roundtrips() {
    let dummy_device = DeviceInfo {
        path: PathBuf::from("/dev/input/event3"),
        name: "Test Keyboard".to_string(),
        phys: "isa0060/serio0/input0".to_string(),
        uniq: "".to_string(),
        bustype: 0x11,
        vendor: 1,
        product: 1,
        version: 1,
        class: DeviceClass::Keyboard,
    };

    let responses = vec![
        IpcResponse::Pong {
            server_time: MonotonicTime::from_secs(123),
        },
        IpcResponse::Ok,
        IpcResponse::LockReleased,
        IpcResponse::LockAcquired {
            devices: vec![dummy_device.clone()],
        },
        IpcResponse::DeviceList(vec![dummy_device.clone()]),
        IpcResponse::Error {
            message: "Test error description".to_string(),
        },
        IpcResponse::Status(GuardStatusInfo {
            version: PROTOCOL_VERSION,
            state: PomodoroState::Break {
                start_time: MonotonicTime::from_secs(100),
                deadline: MonotonicTime::from_secs(400),
                duration_secs: 300,
                lock_status: LockStatus::Active { device_count: 1 },
            },
            server_time: MonotonicTime::from_secs(200),
            active_deadline: Some(MonotonicTime::from_secs(400)),
            input_locked: true,
            grabbed_devices: vec![dummy_device],
            total_discovered_devices: 5,
        }),
    ];

    for resp in responses {
        let json_line = resp.to_json_line().expect("Serialization must succeed");
        assert!(json_line.ends_with('\n'));
        let parsed = IpcResponse::from_json_str(&json_line).expect("Deserialization must succeed");
        let roundtrip_line = parsed.to_json_line().expect("Roundtrip serialize");
        assert_eq!(json_line, roundtrip_line);
    }
}

#[test]
fn test_fuzz_malformed_ipc_requests() {
    let invalid_inputs = [
        "",
        "   ",
        "\n",
        "{",
        "{\"action\": \"UnknownAction\"}",
        "{\"action\": \"StartWork\", \"payload\": \"invalid_type\"}",
        "not a json object at all",
        "\0\0\0\0",
        "{\"action\": null}",
    ];

    for input in &invalid_inputs {
        let res = IpcRequest::from_json_str(input);
        assert!(
            res.is_err(),
            "Malformed input '{}' must be rejected cleanly",
            input
        );
    }
}
