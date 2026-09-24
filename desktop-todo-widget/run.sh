#!/bin/bash
# Launcher for Daily Tasks Desktop Widget
# Uses X11 backend (XWayland) to enable absolute pixel positioning and true desktop layer hints

export DISPLAY="${DISPLAY:-:0}"
export GDK_BACKEND=x11
unset WAYLAND_DISPLAY
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/$(id -u)}"
export DBUS_SESSION_BUS_ADDRESS="${DBUS_SESSION_BUS_ADDRESS:-unix:path=/run/user/$(id -u)/bus}"

# Clean Snap / VSCode inherited variables
unset GTK_PATH
unset LOCPATH
unset LD_PRELOAD

# Wait for X11 display / graphical session to become ready (up to 30 seconds on boot)
for i in {1..30}; do
    if /usr/bin/python3 -c "import gi; gi.require_version('Gtk', '3.0'); from gi.repository import Gtk; exit(0 if Gtk.init_check()[0] else 1)" 2>/dev/null; then
        break
    fi
    sleep 1
done

cd /home/dev/SE/Devendra/widgets/work
exec /usr/bin/python3 /home/dev/SE/Devendra/widgets/work/daily_widget.py "$@"
