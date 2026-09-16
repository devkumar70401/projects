# Emergency Recovery Procedures

`keyboard-lock` is designed with fail-safe engineering. However, in the rare event that input interception behaves unexpectedly, use any of the independent recovery procedures below.

---

## Recovery Method 1: The Toggle Hotkey

Press the standard unlock hotkey:

```text
Ctrl + Alt + K
```

This immediately signals the state machine to release all kernel grabs and restores normal typing.

---

## Recovery Method 2: CLI Command from Terminal or Script

If you have an open terminal, SSH session, or automated script:

```bash
keyboard-lock --unlock
```

or

```bash
keyboard-lock --stop
```

This sends an IPC message to the running daemon, which cleanly releases all grabs and restores input.

---

## Recovery Method 3: Mouse GUI Action / Desktop Shortcut

Since **mouse input remains 100% operational** while the keyboard is locked:

1. Click on your application launcher / desktop shortcut to run `keyboard-lock --unlock`.
2. Or use your mouse to open a terminal / script and click a release button.

---

## Recovery Method 4: Terminate the Daemon Process (`pkill`)

From another terminal, terminal window with existing text, or SSH:

```bash
pkill keyboard-lock
```

* When `keyboard-lock` receives `SIGTERM` or `SIGINT`, it executes its cleanup routine and calls `EVIOCGRAB 0` on all keyboards.

---

## Recovery Method 5: Kernel Automatic Release (`kill -9`)

Even if the process is forcibly killed with `SIGKILL`:

```bash
killall -9 keyboard-lock
```

**Linux Kernel Invariant**: The Linux input core automatically releases all `EVIOCGRAB` locks on file descriptor closure when a process dies. A dead process CANNOT hold an evdev grab.

---

## Recovery Method 6: Linux Virtual Terminal (TTY Switch)

If kernel sysrq or virtual terminal switching is supported:

```text
Ctrl + Alt + F3
```

Switch to TTY3, log in, and run:

```bash
pkill keyboard-lock
```

Then switch back to your desktop session with `Ctrl + Alt + F2` (or `F1`).
