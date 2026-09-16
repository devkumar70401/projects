# Enforced Deterministic Pomodoro System for Ubuntu Linux

> **Core Guarantee**: A mathematically deterministic, hardware-enforced Pomodoro application for Ubuntu Linux (Wayland & X11). During the 25-minute work phase, the workstation operates normally. When the 25 minutes expire, a mandatory 5-minute break begins. During those 5 minutes, ALL physical keyboard and pointing-device inputs (laptop keyboard, touchpad, external USB/Bluetooth keyboards and mice) are blocked at the Linux kernel `evdev` layer via `EVIOCGRAB`. The display rendering, video playback, audio, network, and background computing continue executing uninterrupted across all monitors. After exactly 300 seconds (5 minutes), input is automatically restored.

---

## ⚡ Quick Start & Desktop Floating Indicator

### 1. Launching Pomodoro (Background Execution)
Run from any terminal:
```bash
pomodoro
```
* **Runs in Background**: Immediately forks and detaches from your terminal so you can continue using the terminal for coding and other tasks.
* **Corner Floating Indicator**: Starts in the **top-right corner** of your primary monitor.
* **Movable & Draggable**: The floating indicator window has standard window decorations and stays on top (`AlwaysOnTop`). You can click and drag it to any corner or position on your desktop as you like.
* **Continuous Looping**: Automatically executes the infinite cycle:
  $$\text{25m Work (Input Normal)} \longrightarrow \text{5m Break (Input Blocked)} \longrightarrow \text{25m Work (Input Normal)} \dots$$
* **Multi-Monitor Break Artwork**: During the 5-minute break, fullscreen overlay windows appear across all connected monitors rendering the local artwork and quote:
  > **Take a deep breath, You still have a long way !**

---

### 2. Stopping / Cutting It Anytime
You can stop or cancel the session anytime using either method:
* **From Terminal**:
  ```bash
  pomodoro stop
  ```
* **From Desktop Window**: Click the `[X]` (close) button on the floating indicator window during the work phase to cleanly exit both the UI and background daemon.

---

### 3. CLI Command Reference

| Command | Action |
| :--- | :--- |
| **`pomodoro`** or **`pomodoro start`** | Starts the 25m/5m looping Pomodoro with the floating corner indicator in the background |
| **`pomodoro stop`** | Cleanly stops the Pomodoro session, closes the UI, and releases all input locks |
| **`pomodoro status`** | Displays the real-time monotonic countdown, current state, and grabbed devices |
| **`pomodoro break`** | Manually triggers the 5-minute enforced break early |
| **`pomodoro work`** | Manually restarts the 25-minute work phase |
| **`pomodoro devices`** | Lists all detected keyboard, mouse, and touchpad input devices |

---

## 1. System Architecture

The system uses a strictly decoupled, two-tier architecture:

```text
 ┌──────────────────────────────────────────────────────────────────┐
 │ User Session Controller (`pomodoro-ui`)                          │
 │ - Runs as unprivileged user (`dev`)                              │
 │ - Multi-monitor synchronized overlay windows (Wayland / X11)     │
 │ - Movable floating corner indicator during work phase            │
 │ - Crisp typography & motivational artwork rendering             │
 │ - Live monotonic countdown sync via local IPC                    │
 └────────────────────────────────┬─────────────────────────────────┘
                                  │
                       Unix Domain Socket IPC
                   (/run/pomodoro-input-guard.sock)
                                  │
                                  ▼
 ┌──────────────────────────────────────────────────────────────────┐
 │ Privileged Input Guard Daemon (`pomodoro-guard`)                 │
 │ - Runs with input/system privileges (managed by systemd)         │
 │ - Direct Linux `evdev` discovery & capability bitmask parsing    │
 │ - Atomic `EVIOCGRAB` hardware event interception                 │
 │ - Dynamic `/dev/input` inotify hotplug monitoring                │
 │ - Authoritative `CLOCK_BOOTTIME` monotonic deadline enforcement  │
 │ - Automatic input release on deadline / signal / crash           │
 └──────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Matrix

| Crate | Role | Description |
| :--- | :--- | :--- |
| **`pomodoro-core`** | Domain & State Model | Pure finite state machine, monotonic clock abstraction, input capability classification algorithms, IPC schemas, and `proptest` property suites. |
| **`pomodoro-guard`**| Privileged Input Guard | Authoritative daemon managing `/dev/input/event*` file descriptors, exclusive grabs, hotplug inotify watches, and IPC server. |
| **`pomodoro-ui`**   | User Session Overlay | Multi-display borderless presentation & floating indicator built with `winit`, `softbuffer`, and `tiny-skia`, displaying motivational asset and synchronized timer. |
| **`pomodoro-cli`**  | Control & Diagnostics | Command-line utility for status inspection, manual phase control, device listing, and out-of-band emergency recovery. |

---

## 3. Mathematical Timekeeping & Clock Model

To ensure immunity against NTP synchronization adjustments, wall-clock modifications, timezone changes, and system sleep/suspend cycles:

* **Clock Source**: Linux `CLOCK_BOOTTIME` (via `libc::clock_gettime`), strictly monotonic and inclusive of suspend durations.
* **Work Period**: $W = 25 \times 60 = 1500 \text{ seconds}$
* **Break Period**: $B = 5 \times 60 = 300 \text{ seconds}$
* **Deadlines**:
  $$\text{deadline} = M(t_{\text{start}}) + D$$
* **Remaining Time**:
  $$R(t) = \max(0, \text{deadline} - M(t))$$
* **Scheduling**: Wakeups and transitions are calculated directly against the monotonic deadline without accumulating loop-delay errors.

---

## 4. Input Interception & Device Classification

The guard daemon probes every `/dev/input/event*` node and queries its capability bitmasks using `ioctl`:

1. **Classification Matrix**:
   * **Keyboard**: Verified by presence of alphanumeric character matrices ($Q..P$, $A..L$, $Z..M$, Enter, Space).
   * **Touchpad**: Verified by `INPUT_PROP_BUTTONPAD`, `BTN_TOOL_FINGER`, `BTN_TOUCH`, and multi-touch absolute axes (`ABS_MT_POSITION_X/Y`).
   * **Pointing Stick / TrackPoint**: Verified by `INPUT_PROP_POINTING_STICK`.
   * **Mouse**: Verified by relative axes (`REL_X`, `REL_Y`) and buttons (`BTN_LEFT`, `BTN_RIGHT`).
   * **Non-Target Pass-Through**: Lid switch, power/sleep button, audio jacks, HDMI CEC, and video bus devices are explicitly ignored.

2. **Kernel Grab Mechanism**:
   * On Break: Daemon opens target nodes and invokes `ioctl(fd, EVIOCGRAB, 1)`. The Linux kernel routes physical hardware events solely to the guard process and suppresses them from `libinput`, Mutter, and desktop applications.
   * On Deadline: Daemon invokes `ioctl(fd, EVIOCGRAB, 0)` and closes file descriptors. Normal input dispatch resumes immediately.
   * Crash Safety: If the daemon terminates or panics, the kernel automatically closes all open file descriptors, releasing all grabs instantly.

3. **Dynamic Hotplug**:
   * An inotify watch on `/dev/input` monitors `IN_CREATE` and `IN_ATTRIB`. Any keyboard or mouse plugged in during an active break is classified and locked immediately.

---

## 5. Multi-Monitor Presentation & Visual Asset

During the break phase:
* Synchronized borderless overlay windows cover all active displays (e.g. HDMI-1 2560x1440 and eDP-1 1920x1200).
* Displays the local motivational asset (`assets/break_motivation.png`) accompanied by the required text:
  > **Take a deep breath, You still have a long way !**
* Displays the live, synchronized countdown clock ($05:00 \to 00:00$).

---

## 6. Build & Test Verification

```bash
# Run unit and property-based tests
cargo test --all-targets

# Check strict linter rules
cargo clippy --all-targets --all-features -- -D warnings

# Check code formatting
cargo fmt --check

# Compile production release binaries
cargo build --release
```

---

## 7. Systemd Service Deployment

1. Install binaries into system path:
   ```bash
   sudo cp target/release/pomodoro-guard /usr/local/bin/
   sudo cp target/release/pomodoro-cli /usr/local/bin/
   sudo cp target/release/pomodoro-ui /usr/local/bin/
   ```

2. Enable and start privileged input guard:
   ```bash
   sudo cp systemd/pomodoro-input-guard.service /etc/systemd/system/
   sudo systemctl daemon-reload
   sudo systemctl enable --now pomodoro-input-guard.service
   ```

3. Enable user session controller:
   ```bash
   mkdir -p ~/.config/systemd/user/
   cp systemd/pomodoro-session.service ~/.config/systemd/user/
   systemctl --user daemon-reload
   systemctl --user enable --now pomodoro-session.service
   ```

4. Out-of-band emergency unlock (via SSH or administrative terminal):
   ```bash
   pomodoro-cli emergency-unlock
   # or
   sudo systemctl stop pomodoro-input-guard
   ```
