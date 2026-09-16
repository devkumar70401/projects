# Core Platform & Shared Infrastructure Layer

**Architecture Mandate:** [MODULAR_ARCHITECTURE_CONSTITUTION.md](../MODULAR_ARCHITECTURE_CONSTITUTION.md) (Rule 5, Rule 6, Rule 7)

`core/` provides stable, shared infrastructure components utilized across independent application capabilities.

---

## Architectural Principles (Rule 5 & Rule 6)

1. **Strictly Cross-Cutting**: Only place functionality in `core` that is genuinely shared by multiple capabilities.
2. **Avoid "God Core"**: Core must never become a dumping ground. It contains zero feature-specific business logic.
3. **Dependency Direction**:
   * `app` → `features` & `core`
   * `features` → `core`
   * `core` NEVER depends on `features`.

---

## Core Domains

| Domain | Purpose | Implementation Location |
| :--- | :--- | :--- |
| **`design-system`** | Color tokens, typography, spacing, surface hierarchy, touch feedback | `android/src/.../ui/design/` |
| **`ui`** | Reusable premium widgets (Buttons, Steppers, Cards, Switches, Chips) | `android/src/.../ui/components/` |
| **`security`** | Biometrics authentication, hardware security | `android/src/.../biometrics/` |
| **`sound`** | Audio effects, sound chimes, haptics, notification sound preferences | `android/src/.../sound/` |
| **`theme`** | VS Code Dark & Slate Light theme state manager | `android/src/.../theme/` |
| **`features`** | Central capability registry, tile grid, and custom drag-reordering engine | `android/src/.../features/` |
