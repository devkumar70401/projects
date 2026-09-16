# keyboard-lock: Minimal Keyboard-Only Lock Utility for Linux

> **Core Guarantee**: This utility locks **ONLY keyboard input**. Mouse, displays, animations, video playback, applications, audio, networking, CPU/GPU processing, and all other desktop functionality remain 100% operational.

`keyboard-lock` is a minimal, high-reliability Linux system utility built in C for Ubuntu Linux (Wayland & X11). Its sole purpose is to temporarily suppress keyboard input when locked, while ensuring everything else continues executing without interference.

---

## 1. Core Behavior & State Machine

The utility maintains a strict two-state machine:

```text
                 Ctrl + Alt + K (Press)
  [UNLOCKED] ──────────────────────────────► [LOCKED]
      ▲                                         │
      │                                         │
      └─────────────────────────────────────────┘
                 Ctrl + Alt + K (Press)
```

* **Toggle Hotkey**: `Ctrl + Alt + K`
* **Single Activation**: Auto-repeat is ignored for state transitions. Holding down `Ctrl + Alt + K` toggles the state exactly once.
* **Hotkey Consumption**: The activation hotkey is consumed and intercepted by `keyboard-lock`. It is never leaked to the focused application (e.g. VS Code, terminal, browser).
* **Modifier Isolation**: Combinations like `Ctrl + Alt + Shift + K` or `Ctrl + K` are ignored and will not trigger a state transition.

---

## 2. System Functionality Matrix While Locked

| Component | Status While Locked | Details |
| :--- | :--- | :--- |
| **Keyboard** | ❌ **BLOCKED** | Every single key (A–Z, 0–9, Enter, Space, Esc, Backspace, Ctrl+C, Alt+Tab, F1–F12, Super) is swallowed and discarded. |
| **Unlock Hotkey** | ✅ **ACTIVE** | `Ctrl + Alt + K` is intercepted and immediately restores normal keyboard typing. |
| **Mouse / Touchpad** | ✅ **100% NORMAL** | Left click, right click, middle click, scroll wheel, drag, text selection, window moving, and window resizing work continuously. |
| **Monitors & Displays** | ✅ **100% NORMAL** | Rendering, 60/120Hz refresh, video playback, animations, multi-monitor setups, and display hotplugging remain unaffected. |
| **Applications** | ✅ **100% NORMAL** | Browsers, IDEs, video players, compilers, background tasks, and games run without pausing or freezing. No `SIGSTOP` or window freezing. |
| **Audio & Network** | ✅ **100% NORMAL** | Music playback, voice calls, downloads, and network connections continue uninterrupted. |
| **CPU / GPU / Memory** | ✅ **100% NORMAL** | System resources run at full performance. `keyboard-lock` consumes <0.1% CPU and <3MB RAM. |

---

## 3. Architecture & Input Layer

### Wayland & X11 Compatibility
`keyboard-lock` uses the Linux kernel `evdev` interface (`EVIOCGRAB`):
1. **Direct Kernel Interception**: Applies `ioctl(fd, EVIOCGRAB, 1)` to keyboard `/dev/input/event*` devices. The kernel stops routing keyboard events to `libinput`, Mutter, KWin, Xwayland, or Xorg.
2. **Strict Pointer Isolation**: Pointer and touchpad devices (`/dev/input/event*` with `BTN_LEFT`, `REL_X`, `ABS_X` and lacking alphanumeric keys) are never touched or grabbed.
3. **Hotplug Support**: Uses `inotify` on `/dev/input` to automatically detect and lock newly connected USB or Bluetooth keyboards.
4. **Pure X11 Support**: Also includes an optional `x11` backend using `XGrabKeyboard` for legacy X11 sessions.

---

## 4. Building & Installation

### Prerequisites
* GCC or Clang compiler
* GNU Make or CMake
* Standard Linux C library (`glibc`) and kernel headers

### Compilation
```bash
cd keyboard-lock
make
```

### Run Tests
```bash
make test
```

### Installation
```bash
sudo make install
```
This installs:
* Binary to `/usr/local/bin/keyboard-lock`
* Systemd user service to `~/.config/systemd/user/keyboard-lock.service`

### Permissions Setup (Non-Root User Access)
To allow your user account to grab input devices without `sudo`:
```bash
sudo ./scripts/setup_permissions.sh
```
*Adds user to `input` group and installs `/etc/udev/rules.d/99-keyboard-lock.rules`.*

---

## 5. Usage & CLI Options

### Start in Foreground
```bash
keyboard-lock
```

### Start as Background Daemon
```bash
keyboard-lock --daemon
```

### Start via systemd User Service
```bash
systemctl --user enable --now keyboard-lock
```

### Control Running Instance
```bash
keyboard-lock --status    # Check current status (LOCKED / UNLOCKED, device count, uptime)
keyboard-lock --lock      # Engage keyboard lock
keyboard-lock --unlock    # Release keyboard lock
keyboard-lock --toggle    # Toggle keyboard lock
keyboard-lock --stop      # Stop background daemon
```

### CLI Options
```text
  -d, --daemon                  Fork into background daemon
  -b, --backend <evdev|x11|auto> Input interception backend (default: auto/evdev)
      --hotkey <combo>          Custom hotkey (default: ctrl+alt+k)
  -v, --verbose                 Enable detailed debug logging
  -q, --quiet                   Suppress informative output
      --syslog                  Log output to system syslog / journald
  -V, --version                 Show version information
  -h, --help                    Show help message
```

---

## 6. Privacy & Zero-Keylogging Guarantee

`keyboard-lock` strictly upholds privacy:
* **NO keystrokes are ever stored, buffered, recorded, printed, or transmitted.**
* Only boolean modifier flags (`ctrl_held`, `alt_held`) and target key comparison are evaluated in memory.
* System logs only report lifecycle events: `Daemon started`, `Keyboard locked`, `Keyboard unlocked`, `Daemon stopped`.

---

## 7. Emergency Recovery Procedures

1. **Hotkey**: Press `Ctrl + Alt + K` to unlock.
2. **CLI**: Run `keyboard-lock --unlock` from a terminal or script.
3. **Mouse / GUI**: Click an unlock launcher icon using the mouse.
4. **Process Termination**: Run `pkill keyboard-lock`. Grabs are immediately released.
5. **Kernel Invariant**: If killed (`kill -9`), the Linux kernel automatically revokes all `EVIOCGRAB` locks on file descriptor closure.
6. **Virtual Terminal**: Press `Ctrl + Alt + F3` to switch to a TTY console.

---

## 8. Directory Structure

```text
keyboard-lock/
├── Makefile                      # POSIX/Linux Makefile
├── CMakeLists.txt                # CMake build configuration
├── src/
│   ├── main.c                   # CLI entrypoint & argument parsing
│   ├── daemon.c                 # Epoll multiplexer & signal handling
│   ├── daemon.h
│   ├── state_machine.c          # Deterministic UNLOCKED <-> LOCKED state machine
│   ├── state_machine.h
│   ├── backend.h                # Backend abstraction interface
│   ├── backend_evdev.c          # Kernel EVIOCGRAB backend
│   ├── backend_evdev.h
│   ├── backend_x11.c            # Dynamic X11 XGrabKeyboard backend
│   ├── backend_x11.h
│   ├── device_manager.c         # Keyboard discovery, filtering & inotify
│   ├── device_manager.h
│   ├── ipc.c                    # UNIX domain socket single-instance protocol
│   ├── ipc.h
│   ├── logger.c                 # Privacy-first system logger (zero keylogging)
│   ├── logger.h
│   ├── hotkey.c                 # Keycode mapping & parsing
│   └── hotkey.h
├── systemd/
│   └── keyboard-lock.service    # systemd user service unit
├── udev/
│   └── 99-keyboard-lock.rules   # udev rules for non-root execution
├── tests/
│   ├── test_state_machine.c     # State machine & modifier isolation tests
│   ├── test_device_filter.c     # Device classification & mouse isolation tests
│   ├── test_ipc.c               # Socket IPC unit tests
│   ├── test_hotkey.c            # Hotkey lookup & parser tests
│   ├── test_mock_evdev.c        # End-to-end event stream simulation
│   └── run_tests.sh             # Master test suite runner
├── docs/
│   ├── ARCHITECTURE.md          # Technical architecture & event flow
│   ├── X11_WAYLAND_COMPAT.md    # X11 vs Wayland security analysis
│   └── EMERGENCY_RECOVERY.md    # Fail-safe & recovery guide
├── scripts/
│   ├── setup_permissions.sh     # Permissions setup script
│   └── simulate_typing.sh       # Verification script
└── README.md                    # This document
```
