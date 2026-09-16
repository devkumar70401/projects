mod device_manager;
mod hotplug;
mod server;

use server::GuardServer;
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use tracing::{error, info};
use tracing_subscriber::EnvFilter;

fn main() {
    let env_filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new("pomodoro_guard=info,pomodoro_core=info"));
    tracing_subscriber::fmt()
        .with_env_filter(env_filter)
        .with_target(false)
        .init();

    info!("Starting Pomodoro Privileged Input Guard Daemon");

    let mut preferred_socket = None;
    let mut args = std::env::args().skip(1);
    while let Some(arg) = args.next() {
        if arg == "--socket" || arg == "-s" {
            if let Some(path) = args.next() {
                preferred_socket = Some(PathBuf::from(path));
            }
        } else if arg == "--help" || arg == "-h" {
            println!("Usage: pomodoro-guard [OPTIONS]");
            println!("Options:");
            println!("  -s, --socket <PATH>   Custom Unix domain socket path");
            println!("  -h, --help            Show help");
            return;
        }
    }

    let shutdown_flag = Arc::new(AtomicBool::new(false));
    let flag_clone = Arc::clone(&shutdown_flag);

    // Register POSIX signal handlers
    unsafe {
        use nix::sys::signal::{sigaction, SaFlags, SigAction, SigHandler, SigSet, Signal};

        extern "C" fn handle_signal(_: libc::c_int) {
            SIGNAL_RECEIVED.store(true, Ordering::SeqCst);
        }

        static SIGNAL_RECEIVED: AtomicBool = AtomicBool::new(false);

        let sa = SigAction::new(
            SigHandler::Handler(handle_signal),
            SaFlags::empty(),
            SigSet::empty(),
        );

        let _ = sigaction(Signal::SIGINT, &sa);
        let _ = sigaction(Signal::SIGTERM, &sa);
        let _ = sigaction(Signal::SIGHUP, &sa);

        std::thread::spawn(move || {
            while !flag_clone.load(Ordering::Relaxed) {
                if SIGNAL_RECEIVED.load(Ordering::SeqCst) {
                    info!("Termination signal caught, initiating graceful exit");
                    flag_clone.store(true, Ordering::SeqCst);
                    break;
                }
                std::thread::sleep(std::time::Duration::from_millis(50));
            }
        });
    }

    let mut server = match GuardServer::new(preferred_socket, shutdown_flag) {
        Ok(s) => s,
        Err(e) => {
            error!("Fatal: Failed to initialize Guard Server: {}", e);
            std::process::exit(1);
        }
    };

    if let Err(e) = server.run() {
        error!("Server runtime error: {}", e);
        std::process::exit(1);
    }
}
