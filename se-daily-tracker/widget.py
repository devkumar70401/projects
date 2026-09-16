"""
SE Daily Tracker - Sleek Floating Desktop Widget (HUD)
Designed for Dual-Monitor Setups and Always-In-Front Productivity
Path: /home/dev/SE/Projects/se-daily-tracker/widget.py
"""

import os
import sys
import re
import json
import subprocess
from datetime import datetime, time, timedelta
from pathlib import Path
import tkinter as tk
from tkinter import messagebox

# Add project dir to path
SCRIPT_DIR = Path(__file__).parent.resolve()
sys.path.insert(0, str(SCRIPT_DIR))

from tracker import TaskTracker, DAILY_DIR, DATA_DIR
import audio

WIDGET_CONFIG_FILE = DATA_DIR / "widget_config.json"

DEFAULT_WIDGET_CONFIG = {
    "x": None,
    "y": None,
    "width": 380,
    "height": 560,
    "is_pinned": True,
    "is_mini": False,
    "active_tab": "all",  # "all", "work", "food"
    "monitor_name": None,
    "opacity": 0.96
}


def get_monitors():
    """Detect all connected monitors, their resolutions, and screen offsets."""
    monitors = []
    try:
        out = subprocess.check_output(["xrandr", "--listmonitors"], stderr=subprocess.DEVNULL).decode("utf-8")
        for line in out.strip().splitlines()[1:]:
            parts = line.split()
            if len(parts) >= 4:
                name = parts[-1]
                geom = parts[2]
                m = re.search(r"(\d+)/\d+x(\d+)/\d+\+(\d+)\+(\d+)", geom)
                if m:
                    w, h, x, y = map(int, m.groups())
                    is_primary = '*' in parts[1]
                    monitors.append({
                        "name": name,
                        "width": w,
                        "height": h,
                        "x": x,
                        "y": y,
                        "is_primary": is_primary
                    })
    except Exception:
        pass

    if not monitors:
        monitors.append({
            "name": "Primary",
            "width": 1920,
            "height": 1080,
            "x": 0,
            "y": 0,
            "is_primary": True
        })
    # Sort monitors by x-coordinate (left to right)
    monitors.sort(key=lambda m: (m["x"], m["y"]))
    return monitors


class DailyTrackerWidget:
    def __init__(self, root):
        self.root = root
        self.tracker = TaskTracker()
        self.monitors = get_monitors()
        self.config = self._load_widget_config()

        self.active_tab = self.config.get("active_tab", "all")
        self.is_pinned = self.config.get("is_pinned", True)
        self.is_mini = self.config.get("is_mini", False)
        self.show_add_drawer = False
        self.last_tasks_mtime = 0

        # Drag tracking
        self._drag_start_x = 0
        self._drag_start_y = 0

        # Build UI & apply geometry
        self._init_window()
        self._build_ui()
        self._refresh_all()
        self._start_ticker()

    # -------------------------------------------------------------------------
    # Configuration & Persistence
    # -------------------------------------------------------------------------
    def _load_widget_config(self):
        DATA_DIR.mkdir(parents=True, exist_ok=True)
        if WIDGET_CONFIG_FILE.exists():
            try:
                with open(WIDGET_CONFIG_FILE, "r", encoding="utf-8") as f:
                    return {**DEFAULT_WIDGET_CONFIG, **json.load(f)}
            except Exception:
                pass
        return DEFAULT_WIDGET_CONFIG.copy()

    def _save_widget_config(self):
        try:
            self.config["width"] = self.root.winfo_width()
            curr_h = self.root.winfo_height()
            if not self.is_mini and curr_h >= 250:
                self.config["height"] = curr_h
            self.config["x"] = self.root.winfo_x()
            self.config["y"] = self.root.winfo_y()
            self.config["is_pinned"] = self.is_pinned
            self.config["is_mini"] = self.is_mini
            self.config["active_tab"] = self.active_tab

            curr_mon = self._get_current_monitor()
            if curr_mon:
                self.config["monitor_name"] = curr_mon["name"]

            with open(WIDGET_CONFIG_FILE, "w", encoding="utf-8") as f:
                json.dump(self.config, f, indent=2)
        except Exception:
            pass

    # -------------------------------------------------------------------------
    # Window Configuration & Multi-Monitor Positioning
    # -------------------------------------------------------------------------
    def _init_window(self):
        self.root.title("SE Checkpoints Widget")
        self.root.configure(bg="#181825")
        
        # Tool window attribute for lightweight appearance
        try:
            self.root.wm_attributes('-type', 'utility')
        except Exception:
            pass

        # Apply Pin (Always on top)
        self.root.wm_attributes('-topmost', 1 if self.is_pinned else 0)

        # Determine initial geometry
        w = self.config.get("width", 380)
        saved_h = self.config.get("height", 560)
        if saved_h < 250:
            saved_h = 560
        h = 90 if self.is_mini else saved_h
        
        saved_x = self.config.get("x")
        saved_y = self.config.get("y")

        # Validate saved position falls within at least one monitor's range
        pos_valid = False
        if saved_x is not None and saved_y is not None:
            for mon in self.monitors:
                if (mon["x"] - 50 <= saved_x <= mon["x"] + mon["width"] + 50 and
                    mon["y"] - 50 <= saved_y <= mon["y"] + mon["height"] + 50):
                    pos_valid = True
                    break

        if not pos_valid:
            # Default to top-right corner of preferred monitor or primary monitor
            target_mon = None
            pref_name = self.config.get("monitor_name")
            if pref_name:
                for m in self.monitors:
                    if m["name"] == pref_name:
                        target_mon = m
                        break
            if not target_mon:
                # Default to the first monitor (external screen HDMI-1 if on left, or primary)
                target_mon = self.monitors[0]

            saved_x = target_mon["x"] + target_mon["width"] - w - 24
            saved_y = target_mon["y"] + 40

        self.root.geometry(f"{w}x{h}+{saved_x}+{saved_y}")
        self.root.minsize(320, 85)
        self.root.protocol("WM_DELETE_WINDOW", self._on_close)

    def _get_current_monitor(self):
        """Returns the monitor dictionary where the center of the widget currently lies."""
        try:
            wx = self.root.winfo_x() + (self.root.winfo_width() // 2)
            wy = self.root.winfo_y() + 30
            for mon in self.monitors:
                if (mon["x"] <= wx < mon["x"] + mon["width"] and
                    mon["y"] <= wy < mon["y"] + mon["height"]):
                    return mon
        except Exception:
            pass
        return self.monitors[0] if self.monitors else None

    def _switch_monitor(self):
        """Toggle/cycle widget to the other monitor on a dual-monitor setup."""
        if len(self.monitors) < 2:
            messagebox.showinfo("Monitor Switch", "Only 1 display monitor detected.")
            return

        curr_mon = self._get_current_monitor()
        # Find next monitor in sorted list
        next_idx = 0
        for i, m in enumerate(self.monitors):
            if m["name"] == curr_mon["name"]:
                next_idx = (i + 1) % len(self.monitors)
                break

        target_mon = self.monitors[next_idx]
        w = self.root.winfo_width()
        h = self.root.winfo_height()

        # Place at top-right corner of target monitor
        new_x = target_mon["x"] + target_mon["width"] - w - 24
        new_y = target_mon["y"] + 40

        self.root.geometry(f"{w}x{h}+{new_x}+{new_y}")
        self._update_monitor_btn_text()
        self._save_widget_config()

    def _update_monitor_btn_text(self):
        curr_mon = self._get_current_monitor()
        name = curr_mon["name"] if curr_mon else "M1"
        self.btn_mon.config(text=f"🖥️ {name}")

    # -------------------------------------------------------------------------
    # Drag Handling
    # -------------------------------------------------------------------------
    def _on_drag_start(self, event):
        self._drag_start_x = event.x
        self._drag_start_y = event.y

    def _on_drag_motion(self, event):
        deltax = event.x - self._drag_start_x
        deltay = event.y - self._drag_start_y
        new_x = self.root.winfo_x() + deltax
        new_y = self.root.winfo_y() + deltay
        self.root.geometry(f"+{new_x}+{new_y}")

    def _on_drag_release(self, event):
        self._update_monitor_btn_text()
        self._save_widget_config()

    # -------------------------------------------------------------------------
    # UI Layout Construction
    # -------------------------------------------------------------------------
    def _build_ui(self):
        # 1. Custom Draggable Title Bar / Header
        self.header = tk.Frame(self.root, bg="#11111b", height=36, padx=8, pady=4)
        self.header.pack(fill=tk.X, side=tk.TOP)
        self.header.pack_propagate(False)

        # Bind drag events to header and its static children
        for w in [self.header]:
            w.bind("<ButtonPress-1>", self._on_drag_start)
            w.bind("<B1-Motion>", self._on_drag_motion)
            w.bind("<ButtonRelease-1>", self._on_drag_release)

        # Title & Clock
        self.lbl_title = tk.Label(
            self.header,
            text="⚡ Daily Checkpoints",
            font=("Ubuntu", 10, "bold"),
            bg="#11111b",
            fg="#89b4fa"
        )
        self.lbl_title.pack(side=tk.LEFT)
        self.lbl_title.bind("<ButtonPress-1>", self._on_drag_start)
        self.lbl_title.bind("<B1-Motion>", self._on_drag_motion)
        self.lbl_title.bind("<ButtonRelease-1>", self._on_drag_release)

        self.lbl_clock = tk.Label(
            self.header,
            text="",
            font=("DejaVu Sans Mono", 9, "bold"),
            bg="#11111b",
            fg="#cdd6f4"
        )
        self.lbl_clock.pack(side=tk.LEFT, padx=8)
        self.lbl_clock.bind("<ButtonPress-1>", self._on_drag_start)
        self.lbl_clock.bind("<B1-Motion>", self._on_drag_motion)
        self.lbl_clock.bind("<ButtonRelease-1>", self._on_drag_release)

        # Action Buttons on Right side of Header
        # Close Button
        btn_close = tk.Button(
            self.header,
            text="✕",
            font=("Ubuntu", 9, "bold"),
            bg="#11111b",
            fg="#f38ba8",
            activebackground="#f38ba8",
            activeforeground="#11111b",
            bd=0,
            padx=5,
            command=self._on_close,
            cursor="hand2"
        )
        btn_close.pack(side=tk.RIGHT, padx=1)

        # Full App Button
        btn_full_app = tk.Button(
            self.header,
            text="🗖",
            font=("Ubuntu", 9),
            bg="#11111b",
            fg="#cdd6f4",
            activebackground="#313244",
            activeforeground="#ffffff",
            bd=0,
            padx=4,
            command=self._open_full_gui,
            cursor="hand2"
        )
        btn_full_app.pack(side=tk.RIGHT, padx=1)

        # Mini/Expand Toggle Button
        self.btn_mini = tk.Button(
            self.header,
            text="🔽" if self.is_mini else "🔼",
            font=("Ubuntu", 9),
            bg="#11111b",
            fg="#cdd6f4",
            activebackground="#313244",
            activeforeground="#ffffff",
            bd=0,
            padx=4,
            command=self._toggle_mini_mode,
            cursor="hand2"
        )
        self.btn_mini.pack(side=tk.RIGHT, padx=1)

        # Pin (Always-on-top) Toggle Button
        self.btn_pin = tk.Button(
            self.header,
            text="📌" if self.is_pinned else "📍",
            font=("Ubuntu", 9),
            bg="#313244" if self.is_pinned else "#11111b",
            fg="#a6e3a1" if self.is_pinned else "#6c7086",
            activebackground="#45475a",
            bd=0,
            padx=5,
            command=self._toggle_pin,
            cursor="hand2"
        )
        self.btn_pin.pack(side=tk.RIGHT, padx=1)

        # Monitor Switch Button
        curr_mon = self._get_current_monitor()
        curr_name = curr_mon["name"] if curr_mon else "M1"
        self.btn_mon = tk.Button(
            self.header,
            text=f"🖥️ {curr_name}",
            font=("Ubuntu", 8, "bold"),
            bg="#1e1e2e",
            fg="#89b4fa",
            activebackground="#313244",
            bd=0,
            padx=6,
            command=self._switch_monitor,
            cursor="hand2"
        )
        self.btn_mon.pack(side=tk.RIGHT, padx=4)

        # 2. Audio Alarm Banner (Hidden by default)
        self.alarm_banner = tk.Frame(self.root, bg="#d20f39", padx=8, pady=4)
        self.lbl_alarm_banner = tk.Label(
            self.alarm_banner,
            text="🔔 INTERVAL ALARM RINGING!",
            font=("Ubuntu", 9, "bold"),
            bg="#d20f39",
            fg="white"
        )
        self.lbl_alarm_banner.pack(side=tk.LEFT, fill=tk.X, expand=True)

        self.btn_stop_alarm = tk.Button(
            self.alarm_banner,
            text="🔕 STOP SOUND",
            font=("Ubuntu", 8, "bold"),
            bg="white",
            fg="#d20f39",
            bd=0,
            padx=6,
            pady=1,
            command=self._stop_alarm,
            cursor="hand2"
        )
        self.btn_stop_alarm.pack(side=tk.RIGHT)

        # 3. Active Interval Card (Always visible, even in Mini Mode)
        self.active_card = tk.Frame(self.root, bg="#1e1e2e", padx=10, pady=8, highlightbackground="#313244", highlightthickness=1)
        self.active_card.pack(fill=tk.X, padx=8, pady=6)

        self.active_card_top = tk.Frame(self.active_card, bg="#1e1e2e")
        self.active_card_top.pack(fill=tk.X)

        self.lbl_active_badge = tk.Label(
            self.active_card_top,
            text="● ACTIVE NOW",
            font=("Ubuntu", 8, "bold"),
            bg="#1b3b2b",
            fg="#00e676",
            padx=6,
            pady=1
        )
        self.lbl_active_badge.pack(side=tk.LEFT)

        self.lbl_active_countdown = tk.Label(
            self.active_card_top,
            text="",
            font=("DejaVu Sans Mono", 8, "bold"),
            bg="#1e1e2e",
            fg="#f9e2af"
        )
        self.lbl_active_countdown.pack(side=tk.RIGHT)

        self.active_card_body = tk.Frame(self.active_card, bg="#1e1e2e")
        self.active_card_body.pack(fill=tk.X, pady=(4, 0))

        self.var_active_done = tk.BooleanVar(value=False)
        self.chk_active_done = tk.Checkbutton(
            self.active_card_body,
            variable=self.var_active_done,
            bg="#1e1e2e",
            fg="#cdd6f4",
            selectcolor="#11111b",
            activebackground="#1e1e2e",
            bd=0,
            highlightthickness=0,
            command=self._on_active_card_check
        )
        self.chk_active_done.pack(side=tk.LEFT, padx=(0, 4))

        self.lbl_active_title = tk.Label(
            self.active_card_body,
            text="No active interval right now",
            font=("Ubuntu", 10, "bold"),
            bg="#1e1e2e",
            fg="#cdd6f4",
            anchor="w",
            justify="left"
        )
        self.lbl_active_title.pack(side=tk.LEFT, fill=tk.X, expand=True)

        # 4. Expandable Full List Body (Hidden in Mini Mode)
        self.body_container = tk.Frame(self.root, bg="#181825")
        if not self.is_mini:
            self.body_container.pack(fill=tk.BOTH, expand=True, padx=8, pady=(0, 6))

        # Filter Tabs & Quick Add row
        tab_row = tk.Frame(self.body_container, bg="#181825")
        tab_row.pack(fill=tk.X, pady=(0, 6))

        self.btn_tab_all = tk.Button(
            tab_row,
            text="📋 All",
            font=("Ubuntu", 9, "bold" if self.active_tab == "all" else "normal"),
            bg="#313244" if self.active_tab == "all" else "#181825",
            fg="#cdd6f4",
            bd=0,
            padx=8,
            pady=3,
            command=lambda: self._set_tab("all"),
            cursor="hand2"
        )
        self.btn_tab_all.pack(side=tk.LEFT, padx=(0, 2))

        self.btn_tab_work = tk.Button(
            tab_row,
            text="💼 Work",
            font=("Ubuntu", 9, "bold" if self.active_tab == "work" else "normal"),
            bg="#313244" if self.active_tab == "work" else "#181825",
            fg="#89b4fa",
            bd=0,
            padx=8,
            pady=3,
            command=lambda: self._set_tab("work"),
            cursor="hand2"
        )
        self.btn_tab_work.pack(side=tk.LEFT, padx=2)

        self.btn_tab_food = tk.Button(
            tab_row,
            text="🥗 Food",
            font=("Ubuntu", 9, "bold" if self.active_tab == "food" else "normal"),
            bg="#313244" if self.active_tab == "food" else "#181825",
            fg="#a6e3a1",
            bd=0,
            padx=8,
            pady=3,
            command=lambda: self._set_tab("food"),
            cursor="hand2"
        )
        self.btn_tab_food.pack(side=tk.LEFT, padx=2)

        self.btn_quick_add = tk.Button(
            tab_row,
            text="➕ Add",
            font=("Ubuntu", 8, "bold"),
            bg="#2e7d32",
            fg="white",
            activebackground="#388e3c",
            bd=0,
            padx=8,
            pady=3,
            command=self._toggle_quick_add,
            cursor="hand2"
        )
        self.btn_quick_add.pack(side=tk.RIGHT)

        # 5. Quick Add Drawer (Collapsible)
        self.add_drawer = tk.Frame(self.body_container, bg="#1e1e2e", padx=8, pady=8, highlightbackground="#45475a", highlightthickness=1)

        row_cat = tk.Frame(self.add_drawer, bg="#1e1e2e")
        row_cat.pack(fill=tk.X, pady=(0, 4))
        tk.Label(row_cat, text="Type:", bg="#1e1e2e", fg="#a6adc8", font=("Ubuntu", 8)).pack(side=tk.LEFT)
        self.var_add_cat = tk.StringVar(value="work")
        tk.Radiobutton(row_cat, text="💼 Work", variable=self.var_add_cat, value="work", bg="#1e1e2e", fg="#89b4fa", selectcolor="#11111b", font=("Ubuntu", 8)).pack(side=tk.LEFT, padx=6)
        tk.Radiobutton(row_cat, text="🥗 Food", variable=self.var_add_cat, value="food", bg="#1e1e2e", fg="#a6e3a1", selectcolor="#11111b", font=("Ubuntu", 8)).pack(side=tk.LEFT)

        row_times = tk.Frame(self.add_drawer, bg="#1e1e2e")
        row_times.pack(fill=tk.X, pady=(0, 4))
        tk.Label(row_times, text="Time:", bg="#1e1e2e", fg="#a6adc8", font=("Ubuntu", 8)).pack(side=tk.LEFT)
        self.ent_add_start = tk.Entry(row_times, width=6, bg="#181825", fg="white", insertbackground="white", font=("DejaVu Sans Mono", 8), bd=1)
        self.ent_add_start.pack(side=tk.LEFT, padx=4)
        tk.Label(row_times, text="to", bg="#1e1e2e", fg="#a6adc8", font=("Ubuntu", 8)).pack(side=tk.LEFT)
        self.ent_add_end = tk.Entry(row_times, width=6, bg="#181825", fg="white", insertbackground="white", font=("DejaVu Sans Mono", 8), bd=1)
        self.ent_add_end.pack(side=tk.LEFT, padx=4)

        row_task = tk.Frame(self.add_drawer, bg="#1e1e2e")
        row_task.pack(fill=tk.X, pady=(0, 6))
        self.ent_add_desc = tk.Entry(row_task, bg="#181825", fg="white", insertbackground="white", font=("Ubuntu", 9), bd=1)
        self.ent_add_desc.pack(fill=tk.X)
        self.ent_add_desc.bind("<Return>", lambda e: self._submit_quick_add())

        row_btns = tk.Frame(self.add_drawer, bg="#1e1e2e")
        row_btns.pack(fill=tk.X)
        tk.Button(row_btns, text="Add Checkpoint", bg="#1b5e20", fg="white", font=("Ubuntu", 8, "bold"), bd=0, padx=8, pady=2, command=self._submit_quick_add, cursor="hand2").pack(side=tk.RIGHT, padx=(4, 0))
        tk.Button(row_btns, text="Cancel", bg="#313244", fg="#cdd6f4", font=("Ubuntu", 8), bd=0, padx=6, pady=2, command=self._toggle_quick_add, cursor="hand2").pack(side=tk.RIGHT)

        # 6. Scrollable Checklist Container
        list_card = tk.Frame(self.body_container, bg="#1e1e2e", highlightbackground="#313244", highlightthickness=1)
        list_card.pack(fill=tk.BOTH, expand=True)

        self.canvas = tk.Canvas(list_card, bg="#181825", highlightthickness=0)
        self.scrollbar = tk.Scrollbar(list_card, orient="vertical", command=self.canvas.yview)
        self.list_frame = tk.Frame(self.canvas, bg="#181825")

        self.list_frame.bind(
            "<Configure>",
            lambda e: self.canvas.configure(scrollregion=self.canvas.bbox("all"))
        )
        self.canvas_window = self.canvas.create_window((0, 0), window=self.list_frame, anchor="nw")
        self.canvas.configure(yscrollcommand=self.scrollbar.set)

        # Keep frame width matched to canvas width
        self.canvas.bind(
            "<Configure>",
            lambda e: self.canvas.itemconfig(self.canvas_window, width=e.width)
        )

        self.canvas.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self.scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        # Mousewheel scroll support
        self.canvas.bind("<Button-4>", lambda e: self.canvas.yview_scroll(-1, "units"))
        self.canvas.bind("<Button-5>", lambda e: self.canvas.yview_scroll(1, "units"))
        self.list_frame.bind("<Button-4>", lambda e: self.canvas.yview_scroll(-1, "units"))
        self.list_frame.bind("<Button-5>", lambda e: self.canvas.yview_scroll(1, "units"))

    # -------------------------------------------------------------------------
    # Mode Toggles & Actions
    # -------------------------------------------------------------------------
    def _toggle_pin(self):
        self.is_pinned = not self.is_pinned
        self.root.wm_attributes('-topmost', 1 if self.is_pinned else 0)
        self.btn_pin.config(
            text="📌" if self.is_pinned else "📍",
            bg="#313244" if self.is_pinned else "#11111b",
            fg="#a6e3a1" if self.is_pinned else "#6c7086"
        )
        self._save_widget_config()

    def _toggle_mini_mode(self):
        self.is_mini = not self.is_mini
        w = self.root.winfo_width()
        if self.is_mini:
            self.btn_mini.config(text="🔽")
            self.body_container.pack_forget()
            self.root.geometry(f"{w}x90")
        else:
            self.btn_mini.config(text="🔼")
            self.body_container.pack(fill=tk.BOTH, expand=True, padx=8, pady=(0, 6))
            h = self.config.get("height", 560)
            if h < 250:
                h = 560
            self.root.geometry(f"{w}x{h}")
            self._refresh_list()
        self._save_widget_config()

    def _set_tab(self, tab):
        self.active_tab = tab
        for t, btn in [("all", self.btn_tab_all), ("work", self.btn_tab_work), ("food", self.btn_tab_food)]:
            if t == tab:
                btn.config(bg="#313244", font=("Ubuntu", 9, "bold"))
            else:
                btn.config(bg="#181825", font=("Ubuntu", 9, "normal"))
        self._refresh_list()
        self._save_widget_config()

    def _toggle_quick_add(self):
        self.show_add_drawer = not self.show_add_drawer
        if self.show_add_drawer:
            now = datetime.now()
            start_str = now.strftime("%H:%M")
            end_time = (now + timedelta(hours=1)).strftime("%H:%M")
            self.ent_add_start.delete(0, tk.END)
            self.ent_add_start.insert(0, start_str)
            self.ent_add_end.delete(0, tk.END)
            self.ent_add_end.insert(0, end_time)
            self.ent_add_desc.delete(0, tk.END)
            self.add_drawer.pack(fill=tk.X, before=self.canvas.master, pady=(0, 6))
            self.ent_add_desc.focus_set()
        else:
            self.add_drawer.pack_forget()

    def _submit_quick_add(self):
        cat = self.var_add_cat.get()
        t_start = self.ent_add_start.get().strip()
        t_end = self.ent_add_end.get().strip()
        desc = self.ent_add_desc.get().strip()

        if not desc:
            messagebox.showwarning("Missing Description", "Please enter a task description.")
            return

        ok, msg = self.tracker.add_task(cat, t_start, t_end, desc, force=True)
        if ok:
            self._toggle_quick_add()
            self._refresh_all()
        else:
            messagebox.showerror("Error Adding Checkpoint", msg)

    def _open_full_gui(self):
        """Launches or switches to the main GUI."""
        script_path = SCRIPT_DIR / "cli.py"
        venv_py = sys.executable
        subprocess.Popen([venv_py, str(script_path), "gui"], start_new_session=True)

    def _stop_alarm(self):
        audio.stop_sound()
        if self.alarm_banner.winfo_ismapped():
            self.alarm_banner.pack_forget()

    def _on_close(self):
        self._save_widget_config()
        self.root.destroy()

    # -------------------------------------------------------------------------
    # Real-Time Data Refresh & Ticker
    # -------------------------------------------------------------------------
    def _start_ticker(self):
        def tick():
            now = datetime.now()
            self.lbl_clock.config(text=now.strftime("%H:%M:%S"))

            # 1. Check alarm sound status
            if audio.is_sound_playing():
                if not self.alarm_banner.winfo_ismapped():
                    self.alarm_banner.pack(fill=tk.X, after=self.header)
            else:
                if self.alarm_banner.winfo_ismapped():
                    self.alarm_banner.pack_forget()

            # 2. Check for newly triggered interval alarms
            triggered = audio.check_and_trigger_alarms(self.tracker)
            if triggered:
                item = triggered[0]
                cat = "🥗 FOOD" if item["category"] == "food" else "💼 WORK"
                self.lbl_alarm_banner.config(text=f"🔔 {cat}: {item['task']}")
                if not self.alarm_banner.winfo_ismapped():
                    self.alarm_banner.pack(fill=tk.X, after=self.header)
                # Ensure topmost is respected
                try:
                    self.root.lift()
                except Exception:
                    pass

            # 3. Check if tasks file changed externally
            tasks_file = DATA_DIR / "tasks.json"
            if tasks_file.exists():
                mtime = tasks_file.stat().st_mtime
                if mtime > self.last_tasks_mtime:
                    self.last_tasks_mtime = mtime
                    self._refresh_all()
                else:
                    self._update_active_card()
            else:
                self._update_active_card()

            self.root.after(1000, tick)

        tick()

    def _refresh_all(self):
        self.tracker.reload()
        self._update_active_card()
        self._refresh_list()
        self._update_tab_badges()

    def _update_tab_badges(self):
        day_data = self.tracker.get_day_data()
        work_done, work_total = self.tracker.get_category_stats(day_data, "work")
        food_done, food_total = self.tracker.get_category_stats(day_data, "food")
        total_done = work_done + food_done
        total = work_total + food_total

        self.btn_tab_all.config(text=f"📋 All ({total_done}/{total})")
        self.btn_tab_work.config(text=f"💼 Work ({work_done}/{work_total})")
        self.btn_tab_food.config(text=f"🥗 Food ({food_done}/{food_total})")

    # -------------------------------------------------------------------------
    # Active Card Rendering
    # -------------------------------------------------------------------------
    def _update_active_card(self):
        now = datetime.now()
        now_time = now.time()
        day_data = self.tracker.get_day_data()

        all_items = []
        for idx, itm in enumerate(day_data.get("work", [])):
            all_items.append({**itm, "category": "work", "index": idx})
        for idx, itm in enumerate(day_data.get("food", [])):
            all_items.append({**itm, "category": "food", "index": idx})

        active_task = None
        next_task = None
        min_upcoming_diff = timedelta(days=1)

        for item in all_items:
            in_win, status, _ = self.tracker.check_time_window(item["time_start"], item["time_end"], now_time)
            if in_win and not item.get("completed", False):
                active_task = item
                break
            elif status == "upcoming" and not item.get("completed", False):
                try:
                    p = [int(x) for x in item["time_start"].split(":")]
                    t_start_dt = now.replace(hour=p[0], minute=p[1], second=0)
                    if t_start_dt > now:
                        diff = t_start_dt - now
                        if diff < min_upcoming_diff:
                            min_upcoming_diff = diff
                            next_task = (item, diff)
                except Exception:
                    pass

        self.current_active_item = active_task

        if active_task:
            cat_icon = "💼" if active_task["category"] == "work" else "🥗"
            self.lbl_active_badge.config(
                text=f"● ACTIVE NOW ({cat_icon})",
                bg="#1b3b2b",
                fg="#00e676"
            )
            # Calculate remaining minutes
            try:
                p = [int(x) for x in active_task["time_end"].split(":")]
                end_dt = now.replace(hour=p[0], minute=p[1], second=0)
                rem = max(0, int((end_dt - now).total_seconds() // 60))
                self.lbl_active_countdown.config(text=f"⏳ {rem}m left ({active_task['time_start']}-{active_task['time_end']})")
            except Exception:
                self.lbl_active_countdown.config(text=f"{active_task['time_start']}-{active_task['time_end']}")

            self.lbl_active_title.config(
                text=active_task["task"],
                font=("Ubuntu", 10, "bold"),
                fg="#ffffff"
            )
            self.chk_active_done.config(state=tk.NORMAL)
            self.var_active_done.set(False)
            self.active_card.config(highlightbackground="#00e676")
        elif next_task:
            item, diff = next_task
            mins = int(diff.total_seconds() // 60)
            cat_icon = "💼" if item["category"] == "work" else "🥗"
            self.lbl_active_badge.config(
                text=f"○ NEXT UP ({cat_icon})",
                bg="#3d2c18",
                fg="#f9e2af"
            )
            self.lbl_active_countdown.config(text=f"in {mins}m @ {item['time_start']}")
            self.lbl_active_title.config(
                text=item["task"],
                font=("Ubuntu", 9, "italic"),
                fg="#a6adc8"
            )
            self.chk_active_done.config(state=tk.DISABLED)
            self.var_active_done.set(False)
            self.active_card.config(highlightbackground="#313244")
        else:
            self.lbl_active_badge.config(
                text="✓ ALL CHECKED",
                bg="#1e1e2e",
                fg="#a6e3a1"
            )
            self.lbl_active_countdown.config(text="")
            self.lbl_active_title.config(
                text="All checkpoints done! Great work! 🎉",
                font=("Ubuntu", 9, "italic"),
                fg="#a6adc8"
            )
            self.chk_active_done.config(state=tk.DISABLED)
            self.var_active_done.set(False)
            self.active_card.config(highlightbackground="#313244")

    def _on_active_card_check(self):
        if not hasattr(self, "current_active_item") or not self.current_active_item:
            return
        item = self.current_active_item
        cat = item["category"]
        idx = item["index"]
        ok, msg = self.tracker.toggle_task(cat, idx)
        self._refresh_all()

    # -------------------------------------------------------------------------
    # Checklist Rendering
    # -------------------------------------------------------------------------
    def _refresh_list(self):
        for widget in self.list_frame.winfo_children():
            widget.destroy()

        day_data = self.tracker.get_day_data()
        now_time = datetime.now().time()

        items_to_show = []
        if self.active_tab in ("all", "work"):
            for idx, itm in enumerate(day_data.get("work", [])):
                items_to_show.append({**itm, "category": "work", "index": idx})
        if self.active_tab in ("all", "food"):
            for idx, itm in enumerate(day_data.get("food", [])):
                items_to_show.append({**itm, "category": "food", "index": idx})

        if not items_to_show:
            lbl_empty = tk.Label(
                self.list_frame,
                text="No checkpoints scheduled.\nClick '+ Add' above to create one!",
                font=("Ubuntu", 9, "italic"),
                bg="#181825",
                fg="#6c7086",
                pady=20
            )
            lbl_empty.pack(fill=tk.X)
            return

        for item in items_to_show:
            self._render_item_card(item, now_time)

    def _render_item_card(self, item, now_time):
        is_done = item.get("completed", False)
        t_start = item.get("time_start", "00:00")
        t_end = item.get("time_end", "00:00")
        cat = item.get("category", "work")
        idx = item.get("index", 0)
        task_desc = item.get("task", "")

        in_win, win_status, _ = self.tracker.check_time_window(t_start, t_end, now_time)

        # Card container
        card_bg = "#24273a" if in_win and not is_done else "#1e1e2e"
        border_col = "#00e676" if in_win and not is_done else "#313244"
        card = tk.Frame(self.list_frame, bg=card_bg, padx=6, pady=6, highlightbackground=border_col, highlightthickness=1)
        card.pack(fill=tk.X, pady=2, padx=2)

        # Checkbox
        chk_var = tk.BooleanVar(value=is_done)
        cb = tk.Checkbutton(
            card,
            variable=chk_var,
            bg=card_bg,
            fg="#cdd6f4",
            selectcolor="#11111b",
            activebackground=card_bg,
            bd=0,
            highlightthickness=0,
            command=lambda c=cat, i=idx, v=chk_var: self._on_item_check(c, i, v),
            cursor="hand2"
        )
        cb.pack(side=tk.LEFT, padx=(0, 4))

        # Time tag
        time_fg = "#89b4fa" if cat == "work" else "#a6e3a1"
        lbl_time = tk.Label(
            card,
            text=f"{t_start}-{t_end}",
            font=("DejaVu Sans Mono", 8, "bold"),
            bg="#181825",
            fg=time_fg,
            padx=4,
            pady=1
        )
        lbl_time.pack(side=tk.LEFT, padx=(0, 6))

        # Task description
        task_fg = "#6c7086" if is_done else ("#ffffff" if in_win else "#cdd6f4")
        task_font = ("Ubuntu", 9, "overstrike") if is_done else ("Ubuntu", 9)
        lbl_task = tk.Label(
            card,
            text=task_desc,
            font=task_font,
            fg=task_fg,
            bg=card_bg,
            anchor="w",
            justify="left"
        )
        lbl_task.pack(side=tk.LEFT, fill=tk.X, expand=True)

        # Status badge
        if is_done:
            b_text = "✓ DONE"
            b_bg, b_fg = "#1e3a29", "#a6e3a1"
        elif in_win:
            b_text = "● ACTIVE"
            b_bg, b_fg = "#1b3b2b", "#00e676"
        elif win_status == "upcoming":
            b_text = "○ UPCOMING"
            b_bg, b_fg = "#3d2c18", "#f9e2af"
        else:
            b_text = "✗ EXPIRED"
            b_bg, b_fg = "#3a1e24", "#f38ba8"

        lbl_badge = tk.Label(
            card,
            text=b_text,
            font=("Ubuntu", 7, "bold"),
            bg=b_bg,
            fg=b_fg,
            padx=5,
            pady=1
        )
        lbl_badge.pack(side=tk.RIGHT, padx=2)

    def _on_item_check(self, category, index, chk_var):
        day_data = self.tracker.get_day_data()
        items = day_data.get(category, [])
        if index >= len(items):
            return

        item = items[index]
        already_done = item.get("completed", False)

        if already_done:
            self.tracker.toggle_task(category, index)
            self._refresh_all()
            return

        in_window, status, reason = self.tracker.check_time_window(item["time_start"], item["time_end"])

        if in_window:
            ok, msg = self.tracker.toggle_task(category, index)
            self._refresh_all()
        elif status == "upcoming":
            chk_var.set(False)
            messagebox.showwarning(
                "Cannot Complete Yet",
                f"⏳ Task: '{item['task']}'\n\nScheduled window: {item['time_start']} - {item['time_end']}.\n\nTasks can only be completed between their scheduled timings!"
            )
        else:
            chk_var.set(False)
            confirm = messagebox.askyesno(
                "Time Window Expired",
                f"⚠️ Task: '{item['task']}' was scheduled for {item['time_start']} - {item['time_end']}.\n\nThe scheduled timing has passed.\nDo you want to override and mark it as completed anyway?"
            )
            if confirm:
                self.tracker.force_toggle_task(category, index)
                self._refresh_all()


def run_widget():
    # Auto detect display
    if "DISPLAY" not in os.environ:
        os.environ["DISPLAY"] = ":0"

    root = tk.Tk()
    app = DailyTrackerWidget(root)
    root.mainloop()


if __name__ == "__main__":
    run_widget()
