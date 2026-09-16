# Cleaner Capability (Capability-Oriented Architecture)

**Capability Name:** Deep System Cleaner & Storage Purger  
**Architecture Mandate:** [MODULAR_ARCHITECTURE_CONSTITUTION.md](../../MODULAR_ARCHITECTURE_CONSTITUTION.md) (Rule 1, Rule 3, Rule 9)  
**Package:** `com.devendra.healthtracker.cleaner`

---

## 1. Responsibilities & Boundaries

### Owned Responsibilities:
* CCleaner / SD Maid style junk scan and cache calculation across installed applications (`DeepCleanManager`).
* Android Accessibility Service automation for non-root automated "Clear Cache" button clicks (`DeepCleanAccessibilityService`).
* RAM booster & process memory reclaim.
* Safe temporary file deletion without risking user files or violating zero-delete policies.

### Non-Responsibilities:
* Does NOT own hardware thermal sensors or network bandwidth (owned by `telemetry`).
* Does NOT alter application user data.

---

## 2. Directory Layout

```text
features/cleaner/
├── README.md                      # Capability architectural specification
└── contract/
    └── CleanerContract.java       # Public Java API contract
```

---

## 3. Implementation Directory (Android ART)

```text
android/src/com/devendra/healthtracker/cleaner/
├── DeepCleanAccessibilityService.java
└── DeepCleanManager.java
```
