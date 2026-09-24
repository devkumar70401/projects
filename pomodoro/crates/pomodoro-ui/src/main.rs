mod renderer;

use pomodoro_core::protocol::{
    GuardStatusInfo, IpcRequest, IpcResponse, DEFAULT_SOCKET_PATH, FALLBACK_SOCKET_PATH,
};
use renderer::BreakUiRenderer;
use softbuffer::Surface;
use std::collections::HashMap;
use std::io::{BufRead, BufReader, Write};
use std::num::NonZeroU32;
use std::os::unix::net::UnixStream;
use std::path::PathBuf;
use std::rc::Rc;
use std::time::{Duration, Instant};
use tracing::{debug, error, info, warn};
use tracing_subscriber::EnvFilter;
use winit::application::ApplicationHandler;
use winit::dpi::{PhysicalPosition, PhysicalSize};
use winit::event::WindowEvent;
use winit::event_loop::{ActiveEventLoop, ControlFlow, EventLoop};
use winit::platform::wayland::WindowAttributesExtWayland;
use winit::platform::x11::WindowAttributesExtX11;
use winit::window::{Fullscreen, Window, WindowId, WindowLevel};

fn connect_guard() -> Result<UnixStream, String> {
    let candidate_paths = [
        PathBuf::from(DEFAULT_SOCKET_PATH),
        PathBuf::from(FALLBACK_SOCKET_PATH),
    ];

    for path in &candidate_paths {
        if path.exists() {
            if let Ok(stream) = UnixStream::connect(path) {
                // Step 5: Bounded 50ms timeout prevents worst-case UI stalls
                let _ = stream.set_read_timeout(Some(Duration::from_millis(50)));
                let _ = stream.set_write_timeout(Some(Duration::from_millis(50)));
                return Ok(stream);
            }
        }
    }

    Err("Could not connect to pomodoro-input-guard IPC socket".to_string())
}

fn query_guard_status(stream: &mut UnixStream) -> Result<GuardStatusInfo, String> {
    let req = IpcRequest::GetStatus;
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

    match IpcResponse::from_json_str(&resp_line) {
        Ok(IpcResponse::Status(info)) => Ok(info),
        Ok(other) => Err(format!("Unexpected response: {:?}", other)),
        Err(e) => Err(format!("Parse error: {}", e)),
    }
}

struct WindowContext {
    window: Rc<Window>,
    surface: Surface<Rc<Window>, Rc<Window>>,
    width: u32,
    height: u32,
    #[allow(dead_code)]
    is_break_overlay: bool,
}

struct PomodoroApp {
    ipc_stream: Option<UnixStream>,
    last_query_time: Instant,
    last_status: Option<GuardStatusInfo>,
    renderer: BreakUiRenderer,
    windows: HashMap<WindowId, WindowContext>,
    is_break_active: bool,
    standalone_timer: Option<Instant>,
    work_duration: Duration,
    break_duration: Duration,
    saved_work_pos: Option<PhysicalPosition<i32>>,
    last_rendered_key: Option<(String, bool)>,
    force_redraw: bool,
}

impl PomodoroApp {
    fn new() -> Self {
        let ipc_stream = connect_guard().ok();
        if ipc_stream.is_none() {
            warn!("Guard service not reachable, running in standalone preview mode");
        }

        Self {
            ipc_stream,
            last_query_time: Instant::now() - Duration::from_secs(10),
            last_status: None,
            renderer: BreakUiRenderer::new(),
            windows: HashMap::new(),
            is_break_active: false,
            standalone_timer: Some(Instant::now()),
            work_duration: Duration::from_secs(1800),
            break_duration: Duration::from_secs(600),
            saved_work_pos: None,
            last_rendered_key: None,
            force_redraw: true,
        }
    }

    fn update_status(&mut self) {
        if self.last_query_time.elapsed() < Duration::from_millis(250) {
            return;
        }
        self.last_query_time = Instant::now();

        if let Some(ref mut stream) = self.ipc_stream {
            match query_guard_status(stream) {
                Ok(info) => {
                    self.last_status = Some(info);
                }
                Err(e) => {
                    debug!("Guard status query failed: {}", e);
                    self.ipc_stream = None;
                }
            }
        } else {
            // Attempt reconnect
            if let Ok(mut stream) = connect_guard() {
                if let Ok(info) = query_guard_status(&mut stream) {
                    self.last_status = Some(info);
                    self.ipc_stream = Some(stream);
                }
            }
        }
    }

    fn get_current_remaining_and_phase(&self) -> (String, bool) {
        if let Some(ref status) = self.last_status {
            let is_break = status.state.is_break();
            let remaining = status.state.formatted_remaining(status.server_time);
            (remaining, is_break)
        } else if let Some(start) = self.standalone_timer {
            // Standalone demonstration loop
            let cycle_dur = self.work_duration + self.break_duration;
            let elapsed = start.elapsed();
            let cycle_pos =
                Duration::from_nanos((elapsed.as_nanos() % cycle_dur.as_nanos()) as u64);

            if cycle_pos < self.work_duration {
                let rem = self.work_duration - cycle_pos;
                let mins = rem.as_secs() / 60;
                let secs = rem.as_secs() % 60;
                (format!("{:02}:{:02}", mins, secs), false)
            } else {
                let break_pos = cycle_pos - self.work_duration;
                let rem = self.break_duration - break_pos;
                let mins = rem.as_secs() / 60;
                let secs = rem.as_secs() % 60;
                (format!("{:02}:{:02}", mins, secs), true)
            }
        } else {
            ("30:00".to_string(), false)
        }
    }

    fn ensure_windows(&mut self, event_loop: &ActiveEventLoop) {
        let (_, should_be_break) = self.get_current_remaining_and_phase();

        if should_be_break != self.is_break_active || self.windows.is_empty() {
            // Save work window position if transitioning to break
            if !self.is_break_active && !self.windows.is_empty() {
                if let Some(ctx) = self.windows.values().next() {
                    if let Ok(pos) = ctx.window.outer_position() {
                        self.saved_work_pos = Some(pos);
                    }
                }
            }

            self.is_break_active = should_be_break;
            self.windows.clear();
            self.force_redraw = true;

            if should_be_break {
                // Multi-monitor Fullscreen Break Overlays
                let monitors: Vec<_> = event_loop.available_monitors().collect();
                if monitors.is_empty() {
                    self.create_single_overlay_window(event_loop, None);
                } else {
                    for monitor in monitors {
                        self.create_single_overlay_window(event_loop, Some(monitor));
                    }
                }
            } else {
                // Floating corner indicator window
                self.create_work_window(event_loop);
            }
        }
    }

fn configure_platform_attributes(builder: winit::window::WindowAttributes) -> winit::window::WindowAttributes {
    let b = WindowAttributesExtX11::with_name(builder, "pomodoro-ui", "pomodoro-ui");
    WindowAttributesExtWayland::with_name(b, "pomodoro-ui", "pomodoro-ui")
}

    fn create_single_overlay_window(
        &mut self,
        event_loop: &ActiveEventLoop,
        monitor: Option<winit::monitor::MonitorHandle>,
    ) {
        let mut builder = Self::configure_platform_attributes(
            Window::default_attributes()
                .with_title("Pomodoro Break Overlay")
                .with_decorations(false)
                .with_window_level(WindowLevel::AlwaysOnTop)
                .with_resizable(false)
        );

        if let Some(ref m) = monitor {
            builder = builder
                .with_fullscreen(Some(Fullscreen::Borderless(Some(m.clone()))));
        } else {
            builder = builder
                .with_fullscreen(Some(Fullscreen::Borderless(None)));
        }

        if let Ok(window) = event_loop.create_window(builder) {
            let window = Rc::new(window);
            let size = window.inner_size();
            let width = size.width.max(1);
            let height = size.height.max(1);

            let context = match softbuffer::Context::new(window.clone()) {
                Ok(c) => c,
                Err(e) => {
                    error!("Failed to create softbuffer context: {}", e);
                    return;
                }
            };
            let mut surface = match Surface::new(&context, window.clone()) {
                Ok(s) => s,
                Err(e) => {
                    error!("Failed to create softbuffer surface: {}", e);
                    return;
                }
            };

            // Step 3: Resize softbuffer surface ONCE on creation
            if let (Some(w), Some(h)) = (NonZeroU32::new(width), NonZeroU32::new(height)) {
                let _ = surface.resize(w, h);
            }

            let id = window.id();
            self.windows.insert(
                id,
                WindowContext {
                    window,
                    surface,
                    width,
                    height,
                    is_break_overlay: true,
                },
            );
        }
    }

    fn create_work_window(&mut self, event_loop: &ActiveEventLoop) {
        let (rem_str, _) = self.get_current_remaining_and_phase();
        let mut builder = Self::configure_platform_attributes(
            Window::default_attributes()
                .with_title(format!("🍅 {} - Focus", rem_str))
                .with_inner_size(PhysicalSize::new(240, 88))
                .with_decorations(true)
                .with_window_level(WindowLevel::AlwaysOnTop)
                .with_resizable(true)
        );

        if let Some(pos) = self.saved_work_pos {
            builder = builder.with_position(pos);
        } else {
            // Default to top-right corner of primary display
            let primary_mon = event_loop
                .primary_monitor()
                .or_else(|| event_loop.available_monitors().next());
            if let Some(m) = primary_mon {
                let mon_pos = m.position();
                let mon_size = m.size();
                let default_x = mon_pos.x + (mon_size.width as i32).saturating_sub(280);
                let default_y = mon_pos.y + 40;
                builder = builder.with_position(PhysicalPosition::new(default_x, default_y));
            }
        }

        if let Ok(window) = event_loop.create_window(builder) {
            let window = Rc::new(window);
            let size = window.inner_size();
            let width = size.width.max(1);
            let height = size.height.max(1);

            let context = match softbuffer::Context::new(window.clone()) {
                Ok(c) => c,
                Err(e) => {
                    error!("Failed to create softbuffer context: {}", e);
                    return;
                }
            };
            let mut surface = match Surface::new(&context, window.clone()) {
                Ok(s) => s,
                Err(e) => {
                    error!("Failed to create softbuffer surface: {}", e);
                    return;
                }
            };

            // Step 3: Resize softbuffer surface ONCE on creation
            if let (Some(w), Some(h)) = (NonZeroU32::new(width), NonZeroU32::new(height)) {
                let _ = surface.resize(w, h);
            }

            let id = window.id();
            self.windows.insert(
                id,
                WindowContext {
                    window,
                    surface,
                    width,
                    height,
                    is_break_overlay: false,
                },
            );
        }
    }

    fn render_all(&mut self, force: bool) {
        let (remaining_str, is_break) = self.get_current_remaining_and_phase();

        // Step 2: Skip rendering if displayed content has not changed and redraw is not forced
        if !force && !self.force_redraw {
            if let Some((ref last_rem, last_is_break)) = self.last_rendered_key {
                if last_rem == &remaining_str && last_is_break == is_break {
                    return;
                }
            }
        }

        for ctx in self.windows.values_mut() {
            let width = ctx.width;
            let height = ctx.height;

            if !is_break {
                ctx.window
                    .set_title(&format!("🍅 {} - Focus", remaining_str));
            }

            // Step 3: Do NOT call surface.resize on every frame; only fetch buffer and present
            if let Ok(mut buffer) = ctx.surface.buffer_mut() {
                let u32_buf: &mut [u32] = bytemuck::cast_slice_mut(&mut buffer);

                if is_break {
                    self.renderer
                        .render_break_frame(u32_buf, width, height, &remaining_str);
                } else {
                    self.renderer
                        .render_work_frame(u32_buf, width, height, &remaining_str);
                }

                let _ = buffer.present();
            }
        }

        self.last_rendered_key = Some((remaining_str, is_break));
        self.force_redraw = false;
    }
}

impl ApplicationHandler for PomodoroApp {
    fn resumed(&mut self, event_loop: &ActiveEventLoop) {
        self.update_status();
        self.ensure_windows(event_loop);
        self.render_all(true);
    }

    fn window_event(
        &mut self,
        event_loop: &ActiveEventLoop,
        window_id: WindowId,
        event: WindowEvent,
    ) {
        match event {
            WindowEvent::CloseRequested => {
                if !self.is_break_active {
                    // Send emergency unlock / reset to guard and exit cleanly
                    if let Some(ref mut stream) = self.ipc_stream {
                        let _ = stream.write_all(b"{\"action\":\"EmergencyUnlock\"}\n");
                        let _ = stream.flush();
                    }
                    event_loop.exit();
                }
            }
            WindowEvent::Moved(new_pos) => {
                if !self.is_break_active {
                    self.saved_work_pos = Some(new_pos);
                }
            }
            WindowEvent::Resized(new_size) => {
                if let Some(ctx) = self.windows.get_mut(&window_id) {
                    ctx.width = new_size.width.max(1);
                    ctx.height = new_size.height.max(1);
                    // Step 3: Only resize softbuffer surface when window size actually changes
                    if let (Some(w), Some(h)) =
                        (NonZeroU32::new(ctx.width), NonZeroU32::new(ctx.height))
                    {
                        let _ = ctx.surface.resize(w, h);
                    }
                    ctx.window.request_redraw();
                }
            }
            WindowEvent::RedrawRequested => {
                self.render_all(true);
            }
            _ => {}
        }
    }

    fn about_to_wait(&mut self, event_loop: &ActiveEventLoop) {
        self.update_status();
        self.ensure_windows(event_loop);
        self.render_all(false);

        // Step 2: 250ms interval ensures accurate 1-second countdowns while cutting wakeups
        event_loop.set_control_flow(ControlFlow::WaitUntil(
            Instant::now() + Duration::from_millis(250),
        ));
    }
}

fn main() {
    let env_filter = EnvFilter::try_from_default_env()
        .unwrap_or_else(|_| EnvFilter::new("pomodoro_ui=info,pomodoro_core=info"));
    tracing_subscriber::fmt()
        .with_env_filter(env_filter)
        .with_target(false)
        .init();

    info!("Starting Pomodoro User Session Controller");

    let event_loop = match EventLoop::new() {
        Ok(el) => el,
        Err(e) => {
            error!("Failed to initialize event loop: {}", e);
            std::process::exit(1);
        }
    };

    let mut app = PomodoroApp::new();
    let _ = event_loop.run_app(&mut app);
}
