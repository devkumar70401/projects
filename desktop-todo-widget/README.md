# Daily Tasks Desktop Widget

A modern, transparent, interactive desktop to-do widget with automatic daily Markdown archiving and rollover.

---

## ✨ Features

1. **Native Desktop Widget Feel**:
   - Frameless, borderless, and translucent glassmorphic design (`rgba(18, 20, 26, 0.76)`).
   - **Hidden from Alt+Tab** and **hidden from the Ubuntu Dock**.
   - Pinned to the desktop layer behind regular application windows.
   - Click the **`📌`** button in the header if you want to float it **Always on Top**.

2. **Interactive Checklists**:
   - Click any checkbox directly on your desktop to mark tasks as complete.
   - Completed tasks receive smooth strikethrough styling and dimmed opacity.
   - Live progress indicator (e.g. `2/3 (66%)`) and progress bar.
   - Add new tasks directly from the widget input box at the bottom.
   - Delete tasks with the `✕` button on each item.

3. **Daily Markdown Storage**:
   - Location: `/home/dev/SE/Devendra/Daily/YYYY-MM-DD.md`
   - Clean, standard GitHub-flavored markdown:
     ```markdown
     # Daily Tasks — Sunday, 20 September 2026

     ## Today's Tasks
     - [ ] Review system architecture
     - [x] Morning coffee & setup
     - [ ] Implement daily desktop widget
     ```
   - **Two-Way Synchronization**: If you edit today's `.md` file in VS Code or Obsidian, the widget automatically detects the change and updates within 1.5 seconds!

4. **Automatic Daily Rollover & Refresh**:
   - At midnight (or upon starting on a new day), the widget automatically:
     - Archives/saves the previous day's `.md` file.
     - Creates the new daily Markdown file for the new date.
     - Automatically carries forward any uncompleted (`- [ ]`) tasks from yesterday into a `## Carried Over` section so no task is lost.

5. **Draggable & Remembers Position**:
   - Left-click and drag anywhere on the header bar to move the widget anywhere across your monitors.
   - Automatically saves `(x, y, width, height)` in `~/.config/daily-todo-widget/config.json`.

---

## 🚀 Service Controls

The widget runs automatically in the background via systemd user service:

* **Check status**:
  ```bash
  systemctl --user status daily-todo-widget
  ```
* **Restart widget**:
  ```bash
  systemctl --user restart daily-todo-widget
  ```
* **Stop widget**:
  ```bash
  systemctl --user stop daily-todo-widget
  ```
* **Start widget**:
  ```bash
  systemctl --user start daily-todo-widget
  ```
