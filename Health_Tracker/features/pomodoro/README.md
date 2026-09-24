# Pomodoro Capability (Capability-Oriented Architecture)

**Capability Name:** Pomodoro Focus Timer Engine  
**Architecture Mandate:** [MODULAR_ARCHITECTURE_CONSTITUTION.md](../../MODULAR_ARCHITECTURE_CONSTITUTION.md) (Rule 1, Rule 3, Rule 9)  
**Package:** `com.devendra.healthtracker.pomodoro`

---

## 1. Responsibilities & Boundaries

### Owned Responsibilities:
* Work interval (25m), short break (5m), and long break (15m) countdown finite state machine (`PomodoroManager`).
* Foreground service countdown notification with real-time status updates (`PomodoroService`).
* Exact alarm completion triggers via AlarmManager wake-locks (`PomodoroReceiver`).
* Home screen interactive app widget provider (`PomodoroWidgetProvider`).

### Non-Responsibilities:
* Does NOT store multi-day history (aggregates are dispatched to `wellbeing`).
* Does NOT configure overall application theme (owned by `theme`).

---

## 2. Directory Layout

```text
features/pomodoro/
├── README.md                      # Capability architectural specification
└── contract/
    └── PomodoroContract.java      # Public Java API contract
```

---

## 3. Implementation Directory (Android ART)

```text
android/src/com/devendra/healthtracker/pomodoro/
├── PomodoroManager.java
├── PomodoroReceiver.java
├── PomodoroService.java
└── PomodoroWidgetProvider.java
```
