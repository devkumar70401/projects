"""
SE Daily Tracker - Purely Functional Desktop GUI with Audio Alarms
Path: /home/dev/SE/Projects/se-daily-tracker/gui.py
"""

import os
import sys
import subprocess
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
from datetime import datetime, time
from pathlib import Path

# Add project dir to path
SCRIPT_DIR = Path(__file__).parent.resolve()
sys.path.insert(0, str(SCRIPT_DIR))

from tracker import TaskTracker, DAILY_DIR
import audio


class DailyTrackerGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("SE Daily Checkpoints Tracker")
        self.root.geometry("1100x720")
        self.root.minsize(850, 550)

        # Ensure window is brought to front
        try:
            self.root.lift()
            self.root.attributes('-topmost', True)
            self.root.after_idle(self.root.attributes, '-topmost', False)
            self.root.focus_force()
        except Exception:
            pass

        self.tracker = TaskTracker()
        self.is_edit_mode = False
        self.override_lock = tk.BooleanVar(value=False)
        self.current_ringing_task = None
        self.last_tasks_mtime = 0

        # Temporary edit rows storage
        self.edit_work_rows = []
        self.edit_food_rows = []

        # Catch-up any past days on launch
        self.tracker.sync_and_catchup()

        self._build_ui()
        self._refresh_data()
        self._start_clock()

    def _build_ui(self):
        # 1. Active Alarm Alert Banner (Hidden by default)
        self.alarm_banner = tk.Frame(self.root, bg="#d32f2f", padx=10, pady=6)
        self.lbl_alarm_banner = tk.Label(
            self.alarm_banner,
            text="🔔 ALARM RINGING: Interval started!",
            font=("DejaVu Sans", 11, "bold"),
            bg="#d32f2f",
            fg="white"
        )
        self.lbl_alarm_banner.pack(side=tk.LEFT, padx=10)

        btn_stop_alarm = tk.Button(
            self.alarm_banner,
            text="🔕 STOP SOUND / DISMISS",
            font=("DejaVu Sans", 10, "bold"),
            bg="white",
            fg="#d32f2f",
            relief="raised",
            command=self._stop_alarm
        )
        btn_stop_alarm.pack(side=tk.RIGHT, padx=10)

        # 2. Top Header Frame
        header_frame = ttk.Frame(self.root, padding="10 10 10 5")
        header_frame.pack(fill=tk.X)

        # Title & Date
        self.lbl_date = ttk.Label(
            header_frame,
            text="",
            font=("DejaVu Sans", 14, "bold")
        )
        self.lbl_date.pack(side=tk.LEFT)

        # Action Buttons on Right
        self.btn_edit_toggle = ttk.Button(
            header_frame,
            text="✏️ Edit Page / Schedule",
            command=self._toggle_edit_mode
        )
        self.btn_edit_toggle.pack(side=tk.RIGHT, padx=5)

        btn_sound_settings = ttk.Button(
            header_frame,
            text="🔔 Alarm Sounds",
            command=self._open_sound_settings
        )
        btn_sound_settings.pack(side=tk.RIGHT, padx=5)

        btn_floating_widget = ttk.Button(
            header_frame,
            text="🗗 Floating Widget",
            command=self._open_floating_widget
        )
        btn_floating_widget.pack(side=tk.RIGHT, padx=5)

        self.chk_override = ttk.Checkbutton(
            header_frame,
            text="Override 10 AM Lock",
            variable=self.override_lock,
            command=self._on_override_toggle
        )
        self.chk_override.pack(side=tk.RIGHT, padx=10)

        self.lbl_clock = ttk.Label(
            header_frame,
            text="",
            font=("DejaVu Sans Mono", 13, "bold"),
            foreground="#0055aa"
        )
        self.lbl_clock.pack(side=tk.RIGHT, padx=15)

        # 3. Status & Progress Banner
        banner_frame = ttk.Frame(self.root, padding="10 0 10 5")
        banner_frame.pack(fill=tk.X)

        self.lbl_edit_status = ttk.Label(
            banner_frame,
            text="",
            font=("DejaVu Sans", 10, "bold")
        )
        self.lbl_edit_status.pack(side=tk.LEFT)

        self.lbl_progress = ttk.Label(
            banner_frame,
            text="",
            font=("DejaVu Sans", 10)
        )
        self.lbl_progress.pack(side=tk.RIGHT)

        ttk.Separator(self.root, orient=tk.HORIZONTAL).pack(fill=tk.X, padx=10, pady=5)

        # 4. Main Container Frame (Holds View Mode or Edit Mode)
        self.main_container = ttk.Frame(self.root, padding="10 5 10 5")
        self.main_container.pack(fill=tk.BOTH, expand=True)

        # 5. Bottom Action Bar
        bottom_frame = ttk.Frame(self.root, padding="10 8 10 8")
        bottom_frame.pack(fill=tk.X, side=tk.BOTTOM)

        self.lbl_status_msg = ttk.Label(
            bottom_frame,
            text="Ready. Alarms will ring at the start of each work or food interval.",
            font=("DejaVu Sans", 9, "italic")
        )
        self.lbl_status_msg.pack(side=tk.LEFT, fill=tk.X, expand=True)

        btn_open_folder = ttk.Button(
            bottom_frame,
            text="📁 Open Daily Reports Folder",
            command=self._open_daily_folder
        )
        btn_open_folder.pack(side=tk.RIGHT, padx=4)

        btn_sync = ttk.Button(
            bottom_frame,
            text="🔄 Sync / Save Archive Now",
            command=self._sync_now
        )
        btn_sync.pack(side=tk.RIGHT, padx=4)

    def _start_clock(self):
        def update():
            now = datetime.now()
            today_str = self.tracker.get_today_str()
            self.lbl_date.config(text=f"📅 {now.strftime('%A, %b %d, %Y')}")
            self.lbl_clock.config(text=f"⏰ {now.strftime('%H:%M:%S')}")

            # 1. Update edit permission status
            allowed, reason = self.tracker.is_edit_allowed(today_str, force=self.override_lock.get())
            if allowed:
                self.lbl_edit_status.config(
                    text=f"🟢 EDITING: OPEN ({reason})",
                    foreground="#1b5e20"
                )
            else:
                self.lbl_edit_status.config(
                    text=f"🔒 EDITING: LOCKED ({reason})",
                    foreground="#b71c1c"
                )

            # 2. Check and trigger interval alarms
            triggered = audio.check_and_trigger_alarms(self.tracker)
            if triggered:
                item = triggered[0]
                self.current_ringing_task = item
                cat_label = "🥗 FOOD" if item["category"] == "food" else "💼 WORK"
                self.lbl_alarm_banner.config(
                    text=f"🔔 {cat_label} TIME: {item['task']} ({item['time_start']} - {item['time_end']})"
                )
                self.alarm_banner.pack(fill=tk.X, before=self.lbl_date.master)
                # Bring window to front
                try:
                    self.root.lift()
                    self.root.focus_force()
                except Exception:
                    pass

            # 3. Update alarm banner visibility based on whether sound is actively playing
            if audio.is_sound_playing():
                if not self.alarm_banner.winfo_ismapped():
                    self.alarm_banner.pack(fill=tk.X, before=self.lbl_date.master)
            else:
                if self.alarm_banner.winfo_ismapped() and not self.current_ringing_task:
                    self.alarm_banner.pack_forget()

            # 4. Check if tasks file changed externally (e.g. checked off in widget)
            tasks_file = DAILY_DIR / ".data" / "tasks.json"
            if tasks_file.exists() and not self.is_edit_mode:
                try:
                    mtime = tasks_file.stat().st_mtime
                    if self.last_tasks_mtime == 0:
                        self.last_tasks_mtime = mtime
                    elif mtime > self.last_tasks_mtime:
                        self.last_tasks_mtime = mtime
                        self.tracker.reload()
                        self._refresh_data()
                except Exception:
                    pass

            # Schedule next second
            self.root.after(1000, update)

        update()

    def _open_floating_widget(self):
        script_path = SCRIPT_DIR / "cli.py"
        venv_py = sys.executable
        subprocess.Popen([venv_py, str(script_path), "widget"], start_new_session=True)
        self.lbl_status_msg.config(text="🗗 Floating desktop widget launched! Use 📌 to pin on top or 🖥️ to switch monitors.")

    def _stop_alarm(self):
        audio.stop_sound()
        self.current_ringing_task = None
        if self.alarm_banner.winfo_ismapped():
            self.alarm_banner.pack_forget()
        self.lbl_status_msg.config(text="🔕 Alarm silenced.")

    def _refresh_data(self):
        if self.is_edit_mode:
            self._render_edit_mode()
        else:
            self._render_view_mode()

    # -------------------------------------------------------------------------
    # VIEW / CHECKLIST MODE
    # -------------------------------------------------------------------------
    def _render_view_mode(self):
        for widget in self.main_container.winfo_children():
            widget.destroy()

        today_str = self.tracker.get_today_str()
        day_data = self.tracker.get_day_data(today_str)
        now_time = datetime.now().time()

        work_items = day_data.get("work", [])
        food_items = day_data.get("food", [])

        work_done = sum(1 for i in work_items if i.get("completed", False))
        food_done = sum(1 for i in food_items if i.get("completed", False))
        total_items = len(work_items) + len(food_items)
        total_done = work_done + food_done
        pct = (total_done / total_items * 100) if total_items > 0 else 0

        self.lbl_progress.config(
            text=f"Overall Progress: {total_done}/{total_items} ({pct:.0f}%)  |  Work: {work_done}/{len(work_items)}  |  Food: {food_done}/{len(food_items)}"
        )

        paned = ttk.PanedWindow(self.main_container, orient=tk.HORIZONTAL)
        paned.pack(fill=tk.BOTH, expand=True)

        work_frame = ttk.LabelFrame(
            paned,
            text=f" 💼 WORK CHECKPOINTS ({work_done}/{len(work_items)} Completed) ",
            padding="10"
        )
        paned.add(work_frame, weight=1)

        food_frame = ttk.LabelFrame(
            paned,
            text=f" 🥗 FOOD & NUTRITION ({food_done}/{len(food_items)} Completed) ",
            padding="10"
        )
        paned.add(food_frame, weight=1)

        self._populate_column_view(work_frame, "work", work_items, now_time)
        self._populate_column_view(food_frame, "food", food_items, now_time)

    def _populate_column_view(self, parent_frame, category, items, now_time):
        canvas = tk.Canvas(parent_frame, highlightthickness=0)
        scrollbar = ttk.Scrollbar(parent_frame, orient="vertical", command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)

        scrollable_frame.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )
        canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)

        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")

        if not items:
            msg = "No work checkpoints added yet.\nClick '✏️ Edit Page / Schedule' to add today's tasks." if category == "work" else "No food checkpoints scheduled."
            lbl_empty = ttk.Label(
                scrollable_frame,
                text=msg,
                font=("DejaVu Sans", 10, "italic"),
                padding="20"
            )
            lbl_empty.pack(anchor="w")
            return

        for idx, item in enumerate(items):
            is_done = item.get("completed", False)
            t_start = item.get("time_start", "00:00")
            t_end = item.get("time_end", "00:00")
            task_desc = item.get("task", "")

            in_window, win_status, _ = self.tracker.check_time_window(t_start, t_end, now_time)

            card = ttk.Frame(scrollable_frame, padding="6", relief="groove")
            card.pack(fill=tk.X, expand=True, pady=4, padx=2)

            chk_var = tk.BooleanVar(value=is_done)
            cb = ttk.Checkbutton(
                card,
                variable=chk_var,
                command=lambda cat=category, i=idx, cv=chk_var: self._on_checkbox_click(cat, i, cv)
            )
            cb.pack(side=tk.LEFT, padx=6)

            lbl_time = tk.Label(
                card,
                text=f"{t_start} - {t_end}",
                font=("DejaVu Sans Mono", 9, "bold"),
                bg="#e0e0e0",
                fg="#333333",
                padx=6,
                pady=2
            )
            lbl_time.pack(side=tk.LEFT, padx=6)

            task_font = ("DejaVu Sans", 10, "overstrike") if is_done else ("DejaVu Sans", 10)
            task_fg = "#777777" if is_done else "#111111"
            lbl_task = tk.Label(
                card,
                text=task_desc,
                font=task_font,
                fg=task_fg,
                anchor="w",
                justify="left"
            )
            lbl_task.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=8)

            # Status Badge on right
            if is_done:
                status_text = f"✓ Completed @ {item.get('completed_at', '')[:5]}"
                bg_col, fg_col = "#c8e6c9", "#1b5e20"
            elif in_window:
                status_text = "● ACTIVE NOW"
                bg_col, fg_col = "#bbdefb", "#0d47a1"
            elif win_status == "upcoming":
                status_text = "○ UPCOMING"
                bg_col, fg_col = "#fff3e0", "#e65100"
            else:
                status_text = "✗ EXPIRED"
                bg_col, fg_col = "#ffebee", "#b71c1c"

            lbl_badge = tk.Label(
                card,
                text=status_text,
                font=("DejaVu Sans", 8, "bold"),
                bg=bg_col,
                fg=fg_col,
                padx=6,
                pady=2
            )
            lbl_badge.pack(side=tk.RIGHT, padx=4)

    def _on_checkbox_click(self, category, index, chk_var):
        day_data = self.tracker.get_day_data()
        items = day_data.get(category, [])
        if index >= len(items):
            return

        item = items[index]
        already_done = item.get("completed", False)

        if already_done:
            self.tracker.toggle_task(category, index)
            self.lbl_status_msg.config(text=f"Unticked: {item['task']}")
            self._refresh_data()
            return

        in_window, status, reason = self.tracker.check_time_window(item["time_start"], item["time_end"])

        if in_window:
            ok, msg = self.tracker.toggle_task(category, index)
            self.lbl_status_msg.config(text=msg)
            self._refresh_data()
            self._check_all_completed()
        elif status == "upcoming":
            chk_var.set(False)
            messagebox.showwarning(
                "Cannot Complete Yet",
                f"⏳ Task: '{item['task']}'\n\nScheduled window: {item['time_start']} - {item['time_end']}.\n\nTasks can only be completed between their scheduled timings!"
            )
            self.lbl_status_msg.config(text=f"⏳ Cannot complete: {reason}")
        else:
            chk_var.set(False)
            confirm = messagebox.askyesno(
                "Time Window Expired",
                f"⚠️ Task: '{item['task']}' was scheduled for {item['time_start']} - {item['time_end']}.\n\nThe scheduled timing has passed.\nDo you want to override and mark it as completed anyway?"
            )
            if confirm:
                ok, msg = self.tracker.force_toggle_task(category, index)
                self.lbl_status_msg.config(text=msg)
                self._refresh_data()
                self._check_all_completed()

    def _check_all_completed(self):
        day_data = self.tracker.get_day_data()
        work_done, work_total = self.tracker.get_category_stats(day_data, "work")
        food_done, food_total = self.tracker.get_category_stats(day_data, "food")
        total_tasks = work_total + food_total
        total_done = work_done + food_done

        if total_tasks > 0 and total_done == total_tasks:
            today_str = self.tracker.get_today_str()
            messagebox.showinfo(
                "🎉 Congratulations!",
                f"All daily checkpoints for today are COMPLETED!\n\nDaily archive file created:\n~/SE/Devendra/Daily/✓ {today_str}.txt"
            )

    # -------------------------------------------------------------------------
    # EDIT MODE
    # -------------------------------------------------------------------------
    def _toggle_edit_mode(self):
        today_str = self.tracker.get_today_str()
        allowed, reason = self.tracker.is_edit_allowed(today_str, force=self.override_lock.get())

        if not self.is_edit_mode:
            if not allowed:
                confirm = messagebox.askyesno(
                    "Editing Locked",
                    f"🔒 {reason}\n\nTask planning and editing is strictly allowed only until 10:00 AM.\n\nDo you want to override the lock to edit anyway?"
                )
                if confirm:
                    self.override_lock.set(True)
                else:
                    return

            self.is_edit_mode = True
            self.btn_edit_toggle.config(text="👁️ View Checklist Mode")
            self._load_edit_rows()
            self._render_edit_mode()
        else:
            self.is_edit_mode = False
            self.btn_edit_toggle.config(text="✏️ Edit Page / Schedule")
            self._render_view_mode()

    def _on_override_toggle(self):
        if not self.is_edit_mode:
            self._refresh_data()

    def _load_edit_rows(self):
        day_data = self.tracker.get_day_data()
        self.edit_work_rows = []
        for item in day_data.get("work", []):
            self.edit_work_rows.append({
                "id": item.get("id"),
                "start": tk.StringVar(value=item.get("time_start", "09:00")),
                "end": tk.StringVar(value=item.get("time_end", "10:00")),
                "task": tk.StringVar(value=item.get("task", "")),
                "sound": tk.StringVar(value=item.get("sound_file", "")),
                "completed": item.get("completed", False),
                "completed_at": item.get("completed_at", None)
            })

        self.edit_food_rows = []
        for item in day_data.get("food", []):
            self.edit_food_rows.append({
                "id": item.get("id"),
                "start": tk.StringVar(value=item.get("time_start", "08:30")),
                "end": tk.StringVar(value=item.get("time_end", "09:15")),
                "task": tk.StringVar(value=item.get("task", "")),
                "sound": tk.StringVar(value=item.get("sound_file", "")),
                "completed": item.get("completed", False),
                "completed_at": item.get("completed_at", None)
            })

    def _render_edit_mode(self):
        for widget in self.main_container.winfo_children():
            widget.destroy()

        edit_top_bar = ttk.Frame(self.main_container, padding="0 0 0 10")
        edit_top_bar.pack(fill=tk.X)

        lbl_edit_title = ttk.Label(
            edit_top_bar,
            text="✏️ EDITING DAILY SCHEDULE — Enter timings and task descriptions below:",
            font=("DejaVu Sans", 11, "bold")
        )
        lbl_edit_title.pack(side=tk.LEFT)

        btn_save = tk.Button(
            edit_top_bar,
            text="💾 Save Schedule Changes",
            bg="#2e7d32",
            fg="white",
            font=("DejaVu Sans", 10, "bold"),
            command=self._save_edit_changes,
            padx=10,
            pady=4
        )
        btn_save.pack(side=tk.RIGHT, padx=5)

        btn_apply_template = ttk.Button(
            edit_top_bar,
            text="📋 Load Template Defaults",
            command=self._apply_template_in_edit
        )
        btn_apply_template.pack(side=tk.RIGHT, padx=5)

        btn_save_template = ttk.Button(
            edit_top_bar,
            text="⭐ Save as Default Template",
            command=self._save_as_template
        )
        btn_save_template.pack(side=tk.RIGHT, padx=5)

        paned = ttk.PanedWindow(self.main_container, orient=tk.HORIZONTAL)
        paned.pack(fill=tk.BOTH, expand=True)

        work_box = ttk.LabelFrame(paned, text=" 💼 WORK CHECKPOINTS ", padding="8")
        paned.add(work_box, weight=1)
        self._populate_column_edit(work_box, "work", self.edit_work_rows)

        food_box = ttk.LabelFrame(paned, text=" 🥗 FOOD & NUTRITION ", padding="8")
        paned.add(food_box, weight=1)
        self._populate_column_edit(food_box, "food", self.edit_food_rows)

    def _populate_column_edit(self, parent_frame, category, rows_list):
        canvas = tk.Canvas(parent_frame, highlightthickness=0)
        scrollbar = ttk.Scrollbar(parent_frame, orient="vertical", command=canvas.yview)
        scrollable_frame = ttk.Frame(canvas)

        scrollable_frame.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all"))
        )
        canvas.create_window((0, 0), window=scrollable_frame, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)

        canvas.pack(side="top", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")

        hdr_frame = ttk.Frame(scrollable_frame, padding="2")
        hdr_frame.pack(fill=tk.X, pady=2)
        ttk.Label(hdr_frame, text="Start", width=8, font=("DejaVu Sans", 9, "bold")).pack(side=tk.LEFT, padx=2)
        ttk.Label(hdr_frame, text="End", width=8, font=("DejaVu Sans", 9, "bold")).pack(side=tk.LEFT, padx=2)
        ttk.Label(hdr_frame, text="Task Description", font=("DejaVu Sans", 9, "bold")).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=4)
        ttk.Label(hdr_frame, text="Action", width=8, font=("DejaVu Sans", 9, "bold")).pack(side=tk.RIGHT, padx=2)

        for idx, r in enumerate(rows_list):
            row_frame = ttk.Frame(scrollable_frame, padding="2")
            row_frame.pack(fill=tk.X, pady=3)

            ent_start = ttk.Entry(row_frame, textvariable=r["start"], width=7)
            ent_start.pack(side=tk.LEFT, padx=2)

            ent_end = ttk.Entry(row_frame, textvariable=r["end"], width=7)
            ent_end.pack(side=tk.LEFT, padx=2)

            ent_task = ttk.Entry(row_frame, textvariable=r["task"])
            ent_task.pack(side=tk.LEFT, fill=tk.X, expand=True, padx=4)

            btn_del = tk.Button(
                row_frame,
                text="❌",
                fg="red",
                bg="#fbe9e7",
                relief="flat",
                command=lambda cat=category, i=idx: self._remove_edit_row(cat, i)
            )
            btn_del.pack(side=tk.RIGHT, padx=2)

        btn_add = ttk.Button(
            scrollable_frame,
            text=f"➕ Add New {category.capitalize()} Task",
            command=lambda cat=category: self._add_edit_row(cat)
        )
        btn_add.pack(anchor="w", pady=10, padx=2)

    def _add_edit_row(self, category):
        new_row = {
            "id": None,
            "start": tk.StringVar(value="14:00"),
            "end": tk.StringVar(value="15:00"),
            "task": tk.StringVar(value=""),
            "sound": tk.StringVar(value=""),
            "completed": False,
            "completed_at": None
        }
        if category == "work":
            self.edit_work_rows.append(new_row)
        else:
            self.edit_food_rows.append(new_row)
        self._render_edit_mode()

    def _remove_edit_row(self, category, index):
        if category == "work" and index < len(self.edit_work_rows):
            self.edit_work_rows.pop(index)
        elif category == "food" and index < len(self.edit_food_rows):
            self.edit_food_rows.pop(index)
        self._render_edit_mode()

    def _save_edit_changes(self):
        clean_work = []
        for idx, r in enumerate(self.edit_work_rows):
            s = r["start"].get().strip()
            e = r["end"].get().strip()
            t = r["task"].get().strip()
            if not t:
                continue
            if not self._validate_time(s) or not self._validate_time(e):
                messagebox.showerror(
                    "Invalid Time Format",
                    f"Invalid time format in Work item: '{t}' ({s} - {e}).\nPlease use HH:MM 24-hour format (e.g. 09:30, 14:00)."
                )
                return
            clean_work.append({
                "id": r.get("id"),
                "time_start": s,
                "time_end": e,
                "task": t,
                "sound_file": r["sound"].get().strip() or None,
                "completed": r.get("completed", False),
                "completed_at": r.get("completed_at", None)
            })

        clean_food = []
        for idx, r in enumerate(self.edit_food_rows):
            s = r["start"].get().strip()
            e = r["end"].get().strip()
            t = r["task"].get().strip()
            if not t:
                continue
            if not self._validate_time(s) or not self._validate_time(e):
                messagebox.showerror(
                    "Invalid Time Format",
                    f"Invalid time format in Food item: '{t}' ({s} - {e}).\nPlease use HH:MM 24-hour format (e.g. 08:30, 13:00)."
                )
                return
            clean_food.append({
                "id": r.get("id"),
                "time_start": s,
                "time_end": e,
                "task": t,
                "sound_file": r["sound"].get().strip() or None,
                "completed": r.get("completed", False),
                "completed_at": r.get("completed_at", None)
            })

        today_str = self.tracker.get_today_str()
        ok, msg = self.tracker.update_day_tasks(
            today_str,
            clean_work,
            clean_food,
            force=self.override_lock.get()
        )

        if ok:
            messagebox.showinfo("Saved", "Schedule updated successfully!")
            self.lbl_status_msg.config(text="Schedule saved successfully.")
            self._toggle_edit_mode()
        else:
            messagebox.showerror("Error Saving", msg)

    def _validate_time(self, time_str):
        try:
            parts = time_str.split(":")
            if len(parts) != 2:
                return False
            h, m = int(parts[0]), int(parts[1])
            return 0 <= h <= 23 and 0 <= m <= 59
        except Exception:
            return False

    def _apply_template_in_edit(self):
        confirm = messagebox.askyesno(
            "Load Template",
            "Replace Food items with your default daily routine template?"
        )
        if not confirm:
            return
        tpl = self.tracker.get_template()
        self.edit_food_rows = []
        for item in tpl.get("food", []):
            self.edit_food_rows.append({
                "id": item.get("id"),
                "start": tk.StringVar(value=item.get("time_start", "08:30")),
                "end": tk.StringVar(value=item.get("time_end", "09:15")),
                "task": tk.StringVar(value=item.get("task", "")),
                "sound": tk.StringVar(value=item.get("sound_file", "")),
                "completed": False,
                "completed_at": None
            })
        self._render_edit_mode()

    def _save_as_template(self):
        confirm = messagebox.askyesno(
            "Save Default Template",
            "Save current items as your permanent daily template for future days?"
        )
        if not confirm:
            return
        new_tpl = {
            "work": [
                {"id": f"w_{i}", "time_start": r["start"].get().strip(), "time_end": r["end"].get().strip(), "task": r["task"].get().strip()}
                for i, r in enumerate(self.edit_work_rows) if r["task"].get().strip()
            ],
            "food": [
                {"id": f"f_{i}", "time_start": r["start"].get().strip(), "time_end": r["end"].get().strip(), "task": r["task"].get().strip()}
                for i, r in enumerate(self.edit_food_rows) if r["task"].get().strip()
            ]
        }
        self.tracker.save_template(new_tpl)
        messagebox.showinfo("Template Saved", "Saved as permanent daily routine template!")

    # -------------------------------------------------------------------------
    # SOUND / ALARM SETTINGS DIALOG
    # -------------------------------------------------------------------------
    def _open_sound_settings(self):
        top = tk.Toplevel(self.root)
        top.title("🔔 Interval Sound & Alarm Settings")
        top.geometry("680x420")
        top.minsize(550, 350)
        top.transient(self.root)
        top.grab_set()

        cfg = audio.load_sound_config()

        var_enabled = tk.BooleanVar(value=cfg.get("sound_enabled", True))
        var_work_sound = tk.StringVar(value=cfg.get("sound_work_file", audio.DEFAULT_WORK_SOUND))
        var_food_sound = tk.StringVar(value=cfg.get("sound_food_file", audio.DEFAULT_FOOD_SOUND))
        var_duration = tk.IntVar(value=cfg.get("sound_duration_sec", 30))

        content = ttk.Frame(top, padding="15")
        content.pack(fill=tk.BOTH, expand=True)

        # Title
        ttk.Label(
            content,
            text="🎵 Interval Alarm Settings",
            font=("DejaVu Sans", 13, "bold")
        ).pack(anchor="w", pady=(0, 10))

        # Enable checkbox
        ttk.Checkbutton(
            content,
            text="🔔 Enable audio alarms at the start of each interval (Work and Food)",
            variable=var_enabled
        ).pack(anchor="w", pady=5)

        ttk.Separator(content, orient=tk.HORIZONTAL).pack(fill=tk.X, pady=10)

        # Work Sound Selection
        work_box = ttk.LabelFrame(content, text=" 💼 Work Interval Sound / Song ", padding="10")
        work_box.pack(fill=tk.X, pady=5)

        ttk.Label(work_box, textvariable=var_work_sound, font=("DejaVu Sans Mono", 8), foreground="#555").pack(anchor="w", pady=(0, 5))

        btn_row_work = ttk.Frame(work_box)
        btn_row_work.pack(fill=tk.X)

        def pick_work_sound():
            f = filedialog.askopenfilename(
                title="Select Work Alarm Audio / Song",
                filetypes=[
                    ("Audio Files (*.mp3, *.wav, *.ogg, *.oga, *.flac)", "*.mp3 *.wav *.ogg *.oga *.flac *.m4a *.aac"),
                    ("All Files", "*.*")
                ]
            )
            if f:
                var_work_sound.set(f)

        ttk.Button(btn_row_work, text="📂 Choose File from System...", command=pick_work_sound).pack(side=tk.LEFT, padx=2)
        ttk.Button(btn_row_work, text="▶️ Test Play", command=lambda: audio.play_sound(var_work_sound.get(), 10)).pack(side=tk.LEFT, padx=4)
        ttk.Button(btn_row_work, text="⏹️ Stop", command=audio.stop_sound).pack(side=tk.LEFT, padx=2)

        # Food Sound Selection
        food_box = ttk.LabelFrame(content, text=" 🥗 Food Interval Sound / Song ", padding="10")
        food_box.pack(fill=tk.X, pady=5)

        ttk.Label(food_box, textvariable=var_food_sound, font=("DejaVu Sans Mono", 8), foreground="#555").pack(anchor="w", pady=(0, 5))

        btn_row_food = ttk.Frame(food_box)
        btn_row_food.pack(fill=tk.X)

        def pick_food_sound():
            f = filedialog.askopenfilename(
                title="Select Food Alarm Audio / Song",
                filetypes=[
                    ("Audio Files (*.mp3, *.wav, *.ogg, *.oga, *.flac)", "*.mp3 *.wav *.ogg *.oga *.flac *.m4a *.aac"),
                    ("All Files", "*.*")
                ]
            )
            if f:
                var_food_sound.set(f)

        ttk.Button(btn_row_food, text="📂 Choose File from System...", command=pick_food_sound).pack(side=tk.LEFT, padx=2)
        ttk.Button(btn_row_food, text="▶️ Test Play", command=lambda: audio.play_sound(var_food_sound.get(), 10)).pack(side=tk.LEFT, padx=4)
        ttk.Button(btn_row_food, text="⏹️ Stop", command=audio.stop_sound).pack(side=tk.LEFT, padx=2)

        # Duration setting
        dur_frame = ttk.Frame(content, padding="5 10 5 5")
        dur_frame.pack(fill=tk.X)
        ttk.Label(dur_frame, text="Auto-stop ringing after:").pack(side=tk.LEFT)
        ent_dur = ttk.Entry(dur_frame, textvariable=var_duration, width=5)
        ent_dur.pack(side=tk.LEFT, padx=6)
        ttk.Label(dur_frame, text="seconds (if not dismissed manually)").pack(side=tk.LEFT)

        # Save Button
        def save_and_close():
            audio.stop_sound()
            audio.save_sound_config({
                "sound_enabled": var_enabled.get(),
                "sound_work_file": var_work_sound.get(),
                "sound_food_file": var_food_sound.get(),
                "sound_duration_sec": var_duration.get()
            })
            top.destroy()
            messagebox.showinfo("Saved", "Alarm sounds configuration updated successfully!")

        btn_save_snd = tk.Button(
            content,
            text="💾 Save Audio Settings",
            bg="#2e7d32",
            fg="white",
            font=("DejaVu Sans", 10, "bold"),
            command=save_and_close,
            padx=10,
            pady=4
        )
        btn_save_snd.pack(side=tk.RIGHT, pady=10)

    # -------------------------------------------------------------------------
    # ACTIONS
    # -------------------------------------------------------------------------
    def _sync_now(self):
        today = self.tracker.get_today_str()
        self.tracker.sync_and_catchup()
        p = self.tracker.generate_archive_file(today, is_final=False)
        self.lbl_status_msg.config(text=f"Archive updated: {p.name} in ~/SE/Devendra/Daily/")
        messagebox.showinfo(
            "Daily File Synchronized",
            f"Summary file successfully saved at:\n{p}\n\nAll checkpoints and completion statuses recorded."
        )

    def _open_daily_folder(self):
        folder = str(DAILY_DIR)
        try:
            subprocess.Popen(["xdg-open", folder])
        except Exception:
            messagebox.showinfo("Daily Folder Path", f"Daily reports folder:\n{folder}")


def run_gui():
    root = tk.Tk()
    app = DailyTrackerGUI(root)
    root.mainloop()


if __name__ == "__main__":
    run_gui()
