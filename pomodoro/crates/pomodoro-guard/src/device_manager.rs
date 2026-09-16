use nix::fcntl::{open, OFlag};
use nix::sys::stat::Mode;
use nix::unistd::close;
use pomodoro_core::device::{classify_device, DeviceInfo, EV_ABS, EV_KEY, EV_REL};
use pomodoro_core::error::{PomodoroError, Result};
use std::collections::HashMap;
use std::ffi::CStr;
use std::fs;
use std::os::fd::{IntoRawFd, RawFd};
use std::path::{Path, PathBuf};
use tracing::{debug, error, info, warn};

#[repr(C)]
#[derive(Debug, Default, Clone, Copy)]
pub struct InputId {
    pub bustype: u16,
    pub vendor: u16,
    pub product: u16,
    pub version: u16,
}

// ioctl calculation macro equivalent
const fn ioc(dir: u32, ty: u8, nr: u8, size: u32) -> u64 {
    ((dir as u64) << 30) | ((size as u64) << 16) | ((ty as u64) << 8) | (nr as u64)
}

const IOC_READ: u32 = 2;
const IOC_WRITE: u32 = 1;

pub const EVIOCGRAB: u64 = ioc(
    IOC_WRITE,
    b'E',
    0x90,
    std::mem::size_of::<libc::c_int>() as u32,
);
pub const EVIOCGID: u64 = ioc(IOC_READ, b'E', 0x02, std::mem::size_of::<InputId>() as u32);

pub fn eviocgname(len: usize) -> u64 {
    ioc(IOC_READ, b'E', 0x06, len as u32)
}

pub fn eviocgphys(len: usize) -> u64 {
    ioc(IOC_READ, b'E', 0x07, len as u32)
}

pub fn eviocguniq(len: usize) -> u64 {
    ioc(IOC_READ, b'E', 0x08, len as u32)
}

pub fn eviocgprop(len: usize) -> u64 {
    ioc(IOC_READ, b'E', 0x09, len as u32)
}

pub fn eviocgbit(ev: usize, len: usize) -> u64 {
    ioc(IOC_READ, b'E', (0x20 + ev) as u8, len as u32)
}

#[derive(Debug)]
pub struct DeviceManager {
    /// Map from device path to (DeviceInfo, Option<RawFd>)
    tracked_devices: HashMap<PathBuf, (DeviceInfo, Option<RawFd>)>,
    is_locked: bool,
}

impl Default for DeviceManager {
    fn default() -> Self {
        Self::new()
    }
}

impl DeviceManager {
    pub fn new() -> Self {
        Self {
            tracked_devices: HashMap::new(),
            is_locked: false,
        }
    }

    pub fn is_locked(&self) -> bool {
        self.is_locked
    }

    /// Enumerate all /dev/input/event* nodes and populate tracked_devices.
    pub fn scan_devices(&mut self) -> Result<Vec<DeviceInfo>> {
        let mut list = Vec::new();
        let input_dir = Path::new("/dev/input");
        if !input_dir.exists() {
            return Ok(list);
        }

        let entries = fs::read_dir(input_dir).map_err(|e| PomodoroError::DeviceIo {
            device: "/dev/input".to_string(),
            message: e.to_string(),
        })?;

        for entry in entries.flatten() {
            let path = entry.path();
            if let Some(file_name) = path.file_name().and_then(|n| n.to_str()) {
                if file_name.starts_with("event") {
                    if let Ok(info) = self.probe_device(&path) {
                        list.push(info.clone());
                        let existing_fd = self.tracked_devices.get(&path).and_then(|(_, fd)| *fd);
                        self.tracked_devices.insert(path, (info, existing_fd));
                    }
                }
            }
        }

        list.sort_by(|a, b| a.path.cmp(&b.path));
        Ok(list)
    }

    /// Probe a single /dev/input/eventX node to extract metadata and capability bitmasks.
    pub fn probe_device(&self, path: &Path) -> Result<DeviceInfo> {
        let path_str = path.to_string_lossy().to_string();
        let owned_fd = open(
            path,
            OFlag::O_RDONLY | OFlag::O_NONBLOCK | OFlag::O_CLOEXEC,
            Mode::empty(),
        )
        .map_err(|e| PomodoroError::DeviceIo {
            device: path_str.clone(),
            message: format!("open failed: {}", e),
        })?;

        let fd = owned_fd.into_raw_fd();
        let res = Self::query_device_fd(fd, path);
        let _ = close(fd);
        res
    }

    fn query_device_fd(fd: RawFd, path: &Path) -> Result<DeviceInfo> {
        // 1. Query device name
        let mut name_buf = [0u8; 256];
        let name = unsafe {
            if libc::ioctl(fd, eviocgname(name_buf.len()), name_buf.as_mut_ptr()) >= 0 {
                CStr::from_ptr(name_buf.as_ptr() as *const libc::c_char)
                    .to_string_lossy()
                    .to_string()
            } else {
                "Unknown Input Device".to_string()
            }
        };

        // 2. Query phys
        let mut phys_buf = [0u8; 256];
        let phys = unsafe {
            if libc::ioctl(fd, eviocgphys(phys_buf.len()), phys_buf.as_mut_ptr()) >= 0 {
                CStr::from_ptr(phys_buf.as_ptr() as *const libc::c_char)
                    .to_string_lossy()
                    .to_string()
            } else {
                String::new()
            }
        };

        // 3. Query uniq
        let mut uniq_buf = [0u8; 256];
        let uniq = unsafe {
            if libc::ioctl(fd, eviocguniq(uniq_buf.len()), uniq_buf.as_mut_ptr()) >= 0 {
                CStr::from_ptr(uniq_buf.as_ptr() as *const libc::c_char)
                    .to_string_lossy()
                    .to_string()
            } else {
                String::new()
            }
        };

        // 4. Query Input ID
        let mut input_id = InputId::default();
        unsafe {
            let _ = libc::ioctl(fd, EVIOCGID, &mut input_id);
        }

        // 5. Query event bits
        let mut ev_bits = [0u8; 8];
        unsafe {
            let _ = libc::ioctl(fd, eviocgbit(0, ev_bits.len()), ev_bits.as_mut_ptr());
        }

        // 6. Query key bits
        let mut key_bits = [0u8; 96];
        unsafe {
            let _ = libc::ioctl(fd, eviocgbit(EV_KEY, key_bits.len()), key_bits.as_mut_ptr());
        }

        // 7. Query rel bits
        let mut rel_bits = [0u8; 8];
        unsafe {
            let _ = libc::ioctl(fd, eviocgbit(EV_REL, rel_bits.len()), rel_bits.as_mut_ptr());
        }

        // 8. Query abs bits
        let mut abs_bits = [0u8; 64];
        unsafe {
            let _ = libc::ioctl(fd, eviocgbit(EV_ABS, abs_bits.len()), abs_bits.as_mut_ptr());
        }

        // 9. Query property bits
        let mut prop_bits = [0u8; 8];
        unsafe {
            let _ = libc::ioctl(fd, eviocgprop(prop_bits.len()), prop_bits.as_mut_ptr());
        }

        // Classify device
        let class = classify_device(&name, &ev_bits, &key_bits, &rel_bits, &abs_bits, &prop_bits);

        Ok(DeviceInfo {
            path: path.to_path_buf(),
            name,
            phys,
            uniq,
            bustype: input_id.bustype,
            vendor: input_id.vendor,
            product: input_id.product,
            version: input_id.version,
            class,
        })
    }

    /// Acquires exclusive kernel evdev grab on all target input devices.
    pub fn acquire_lock(&mut self) -> Result<Vec<DeviceInfo>> {
        self.scan_devices()?;

        let mut grabbed_devices = Vec::new();
        let mut grab_errors = Vec::new();

        let paths: Vec<PathBuf> = self
            .tracked_devices
            .iter()
            .filter(|(_, (info, _))| info.class.is_enforcement_target())
            .map(|(p, _)| p.clone())
            .collect();

        if paths.is_empty() {
            warn!("No target input devices found during lock acquisition");
            self.is_locked = true;
            return Ok(grabbed_devices);
        }

        for path in paths {
            let path_str = path.to_string_lossy().to_string();
            let (info, existing_fd) = self.tracked_devices.get_mut(&path).unwrap();

            // Open if not already open
            let fd = match existing_fd {
                Some(fd) => *fd,
                None => match open(
                    &path,
                    OFlag::O_RDONLY | OFlag::O_NONBLOCK | OFlag::O_CLOEXEC,
                    Mode::empty(),
                ) {
                    Ok(owned_fd) => {
                        let new_fd = owned_fd.into_raw_fd();
                        *existing_fd = Some(new_fd);
                        new_fd
                    }
                    Err(e) => {
                        let err_msg = format!("Failed to open {}: {}", path_str, e);
                        error!("{}", err_msg);
                        grab_errors.push(err_msg);
                        continue;
                    }
                },
            };

            // Issue EVIOCGRAB 1
            let res = unsafe { libc::ioctl(fd, EVIOCGRAB, 1 as libc::c_int) };
            if res < 0 {
                let err = std::io::Error::last_os_error();
                let err_msg = format!(
                    "EVIOCGRAB(1) failed on {} ({}): {}",
                    path_str, info.name, err
                );
                error!("{}", err_msg);
                grab_errors.push(err_msg);
                if let Some(old_fd) = existing_fd.take() {
                    let _ = close(old_fd);
                }
            } else {
                info!(
                    "Acquired exclusive grab on [{:?}] {} ({})",
                    info.class, info.name, path_str
                );
                grabbed_devices.push(info.clone());
            }
        }

        if !grab_errors.is_empty() && grabbed_devices.is_empty() {
            self.release_lock()?;
            return Err(PomodoroError::GrabFailed {
                device: "all target devices".to_string(),
                message: grab_errors.join("; "),
            });
        }

        self.is_locked = true;
        Ok(grabbed_devices)
    }

    /// Releases all active kernel grabs and closes open device descriptors.
    pub fn release_lock(&mut self) -> Result<()> {
        let mut release_errors = Vec::new();

        for (path, (info, fd_opt)) in self.tracked_devices.iter_mut() {
            if let Some(fd) = fd_opt.take() {
                let path_str = path.to_string_lossy().to_string();
                let res = unsafe { libc::ioctl(fd, EVIOCGRAB, 0 as libc::c_int) };
                if res < 0 {
                    let err = std::io::Error::last_os_error();
                    let msg = format!("EVIOCGRAB(0) release failed on {}: {}", path_str, err);
                    warn!("{}", msg);
                    release_errors.push(msg);
                } else {
                    debug!("Released grab on {} ({})", info.name, path_str);
                }
                let _ = close(fd);
            }
        }

        self.is_locked = false;
        info!("All input device grabs released successfully");

        if release_errors.is_empty() {
            Ok(())
        } else {
            Err(PomodoroError::ReleaseFailed {
                device: "devices".to_string(),
                message: release_errors.join("; "),
            })
        }
    }

    /// Handles hotplugged device. If currently locked, grabs the new device immediately.
    pub fn handle_device_added(&mut self, path: &Path) -> Result<Option<DeviceInfo>> {
        let info = match self.probe_device(path) {
            Ok(i) => i,
            Err(e) => {
                debug!("Cannot probe newly added device {}: {}", path.display(), e);
                return Ok(None);
            }
        };

        let path_buf = path.to_path_buf();
        info!(
            "Hotplug device discovered: {} ({:?})",
            info.name, info.class
        );

        if self.is_locked && info.class.is_enforcement_target() {
            info!("Lock is active: grabbing hotplugged device {}", info.name);
            let path_str = path.to_string_lossy().to_string();
            match open(
                path,
                OFlag::O_RDONLY | OFlag::O_NONBLOCK | OFlag::O_CLOEXEC,
                Mode::empty(),
            ) {
                Ok(owned_fd) => {
                    let fd = owned_fd.into_raw_fd();
                    let res = unsafe { libc::ioctl(fd, EVIOCGRAB, 1 as libc::c_int) };
                    if res < 0 {
                        let err = std::io::Error::last_os_error();
                        error!("Failed to grab hotplugged device {}: {}", path_str, err);
                        let _ = close(fd);
                        self.tracked_devices.insert(path_buf, (info.clone(), None));
                    } else {
                        info!("Successfully grabbed hotplugged device {}", info.name);
                        self.tracked_devices
                            .insert(path_buf, (info.clone(), Some(fd)));
                    }
                }
                Err(e) => {
                    error!("Failed to open hotplugged device {}: {}", path_str, e);
                    self.tracked_devices.insert(path_buf, (info.clone(), None));
                }
            }
        } else {
            self.tracked_devices.insert(path_buf, (info.clone(), None));
        }

        Ok(Some(info))
    }

    /// Handles device removed event.
    pub fn handle_device_removed(&mut self, path: &Path) -> Option<DeviceInfo> {
        if let Some((info, fd_opt)) = self.tracked_devices.remove(path) {
            if let Some(fd) = fd_opt {
                let _ = close(fd);
            }
            info!("Device removed: {} ({:?})", info.name, info.class);
            Some(info)
        } else {
            None
        }
    }

    pub fn list_devices(&self) -> Vec<DeviceInfo> {
        let mut list: Vec<DeviceInfo> = self
            .tracked_devices
            .values()
            .map(|(info, _)| info.clone())
            .collect();
        list.sort_by(|a, b| a.path.cmp(&b.path));
        list
    }

    pub fn grabbed_devices(&self) -> Vec<DeviceInfo> {
        let mut list: Vec<DeviceInfo> = self
            .tracked_devices
            .values()
            .filter(|(_, fd)| fd.is_some())
            .map(|(info, _)| info.clone())
            .collect();
        list.sort_by(|a, b| a.path.cmp(&b.path));
        list
    }
}

impl Drop for DeviceManager {
    fn drop(&mut self) {
        if self.is_locked {
            let _ = self.release_lock();
        }
    }
}
