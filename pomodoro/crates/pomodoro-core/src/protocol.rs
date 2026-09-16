use crate::device::DeviceInfo;
use crate::state::PomodoroState;
use crate::time::MonotonicTime;
use serde::{Deserialize, Serialize};

pub const DEFAULT_SOCKET_PATH: &str = "/run/pomodoro-input-guard.sock";
pub const FALLBACK_SOCKET_PATH: &str = "/tmp/pomodoro-input-guard.sock";
pub const PROTOCOL_VERSION: u32 = 1;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GuardStatusInfo {
    pub version: u32,
    pub state: PomodoroState,
    pub server_time: MonotonicTime,
    pub active_deadline: Option<MonotonicTime>,
    pub input_locked: bool,
    pub grabbed_devices: Vec<DeviceInfo>,
    pub total_discovered_devices: usize,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "action", content = "payload")]
pub enum IpcRequest {
    Ping,
    GetStatus,
    StartWork { duration_secs: Option<u64> },
    StartBreak { duration_secs: Option<u64> },
    AcquireLock { deadline: MonotonicTime },
    ReleaseLock,
    ListDevices,
    EmergencyUnlock,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "response", content = "payload")]
pub enum IpcResponse {
    Pong { server_time: MonotonicTime },
    Status(GuardStatusInfo),
    LockAcquired { devices: Vec<DeviceInfo> },
    LockReleased,
    DeviceList(Vec<DeviceInfo>),
    Ok,
    Error { message: String },
}

impl IpcRequest {
    pub fn to_json_line(&self) -> Result<String, serde_json::Error> {
        let mut s = serde_json::to_string(self)?;
        s.push('\n');
        Ok(s)
    }

    pub fn from_json_str(s: &str) -> Result<Self, serde_json::Error> {
        serde_json::from_str(s.trim())
    }
}

impl IpcResponse {
    pub fn to_json_line(&self) -> Result<String, serde_json::Error> {
        let mut s = serde_json::to_string(self)?;
        s.push('\n');
        Ok(s)
    }

    pub fn from_json_str(s: &str) -> Result<Self, serde_json::Error> {
        serde_json::from_str(s.trim())
    }
}
