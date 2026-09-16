use thiserror::Error;

#[derive(Error, Debug, Clone, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
pub enum PomodoroError {
    #[error("Device I/O error on {device}: {message}")]
    DeviceIo { device: String, message: String },

    #[error("Device grab failed for {device}: {message}")]
    GrabFailed { device: String, message: String },

    #[error("Device release failed for {device}: {message}")]
    ReleaseFailed { device: String, message: String },

    #[error("No target input devices found to lock")]
    NoDevicesFound,

    #[error("Partial device lock failure: required {required}, grabbed {grabbed}")]
    PartialGrab { required: usize, grabbed: usize },

    #[error("IPC communication error: {0}")]
    Ipc(String),

    #[error("IPC protocol version mismatch: expected {expected}, got {found}")]
    ProtocolVersionMismatch { expected: u32, found: u32 },

    #[error("State machine error: {0}")]
    InvalidTransition(String),

    #[error("Invalid configuration: {0}")]
    InvalidConfig(String),

    #[error("Permission denied: {0}")]
    PermissionDenied(String),

    #[error("System error: {0}")]
    System(String),
}

pub type Result<T> = std::result::Result<T, PomodoroError>;
