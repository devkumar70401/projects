#!/usr/bin/env python3
"""
Terminal Desktop Widget
Modern, transparent, interactive desktop terminal widget built with VTE and GTK3.
Location: /home/dev/SE/Projects/desktop-todo-widget/terminal_widget.py
"""

import sys
import os
import time
import signal
import json
import subprocess
import fcntl
from pathlib import Path

_lock_file = None
def _acquire_single_instance_lock():
    global _lock_file
    runtime_dir = os.environ.get("XDG_RUNTIME_DIR")
    if runtime_dir and os.path.isdir(runtime_dir):
        lock_path = Path(runtime_dir) / "terminal_widget.lock"
    else:
        lock_path = Path.home() / ".config" / "terminal-widget" / "terminal_widget.lock"
        lock_path.parent.mkdir(parents=True, exist_ok=True)
    try:
        _lock_file = open(lock_path, "a+")
        fcntl.flock(_lock_file, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except (IOError, BlockingIOError):
        print("Another instance of terminal_widget is already running. Exiting.")
        sys.exit(0)
    except Exception as e:
        print(f"Warning: Could not acquire lock at {lock_path}: {e}")

# Ensure GTK 3, VTE 2.91 & Pango are loaded
import gi
gi.require_version('Gtk', '3.0')
gi.require_version('Gdk', '3.0')
gi.require_version('Vte', '2.91')
gi.require_version('Pango', '1.0')
from gi.repository import Gtk, Gdk, GLib, Vte, Pango

SCRIPT_DIR = Path(__file__).parent.resolve()
DEFAULT_CWD = Path.home() / "SE"
CONFIG_DIR = Path.home() / ".config" / "terminal-widget"
CONFIG_FILE = CONFIG_DIR / "config.json"

DEFAULT_CONFIG = {
    "x": 3882,
    "y": 1034,
    "width": 580,
    "height": 380,
    "always_on_top": False
}


class TerminalWidget:
    def __init__(self):
        self.config = self._load_config()
        self.target_x = self.config.get("x", 3882)
        self.target_y = self.config.get("y", 1034)
        self._initial_positioned = False
        self._is_pinned_top = self.config.get("always_on_top", False)
        self.child_pid = None
        self._save_timer_id = None
        self._last_spawn_time = 0.0
        self._is_restarting = False

        self._init_window()
        self._load_css()
        self._build_ui()
        self._spawn_shell()

    def _load_config(self) -> dict:
        CONFIG_DIR.mkdir(parents=True, exist_ok=True)
        if CONFIG_FILE.exists():
            try:
                with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                    cfg = json.load(f)
                    merged = DEFAULT_CONFIG.copy()
                    merged.update(cfg)
                    return merged
            except Exception:
                pass
        return DEFAULT_CONFIG.copy()

    def _save_config(self):
        CONFIG_DIR.mkdir(parents=True, exist_ok=True)
        try:
            with open(CONFIG_FILE, "w", encoding="utf-8") as f:
                json.dump(self.config, f, indent=2)
        except Exception as e:
            print(f"Error saving terminal widget config: {e}")

    def _init_window(self):
        self.window = Gtk.Window(type=Gtk.WindowType.TOPLEVEL)
        self.window.set_title("Terminal Widget")
        self.window.set_decorated(False)
        self.window.set_skip_taskbar_hint(True)
        self.window.set_skip_pager_hint(True)
        self.window.set_app_paintable(True)
        self.window.set_type_hint(Gdk.WindowTypeHint.UTILITY)
        self.window.stick()

        self._apply_layer()

        screen = self.window.get_screen()
        visual = screen.get_rgba_visual()
        if visual:
            self.window.set_visual(visual)

        self.window.set_default_size(self.config.get("width", 580), self.config.get("height", 380))
        self.window.move(self.target_x, self.target_y)

        self.window.connect("map-event", self._on_map)
        self.window.connect("configure-event", self._on_configure)
        self.window.connect("destroy", self._on_destroy)

    def _on_map(self, widget, event):
        self.window.move(self.target_x, self.target_y)
        GLib.timeout_add(150, self._apply_initial_position)
        return False

    def _apply_initial_position(self):
        self.window.move(self.target_x, self.target_y)
        self._apply_layer()
        self._initial_positioned = True
        return False

    def _apply_layer(self):
        if self._is_pinned_top:
            self.window.set_keep_above(True)
            self.window.set_keep_below(False)
        else:
            self.window.set_keep_above(False)
            self.window.set_keep_below(True)

    def _load_css(self):
        css = b"""
        window {
            background-color: transparent;
        }
        .term-card {
            background-color: rgba(18, 20, 26, 0.88);
            border: 1px solid rgba(255, 255, 255, 0.13);
            border-radius: 20px;
            box-shadow: 0 16px 40px rgba(0, 0, 0, 0.6);
            padding: 12px 14px;
        }
        .term-header {
            border-bottom: 1px solid rgba(255, 255, 255, 0.08);
            padding-bottom: 6px;
            margin-bottom: 6px;
        }
        .term-title {
            font-size: 12px;
            font-weight: bold;
            color: #d1d5db;
        }
        .badge-shell {
            background-color: rgba(14, 165, 233, 0.22);
            color: #7dd3fc;
            border-radius: 10px;
            padding: 2px 8px;
            font-size: 10px;
            font-weight: bold;
            border: 1px solid rgba(14, 165, 233, 0.4);
        }
        .btn-icon {
            background: transparent;
            border: none;
            color: #9ca3af;
            padding: 2px 5px;
            border-radius: 6px;
            font-size: 12px;
        }
        .btn-icon:hover {
            background-color: rgba(255, 255, 255, 0.1);
            color: #ffffff;
        }
        .term-container {
            border-radius: 12px;
            background-color: rgba(10, 12, 16, 0.75);
            padding: 6px;
        }
        """
        provider = Gtk.CssProvider()
        provider.load_from_data(css)
        screen = Gdk.Screen.get_default()
        Gtk.StyleContext.add_provider_for_screen(
            screen, provider, Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION
        )

    def _build_ui(self):
        self.card = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=0)
        self.card.get_style_context().add_class("term-card")

        # --- HEADER (Draggable Area) ---
        self.header_event_box = Gtk.EventBox()
        self.header_event_box.add_events(Gdk.EventMask.BUTTON_PRESS_MASK)
        self.header_event_box.connect("button-press-event", self._on_header_press)

        header_box = Gtk.Box(orientation=Gtk.Orientation.HORIZONTAL, spacing=6)
        header_box.get_style_context().add_class("term-header")

        lbl_icon = Gtk.Label(label="💻")
        header_box.pack_start(lbl_icon, False, False, 0)

        lbl_title = Gtk.Label(label="Terminal")
        lbl_title.get_style_context().add_class("term-title")
        header_box.pack_start(lbl_title, False, False, 0)

        # Shell badge
        lbl_badge = Gtk.Label(label="BASH")
        lbl_badge.get_style_context().add_class("badge-shell")
        header_box.pack_start(lbl_badge, True, True, 0)
        lbl_badge.set_xalign(1.0)

        # Clear / Restart button
        btn_restart = Gtk.Button(label="🔄")
        btn_restart.set_tooltip_text("Restart / Clear Shell")
        btn_restart.get_style_context().add_class("btn-icon")
        btn_restart.connect("clicked", self._on_restart_shell)
        header_box.pack_start(btn_restart, False, False, 0)

        # Open Directory Folder
        btn_folder = Gtk.Button(label="📂")
        btn_folder.set_tooltip_text("Open ~/SE in File Manager")
        btn_folder.get_style_context().add_class("btn-icon")
        btn_folder.connect("clicked", self._on_open_folder)
        header_box.pack_start(btn_folder, False, False, 0)

        # Pin Button
        self.btn_pin = Gtk.Button(label="📌" if self._is_pinned_top else "📍")
        self.btn_pin.set_tooltip_text("Toggle Always On Top vs Desktop Pinned")
        self.btn_pin.get_style_context().add_class("btn-icon")
        self.btn_pin.connect("clicked", self._on_toggle_pin)
        header_box.pack_start(self.btn_pin, False, False, 0)

        self.header_event_box.add(header_box)
        self.card.pack_start(self.header_event_box, False, False, 0)

        # --- VTE TERMINAL CONTAINER ---
        term_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=0)
        term_box.get_style_context().add_class("term-container")

        self.terminal = Vte.Terminal()
        self.terminal.set_cursor_blink_mode(Vte.CursorBlinkMode.ON)
        self.terminal.set_cursor_shape(Vte.CursorShape.IBEAM)
        self.terminal.set_scrollback_lines(2000)

        # Font
        font_desc = Pango.FontDescription("Monospace 10")
        self.terminal.set_font(font_desc)

        # Colors (Modern Obsidian Dark Theme)
        fg_color = Gdk.RGBA(0.90, 0.92, 0.95, 1.0)
        bg_color = Gdk.RGBA(0.06, 0.08, 0.11, 0.85)

        # 16-color ANSI Palette
        palette = [
            Gdk.RGBA(0.12, 0.14, 0.18, 1.0),  # Black
            Gdk.RGBA(0.94, 0.27, 0.27, 1.0),  # Red
            Gdk.RGBA(0.13, 0.77, 0.37, 1.0),  # Green
            Gdk.RGBA(0.96, 0.62, 0.04, 1.0),  # Yellow
            Gdk.RGBA(0.24, 0.51, 0.96, 1.0),  # Blue
            Gdk.RGBA(0.66, 0.33, 0.97, 1.0),  # Magenta
            Gdk.RGBA(0.06, 0.73, 0.85, 1.0),  # Cyan
            Gdk.RGBA(0.85, 0.87, 0.91, 1.0),  # White
            # Bright colors
            Gdk.RGBA(0.30, 0.33, 0.39, 1.0),
            Gdk.RGBA(0.97, 0.44, 0.44, 1.0),
            Gdk.RGBA(0.29, 0.87, 0.50, 1.0),
            Gdk.RGBA(0.98, 0.75, 0.18, 1.0),
            Gdk.RGBA(0.38, 0.65, 0.98, 1.0),
            Gdk.RGBA(0.75, 0.49, 0.98, 1.0),
            Gdk.RGBA(0.22, 0.83, 0.93, 1.0),
            Gdk.RGBA(1.00, 1.00, 1.00, 1.0)
        ]

        self.terminal.set_colors(fg_color, bg_color, palette)
        self.terminal.connect("child-exited", self._on_child_exited)
        self.terminal.connect("key-press-event", self._on_term_key_press)
        self.terminal.connect("button-press-event", self._on_term_button_press)

        term_box.pack_start(self.terminal, True, True, 0)
        self.card.pack_start(term_box, True, True, 0)

        self.window.add(self.card)

    def _spawn_shell(self):
        shell = os.environ.get("SHELL", "/bin/bash")
        cwd = str(DEFAULT_CWD) if DEFAULT_CWD.exists() else str(Path.home())
        self._last_spawn_time = time.time()

        def _on_spawned(terminal, pid, error, user_data=None):
            if error:
                print(f"Error spawning shell: {error}")
            else:
                self.child_pid = pid

        self.terminal.spawn_async(
            Vte.PtyFlags.DEFAULT,
            cwd,
            [shell],
            [],
            GLib.SpawnFlags.DEFAULT,
            None, None,
            -1,
            None,
            _on_spawned
        )

    def _kill_current_child(self):
        if self.child_pid:
            try:
                import signal
                os.kill(self.child_pid, signal.SIGTERM)
            except ProcessLookupError:
                self.child_pid = None
            except Exception as e:
                print(f"Error terminating child shell: {e}")
                self.child_pid = None

    def _on_child_exited(self, terminal, status):
        self.child_pid = None
        if self._is_restarting:
            self._is_restarting = False
            self.terminal.reset(True, True)
            self._spawn_shell()
            return
        # Auto-respawn if shell exits, but throttle if it exited too quickly
        elapsed = time.time() - self._last_spawn_time
        delay = 300 if elapsed > 1.0 else 1500
        GLib.timeout_add(delay, self._respawn)

    def _respawn(self):
        self._spawn_shell()
        return False

    def _on_restart_shell(self, button):
        if self._is_restarting:
            return
        self._is_restarting = True
        if self.child_pid:
            self._kill_current_child()
        else:
            self._is_restarting = False
            self.terminal.reset(True, True)
            self._spawn_shell()

    def _on_destroy(self, widget):
        self._kill_current_child()
        Gtk.main_quit()

    def _on_open_folder(self, button):
        try:
            cwd = str(DEFAULT_CWD) if DEFAULT_CWD.exists() else str(Path.home())
            subprocess.Popen(["xdg-open", cwd])
        except Exception as e:
            print(f"Error opening folder: {e}")

    def _on_header_press(self, widget, event):
        if event.button == 1:
            self.window.begin_move_drag(
                event.button, int(event.x_root), int(event.y_root), event.time
            )

    def _on_configure(self, widget, event):
        if not self._initial_positioned:
            return
        x, y = self.window.get_position()
        w, h = self.window.get_size()
        if x > -10000 and y > -10000:
            self.config["x"] = x
            self.config["y"] = y
            self.config["width"] = w
            self.config["height"] = h
            self.target_x = x
            self.target_y = y
            if self._save_timer_id:
                GLib.source_remove(self._save_timer_id)
            self._save_timer_id = GLib.timeout_add(500, self._debounced_save_config)

    def _debounced_save_config(self):
        self._save_config()
        self._save_timer_id = None
        return False

    def _on_toggle_pin(self, button):
        self._is_pinned_top = not self._is_pinned_top
        self.config["always_on_top"] = self._is_pinned_top
        self.btn_pin.set_label("📌" if self._is_pinned_top else "📍")
        self._apply_layer()
        self._save_config()

    def _on_term_key_press(self, widget, event):
        ctrl_shift = (Gdk.ModifierType.CONTROL_MASK | Gdk.ModifierType.SHIFT_MASK)
        state = event.state & ctrl_shift
        # Ctrl + Shift + V -> Paste
        if state == ctrl_shift and event.keyval in (Gdk.KEY_V, Gdk.KEY_v):
            self.terminal.paste_clipboard()
            return True
        # Ctrl + Shift + C -> Copy
        elif state == ctrl_shift and event.keyval in (Gdk.KEY_C, Gdk.KEY_c):
            if hasattr(self.terminal, "copy_clipboard_format"):
                self.terminal.copy_clipboard_format(Vte.Format.TEXT)
            else:
                self.terminal.copy_clipboard()
            return True
        # Shift + Insert -> Paste
        elif (event.state & Gdk.ModifierType.SHIFT_MASK) and event.keyval == Gdk.KEY_Insert:
            self.terminal.paste_clipboard()
            return True
        return False

    def _on_term_button_press(self, widget, event):
        if event.button == 3:  # Right click context menu
            menu = Gtk.Menu()
            item_copy = Gtk.MenuItem(label="Copy (Ctrl+Shift+C)")
            item_copy.connect("activate", lambda _: self.terminal.copy_clipboard_format(Vte.Format.TEXT) if hasattr(self.terminal, "copy_clipboard_format") else self.terminal.copy_clipboard())
            menu.append(item_copy)

            item_paste = Gtk.MenuItem(label="Paste (Ctrl+Shift+V)")
            item_paste.connect("activate", lambda _: self.terminal.paste_clipboard())
            menu.append(item_paste)

            menu.show_all()
            menu.popup_at_pointer(event)
            return True
        return False

    def run(self):
        self.window.show_all()
        Gtk.main()


if __name__ == "__main__":
    _acquire_single_instance_lock()
    app = TerminalWidget()
    app.run()
