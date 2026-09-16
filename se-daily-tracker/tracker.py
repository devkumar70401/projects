"""
SE Daily Tracker - Core Business Logic & Data Store
Path: /home/dev/SE/Projects/se-daily-tracker/tracker.py
"""

import os
import sys
import json
import shutil
from datetime import datetime, time, date, timedelta
from pathlib import Path

# Paths
BASE_DIR = Path("/home/dev/SE")
DEVENDRA_DIR = BASE_DIR / "Devendra"
DAILY_DIR = DEVENDRA_DIR / "Daily"
DATA_DIR = DAILY_DIR / ".data"
DELETED_DIR = BASE_DIR / "deleted"

TASKS_FILE = DATA_DIR / "tasks.json"
TEMPLATE_FILE = DATA_DIR / "template.json"
CONFIG_FILE = DATA_DIR / "config.json"

DEFAULT_CONFIG = {
    "edit_cutoff_hour": 10,
    "edit_cutoff_minute": 0,
    "archive_cutoff_hour": 23,
    "archive_cutoff_minute": 0,
}

DEFAULT_TEMPLATE = {
    "work": [],
    "food": [
        {"id": "f1", "time_start": "08:30", "time_end": "09:15", "task": "Healthy Breakfast & Hydration"},
        {"id": "f2", "time_start": "13:00", "time_end": "14:00", "task": "Nutritious Lunch & Rest"},
        {"id": "f3", "time_start": "17:00", "time_end": "17:30", "task": "Evening Refreshment / Green Tea"},
        {"id": "f4", "time_start": "20:30", "time_end": "21:30", "task": "Healthy Dinner & Wind Down"}
    ]
}


def ensure_directories():
    """Ensure all required directories exist."""
    DAILY_DIR.mkdir(parents=True, exist_ok=True)
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    DELETED_DIR.mkdir(parents=True, exist_ok=True)


def safe_relocate(file_path: Path):
    """
    ABSOLUTE ZERO-DELETE MANDATE:
    Never delete or unlink. Safely move to /home/dev/SE/deleted/
    """
    if not file_path.exists():
        return
    ensure_directories()
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    dest_name = f"{timestamp}_{file_path.name}"
    dest_path = DELETED_DIR / dest_name
    shutil.move(str(file_path), str(dest_path))


class TaskTracker:
    def __init__(self):
        ensure_directories()
        self.config = self._load_config()
        self.data = self._load_tasks()
        self._ensure_template()

    def _load_config(self):
        if CONFIG_FILE.exists():
            try:
                with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                    return {**DEFAULT_CONFIG, **json.load(f)}
            except Exception:
                return DEFAULT_CONFIG.copy()
        else:
            with open(CONFIG_FILE, "w", encoding="utf-8") as f:
                json.dump(DEFAULT_CONFIG, f, indent=2)
            return DEFAULT_CONFIG.copy()

    def _ensure_template(self):
        if not TEMPLATE_FILE.exists():
            with open(TEMPLATE_FILE, "w", encoding="utf-8") as f:
                json.dump(DEFAULT_TEMPLATE, f, indent=2)

    def get_template(self):
        if TEMPLATE_FILE.exists():
            try:
                with open(TEMPLATE_FILE, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                pass
        return DEFAULT_TEMPLATE

    def save_template(self, template_data):
        with open(TEMPLATE_FILE, "w", encoding="utf-8") as f:
            json.dump(template_data, f, indent=2)

    def _load_tasks(self):
        if TASKS_FILE.exists():
            try:
                with open(TASKS_FILE, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception:
                return {}
        return {}

    def save_tasks(self):
        with open(TASKS_FILE, "w", encoding="utf-8") as f:
            json.dump(self.data, f, indent=2)

    def get_today_str(self):
        return datetime.now().strftime("%Y-%m-%d")

    def reload(self):
        self.data = self._load_tasks()
        return self.data

    def get_day_data(self, date_str=None, auto_init=True):
        self.reload()
        if date_str is None:
            date_str = self.get_today_str()
        
        if date_str not in self.data and auto_init:
            tpl = self.get_template()
            self.data[date_str] = {
                "date": date_str,
                "finalized": False,
                "finalized_at": None,
                "archive_file": None,
                "work": [],  # Blank for every new day
                "food": [
                    {
                        "id": item.get("id", f"f_{idx}"),
                        "time_start": item.get("time_start", "08:30"),
                        "time_end": item.get("time_end", "09:00"),
                        "task": item.get("task", ""),
                        "completed": False,
                        "completed_at": None
                    }
                    for idx, item in enumerate(tpl.get("food", []))
                ]
            }
            self.save_tasks()

        return self.data.get(date_str, {"date": date_str, "work": [], "food": [], "finalized": False})

    def is_edit_allowed(self, target_date=None, force=False):
        """
        Editing is only allowed until 10:00 AM on the given day.
        If force=True, override is permitted.
        """
        if force:
            return True, "Unlocked (Override Active)"

        now = datetime.now()
        today = self.get_today_str()

        if target_date is None:
            target_date = today

        if target_date > today:
            return True, "Future date planning allowed"
        
        if target_date < today:
            return False, "Past date is locked"

        cutoff_time = time(self.config["edit_cutoff_hour"], self.config["edit_cutoff_minute"])
        if now.time() <= cutoff_time:
            return True, f"Editing open until {cutoff_time.strftime('%I:%M %p')}"
        else:
            return False, f"Editing locked after {cutoff_time.strftime('%I:%M %p')} (Planning window is 00:00 - 10:00 AM)"

    def check_time_window(self, time_start_str, time_end_str, check_time=None):
        """
        Returns (is_active, status_label, reason)
        status_label in ['active', 'upcoming', 'expired']
        """
        if check_time is None:
            now_time = datetime.now().time()
        else:
            now_time = check_time

        try:
            start_parts = [int(p) for p in time_start_str.strip().split(":")]
            end_parts = [int(p) for p in time_end_str.strip().split(":")]
            t_start = time(start_parts[0], start_parts[1])
            t_end = time(end_parts[0], end_parts[1])
        except Exception:
            return True, "active", "Invalid time format"

        if t_start <= t_end:
            if t_start <= now_time <= t_end:
                return True, "active", f"In window ({time_start_str} - {time_end_str})"
            elif now_time < t_start:
                return False, "upcoming", f"Upcoming (Scheduled for {time_start_str})"
            else:
                return False, "expired", f"Window expired at {time_end_str}"
        else:
            if now_time >= t_start or now_time <= t_end:
                return True, "active", f"In overnight window ({time_start_str} - {time_end_str})"
            else:
                return False, "expired", f"Window closed"

    def toggle_task(self, category: str, task_index: int, date_str=None, force=False):
        """
        Toggles a checkpoint between completed and uncompleted.
        Strict check: Between those timings only those checkpoints can be completed.
        """
        day_data = self.get_day_data(date_str)
        items = day_data.get(category, [])
        if task_index < 0 or task_index >= len(items):
            return False, "Invalid task index"

        item = items[task_index]
        now = datetime.now()
        today = self.get_today_str()
        current_date = date_str or today

        if item.get("completed", False):
            item["completed"] = False
            item["completed_at"] = None
            self.save_tasks()
            self.sync_and_catchup()
            return True, f"Unticked: {item['task']}"

        if current_date == today and not force:
            in_window, status, reason = self.check_time_window(item["time_start"], item["time_end"])
            if not in_window:
                if status == "upcoming":
                    return False, f"⏳ Cannot complete yet: Scheduled for {item['time_start']} - {item['time_end']}"
                else:
                    return False, f"⚠️ Window passed ({item['time_start']} - {item['time_end']}). Override required."

        item["completed"] = True
        item["completed_at"] = now.strftime("%H:%M:%S")
        self.save_tasks()

        work_done, work_total = self.get_category_stats(day_data, "work")
        food_done, food_total = self.get_category_stats(day_data, "food")
        total_tasks = work_total + food_total
        total_done = work_done + food_done

        if total_tasks > 0 and total_done == total_tasks:
            self.generate_archive_file(current_date, is_final=True)
            return True, f"🎉 ALL TASKS COMPLETED! Created ✓ {current_date}.txt in Daily folder!"

        return True, f"✓ Completed: {item['task']}"

    def force_toggle_task(self, category: str, task_index: int, date_str=None):
        return self.toggle_task(category, task_index, date_str, force=True)

    def add_task(self, category: str, time_start: str, time_end: str, task: str, date_str=None, force=False):
        allowed, msg = self.is_edit_allowed(date_str, force=force)
        if not allowed:
            return False, msg

        day_data = self.get_day_data(date_str)
        items = day_data.setdefault(category, [])
        new_id = f"{category[0]}_{datetime.now().strftime('%H%M%S%f')[:8]}"
        items.append({
            "id": new_id,
            "time_start": time_start.strip(),
            "time_end": time_end.strip(),
            "task": task.strip(),
            "completed": False,
            "completed_at": None
        })
        items.sort(key=lambda x: x.get("time_start", "00:00"))
        self.save_tasks()
        return True, f"Added task to {category.upper()}: {task}"

    def edit_task(self, category: str, task_index: int, time_start: str, time_end: str, task: str, date_str=None, force=False):
        allowed, msg = self.is_edit_allowed(date_str, force=force)
        if not allowed:
            return False, msg

        day_data = self.get_day_data(date_str)
        items = day_data.get(category, [])
        if task_index < 0 or task_index >= len(items):
            return False, "Invalid task index"

        items[task_index]["time_start"] = time_start.strip()
        items[task_index]["time_end"] = time_end.strip()
        items[task_index]["task"] = task.strip()
        items.sort(key=lambda x: x.get("time_start", "00:00"))
        self.save_tasks()
        return True, f"Updated task: {task}"

    def delete_task(self, category: str, task_index: int, date_str=None, force=False):
        allowed, msg = self.is_edit_allowed(date_str, force=force)
        if not allowed:
            return False, msg

        day_data = self.get_day_data(date_str)
        items = day_data.get(category, [])
        if task_index < 0 or task_index >= len(items):
            return False, "Invalid task index"

        removed = items.pop(task_index)
        deleted_log = DATA_DIR / "deleted_tasks.jsonl"
        with open(deleted_log, "a", encoding="utf-8") as f:
            record = {
                "deleted_at": datetime.now().isoformat(),
                "date": date_str or self.get_today_str(),
                "category": category,
                "task": removed
            }
            f.write(json.dumps(record) + "\n")

        self.save_tasks()
        return True, f"Removed: {removed['task']}"

    def update_day_tasks(self, date_str, work_items, food_items, force=False):
        """Bulk update work and food items from GUI edit mode."""
        allowed, msg = self.is_edit_allowed(date_str, force=force)
        if not allowed:
            return False, msg

        day_data = self.get_day_data(date_str)

        for idx, item in enumerate(work_items):
            if not item.get("id"):
                item["id"] = f"w_{idx}_{datetime.now().strftime('%H%M%S')}"
            if "completed" not in item:
                item["completed"] = False
            if "completed_at" not in item:
                item["completed_at"] = None

        for idx, item in enumerate(food_items):
            if not item.get("id"):
                item["id"] = f"f_{idx}_{datetime.now().strftime('%H%M%S')}"
            if "completed" not in item:
                item["completed"] = False
            if "completed_at" not in item:
                item["completed_at"] = None

        work_items.sort(key=lambda x: x.get("time_start", "00:00"))
        food_items.sort(key=lambda x: x.get("time_start", "00:00"))

        day_data["work"] = work_items
        day_data["food"] = food_items
        self.save_tasks()
        self.sync_and_catchup()
        return True, "Schedule saved successfully."

    def get_category_stats(self, day_data, category):
        items = day_data.get(category, [])
        total = len(items)
        completed = sum(1 for item in items if item.get("completed", False))
        return completed, total

    def generate_archive_file(self, date_str: str, is_final: bool = False):
        day_data = self.get_day_data(date_str, auto_init=False)
        if not day_data:
            return None

        work_items = day_data.get("work", [])
        food_items = day_data.get("food", [])

        work_done = sum(1 for i in work_items if i.get("completed", False))
        food_done = sum(1 for i in food_items if i.get("completed", False))
        total_items = len(work_items) + len(food_items)
        total_done = work_done + food_done

        all_completed = (total_items > 0) and (total_done == total_items)

        mark = "✓" if all_completed else "✗"
        target_filename = f"{mark} {date_str}.txt"
        target_path = DAILY_DIR / target_filename

        other_mark = "✗" if all_completed else "✓"
        old_file = DAILY_DIR / f"{other_mark} {date_str}.txt"
        if old_file.exists():
            safe_relocate(old_file)

        now_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        status_text = "ALL COMPLETED" if all_completed else f"INCOMPLETE ({total_done}/{total_items} completed)"
        pct = (total_done / total_items * 100) if total_items > 0 else 0

        lines = [
            "=" * 80,
            f"DAILY CHECKPOINTS REPORT — {date_str}",
            f"Status: [{mark}] {status_text}",
            f"Generated: {now_str}",
            "=" * 80,
            "",
            f"--- WORK CHECKPOINTS ({work_done}/{len(work_items)} Completed) ---",
        ]

        if not work_items:
            lines.append("  (No work checkpoints scheduled)")
        else:
            for item in work_items:
                chk = "✓" if item.get("completed", False) else "✗"
                t_window = f"{item.get('time_start', '??:??')} - {item.get('time_end', '??:??')}"
                if item.get("completed", False):
                    at = item.get("completed_at", "Yes")
                    lines.append(f"  [{chk}] {t_window:<15} {item.get('task', '')} (Completed at {at})")
                else:
                    lines.append(f"  [{chk}] {t_window:<15} {item.get('task', '')} (INCOMPLETE)")

        lines.extend([
            "",
            f"--- FOOD & NUTRITION CHECKPOINTS ({food_done}/{len(food_items)} Completed) ---",
        ])

        if not food_items:
            lines.append("  (No food checkpoints scheduled)")
        else:
            for item in food_items:
                chk = "✓" if item.get("completed", False) else "✗"
                t_window = f"{item.get('time_start', '??:??')} - {item.get('time_end', '??:??')}"
                if item.get("completed", False):
                    at = item.get("completed_at", "Yes")
                    lines.append(f"  [{chk}] {t_window:<15} {item.get('task', '')} (Completed at {at})")
                else:
                    lines.append(f"  [{chk}] {t_window:<15} {item.get('task', '')} (INCOMPLETE)")

        lines.extend([
            "",
            "=" * 80,
            f"TOTAL PROGRESS: {total_done} / {total_items} tasks completed ({pct:.1f}%)",
            f"FINAL RESULT: {'SUCCESS - ALL CHECKPOINTS ACHIEVED ✓' if all_completed else 'INCOMPLETE ✗'}",
            "=" * 80,
            ""
        ])

        with open(target_path, "w", encoding="utf-8") as f:
            f.write("\n".join(lines))

        day_data["finalized"] = is_final
        day_data["finalized_at"] = now_str
        day_data["archive_file"] = str(target_path)
        self.save_tasks()

        return target_path

    def sync_and_catchup(self):
        processed = []
        today = self.get_today_str()
        now = datetime.now()

        for date_str, day_data in list(self.data.items()):
            if date_str < today:
                archive_file = day_data.get("archive_file")
                is_final = day_data.get("finalized", False)
                arch_exists = archive_file and Path(archive_file).exists()

                if not is_final or not arch_exists:
                    path = self.generate_archive_file(date_str, is_final=True)
                    processed.append((date_str, path.name if path else "none", "past_catchup"))

        today_data = self.get_day_data(today, auto_init=False)
        if today_data:
            work_items = today_data.get("work", [])
            food_items = today_data.get("food", [])
            total_items = len(work_items) + len(food_items)
            total_done = sum(1 for i in work_items if i.get("completed", False)) + \
                         sum(1 for i in food_items if i.get("completed", False))

            archive_cutoff = time(self.config["archive_cutoff_hour"], self.config["archive_cutoff_minute"])

            if total_items > 0 and total_done == total_items:
                path = self.generate_archive_file(today, is_final=True)
                processed.append((today, path.name if path else "none", "today_completed"))
            elif now.time() >= archive_cutoff:
                path = self.generate_archive_file(today, is_final=True)
                processed.append((today, path.name if path else "none", "today_11pm_cutoff"))

        return processed
