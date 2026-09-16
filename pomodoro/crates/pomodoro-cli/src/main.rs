use pomodoro_core::protocol::{
    GuardStatusInfo, IpcRequest, IpcResponse, DEFAULT_SOCKET_PATH, FALLBACK_SOCKET_PATH,
};
use pomodoro_core::time::MonotonicTime;
use std::io::{BufRead, BufReader, Write};
use std::os::unix::net::UnixStream;
use std::path::{Path, PathBuf};
use std::time::Duration;

fn connect_guard(custom_path: Option<&Path>) -> Result<UnixStream, String> {
    let candidate_paths = if let Some(p) = custom_path {
        vec![p.to_path_buf()]
    } else {
        vec![
            PathBuf::from(DEFAULT_SOCKET_PATH),
            PathBuf::from(FALLBACK_SOCKET_PATH),
        ]
    };

    for path in candidate_paths {
        if path.exists() {
            if let Ok(stream) = UnixStream::connect(&path) {
                let _ = stream.set_read_timeout(Some(Duration::from_secs(3)));
                let _ = stream.set_write_timeout(Some(Duration::from_secs(3)));
                return Ok(stream);
            }
        }
    }

    Err("Could not connect to pomodoro-input-guard. Is the service running?".to_string())
}

fn send_request(stream: &mut UnixStream, req: IpcRequest) -> Result<IpcResponse, String> {
    let req_str = req
        .to_json_line()
        .map_err(|e| format!("Serialization error: {}", e))?;
    stream
        .write_all(req_str.as_bytes())
        .map_err(|e| format!("IPC write failed: {}", e))?;
    stream
        .flush()
        .map_err(|e| format!("IPC flush failed: {}", e))?;

    let mut reader = BufReader::new(stream);
    let mut resp_line = String::new();
    reader
        .read_line(&mut resp_line)
        .map_err(|e| format!("IPC read failed: {}", e))?;

    IpcResponse::from_json_str(&resp_line).map_err(|e| format!("Response parse error: {}", e))
}

fn print_status(info: &GuardStatusInfo) {
    println!("============================================================");
    println!("            POMODORO INPUT GUARD STATUS REPORT              ");
    println!("============================================================");
    println!("Protocol Version:  {}", info.version);
    println!("Current State:     {}", info.state.phase_name());
    println!(
        "Input Locked:      {}",
        if info.input_locked {
            "YES (ENFORCED)"
        } else {
            "NO (NORMAL)"
        }
    );
    println!("Server Monotonic:  {}", info.server_time);

    let remaining_str = info.state.formatted_remaining(info.server_time);
    println!("Remaining Time:    {}", remaining_str);

    if let Some(deadline) = info.active_deadline {
        println!("Phase Deadline:    {}", deadline);
    }

    println!(
        "\nTotal Discovered Input Devices: {}",
        info.total_discovered_devices
    );
    println!(
        "Currently Grabbed Target Devices: {}",
        info.grabbed_devices.len()
    );
    for dev in &info.grabbed_devices {
        println!(
            "  * [{:?}] {} ({})",
            dev.class,
            dev.name,
            dev.path.display()
        );
    }
    println!("============================================================");
}

fn main() {
    let args: Vec<String> = std::env::args().skip(1).collect();
    let mut socket_path = None;

    // Parse options
    let mut clean_args = Vec::new();
    let mut i = 0;
    while i < args.len() {
        if (args[i] == "--socket" || args[i] == "-s") && i + 1 < args.len() {
            socket_path = Some(PathBuf::from(&args[i + 1]));
            i += 2;
            continue;
        }
        clean_args.push(args[i].clone());
        i += 1;
    }

    let command = clean_args.first().map(|s| s.as_str()).unwrap_or("status");

    let mut stream = match connect_guard(socket_path.as_deref()) {
        Ok(s) => s,
        Err(e) => {
            eprintln!("Error: {}", e);
            std::process::exit(1);
        }
    };

    match command {
        "status" => match send_request(&mut stream, IpcRequest::GetStatus) {
            Ok(IpcResponse::Status(info)) => print_status(&info),
            Ok(other) => println!("Received unexpected response: {:?}", other),
            Err(e) => eprintln!("Error querying status: {}", e),
        },

        "start-work" => {
            let duration_mins: Option<u64> = clean_args.get(1).and_then(|s| s.parse().ok());
            let duration_secs = duration_mins.map(|m| m * 60);
            match send_request(&mut stream, IpcRequest::StartWork { duration_secs }) {
                Ok(IpcResponse::Ok) => println!(
                    "Work phase started successfully ({}m)",
                    duration_mins.unwrap_or(25)
                ),
                Ok(IpcResponse::Error { message }) => eprintln!("Error: {}", message),
                Ok(other) => println!("Response: {:?}", other),
                Err(e) => eprintln!("Error: {}", e),
            }
        }

        "start-break" => {
            let duration_mins: Option<u64> = clean_args.get(1).and_then(|s| s.parse().ok());
            let duration_secs = duration_mins.map(|m| m * 60);
            match send_request(&mut stream, IpcRequest::StartBreak { duration_secs }) {
                Ok(IpcResponse::Ok) => println!(
                    "Break phase started successfully ({}m)",
                    duration_mins.unwrap_or(5)
                ),
                Ok(IpcResponse::Error { message }) => eprintln!("Error: {}", message),
                Ok(other) => println!("Response: {:?}", other),
                Err(e) => eprintln!("Error: {}", e),
            }
        }

        "lock" => {
            let deadline = MonotonicTime::ZERO;
            match send_request(&mut stream, IpcRequest::AcquireLock { deadline }) {
                Ok(IpcResponse::LockAcquired { devices }) => {
                    println!(
                        "Successfully acquired exclusive grab on {} input devices:",
                        devices.len()
                    );
                    for dev in devices {
                        println!(
                            "  * [{:?}] {} ({})",
                            dev.class,
                            dev.name,
                            dev.path.display()
                        );
                    }
                }
                Ok(IpcResponse::Error { message }) => eprintln!("Lock failed: {}", message),
                Ok(other) => println!("Response: {:?}", other),
                Err(e) => eprintln!("Error: {}", e),
            }
        }

        "unlock" | "release" => match send_request(&mut stream, IpcRequest::ReleaseLock) {
            Ok(IpcResponse::LockReleased) => println!("Input lock released successfully."),
            Ok(IpcResponse::Error { message }) => eprintln!("Release error: {}", message),
            Ok(other) => println!("Response: {:?}", other),
            Err(e) => eprintln!("Error: {}", e),
        },

        "emergency-unlock" => match send_request(&mut stream, IpcRequest::EmergencyUnlock) {
            Ok(IpcResponse::LockReleased) => {
                println!("Emergency unlock successful. System input released and reset to Idle.")
            }
            Ok(IpcResponse::Error { message }) => eprintln!("Error: {}", message),
            Ok(other) => println!("Response: {:?}", other),
            Err(e) => eprintln!("Error: {}", e),
        },

        "devices" | "list-devices" => match send_request(&mut stream, IpcRequest::ListDevices) {
            Ok(IpcResponse::DeviceList(devices)) => {
                println!("Discovered Linux Input Devices (Total: {}):", devices.len());
                println!("------------------------------------------------------------");
                for dev in devices {
                    let target_badge = if dev.class.is_enforcement_target() {
                        "[TARGET - WILL LOCK]"
                    } else {
                        "[IGNORED - PASS-THRU]"
                    };
                    println!(
                        "{:<22} {:<15} {:<35} {}",
                        dev.path.display(),
                        format!("{:?}", dev.class),
                        dev.name,
                        target_badge
                    );
                }
                println!("------------------------------------------------------------");
            }
            Ok(IpcResponse::Error { message }) => eprintln!("Error: {}", message),
            Ok(other) => println!("Response: {:?}", other),
            Err(e) => eprintln!("Error: {}", e),
        },

        "ping" => match send_request(&mut stream, IpcRequest::Ping) {
            Ok(IpcResponse::Pong { server_time }) => {
                println!("Pong from Guard Server! Monotonic time: {}", server_time);
            }
            Ok(other) => println!("Response: {:?}", other),
            Err(e) => eprintln!("Error: {}", e),
        },

        "help" | "--help" | "-h" => {
            println!("Usage: pomodoro-cli [OPTIONS] <COMMAND>");
            println!("\nCommands:");
            println!("  status              Query current Pomodoro status and grabbed devices");
            println!("  start-work [MINS]   Start Pomodoro work phase (default: 25 mins)");
            println!("  start-break [MINS]  Start Pomodoro break phase (default: 5 mins)");
            println!("  lock                Directly acquire input lock on all target devices");
            println!("  unlock              Release active input locks");
            println!("  emergency-unlock    Out-of-band administrative override and release");
            println!("  devices             List all classified input devices");
            println!("  ping                Ping the privileged guard daemon");
            println!("\nOptions:");
            println!("  -s, --socket <PATH> Custom Unix socket path");
        }

        unknown => {
            eprintln!(
                "Unknown command: '{}'. Run 'pomodoro-cli help' for usage.",
                unknown
            );
            std::process::exit(1);
        }
    }
}
