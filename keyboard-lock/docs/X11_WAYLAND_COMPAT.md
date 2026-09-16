# X11 & Wayland Compatibility and Security Model Analysis

## 1. Executive Summary

| Environment | Supported Backend | Status | Interception Guarantee |
| :--- | :--- | :--- | :--- |
| **Wayland (GNOME / Mutter)** | `evdev` (Kernel EVIOCGRAB) | **SUPPORTED** | 100% Kernel-level exclusive suppression |
| **Wayland (KDE Plasma / KWin)** | `evdev` (Kernel EVIOCGRAB) | **SUPPORTED** | 100% Kernel-level exclusive suppression |
| **Wayland (Sway / Hyprland / wlroots)** | `evdev` (Kernel EVIOCGRAB) | **SUPPORTED** | 100% Kernel-level exclusive suppression |
| **X11 (Xorg Session)** | `evdev` OR `x11` (XGrabKeyboard) | **SUPPORTED** | 100% Suppression via kernel or X11 server |
| **Linux Virtual Console (TTY)** | `evdev` (Kernel EVIOCGRAB) | **SUPPORTED** | 100% Suppression at console level |

---

## 2. Why Wayland Restricts Application-Level Global Hooks

Under the Wayland protocol architecture:
1. **Surface Isolation**: Client windows are intentionally isolated from each other. An ordinary unprivileged Wayland client cannot see, snoop, intercept, or block keyboard events intended for another Wayland client surface.
2. **No Global XGrabKeyboard Equivalent in Wayland Protocol**: Wayland deliberately avoids global keyboard grabbing protocols for standard clients to prevent malicious keyloggers or UI freezes.
3. **Xwayland Boundary**: In a Wayland session, Xwayland only handles legacy X11 applications. If an application calls `XGrabKeyboard` inside Xwayland, only X11 clients are affected; native Wayland applications (such as GNOME Terminal, modern Firefox, Wayland text editors, GNOME Shell) receive input directly from the Wayland compositor via `libinput`, bypassing Xwayland completely.

---

## 3. The Authoritative Solution: Kernel `evdev` EVIOCGRAB

Because of Wayland's design, attempting to suppress keyboard input via GUI overlay tricks or Xwayland hooks will **leak keystrokes** into native Wayland applications.

`keyboard-lock` solves this by operating at the **Linux kernel `evdev` layer (`/dev/input/event*`)**:

1. Both Wayland compositors (Mutter, KWin, wlroots) and Xorg servers obtain physical input by opening `/dev/input/event*` devices with `libinput`.
2. When `keyboard-lock` applies `ioctl(fd, EVIOCGRAB, 1)` on keyboard event nodes:
   - The Linux kernel routes all physical keystrokes exclusively to `keyboard-lock`.
   - `libinput` (and therefore the Wayland compositor) receives **zero keyboard events**.
   - No Wayland native app, Xwayland app, or desktop shell receives any keystrokes.
3. **Mouse Isolation**:
   - Mice, touchpads, and trackballs reside on separate event nodes (`event5`, `event7`, etc.).
   - `keyboard-lock` explicitly filters out pointer devices and leaves their `/dev/input/event*` nodes ungrabbed.
   - `libinput` continues reading mouse events normally, allowing full cursor movement, clicking, dragging, scrolling, and window interactions.

---

## 4. Permissions & Privilege Requirements

Because `/dev/input/event*` character devices are protected system nodes (typically owned by `root:input` with permissions `0660`):

* To run `keyboard-lock` without `sudo`, the user must be added to the `input` group:
  ```bash
  sudo usermod -aG input $USER
  ```
  *(requires re-login or `newgrp input`)*
* Alternatively, install the provided udev rule (`udev/99-keyboard-lock.rules`):
  ```bash
  sudo cp udev/99-keyboard-lock.rules /etc/udev/rules.d/
  sudo udevadm control --reload-rules && sudo udevadm trigger
  ```

---

## 5. Pure X11 Session (`x11` Backend)

If running in a pure X11 session (`XDG_SESSION_TYPE=x11`), `keyboard-lock` also supports the `x11` backend (`keyboard-lock --backend x11`):
* Calls `XGrabKeyboard()` on the root window.
* Captures all X11 keyboard events session-wide.
* Does not require group permissions on `/dev/input`.
* Leaves pointer ungrabbed (`XGrabPointer` is not called).
