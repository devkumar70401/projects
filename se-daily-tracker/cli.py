#!/home/dev/SE/Notes/.venv/bin/python3
"""
SE Daily Tracker - Unified CLI & GUI Launcher with Audio Alarms
Usage:
  se                     Launch functional Desktop GUI
  se -cli                Print CLI overview and status in terminal
  se --cli / se cli      Print CLI overview and status in terminal
  se stop / se silence   Stop any currently playing alarm sound
  se monitor             Run background alarm monitor daemon
  se test-sound          Play test sound for work or food
  se tui / --tui         Launch curses terminal interface
  se sync                Run catch-up for past days and check 11 PM cutoff
  se add <work|food> "<start>-<end>" "<task>"
  se tick <work|food> <index>
  se template            View daily template
  se help                Show help
"""

import sys
import os
import time
from datetime import datetime
from pathlib import Path

# Add project dir to sys.path
SCRIPT_DIR = Path(__file__).parent.resolve()
sys.path.insert(0, str(SCRIPT_DIR))


def ensure_x11_env():
    """Ensure DISPLAY and XAUTHORITY are properly set for graphical applications."""
    if "DISPLAY" not in os.environ:
        os.environ["DISPLAY"] = ":0"

    if "XAUTHORITY" not in os.environ or not os.path.exists(os.environ.get("XAUTHORITY", "")):
        uid = os.getuid()
        run_user = Path(f"/run/user/{uid}")
        if run_user.exists():
            matches = list(run_user.glob(".mutter-Xwaylandauth.*")) + list(run_user.glob("*auth*"))
            if matches:
                os.environ["XAUTHORITY"] = str(matches[0])
        if "XAUTHORITY" not in os.environ or not os.path.exists(os.environ.get("XAUTHORITY", "")):
            home_auth = Path.home() / ".Xauthority"
            if home_auth.exists():
                os.environ["XAUTHORITY"] = str(home_auth)


ensure_x11_env()

from tracker import TaskTracker, DAILY_DIR
import audio


def launch_gui():
    ensure_x11_env()
    try:
        from gui import run_gui
        run_gui()
    except Exception as e:
        print(f"Failed to open GUI ({e}). Falling back to CLI status...")
        print_status()


def launch_widget():
    ensure_x11_env()
    try:
        from widget import run_widget
        run_widget()
    except Exception as e:
        print(f"Failed to open Widget ({e}). Falling back to CLI status...")
        print_status()


def launch_tui(force_edit=False):
    from ui import DailyTrackerUI
    DailyTrackerUI(force_edit=force_edit).run()


def print_status():
    tracker = TaskTracker()
    tracker.sync_and_catchup()
    today_str = tracker.get_today_str()
    day_data = tracker.get_day_data(today_str)

    work_items = day_data.get("work", [])
    food_items = day_data.get("food", [])
    work_done = sum(1 for i in work_items if i.get("completed", False))
    food_done = sum(1 for i in food_items if i.get("completed", False))
    total_items = len(work_items) + len(food_items)
    total_done = work_done + food_done
    pct = (total_done / total_items * 100) if total_items > 0 else 0

    edit_allowed, edit_msg = tracker.is_edit_allowed()
    now_time = datetime.now().time()
    snd_cfg = audio.load_sound_config()

    print("=" * 80)
    print(f"  SE DAILY CHECKPOINTS — {today_str} ({datetime.now().strftime('%A')})")
    print(f"  Progress: {total_done}/{total_items} ({pct:.0f}%) | Editing: {'OPEN' if edit_allowed else 'LOCKED'} | Audio Alarms: {'ON' if snd_cfg.get('sound_enabled') else 'OFF'}")
    print("=" * 80)

    # WORK COLUMN
    print(f"\n[ 💼 WORK CHECKPOINTS ] ({work_done}/{len(work_items)} Completed)")
    if not work_items:
        print("  (No work checkpoints scheduled yet. Launch GUI with 'se' to add tasks)")
    for idx, item in enumerate(work_items):
        is_comp = item.get("completed", False)
        in_win, win_stat, _ = tracker.check_time_window(item["time_start"], item["time_end"], now_time)
        mark = "✓" if is_comp else ("●" if in_win else ("○" if win_stat == "upcoming" else "✗"))
        stat = f"DONE @ {item.get('completed_at', '')[:5]}" if is_comp else ("ACTIVE" if in_win else win_stat.upper())
        print(f"  {idx+1}. [{mark}] {item['time_start']}-{item['time_end']:<7}  {item['task']:<35}  [{stat}]")

    # FOOD COLUMN
    print(f"\n[ 🥗 FOOD & NUTRITION ] ({food_done}/{len(food_items)} Completed)")
    if not food_items:
        print("  (No food checkpoints scheduled)")
    for idx, item in enumerate(food_items):
        is_comp = item.get("completed", False)
        in_win, win_stat, _ = tracker.check_time_window(item["time_start"], item["time_end"], now_time)
        mark = "✓" if is_comp else ("●" if in_win else ("○" if win_stat == "upcoming" else "✗"))
        stat = f"DONE @ {item.get('completed_at', '')[:5]}" if is_comp else ("ACTIVE" if in_win else win_stat.upper())
        print(f"  {idx+1}. [{mark}] {item['time_start']}-{item['time_end']:<7}  {item['task']:<35}  [{stat}]")

    print("\n" + "=" * 80)
    print(f"Archive Folder: {DAILY_DIR}/")
    print("Run 'se' to open GUI  |  'se stop' to silence alarms  |  'se help' for commands")
    print("=" * 80)


def run_monitor():
    """Background daemon that monitors intervals and rings alarms when each starts."""
    print("SE Alarm Monitor daemon starting...")
    tracker = TaskTracker()
    while True:
        try:
            triggered = audio.check_and_trigger_alarms(tracker)
            if triggered:
                for t in triggered:
                    print(f"[{datetime.now().strftime('%H:%M:%S')}] 🔔 ALARM: [{t['category'].upper()}] {t['task']} ({t['time_start']} - {t['time_end']})")
        except Exception as e:
            print(f"Monitor error: {e}")
        time.sleep(5)


def print_help():
    print("""
SE Daily Checkpoints & Nutrition Tracker

USAGE:
  se                     Launch Desktop GUI (default)
  se widget | se -w      Launch floating desktop widget (HUD)
  se -cli                Display CLI summary and status in terminal
  se --cli | se cli      Display CLI summary and status in terminal
  se stop | se silence   Stop / Silence currently playing alarm sound
  se monitor             Run background interval alarm monitor
  se test-sound [work|food]
                         Preview configured interval alarm sound
  se gui                 Explicitly launch Desktop GUI
  se tui                 Launch curses full-screen terminal interface
  se status | se list    View today's checkpoints in terminal output
  se sync                Run catch-up for past days and check 11 PM cutoff
  se add <work|food> "<start>-<end>" "<task description>"
                         Example: se add work "14:00-15:30" "Build API endpoints"
  se tick <work|food> <task_number>
                         Example: se tick food 1
  se template            View daily template
  se help                Display this help message

AUDIO ALARMS:
  At the start of each interval (e.g. 12:00 PM for lunch/banana), the system:
  1. Rings/plays your chosen audio or song.
  2. Sends a desktop notification.
  3. Displays a prominent [Stop Alarm] button in the GUI.
""")


def main():
    args = sys.argv[1:]

    if not args:
        launch_gui()
        return

    cmd = args[0].lower()

    if cmd in ("-cli", "--cli", "cli", "-c"):
        print_status()
    elif cmd in ("stop", "silence", "--stop"):
        if audio.stop_sound():
            print("🔕 Alarm stopped.")
        else:
            print("No active alarm was playing.")
    elif cmd in ("monitor", "daemon"):
        run_monitor()
    elif cmd in ("test-sound", "test"):
        cat = "food" if len(args) > 1 and args[1].lower() == "food" else "work"
        cfg = audio.load_sound_config()
        snd_file = cfg.get("sound_food_file" if cat == "food" else "sound_work_file")
        print(f"Playing test sound for {cat.upper()}: {snd_file}")
        ok, msg = audio.play_sound(snd_file, duration_sec=10)
        print(msg)
    elif cmd in ("gui", "--gui"):
        launch_gui()
    elif cmd in ("widget", "--widget", "-w"):
        launch_widget()
    elif cmd in ("tui", "--tui"):
        launch_tui()
    elif cmd in ("status", "list", "ls"):
        print_status()
    elif cmd in ("-h", "--help", "help"):
        print_help()
    elif cmd in ("--force-edit", "-e", "edit"):
        launch_tui(force_edit=True)
    elif cmd in ("sync", "--sync"):
        tracker = TaskTracker()
        results = tracker.sync_and_catchup()
        if results:
            print(f"Sync complete. Processed {len(results)} date(s):")
            for dt, name, reason in results:
                print(f"  - {dt}: {name} ({reason})")
        else:
            today = tracker.get_today_str()
            p = tracker.generate_archive_file(today, is_final=False)
            print(f"Sync complete. Updated today's archive: {p.name}")
    elif cmd == "add":
        if len(args) < 4:
            print("Usage: se add <work|food> \"<start>-<end>\" \"<task>\"")
            sys.exit(1)
        category = args[1].lower()
        if category not in ("work", "food"):
            print("Category must be 'work' or 'food'")
            sys.exit(1)
        time_range = args[2]
        task_desc = args[3]
        if "-" not in time_range:
            print("Time range must be formatted as START-END, e.g. 09:00-10:30")
            sys.exit(1)
        parts = time_range.split("-")
        t_start, t_end = parts[0].strip(), parts[1].strip()
        tracker = TaskTracker()
        ok, msg = tracker.add_task(category, t_start, t_end, task_desc)
        print(msg)
    elif cmd == "tick":
        if len(args) < 3:
            print("Usage: se tick <work|food> <number>")
            sys.exit(1)
        category = args[1].lower()
        if category not in ("work", "food"):
            print("Category must be 'work' or 'food'")
            sys.exit(1)
        try:
            num = int(args[2]) - 1
        except ValueError:
            print("Invalid task number")
            sys.exit(1)
        tracker = TaskTracker()
        ok, msg = tracker.toggle_task(category, num)
        print(msg)
    elif cmd == "template":
        tracker = TaskTracker()
        tpl = tracker.get_template()
        print("=== Work Template ===")
        for item in tpl.get("work", []):
            print(f"  {item['time_start']}-{item['time_end']}: {item['task']}")
        print("\n=== Food Template ===")
        for item in tpl.get("food", []):
            print(f"  {item['time_start']}-{item['time_end']}: {item['task']}")
    else:
        print(f"Unknown command: {cmd}")
        print("Run 'se help' for available commands.")
        sys.exit(1)


if __name__ == "__main__":
    main()
