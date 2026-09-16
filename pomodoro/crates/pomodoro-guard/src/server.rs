use crate::device_manager::DeviceManager;
use crate::hotplug::{HotplugEvent, HotplugMonitor};
use pomodoro_core::error::{PomodoroError, Result};
use pomodoro_core::protocol::{
    GuardStatusInfo, IpcRequest, IpcResponse, DEFAULT_SOCKET_PATH, FALLBACK_SOCKET_PATH,
    PROTOCOL_VERSION,
};
use pomodoro_core::state::{PomodoroEvent, PomodoroState, PomodoroStateMachine, StateEffect};
use pomodoro_core::time::{Clock, MonotonicTime, SystemBootClock};
use std::collections::HashMap;
use std::fs;
use std::io::{BufRead, BufReader, Write};
use std::os::fd::{AsRawFd, RawFd};
use std::os::unix::fs::PermissionsExt;
use std::os::unix::net::{UnixListener, UnixStream};
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::time::Duration;
use tracing::{debug, error, info, warn};

pub struct GuardServer {
    socket_path: PathBuf,
    listener: UnixListener,
    device_manager: DeviceManager,
    hotplug_monitor: Option<HotplugMonitor>,
    state_machine: PomodoroStateMachine,
    clock: SystemBootClock,
    shutdown_flag: Arc<AtomicBool>,
    clients: HashMap<RawFd, UnixStream>,
}

impl GuardServer {
    pub fn new(preferred_path: Option<PathBuf>, shutdown_flag: Arc<AtomicBool>) -> Result<Self> {
        let clock = SystemBootClock::new();
        let state_machine = PomodoroStateMachine::default();
        let mut device_manager = DeviceManager::new();
        let _ = device_manager.scan_devices();

        let hotplug_monitor = match HotplugMonitor::new() {
            Ok(m) => Some(m),
            Err(e) => {
                warn!("Hotplug monitor initialization failed: {}", e);
                None
            }
        };

        // Determine socket path
        let candidate_paths = if let Some(p) = preferred_path {
            vec![p]
        } else {
            vec![
                PathBuf::from(DEFAULT_SOCKET_PATH),
                PathBuf::from(FALLBACK_SOCKET_PATH),
            ]
        };

        let mut bound_listener = None;
        let mut active_path = None;

        for path in candidate_paths {
            if path.exists() {
                let _ = fs::remove_file(&path);
            }
            if let Some(parent) = path.parent() {
                let _ = fs::create_dir_all(parent);
            }

            match UnixListener::bind(&path) {
                Ok(l) => {
                    let _ = l.set_nonblocking(true);
                    let _ = fs::set_permissions(&path, fs::Permissions::from_mode(0o666));
                    info!("Input Guard IPC listening on {}", path.display());
                    bound_listener = Some(l);
                    active_path = Some(path);
                    break;
                }
                Err(e) => {
                    warn!("Failed to bind socket on {}: {}", path.display(), e);
                }
            }
        }

        let listener = bound_listener.ok_or_else(|| {
            PomodoroError::Ipc("Failed to bind any Unix domain socket candidate".to_string())
        })?;
        let socket_path = active_path.unwrap();

        Ok(Self {
            socket_path,
            listener,
            device_manager,
            hotplug_monitor,
            state_machine,
            clock,
            shutdown_flag,
            clients: HashMap::new(),
        })
    }

    pub fn run(&mut self) -> Result<()> {
        info!("Pomodoro Privileged Input Guard Server started");

        while !self.shutdown_flag.load(Ordering::Relaxed) {
            let now = self.clock.now();

            // 1. Accept incoming client connections
            self.accept_clients();

            // 2. Poll inotify hotplug events
            if let Some(ref monitor) = self.hotplug_monitor {
                let hotplug_events = monitor.poll_events();
                for ev in hotplug_events {
                    match ev {
                        HotplugEvent::Added(path) => {
                            let _ = self.device_manager.handle_device_added(&path);
                        }
                        HotplugEvent::Removed(path) => {
                            let _ = self.device_manager.handle_device_removed(&path);
                        }
                    }
                }
            }

            // 3. Process requests from connected clients
            self.process_client_requests(now);

            // 4. Tick state machine with authoritative monotonic time
            let effects = self
                .state_machine
                .step(PomodoroEvent::Tick { current_time: now }, now);
            self.handle_effects(effects);

            // 5. Short sleep to prevent busy-spinning (50ms gives 20Hz responsiveness with ~0% CPU)
            std::thread::sleep(Duration::from_millis(50));
        }

        info!("Shutting down Input Guard Server...");
        self.cleanup();
        Ok(())
    }

    fn accept_clients(&mut self) {
        while let Ok((stream, _)) = self.listener.accept() {
            let _ = stream.set_nonblocking(true);
            let fd = stream.as_raw_fd();
            self.clients.insert(fd, stream);
            debug!("New IPC client connected (fd: {})", fd);
        }
    }

    fn process_client_requests(&mut self, now: MonotonicTime) {
        let mut disconnected = Vec::new();
        let fds: Vec<RawFd> = self.clients.keys().cloned().collect();

        for fd in fds {
            if let Some(mut stream) = self.clients.remove(&fd) {
                let mut reader = BufReader::new(&stream);
                let mut line = String::new();

                match reader.read_line(&mut line) {
                    Ok(0) => {
                        debug!("IPC client disconnected (fd: {})", fd);
                        disconnected.push(fd);
                    }
                    Ok(_) => {
                        let trimmed = line.trim();
                        if !trimmed.is_empty() {
                            let response = self.handle_request_str(trimmed, now);
                            if let Ok(resp_json) = response.to_json_line() {
                                let _ = stream.write_all(resp_json.as_bytes());
                                let _ = stream.flush();
                            }
                        }
                        self.clients.insert(fd, stream);
                    }
                    Err(ref e) if e.kind() == std::io::ErrorKind::WouldBlock => {
                        self.clients.insert(fd, stream);
                    }
                    Err(e) => {
                        debug!("Read error on client fd {}: {}", fd, e);
                        disconnected.push(fd);
                    }
                }
            }
        }

        for fd in disconnected {
            self.clients.remove(&fd);
        }
    }

    fn handle_request_str(&mut self, req_str: &str, now: MonotonicTime) -> IpcResponse {
        match IpcRequest::from_json_str(req_str) {
            Ok(req) => self.handle_ipc_request(req, now),
            Err(e) => IpcResponse::Error {
                message: format!("Malformed request JSON: {}", e),
            },
        }
    }

    fn handle_ipc_request(&mut self, req: IpcRequest, now: MonotonicTime) -> IpcResponse {
        match req {
            IpcRequest::Ping => IpcResponse::Pong { server_time: now },

            IpcRequest::GetStatus => {
                let active_deadline = match self.state_machine.state() {
                    PomodoroState::Working { deadline, .. } => Some(*deadline),
                    PomodoroState::Break { deadline, .. } => Some(*deadline),
                    PomodoroState::Idle => None,
                };
                let status = GuardStatusInfo {
                    version: PROTOCOL_VERSION,
                    state: self.state_machine.state().clone(),
                    server_time: now,
                    active_deadline,
                    input_locked: self.device_manager.is_locked(),
                    grabbed_devices: self.device_manager.grabbed_devices(),
                    total_discovered_devices: self.device_manager.list_devices().len(),
                };
                IpcResponse::Status(status)
            }

            IpcRequest::StartWork { duration_secs } => {
                let effects = self
                    .state_machine
                    .step(PomodoroEvent::StartWork { duration_secs }, now);
                self.handle_effects(effects);
                IpcResponse::Ok
            }

            IpcRequest::StartBreak { duration_secs } => {
                let effects = self
                    .state_machine
                    .step(PomodoroEvent::StartBreak { duration_secs }, now);
                self.handle_effects(effects);
                IpcResponse::Ok
            }

            IpcRequest::AcquireLock { deadline: _ } => match self.device_manager.acquire_lock() {
                Ok(grabbed) => {
                    let count = grabbed.len();
                    let eff = self.state_machine.step(
                        PomodoroEvent::LockEstablished {
                            device_count: count,
                        },
                        now,
                    );
                    self.handle_effects(eff);
                    IpcResponse::LockAcquired { devices: grabbed }
                }
                Err(e) => {
                    let eff = self.state_machine.step(
                        PomodoroEvent::LockFailed {
                            reason: e.to_string(),
                        },
                        now,
                    );
                    self.handle_effects(eff);
                    IpcResponse::Error {
                        message: format!("Acquisition failed: {}", e),
                    }
                }
            },

            IpcRequest::ReleaseLock => {
                let _ = self.device_manager.release_lock();
                IpcResponse::LockReleased
            }

            IpcRequest::ListDevices => {
                let devices = self.device_manager.list_devices();
                IpcResponse::DeviceList(devices)
            }

            IpcRequest::EmergencyUnlock => {
                info!("Emergency unlock triggered");
                let effects = self.state_machine.step(PomodoroEvent::EmergencyUnlock, now);
                self.handle_effects(effects);
                let _ = self.device_manager.release_lock();
                IpcResponse::LockReleased
            }
        }
    }

    fn handle_effects(&mut self, effects: Vec<StateEffect>) {
        for effect in effects {
            match effect {
                StateEffect::AcquireInputLock { deadline } => {
                    info!(
                        "State effect: Acquiring input lock for deadline {}",
                        deadline
                    );
                    match self.device_manager.acquire_lock() {
                        Ok(grabbed) => {
                            info!("Lock acquired: {} devices grabbed", grabbed.len());
                            let now = self.clock.now();
                            let eff = self.state_machine.step(
                                PomodoroEvent::LockEstablished {
                                    device_count: grabbed.len(),
                                },
                                now,
                            );
                            self.handle_effects(eff);
                        }
                        Err(e) => {
                            error!("Lock acquisition failed: {}", e);
                            let now = self.clock.now();
                            let eff = self.state_machine.step(
                                PomodoroEvent::LockFailed {
                                    reason: e.to_string(),
                                },
                                now,
                            );
                            self.handle_effects(eff);
                        }
                    }
                }

                StateEffect::ReleaseInputLock => {
                    info!("State effect: Releasing input lock");
                    let _ = self.device_manager.release_lock();
                }

                StateEffect::WorkPhaseStarted {
                    deadline,
                    duration_secs,
                } => {
                    info!(
                        "Work phase started (duration: {}s, deadline: {})",
                        duration_secs, deadline
                    );
                }

                StateEffect::BreakPhaseStarted {
                    deadline,
                    duration_secs,
                } => {
                    info!(
                        "Mandatory break phase started (duration: {}s, deadline: {})",
                        duration_secs, deadline
                    );
                }

                StateEffect::WorkPhaseEnded => {
                    info!("Work phase completed, transitioning to mandatory break");
                }

                StateEffect::BreakPhaseEnded => {
                    info!("Break phase completed, input automatically restored, transitioning to work");
                }

                StateEffect::LockEstablishmentFailed { reason } => {
                    error!("Lock establishment failed: {}", reason);
                }
            }
        }
    }

    fn cleanup(&mut self) {
        let _ = self.device_manager.release_lock();
        if self.socket_path.exists() {
            let _ = fs::remove_file(&self.socket_path);
        }
    }
}

impl Drop for GuardServer {
    fn drop(&mut self) {
        self.cleanup();
    }
}
