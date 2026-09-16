# SE Daily Checkpoints & Nutrition Tracker (`se`)

A lightweight, purely functional desktop GUI and CLI utility designed to manage daily time-bounded checkpoints across two distinct categories: **Work** and **Food & Nutrition**.

---

## 🚀 How to Launch

* **Open Desktop GUI**:
  ```bash
  se
  ```
* **Open Floating Desktop Widget (HUD)**:
  ```bash
  se widget
  # or: se -w
  ```
* **View Terminal CLI Status**:
  ```bash
  se -cli
  ```
* **Silence Active Alarm**:
  ```bash
  se stop
  ```

---

## 🔔 Audio Alarms & Sound Customization

1. **Interval Start Alarm**:
   - At the beginning of each scheduled interval (e.g. at **12:00 PM** when your banana/food interval starts), the system:
     - Automatically rings or plays your chosen audio file or song (`.mp3`, `.wav`, `.ogg`, `.oga`, `.flac`).
     - Sends a native desktop notification (`notify-send`).
     - Displays a prominent red **`[ 🔕 STOP SOUND / DISMISS ]`** bar in the GUI.

2. **Choose Custom Songs or Sounds**:
   - Click the **`🔔 Alarm Sounds`** button in the GUI header.
   - Use the **`📂 Choose File from System...`** file picker to select any music track, song, or sound effect stored on your computer.
   - Set custom sounds independently for **Work** and **Food & Nutrition**.
   - Test playback directly in the dialog (`▶️ Test Play` / `⏹️ Stop`).
   - Configure max auto-stop duration (e.g. 15s, 30s, 60s).

3. **Background Alarm Monitor (`se-alarm.service`)**:
   - Even when the GUI is minimized or closed, the background monitor service (`se-alarm.service`) continues watching your checkpoints and plays your chosen sound and notification on time!
   - To silence from terminal at any time, run: `se stop`.

---

## 🖥️ Graphical Interface (GUI) Features

1. **Dual-Column At-A-Glance Layout**:
   - **Column 1: WORK CHECKPOINTS** (Starts blank for each new day).
   - **Column 2: FOOD & NUTRITION** (Retained automatically from your routine).
   - Live status badges: `● ACTIVE NOW`, `○ UPCOMING`, `✗ EXPIRED`, and `✓ Completed @ HH:MM`.

2. **Simple Click-to-Tick**:
   - Click the checkbox next to any task.
   - **Timing Rules**: Only tasks currently in their active window can be completed without override.
   - When all tasks are checked off: automatically generates `✓ YYYY-MM-DD.txt` in `~/SE/Devendra/Daily/`.

3. **Page / Schedule Editing (Toggle Button)**:
   - Click **`✏️ Edit Page / Schedule`** at top right.
   - Editable until 10:00 AM (or check **`Override 10 AM Lock`**).
   - Edit timings, task descriptions, add new checkpoints (`➕ Add New Task`), or delete (`❌`).

4. **11:00 PM & Offline Boot Auto-Archiving**:
   - **11:00 PM (23:00)**: Systemd timer automatically saves the day's summary.
   - If computer is off at 11 PM, boot service (`se-boot.service`) automatically finalizes previous days upon next startup.

---

## 🗗 Floating Desktop Widget (HUD) Features

Designed specifically for **Dual-Monitor setups** to keep your Work and Food schedule in front of your eyes without cluttering your workspace.

* **Always In Front (`📌` Pin Mode)**:
  - Floats above all windows (VSCode, browser, terminals) so you never miss an interval.
  - Click `📌` to toggle Always-On-Top on or off.

* **Dual-Monitor Aware (`🖥️` Switch Monitor)**:
  - Detects both displays (`HDMI-1` and `eDP-1`).
  - Click the **`🖥️ M1 / M2`** button in the header to instantly jump the widget to the top-right corner of the other monitor!
  - Fully draggable: click and drag anywhere on the header bar to place it anywhere across both screens.
  - Automatically remembers its exact screen position, monitor, width, and height in `widget_config.json`.

* **Mini-HUD Mode (`🔼` / `🔽`)**:
  - Click `🔼` to collapse the widget into a compact 90px floating pill showing just the active checkpoint, countdown timer, and quick tick checkbox.
  - Click `🔽` to expand back to the full interactive checklist.

* **Interactive Checklist & Filter Tabs**:
  - Filter by `[ 📋 All ]`, `[ 💼 Work ]`, or `[ 🥗 Food ]`.
  - Click any checkbox to mark a task complete right from the widget.
  - Live highlight on active intervals with real-time countdown (`⏳ 35m left`).
  - In-widget alarm dismissal: when an alarm rings, an alert banner appears with a direct `🔕 Stop Sound` button.
  - `➕ Add` button to quickly add new checkpoints directly from the widget.
  - `🗖` button to instantly launch or bring the full GUI to the front.

