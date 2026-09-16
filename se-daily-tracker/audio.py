"""
SE Daily Tracker - Audio Alarm & Notification Engine
Path: /home/dev/SE/Projects/se-daily-tracker/audio.py
"""

import os
import sys
import json
import time
import subprocess
import signal
from pathlib import Path
from datetime import datetime

# Paths
BASE_DIR = Path("/home/dev/SE")
DEVENDRA_DIR = BASE_DIR / "Devendra"
DAILY_DIR = DEVENDRA_DIR / "Daily"
DATA_DIR = DAILY_DIR / ".data"

PID_FILE = DATA_DIR / "alarm.pid"
TRIGGERED_FILE = DATA_DIR / "alarm_triggered.json"
CONFIG_FILE = DATA_DIR / "config.json"

DEFAULT_WORK_SOUND = "/usr/share/sounds/freedesktop/stereo/bell.oga"
DEFAULT_FOOD_SOUND = "/usr/share/sounds/freedesktop/stereo/alarm-clock-elapsed.oga"


def load_sound_config():
    """Loads sound configuration from config.json with safe defaults."""
    cfg = {}
    if CONFIG_FILE.exists():
        try:
            with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                cfg = json.load(f)
        except Exception:
            pass

    defaults = {
        "sound_enabled": True,
        "sound_work_file": DEFAULT_WORK_SOUND,
        "sound_food_file": DEFAULT_FOOD_SOUND,
        "sound_duration_sec": 30,  # Max alarm duration if not stopped
    }
    for k, v in defaults.items():
        if k not in cfg:
            cfg[k] = v
    return cfg


def save_sound_config(cfg):
    """Saves sound configuration to config.json."""
    existing = {}
    if CONFIG_FILE.exists():
        try:
            with open(CONFIG_FILE, "r", encoding="utf-8") as f:
                existing = json.load(f)
        except Exception:
            pass
    existing.update(cfg)
    with open(CONFIG_FILE, "w", encoding="utf-8") as f:
        json.dump(existing, f, indent=2)


def is_sound_playing():
    """Checks if an alarm process is currently playing."""
    if not PID_FILE.exists():
        return False
    try:
        with open(PID_FILE, "r") as f:
            pid = int(f.read().strip())
        os.kill(pid, 0)
        return True
    except (OSError, ValueError):
        # Stale PID file
        try:
            PID_FILE.unlink(missing_ok=True)
        except Exception:
            pass
        return False


def stop_sound():
    """Stops any currently playing alarm sound."""
    if not PID_FILE.exists():
        return False
    try:
        with open(PID_FILE, "r") as f:
            pid = int(f.read().strip())
        # Try graceful terminate then kill process group
        try:
            os.kill(pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
        try:
            PID_FILE.unlink(missing_ok=True)
        except Exception:
            pass
        return True
    except Exception:
        try:
            PID_FILE.unlink(missing_ok=True)
        except Exception:
            pass
        return False


def play_sound(file_path: str, duration_sec: int = 30):
    """
    Plays an audio file (mp3, wav, oga, ogg, flac, etc.) in the background.
    Automatically stops previous audio and caps playback at duration_sec.
    """
    stop_sound()

    if not file_path or not os.path.exists(file_path):
        # Fallback to default
        file_path = DEFAULT_WORK_SOUND

    # Choose player: ffplay can play ANY audio format (mp3, flac, wav, ogg)
    cmd = []
    if file_path.endswith((".oga", ".wav")) and os.path.exists("/usr/bin/pw-play"):
        cmd = ["/usr/bin/pw-play", file_path]
    elif os.path.exists("/usr/bin/ffplay"):
        cmd = ["/usr/bin/ffplay", "-nodisp", "-autoexit", "-loglevel", "quiet", file_path]
    elif os.path.exists("/usr/bin/pw-play"):
        cmd = ["/usr/bin/pw-play", file_path]
    elif os.path.exists("/usr/bin/aplay") and file_path.endswith(".wav"):
        cmd = ["/usr/bin/aplay", "-q", file_path]
    else:
        return False, "No compatible audio player found (pw-play, ffplay, aplay)"

    try:
        # Launch player process
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            preexec_fn=os.setpgrp
        )

        # Write PID
        with open(PID_FILE, "w") as f:
            f.write(str(proc.pid))

        # If duration specified, schedule auto-stop in background thread/subshell
        if duration_sec and duration_sec > 0:
            stopper_cmd = f"sleep {int(duration_sec)} && kill {proc.pid} 2>/dev/null || true"
            subprocess.Popen(stopper_cmd, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

        return True, f"Playing {os.path.basename(file_path)}"
    except Exception as e:
        return False, str(e)


def trigger_interval_alarm(category: str, task_name: str, time_start: str, time_end: str, custom_sound: str = None):
    """
    Fires the alarm for a starting interval:
    1. Plays chosen audio or custom song.
    2. Sends high-priority desktop notification.
    """
    cfg = load_sound_config()
    if not cfg.get("sound_enabled", True):
        return False, "Sound disabled in settings"

    # Select audio file
    audio_file = custom_sound
    if not audio_file or not os.path.exists(audio_file):
        if category == "food":
            audio_file = cfg.get("sound_food_file", DEFAULT_FOOD_SOUND)
        else:
            audio_file = cfg.get("sound_work_file", DEFAULT_WORK_SOUND)

    duration = cfg.get("sound_duration_sec", 30)

    # Play audio
    play_sound(audio_file, duration_sec=duration)

    # Send desktop notification
    icon = "stock_food" if category == "food" else "appointment-soon"
    cat_title = "🥗 FOOD & NUTRITION TIME" if category == "food" else "💼 WORK CHECKPOINT"
    summary = f"🔔 {cat_title}: {task_name}"
    body = f"Scheduled Interval: {time_start} - {time_end}\nTime to take action!"

    try:
        subprocess.Popen([
            "notify-send",
            "-u", "critical",
            "-i", icon,
            summary,
            body
        ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except Exception:
        pass

    return True, f"Alarm triggered for {task_name}"


def get_triggered_records():
    if TRIGGERED_FILE.exists():
        try:
            with open(TRIGGERED_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            return {}
    return {}


def save_triggered_record(trigger_key: str):
    data = get_triggered_records()
    data[trigger_key] = datetime.now().isoformat()
    # Prune records older than 2 days
    cutoff = (datetime.now().date()).strftime("%Y-%m-%d")
    pruned = {k: v for k, v in data.items() if k.startswith(cutoff)}
    with open(TRIGGERED_FILE, "w", encoding="utf-8") as f:
        json.dump(pruned, f, indent=2)


def check_and_trigger_alarms(tracker):
    """
    Checks all checkpoints for today.
    If current time matches time_start of any checkpoint (within the same minute)
    and has not been triggered yet today, rings the alarm.
    Returns list of triggered checkpoints: [{'category', 'task', 'time_start', 'time_end'}].
    """
    today_str = tracker.get_today_str()
    day_data = tracker.get_day_data(today_str, auto_init=False)
    if not day_data:
        return []

    now_hm = datetime.now().strftime("%H:%M")
    triggered_records = get_triggered_records()
    triggered_now = []

    for cat in ("work", "food"):
        items = day_data.get(cat, [])
        for item in items:
            t_start = item.get("time_start", "").strip()
            item_id = item.get("id", item.get("task", ""))
            trigger_key = f"{today_str}_{cat}_{item_id}_{t_start}"

            # If checkpoint starts right now and hasn't triggered yet
            if t_start == now_hm and trigger_key not in triggered_records:
                # Trigger alarm!
                custom_snd = item.get("sound_file")
                trigger_interval_alarm(
                    category=cat,
                    task_name=item.get("task", ""),
                    time_start=t_start,
                    time_end=item.get("time_end", ""),
                    custom_sound=custom_snd
                )
                save_triggered_record(trigger_key)
                triggered_now.append({
                    "category": cat,
                    "task": item.get("task", ""),
                    "time_start": t_start,
                    "time_end": item.get("time_end", "")
                })

    return triggered_now
