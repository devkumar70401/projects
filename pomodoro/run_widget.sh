#!/bin/bash
# Launcher for Pomodoro Desktop Widget
# Uses X11 backend (XWayland) for absolute pixel positioning and desktop layer hints

export DISPLAY="${DISPLAY:-:0}"
export GDK_BACKEND=x11
unset WAYLAND_DISPLAY
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}"
export DBUS_SESSION_BUS_ADDRESS="${DBUS_SESSION_BUS_ADDRESS:-unix:path=/run/user/$(id -u)/bus}"

# Clean Snap / VSCode inherited variables
unset GTK_PATH
unset LOCPATH
unset LD_PRELOAD

cd /home/dev/SE/Projects/pomodoro
exec /usr/bin/python3 /home/dev/SE/Projects/pomodoro/pomodoro_widget.py "$@"
