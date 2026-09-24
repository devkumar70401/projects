# Digital Wellbeing Capability (Capability-Oriented Architecture)

**Capability Name:** Digital Wellbeing & Habit Analytics  
**Architecture Mandate:** [MODULAR_ARCHITECTURE_CONSTITUTION.md](../../MODULAR_ARCHITECTURE_CONSTITUTION.md) (Rule 1, Rule 3, Rule 9)  
**Package:** `com.devendra.healthtracker.wellbeing`

---

## 1. Responsibilities & Boundaries

### Owned Responsibilities:
* `UsageStatsManager` screen time tracking and app engagement analytics (`WellbeingManager`).
* Daily multi-day history state machine with automatic midnight rollover (`WellbeingHistoryManager`).
* Telemetry record serialization and persistence (`DailyWellbeingRecord`).
* Focus minutes tracking combined with Pomodoro focus sessions.

### Non-Responsibilities:
* Does NOT trigger system alarms (owned by `routine`).
* Does NOT execute focus timers (owned by `pomodoro`).

---

## 2. Directory Layout

```text
features/digital-wellbeing/
├── README.md                      # Capability architectural specification
└── contract/
    └── WellbeingContract.java     # Public Java API contract
```

---

## 3. Implementation Directory (Android ART)

```text
android/src/com/devendra/healthtracker/wellbeing/
├── DailyWellbeingRecord.java
├── WellbeingHistoryManager.java
└── WellbeingManager.java
```
