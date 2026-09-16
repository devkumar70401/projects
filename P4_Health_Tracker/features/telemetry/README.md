# Telemetry Capability (Capability-Oriented Architecture)

**Capability Name:** System & Hardware Telemetry Monitor  
**Architecture Mandate:** [MODULAR_ARCHITECTURE_CONSTITUTION.md](../../MODULAR_ARCHITECTURE_CONSTITUTION.md) (Rule 1, Rule 3, Rule 9)  
**Package:** `com.devendra.healthtracker.telemetry`

---

## 1. Responsibilities & Boundaries

### Owned Responsibilities:
* Real-time hardware vitals monitoring: RAM usage, storage breakdown, battery level, battery temperature, battery health (`DeviceTelemetryManager`).
* Per-application active network usage computation via `TrafficStats`.
* Running background processes inspector and process memory calculation.

### Non-Responsibilities:
* Does NOT perform cache cleaning (delegated to `cleaner`).
* Does NOT track daily routine schedules (owned by `routine`).

---

## 2. Directory Layout

```text
features/telemetry/
├── README.md                      # Capability architectural specification
└── contract/
    └── TelemetryContract.java     # Public Java API contract
```

---

## 3. Implementation Directory (Android ART)

```text
android/src/com/devendra/healthtracker/telemetry/
└── DeviceTelemetryManager.java
```
