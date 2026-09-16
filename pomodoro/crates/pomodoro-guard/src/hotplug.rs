use nix::unistd::close;
use pomodoro_core::error::{PomodoroError, Result};
use std::ffi::CStr;
use std::os::fd::RawFd;
use std::path::{Path, PathBuf};
use tracing::{debug, info};

pub enum HotplugEvent {
    Added(PathBuf),
    Removed(PathBuf),
}

pub struct HotplugMonitor {
    inotify_fd: RawFd,
    watch_wd: i32,
}

impl HotplugMonitor {
    pub fn new() -> Result<Self> {
        let inotify_fd = unsafe { libc::inotify_init1(libc::IN_NONBLOCK | libc::IN_CLOEXEC) };
        if inotify_fd < 0 {
            return Err(PomodoroError::System(format!(
                "inotify_init1 failed: {}",
                std::io::Error::last_os_error()
            )));
        }

        let input_path = std::ffi::CString::new("/dev/input").unwrap();
        let mask = libc::IN_CREATE | libc::IN_DELETE | libc::IN_ATTRIB;
        let watch_wd = unsafe { libc::inotify_add_watch(inotify_fd, input_path.as_ptr(), mask) };
        if watch_wd < 0 {
            let err = std::io::Error::last_os_error();
            let _ = close(inotify_fd);
            return Err(PomodoroError::System(format!(
                "inotify_add_watch(/dev/input) failed: {}",
                err
            )));
        }

        info!(
            "Hotplug monitor active on /dev/input (inotify wd: {})",
            watch_wd
        );

        Ok(Self {
            inotify_fd,
            watch_wd,
        })
    }

    #[allow(dead_code)]
    pub fn fd(&self) -> RawFd {
        self.inotify_fd
    }

    /// Reads pending inotify events and returns list of HotplugEvents.
    pub fn poll_events(&self) -> Vec<HotplugEvent> {
        let mut events = Vec::new();
        let mut buf = [0u8; 4096];

        let bytes_read = unsafe {
            libc::read(
                self.inotify_fd,
                buf.as_mut_ptr() as *mut libc::c_void,
                buf.len(),
            )
        };

        if bytes_read <= 0 {
            return events;
        }

        let mut offset = 0;
        let total = bytes_read as usize;

        while offset + std::mem::size_of::<libc::inotify_event>() <= total {
            let event_ptr = unsafe { &*(buf.as_ptr().add(offset) as *const libc::inotify_event) };

            let name_len = event_ptr.len as usize;
            let event_size = std::mem::size_of::<libc::inotify_event>() + name_len;

            if name_len > 0 && offset + event_size <= total {
                let name_bytes =
                    &buf[offset + std::mem::size_of::<libc::inotify_event>()..offset + event_size];
                if let Ok(c_str) = CStr::from_bytes_until_nul(name_bytes) {
                    if let Ok(name_str) = c_str.to_str() {
                        if name_str.starts_with("event") {
                            let dev_path = Path::new("/dev/input").join(name_str);
                            let mask = event_ptr.mask;

                            if (mask & (libc::IN_CREATE | libc::IN_ATTRIB)) != 0 {
                                debug!("Inotify device added/attrib: {}", dev_path.display());
                                events.push(HotplugEvent::Added(dev_path));
                            } else if (mask & libc::IN_DELETE) != 0 {
                                debug!("Inotify device deleted: {}", dev_path.display());
                                events.push(HotplugEvent::Removed(dev_path));
                            }
                        }
                    }
                }
            }

            offset += event_size;
        }

        events
    }
}

impl Drop for HotplugMonitor {
    fn drop(&mut self) {
        if self.watch_wd >= 0 {
            unsafe {
                libc::inotify_rm_watch(self.inotify_fd, self.watch_wd);
            }
        }
        if self.inotify_fd >= 0 {
            let _ = close(self.inotify_fd);
        }
    }
}
