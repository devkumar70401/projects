# Keyboard-Lock Architecture Documentation

## 1. System Overview

`keyboard-lock` is a purpose-built Linux system utility designed to temporarily suppress **only keyboard input** while leaving all other desktop and system functions (mouse, touchpads, monitors, GPU/display rendering, audio, networking, running applications, and background services) completely operational and uninterrupted.

```text
                               ┌─────────────────────────┐
                               │       User Inputs       │
                               └────────────┬────────────┘
                                            │
                    ┌───────────────────────┴───────────────────────┐
                    │                                               │
             Keyboard Devices                                 Mouse & Touchpads
             (/dev/input/event3, etc.)                       (/dev/input/event5, etc.)
                    │                                               │
                    ▼                                               ▼
         ┌─────────────────────┐                          ┌──────────────────┐
         │    keyboard-lock    │                          │  libinput / OS   │
         │  (EVIOCGRAB Engine) │                          │  Display Server  │
         └──────────┬──────────┘                          └────────┬─────────┘
                    │                                              │
         ┌──────────┴──────────┐                                   │
         │ LOCKED vs UNLOCKED? │                                   │
         └──────────┬──────────┘                                   │
                    │                                              │
       ┌────────────┴────────────┐                                 │
       ▼                         ▼                                 ▼
   [UNLOCKED]                [LOCKED]                      [ALWAYS NORMAL]
   Passes to OS            Drops all keys               Mouse clicks, scrolls,
                          (except Ctrl+Alt+K)           drags, window moves
```

---

## 2. Low-Level Input Interception Semantics

### Kernel `evdev` Engine (`EVIOCGRAB`)
Under Linux, the kernel input subsystem routes hardware events from drivers to character device nodes in `/dev/input/event*`. 

When `keyboard-lock` engages its lock state:
1. It issues the `ioctl(fd, EVIOCGRAB, 1)` syscall on all detected keyboard file descriptors.
2. The Linux kernel grants **exclusive access** to `keyboard-lock`.
3. The kernel immediately halts event forwarding from those keyboard devices to all other user-space processes (including `libinput`, `mutter`, `kwin`, `weston`, `Xorg`, and `Xwayland`).
4. `keyboard-lock` reads every `struct input_event` from the grabbed descriptors.
5. If the event matches the unlock hotkey (`Ctrl + Alt + K`), `keyboard-lock` transitions to `UNLOCKED` and calls `ioctl(fd, EVIOCGRAB, 0)`.
6. Any other key event (letters, numbers, Enter, Esc, Ctrl+C, Alt+Tab, Super) is **discarded immediately**.
7. Because mouse and touchpad devices reside on distinct `/dev/input/event*` nodes (e.g. `event5`, `event7`), they are **never grabbed**. Their events continue to flow directly to `libinput` and the active window manager without even a microsecond of latency.

---

## 3. State Machine & Modifier Isolation

The internal state machine (`src/state_machine.c`) guarantees deterministic, race-free state transitions:

```text
                        Ctrl + Alt + K (Press)
          UNLOCKED ────────────────────────────────► LOCKED
             ▲                                         │
             │                                         │
             └─────────────────────────────────────────┘
                        Ctrl + Alt + K (Press)
```

### Auto-Repeat & Debounce
When a physical key is held down, the Linux input subsystem generates auto-repeat events (`value == 2`). 
* The state machine only transitions on the initial keydown edge (`value == 1`).
* Repeat events (`value == 2`) are ignored for state transitions. This prevents rapid toggling flip-flops if the user holds `Ctrl + Alt + K` for more than 200ms.
* A flag `hotkey_consumed` ensures that another state transition requires a physical release (`value == 0`) of key `K`.

### Modifier Isolation
The hotkey requires **strictly Ctrl and Alt**.
* If `Shift` is also held (`Ctrl + Alt + Shift + K`), the hotkey does not match.
* If `Super/Meta` is held (`Ctrl + Alt + Super + K`), the hotkey does not match.
* If only `Ctrl + K` or `Alt + K` is pressed, the hotkey does not match.

---

## 4. Multi-Monitor Independence

The keyboard is a session-wide and system-wide input peripheral, independent of screen resolutions, monitor counts, or display topology.

* `keyboard-lock` does NOT create fullscreen transparent overlay windows that steal focus.
* `keyboard-lock` operates directly at the device and session layer.
* Whether the workstation has 1 monitor, 2 monitors, 4 monitors, or hotplugs an external display via HDMI/DisplayPort, keyboard suppression remains 100% active and uniform.

---

## 5. Single-Instance & IPC Protocol

To prevent multiple controllers from competing for keyboard grabs, `keyboard-lock` implements a single-instance architecture over a UNIX domain socket (`$XDG_RUNTIME_DIR/kblock.sock` or `/tmp/kblock-$UID.sock`).

* When launched, `keyboard-lock` checks if an active daemon is responding.
* If active, CLI commands (`--status`, `--lock`, `--unlock`, `--toggle`, `--stop`) send a lightweight message over the socket:

| Command | Server Action | Response |
| :--- | :--- | :--- |
| `LOCK` | Applies grabs, sets state to `LOCKED` | `OK LOCKED` |
| `UNLOCK` | Releases grabs, sets state to `UNLOCKED` | `OK UNLOCKED` |
| `TOGGLE` | Inverts current lock state | `OK <STATE>` |
| `STATUS` | Queries lock state, active keyboards, uptime | `STATUS state=... devices=...` |
| `STOP` | Cleanly ungrabs all keyboards and exits daemon | `OK STOPPING` |
| `VERSION` | Returns version string | `keyboard-lock v1.0.0 (Ubuntu Linux)` |

---

## 6. Fault Tolerance & Safety Guarantees

1. **Kernel Automatic Release**: In Linux, when a process holding an `EVIOCGRAB` exits or is terminated (even by `SIGKILL` / `kill -9`), the kernel automatically closes open file descriptors and revokes all active grabs (`input_release_device`). A crash can NEVER permanently freeze the keyboard.
2. **Synchronous Signal Handling (`signalfd`)**: `SIGINT`, `SIGTERM`, `SIGHUP`, `SIGQUIT` are caught cleanly in the `epoll` multiplexer, triggering graceful ungrab and resource release.
3. **Emergency Crash Handlers (`SIGSEGV`, `SIGABRT`)**: Low-level signal handlers immediately invoke `EVIOCGRAB 0` before aborting.
4. **Hotplug Support (`inotify`)**: Keyboards plugged in while locked are immediately grabbed; keyboards disconnected while locked are removed from epoll without crashing.
