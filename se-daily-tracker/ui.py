"""
SE Daily Tracker - Terminal User Interface (Curses)
Path: /home/dev/SE/Projects/se-daily-tracker/ui.py
"""

import curses
import locale
import time
from datetime import datetime
from tracker import TaskTracker

locale.setlocale(locale.LC_ALL, '')


class DailyTrackerUI:
    def __init__(self, force_edit=False):
        self.tracker = TaskTracker()
        self.force_edit = force_edit
        self.current_column = "work"  # "work" or "food"
        self.selected_index = {"work": 0, "food": 0}
        self.status_message = "Ready. Use [Space] to check off active tasks."
        self.status_color = 1  # 1=normal/green, 2=warning, 3=error

    def run(self):
        curses.wrapper(self._main_loop)

    def _main_loop(self, stdscr):
        self.stdscr = stdscr
        curses.curs_set(0)  # Hide cursor
        stdscr.nodelay(True)  # Non-blocking for live clock
        curses.start_color()
        curses.use_default_colors()

        # Initialize color pairs
        try:
            curses.init_pair(1, curses.COLOR_GREEN, -1)    # Success / Completed
            curses.init_pair(2, curses.COLOR_YELLOW, -1)   # Upcoming / Notice
            curses.init_pair(3, curses.COLOR_RED, -1)      # Expired / Locked / Incomplete
            curses.init_pair(4, curses.COLOR_CYAN, -1)     # Work Header / Focus
            curses.init_pair(5, curses.COLOR_MAGENTA, -1)  # Food Header / Focus
            curses.init_pair(6, curses.COLOR_BLACK, curses.COLOR_CYAN)    # Selected row Work
            curses.init_pair(7, curses.COLOR_BLACK, curses.COLOR_MAGENTA) # Selected row Food
            curses.init_pair(8, curses.COLOR_WHITE, curses.COLOR_BLUE)    # Dialog header
        except Exception:
            pass

        # Perform catch-up on entry
        catchup_res = self.tracker.sync_and_catchup()
        if catchup_res:
            self.status_message = f"Catch-up synced {len(catchup_res)} daily file(s)."
            self.status_color = 2

        last_tick = 0

        while True:
            # Re-draw UI
            self._render()

            # Handle input
            try:
                ch = stdscr.getch()
            except Exception:
                ch = -1

            if ch != -1:
                if ch in (ord('q'), ord('Q')):
                    break
                elif ch in (curses.KEY_LEFT, ord('h'), ord('H')):
                    self.current_column = "work"
                elif ch in (curses.KEY_RIGHT, ord('l'), ord('L')):
                    self.current_column = "food"
                elif ch in (9, ord('\t')):  # Tab key
                    self.current_column = "food" if self.current_column == "work" else "work"
                elif ch in (curses.KEY_UP, ord('k'), ord('K')):
                    self._move_selection(-1)
                elif ch in (curses.KEY_DOWN, ord('j'), ord('J')):
                    self._move_selection(1)
                elif ch in (32, 10, 13):  # Space or Enter: toggle task
                    self._handle_toggle(force=False)
                elif ch in (ord('f'), ord('F')):  # Force toggle
                    self._handle_toggle(force=True)
                elif ch in (ord('a'), ord('A')):  # Add task
                    self._handle_add()
                elif ch in (ord('e'), ord('E')):  # Edit task
                    self._handle_edit()
                elif ch in (ord('d'), ord('D')):  # Delete task
                    self._handle_delete()
                elif ch in (ord('t'), ord('T')):  # Template menu
                    self._handle_template()
                elif ch in (ord('s'), ord('S')):  # Manual sync
                    self._handle_sync()

            # Update clock every 500ms
            time.sleep(0.05)

    def _move_selection(self, delta):
        day_data = self.tracker.get_day_data()
        items = day_data.get(self.current_column, [])
        if not items:
            self.selected_index[self.current_column] = 0
            return
        new_idx = self.selected_index[self.current_column] + delta
        new_idx = max(0, min(new_idx, len(items) - 1))
        self.selected_index[self.current_column] = new_idx

    def _handle_toggle(self, force=False):
        day_data = self.tracker.get_day_data()
        items = day_data.get(self.current_column, [])
        idx = self.selected_index[self.current_column]
        if not items or idx >= len(items):
            return

        if force:
            ok, msg = self.tracker.force_toggle_task(self.current_column, idx)
        else:
            ok, msg = self.tracker.toggle_task(self.current_column, idx)

        self.status_message = msg
        self.status_color = 1 if ok else 3

    def _handle_add(self):
        allowed, reason = self.tracker.is_edit_allowed(force=self.force_edit)
        if not allowed:
            self.status_message = f"🔒 Cannot Add: {reason}"
            self.status_color = 3
            return

        t_start = self._prompt_input("Add Task - Start Time (HH:MM, e.g. 14:00):", "12:00")
        if not t_start:
            return
        t_end = self._prompt_input("Add Task - End Time (HH:MM, e.g. 15:30):", "13:00")
        if not t_end:
            return
        task_desc = self._prompt_input("Add Task - Description:", "")
        if not task_desc:
            return

        ok, msg = self.tracker.add_task(
            self.current_column,
            t_start,
            t_end,
            task_desc,
            force=self.force_edit
        )
        self.status_message = msg
        self.status_color = 1 if ok else 3

    def _handle_edit(self):
        allowed, reason = self.tracker.is_edit_allowed(force=self.force_edit)
        if not allowed:
            self.status_message = f"🔒 Cannot Edit: {reason}"
            self.status_color = 3
            return

        day_data = self.tracker.get_day_data()
        items = day_data.get(self.current_column, [])
        idx = self.selected_index[self.current_column]
        if not items or idx >= len(items):
            return

        current_item = items[idx]
        t_start = self._prompt_input("Edit Start Time (HH:MM):", current_item["time_start"])
        if not t_start:
            return
        t_end = self._prompt_input("Edit End Time (HH:MM):", current_item["time_end"])
        if not t_end:
            return
        task_desc = self._prompt_input("Edit Description:", current_item["task"])
        if not task_desc:
            return

        ok, msg = self.tracker.edit_task(
            self.current_column,
            idx,
            t_start,
            t_end,
            task_desc,
            force=self.force_edit
        )
        self.status_message = msg
        self.status_color = 1 if ok else 3

    def _handle_delete(self):
        allowed, reason = self.tracker.is_edit_allowed(force=self.force_edit)
        if not allowed:
            self.status_message = f"🔒 Cannot Delete: {reason}"
            self.status_color = 3
            return

        day_data = self.tracker.get_day_data()
        items = day_data.get(self.current_column, [])
        idx = self.selected_index[self.current_column]
        if not items or idx >= len(items):
            return

        confirm = self._prompt_input(f"Remove '{items[idx]['task']}'? (y/N):", "n")
        if confirm and confirm.lower() == 'y':
            ok, msg = self.tracker.delete_task(self.current_column, idx, force=self.force_edit)
            self.status_message = msg
            self.status_color = 2
            if idx >= len(day_data.get(self.current_column, [])):
                self.selected_index[self.current_column] = max(0, len(day_data.get(self.current_column, [])) - 1)

    def _handle_template(self):
        allowed, reason = self.tracker.is_edit_allowed(force=self.force_edit)
        choice = self._prompt_input("Template: [1] Apply Template to Today  [2] Save Today as Template  [Esc to Cancel]:", "")
        if choice == "1":
            if not allowed:
                self.status_message = f"🔒 Cannot overwrite today: {reason}"
                self.status_color = 3
                return
            tpl = self.tracker.get_template()
            day = self.tracker.get_day_data()
            day["work"] = [dict(x, completed=False, completed_at=None) for x in tpl.get("work", [])]
            day["food"] = [dict(x, completed=False, completed_at=None) for x in tpl.get("food", [])]
            self.tracker.save_tasks()
            self.status_message = "Default template applied to today's schedule."
            self.status_color = 1
        elif choice == "2":
            day = self.tracker.get_day_data()
            clean_tpl = {
                "work": [{"id": x["id"], "time_start": x["time_start"], "time_end": x["time_end"], "task": x["task"]} for x in day.get("work", [])],
                "food": [{"id": x["id"], "time_start": x["time_start"], "time_end": x["time_end"], "task": x["task"]} for x in day.get("food", [])],
            }
            self.tracker.save_template(clean_tpl)
            self.status_message = "Saved today's tasks as the default template."
            self.status_color = 1

    def _handle_sync(self):
        res = self.tracker.sync_and_catchup()
        today = self.tracker.get_today_str()
        # Force write current archive
        p = self.tracker.generate_archive_file(today, is_final=False)
        self.status_message = f"Archive synced: {p.name} updated in ~/SE/Devendra/Daily/"
        self.status_color = 1

    def _prompt_input(self, prompt_text, default_val=""):
        """Displays a clean input box in the center of the terminal."""
        max_y, max_x = self.stdscr.getmaxyx()
        box_w = min(max_x - 4, 70)
        box_h = 5
        start_y = max((max_y - box_h) // 2, 0)
        start_x = max((max_x - box_w) // 2, 0)

        win = curses.newwin(box_h, box_w, start_y, start_x)
        win.keypad(True)
        curses.curs_set(1)

        win.box()
        win.addstr(1, 2, prompt_text[:box_w - 4], curses.color_pair(4) | curses.A_BOLD)

        val = list(default_val)
        cursor_pos = len(val)

        while True:
            # Render input field
            win.addstr(2, 2, " " * (box_w - 4))
            display_str = "".join(val)
            win.addstr(2, 2, display_str[:box_w - 4])
            win.move(2, 2 + min(cursor_pos, box_w - 5))
            win.refresh()

            ch = win.getch()
            if ch in (10, 13):  # Enter
                curses.curs_set(0)
                return "".join(val)
            elif ch == 27:  # Escape
                curses.curs_set(0)
                return None
            elif ch in (curses.KEY_BACKSPACE, 127, 8):
                if cursor_pos > 0:
                    val.pop(cursor_pos - 1)
                    cursor_pos -= 1
            elif ch == curses.KEY_DC:  # Delete
                if cursor_pos < len(val):
                    val.pop(cursor_pos)
            elif ch == curses.KEY_LEFT:
                cursor_pos = max(0, cursor_pos - 1)
            elif ch == curses.KEY_RIGHT:
                cursor_pos = min(len(val), cursor_pos + 1)
            elif 32 <= ch <= 126:
                val.insert(cursor_pos, chr(ch))
                cursor_pos += 1

    def _render(self):
        self.stdscr.erase()
        max_y, max_x = self.stdscr.getmaxyx()

        if max_y < 15 or max_x < 60:
            self.stdscr.addstr(0, 0, "Terminal window too small. Please resize.")
            self.stdscr.refresh()
            return

        now = datetime.now()
        today_str = self.tracker.get_today_str()
        day_data = self.tracker.get_day_data(today_str)

        work_done, work_total = self.tracker.get_category_stats(day_data, "work")
        food_done, food_total = self.tracker.get_category_stats(day_data, "food")
        total_tasks = work_total + food_total
        total_done = work_done + food_done
        total_pct = (total_done / total_tasks * 100) if total_tasks > 0 else 0

        edit_allowed, edit_msg = self.tracker.is_edit_allowed(force=self.force_edit)

        # 1. Header Banner
        header_line = f" SE DAILY CHECKPOINTS TRACKER — {now.strftime('%A, %b %d, %Y')} "
        clock_str = now.strftime("%H:%M:%S")
        edit_badge = " [🔓 EDIT OPEN] " if edit_allowed else " [🔒 EDIT LOCKED] "
        
        self.stdscr.attron(curses.A_BOLD)
        self.stdscr.addstr(0, 2, header_line, curses.color_pair(4))
        self.stdscr.addstr(0, max_x - len(clock_str) - len(edit_badge) - 4, edit_badge, 
                           curses.color_pair(1 if edit_allowed else 3))
        self.stdscr.addstr(0, max_x - len(clock_str) - 2, clock_str, curses.color_pair(2))
        self.stdscr.attroff(curses.A_BOLD)

        # Progress bar line
        prog_text = f" Progress: {total_done}/{total_tasks} ({total_pct:.0f}%) | Work: {work_done}/{work_total} | Food: {food_done}/{food_total} "
        self.stdscr.addstr(1, 2, prog_text, curses.A_DIM)
        if total_tasks > 0 and total_done == total_tasks:
            self.stdscr.addstr(1, len(prog_text) + 4, "★ ALL COMPLETE TODAY! ✓ ★", curses.color_pair(1) | curses.A_BOLD)

        # Separator line
        self.stdscr.hline(2, 0, curses.ACS_HLINE, max_x)

        # 2. Two Columns Layout
        col_y = 3
        col_h = max_y - 7  # reserve top 3 lines and bottom 4 lines
        half_w = (max_x - 3) // 2

        # Left Column: WORK
        is_work_active = (self.current_column == "work")
        self._render_column(
            start_y=col_y,
            start_x=1,
            width=half_w,
            height=col_h,
            title="WORK CHECKPOINTS",
            category="work",
            items=day_data.get("work", []),
            is_active=is_work_active,
            color_pair=4
        )

        # Center separator
        for y in range(col_y, col_y + col_h):
            self.stdscr.addch(y, half_w + 1, curses.ACS_VLINE, curses.A_DIM)

        # Right Column: FOOD
        is_food_active = (self.current_column == "food")
        self._render_column(
            start_y=col_y,
            start_x=half_w + 2,
            width=half_w,
            height=col_h,
            title="FOOD & NUTRITION",
            category="food",
            items=day_data.get("food", []),
            is_active=is_food_active,
            color_pair=5
        )

        # 3. Bottom Status Message & Help Bar
        status_y = max_y - 4
        self.stdscr.hline(status_y, 0, curses.ACS_HLINE, max_x)

        # Status alert line
        status_display = f" Status: {self.status_message}"
        self.stdscr.addstr(status_y + 1, 2, status_display[:max_x - 4], 
                           curses.color_pair(self.status_color) | curses.A_BOLD)

        # Shortcut Keys Bar
        edit_indicator = "[E] Edit  [A] Add  [D] Delete" if edit_allowed else "[E/A/D Locked after 10 AM]"
        shortcuts = f" [Tab/←/→] Switch Col | [↑/↓] Move | [Space] Tick | [F] Force | {edit_indicator} | [T] Template | [S] Sync | [Q] Quit"
        self.stdscr.addstr(status_y + 2, 2, shortcuts[:max_x - 4], curses.A_DIM)

        self.stdscr.refresh()

    def _render_column(self, start_y, start_x, width, height, title, category, items, is_active, color_pair):
        """Renders one side-by-side column with border and items."""
        # Box header
        border_attr = (curses.color_pair(color_pair) | curses.A_BOLD) if is_active else curses.A_DIM
        done_count = sum(1 for x in items if x.get("completed", False))
        title_str = f" [ {title} ({done_count}/{len(items)}) ] "

        # Draw top header
        self.stdscr.addstr(start_y, start_x, "─" * (width - 1), border_attr)
        self.stdscr.addstr(start_y, start_x + 2, title_str, curses.color_pair(color_pair) | curses.A_BOLD)

        # Ensure selected index within range
        sel_idx = self.selected_index.get(category, 0)
        if items and sel_idx >= len(items):
            sel_idx = len(items) - 1
            self.selected_index[category] = sel_idx

        # Render rows
        row_y = start_y + 1
        max_rows = height - 2

        if not items:
            self.stdscr.addstr(row_y + 1, start_x + 4, "(No checkpoints yet)", curses.A_DIM)
            if self.tracker.is_edit_allowed(force=self.force_edit)[0]:
                self.stdscr.addstr(row_y + 2, start_x + 4, "Press [A] to add a task.", curses.color_pair(2))
            return

        now_time = datetime.now().time()

        for idx, item in enumerate(items):
            if idx >= max_rows:
                self.stdscr.addstr(start_y + height - 1, start_x + 2, f"... +{len(items) - max_rows} more", curses.A_DIM)
                break

            current_y = row_y + idx
            is_selected = (is_active and idx == sel_idx)

            is_completed = item.get("completed", False)
            t_start = item.get("time_start", "00:00")
            t_end = item.get("time_end", "00:00")
            task_desc = item.get("task", "")

            # Check timing window
            in_window, win_status, _ = self.tracker.check_time_window(t_start, t_end, check_time=now_time)

            # Checkbox & Status
            if is_completed:
                check_sym = "[✓]"
                status_pill = f"DONE @ {item.get('completed_at', '')[:5]}"
                pill_color = 1  # Green
            elif in_window:
                check_sym = "[ ]"
                status_pill = "ACTIVE NOW"
                pill_color = 1  # Green bold
            elif win_status == "upcoming":
                check_sym = "[ ]"
                status_pill = "UPCOMING"
                pill_color = 2  # Yellow
            else:
                check_sym = "[✗]"
                status_pill = "EXPIRED"
                pill_color = 3  # Red

            time_str = f"{t_start}-{t_end}"
            
            # Format row content
            # Calculate width available for task description
            # Layout: [cursor 2] [check 4] [time 12] [desc ...] [status 14]
            fixed_len = 2 + 4 + 13 + 15
            desc_len = max(10, width - fixed_len - 2)
            truncated_desc = (task_desc[:desc_len - 3] + "...") if len(task_desc) > desc_len else task_desc
            desc_padded = f"{truncated_desc:<{desc_len}}"

            cursor_marker = "▶ " if is_selected else "  "
            row_text = f"{cursor_marker}{check_sym} {time_str:<11} {desc_padded} {status_pill:>12}"

            # Drawing attributes
            if is_selected:
                highlight_pair = 6 if category == "work" else 7
                self.stdscr.addstr(current_y, start_x, row_text[:width - 1], curses.color_pair(highlight_pair) | curses.A_BOLD)
            else:
                # Normal row with colors
                self.stdscr.addstr(current_y, start_x, cursor_marker, curses.A_DIM)
                chk_attr = curses.color_pair(1 if is_completed else (3 if win_status == 'expired' else 2))
                self.stdscr.addstr(current_y, start_x + 2, f"{check_sym} ", chk_attr | curses.A_BOLD)
                self.stdscr.addstr(current_y, start_x + 6, f"{time_str:<11} ", curses.A_DIM)
                
                # Desc
                desc_attr = curses.A_NORMAL if not is_completed else (curses.color_pair(1) | curses.A_DIM)
                self.stdscr.addstr(current_y, start_x + 18, desc_padded, desc_attr)
                
                # Status pill
                self.stdscr.addstr(current_y, start_x + 18 + desc_len + 1, f"{status_pill:>12}", 
                                   curses.color_pair(pill_color) | (curses.A_BOLD if in_window else curses.A_DIM))
