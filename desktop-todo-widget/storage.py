import os
import re
import json
from datetime import datetime, timedelta
from pathlib import Path

DEFAULT_DAILY_DIR = Path("/home/dev/SE/Devendra/Daily").resolve()
CONFIG_DIR = Path.home() / ".config" / "daily-todo-widget"
CONFIG_FILE = CONFIG_DIR / "config.json"

DEFAULT_CONFIG = {
    "x": 100,
    "y": 100,
    "width": 360,
    "height": 520,
    "carry_over_incomplete": True,
    "opacity": 0.82,
    "always_on_top": False
}


class TaskItem:
    def __init__(self, text: str, completed: bool = False, raw_line: str = ""):
        self.text = text
        self.completed = completed
        self.raw_line = raw_line

    def to_markdown(self) -> str:
        marker = "x" if self.completed else " "
        return f"- [{marker}] {self.text}"


class DailyMarkdownStore:
    def __init__(self, daily_dir: Path = DEFAULT_DAILY_DIR):
        self.daily_dir = Path(daily_dir)
        self.daily_dir.mkdir(parents=True, exist_ok=True)
        self.config = self.load_config()
        self.current_date = datetime.now().strftime("%Y-%m-%d")
        self.last_mtime = 0.0

    def load_config(self) -> dict:
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

    def save_config(self, cfg: dict):
        CONFIG_DIR.mkdir(parents=True, exist_ok=True)
        try:
            with open(CONFIG_FILE, "w", encoding="utf-8") as f:
                json.dump(cfg, f, indent=2)
            self.config = cfg
        except Exception as e:
            print(f"Error saving config: {e}")

    def get_file_path(self, date_str: str = None) -> Path:
        if not date_str:
            date_str = self.current_date
        return self.daily_dir / f"{date_str}.md"

    def ensure_today_file(self) -> Path:
        today_file = self.get_file_path(self.current_date)
        if not today_file.exists():
            self._initialize_today_file(today_file)
        return today_file

    def _initialize_today_file(self, target_path: Path):
        date_obj = datetime.strptime(self.current_date, "%Y-%m-%d")
        formatted_date = date_obj.strftime("%A, %d %B %Y")
        lines = [
            f"# Daily Tasks — {formatted_date}\n\n",
        ]

        # Check if we should carry over incomplete tasks from yesterday / last file
        carried_tasks = []
        if self.config.get("carry_over_incomplete", True):
            carried_tasks = self._find_incomplete_from_previous_day()

        if carried_tasks:
            lines.append("## Carried Over\n")
            for t in carried_tasks:
                lines.append(f"- [ ] {t}\n")
            lines.append("\n## Today's Tasks\n")
        else:
            lines.append("## Today's Tasks\n")

        with open(target_path, "w", encoding="utf-8") as f:
            f.writelines(lines)

    def _find_incomplete_from_previous_day(self) -> list:
        # Find the most recent .md file before today
        md_files = sorted(
            [f for f in self.daily_dir.glob("*.md") if f.stem < self.current_date and re.match(r"^\d{4}-\d{2}-\d{2}$", f.stem)],
            key=lambda x: x.stem
        )
        if not md_files:
            return []

        last_file = md_files[-1]
        incomplete = []
        task_regex = re.compile(r"^\s*-\s*\[([ ])\]\s*(.+)$")
        try:
            with open(last_file, "r", encoding="utf-8") as f:
                for line in f:
                    m = task_regex.match(line)
                    if m:
                        incomplete.append(m.group(2).strip())
        except Exception as e:
            print(f"Error reading previous day tasks: {e}")
        return incomplete

    def load_tasks(self) -> tuple[list[TaskItem], list[str]]:
        """
        Reads tasks and non-task lines from today's markdown file.
        Returns (tasks_list, all_lines_with_task_indices).
        """
        target_path = self.ensure_today_file()
        try:
            self.last_mtime = target_path.stat().st_mtime
        except Exception:
            self.last_mtime = 0.0

        tasks = []
        all_lines = []
        task_regex = re.compile(r"^\s*-\s*\[([ xX])\]\s*(.+)$")

        try:
            with open(target_path, "r", encoding="utf-8") as f:
                for line in f:
                    all_lines.append(line)
                    m = task_regex.match(line)
                    if m:
                        is_done = (m.group(1).lower() == "x")
                        text = m.group(2).strip()
                        tasks.append(TaskItem(text=text, completed=is_done, raw_line=line))
        except Exception as e:
            print(f"Error reading {target_path}: {e}")

        return tasks

    def toggle_task(self, index: int, completed: bool):
        target_path = self.ensure_today_file()
        task_regex = re.compile(r"^(\s*-\s*\[)([ xX])(\]\s*.+)$")
        new_lines = []
        current_task_idx = 0

        try:
            with open(target_path, "r", encoding="utf-8") as f:
                for line in f:
                    m = task_regex.match(line)
                    if m:
                        if current_task_idx == index:
                            marker = "x" if completed else " "
                            line = f"{m.group(1)}{marker}{m.group(3)}\n"
                        current_task_idx += 1
                    new_lines.append(line)

            with open(target_path, "w", encoding="utf-8") as f:
                f.writelines(new_lines)

            self.last_mtime = target_path.stat().st_mtime
        except Exception as e:
            print(f"Error updating task: {e}")

    def add_task(self, text: str):
        text = text.strip()
        if not text:
            return
        target_path = self.ensure_today_file()
        try:
            # Append cleanly
            with open(target_path, "r", encoding="utf-8") as f:
                content = f.read()

            if not content.endswith("\n"):
                content += "\n"
            content += f"- [ ] {text}\n"

            with open(target_path, "w", encoding="utf-8") as f:
                f.write(content)

            self.last_mtime = target_path.stat().st_mtime
        except Exception as e:
            print(f"Error adding task: {e}")

    def delete_task(self, index: int):
        target_path = self.ensure_today_file()
        task_regex = re.compile(r"^\s*-\s*\[([ xX])\]\s*(.+)$")
        new_lines = []
        current_task_idx = 0

        try:
            with open(target_path, "r", encoding="utf-8") as f:
                for line in f:
                    m = task_regex.match(line)
                    if m:
                        if current_task_idx == index:
                            current_task_idx += 1
                            continue  # Skip this line (delete)
                        current_task_idx += 1
                    new_lines.append(line)

            with open(target_path, "w", encoding="utf-8") as f:
                f.writelines(new_lines)

            self.last_mtime = target_path.stat().st_mtime
        except Exception as e:
            print(f"Error deleting task: {e}")

    def check_date_rollover(self) -> bool:
        """Returns True if the date has changed."""
        today = datetime.now().strftime("%Y-%m-%d")
        if today != self.current_date:
            self.current_date = today
            return True
        return False

    def check_file_changed(self) -> bool:
        """Returns True if the file was modified externally."""
        target_path = self.get_file_path()
        if not target_path.exists():
            return True
        try:
            mtime = target_path.stat().st_mtime
            if mtime > self.last_mtime + 0.1:
                return True
        except Exception:
            pass
        return False
