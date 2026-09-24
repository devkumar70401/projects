# Routine Capability (Capability-Oriented Architecture)

**Capability Name:** Circadian Routine, Granular Block Matrix & Daily Checklist  
**Architecture Mandate:** [MODULAR_ARCHITECTURE_CONSTITUTION.md](../../MODULAR_ARCHITECTURE_CONSTITUTION.md) (Rule 1, Rule 3, Rule 9)  
**Package:** `com.devendra.healthtracker.routine`

---

## 1. Responsibilities & Boundaries

### Owned Responsibilities:
* Pure 14-phase Roman temporal schedule calculation and circadian alignment (`ScheduleManager`).
* Granular routine block decomposition, ingredient isolation, and daily tickmarking (`RoutineBlock`, `RoutineCompletionManager`).
* Exact Android `AlarmManager` wakeup triggers with `WAKE_LOCK` and Doze-bypass (`ReminderReceiver`, `BootReceiver`).
* Foreground audio service executing Stephen Chow's *Li Xianglan* (`AlarmAudioService`).
* Daily checklist persistence, task state machine, and task completion metrics (`ChecklistManager`, `ChecklistItem`).
* System boot auto-reschedule reconciliation.

### Non-Responsibilities:
* Does NOT own hardware or battery telemetry (owned by `telemetry`).
* Does NOT own Pomodoro focus timers (owned by `pomodoro`).

---

## 2. Directory Layout

```text
features/routine/
├── README.md                      # Capability architectural specification
└── contract/
    └── RoutineContract.java       # Public Java API contract
```

---

## 3. Implementation Directory (Android ART)

```text
android/src/com/devendra/healthtracker/routine/
├── AlarmAudioService.java
├── BootReceiver.java
├── ChecklistItem.java
├── ChecklistManager.java
├── ReminderReceiver.java
├── RoutineBlock.java              # Granular routine part & ingredient model
├── RoutineCompletionManager.java  # Daily completion persistence & tickmarking
└── ScheduleManager.java           # Schedule engine & granular block registry
```
