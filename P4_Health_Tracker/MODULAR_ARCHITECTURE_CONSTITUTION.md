# MODULAR ARCHITECTURE CONSTITUTION

## Capability-Oriented Android Application

You are an agentic software engineer working on a long-lived, high-end Android application. The application is designed as a collection of **independent capabilities built on top of a shared platform/core**.

This document defines the permanent modular architecture philosophy of the project.

You MUST follow these principles whenever you create, modify, refactor, debug, remove, or reorganize any code.

---

## 1. FUNDAMENTAL PHILOSOPHY

Do not treat this application as one large monolithic application containing many screens.

Treat it as:

```text
                    APPLICATION
                         |
              +----------+----------+
              |                     |
            CORE               CAPABILITIES
              |                     |
       Shared infrastructure   Independent domains
```

Examples of capabilities may include:

```text
Calculator
Routine
Cleaner
Digital Wellbeing
Pomodoro
Downloader
Keyboard Lock
```

Future capabilities must be able to be added without requiring unrelated capabilities to be rewritten.

The fundamental objective is:

> **High cohesion within each capability, low coupling between capabilities, and stable shared infrastructure.**

---

# 2. RECOMMENDED STRUCTURE

The project should conceptually follow:

```text
android/
│
├── app/
│
├── core/
│   ├── common/
│   ├── model/
│   ├── ui/
│   ├── design-system/
│   ├── navigation/
│   ├── database/
│   ├── datastore/
│   ├── network/
│   ├── security/
│   └── ...
│
└── features/
    ├── calculator/
    ├── routine/
    ├── cleaner/
    ├── digital-wellbeing/
    ├── pomodoro/
    ├── downloader/
    └── keyboard-lock/
```

The exact structure may evolve, but the architectural principles must remain.

A capability may itself contain:

```text
calculator/
    presentation/
    domain/
    data/
    infrastructure/
    navigation/
    tests/
```

Do not create every directory automatically. Structure should reflect actual complexity.

---

# 3. CAPABILITIES ARE PRODUCT BOUNDARIES

Each capability must represent a coherent user-facing responsibility.

For example:

```text
Calculator
    → owns calculator functionality

Routine
    → owns routine functionality

Downloader
    → owns downloading functionality
```

A capability must have clearly defined:

* Responsibilities
* Non-responsibilities
* Internal implementation
* Data ownership
* Public interfaces
* Dependencies

Before creating a new capability, determine why it deserves to exist as a separate architectural boundary.

---

# 4. CAPABILITIES MUST BE INDEPENDENT

A capability must not directly access another capability's internal implementation.

Avoid:

```text
Calculator → RoutineRepository
Routine → CalculatorViewModel
Cleaner → DownloaderInternalManager
```

Instead, if communication is genuinely necessary, use a clearly defined contract or application-level abstraction.

The rule is:

> **A capability may consume another capability's public contract, but never its private implementation.**

Avoid unnecessary feature-to-feature dependencies entirely.

---

# 5. CORE IS SHARED INFRASTRUCTURE

`core/` exists for functionality genuinely shared by multiple capabilities.

Examples:

```text
Design System
Navigation infrastructure
Database infrastructure
Persistence infrastructure
Networking infrastructure
Security infrastructure
Logging
Common models
Shared utilities with clear responsibility
```

Core must NOT become a dumping ground.

Do not place feature-specific logic into `core` merely because another feature might use it someday.

If only Calculator needs a mathematical parser, it belongs inside Calculator.

If only Routine needs scheduling logic, it belongs inside Routine.

---

# 6. AVOID THE "GOD CORE"

Never allow:

```text
core/
    EverythingManager
    UniversalRepository
    GlobalHelper
    CommonUtils
```

to gradually become responsible for every feature.

The healthier architecture is:

```text
Core
 ↓
shared infrastructure

Features
 ↓
feature-specific business logic
```

Core should remain stable, predictable, and relatively small.

---

# 7. DEPENDENCY DIRECTION

Prefer:

```text
app
 ├── features
 └── core

features
 └── core
```

Avoid:

```text
core → feature
```

Avoid unnecessary:

```text
feature A → feature B → feature C
```

Most importantly, prevent cycles:

```text
Feature A → Feature B
Feature B → Feature A
```

Circular dependencies indicate an architectural problem and must be investigated rather than patched around.

---

# 8. SHARED CODE VS DUPLICATION

Do not extract code into Core merely because two implementations look similar.

Ask:

> Do these components share the same conceptual responsibility?

If yes, a shared abstraction may be appropriate.

If not, keeping two small implementations separate may be healthier.

Remember:

> **Incorrect coupling is often worse than controlled duplication.**

---

# 9. PUBLIC API VS INTERNAL IMPLEMENTATION

Each capability should expose the smallest possible public surface.

Conceptually:

```text
Capability
│
├── Public API / Contract
│
└── Internal implementation
      ├── UI
      ├── Domain
      ├── Data
      └── Infrastructure
```

Other capabilities should not depend on internal classes simply because those classes are technically accessible.

Minimize exposed implementation details.

---

# 10. MODULARITY DOES NOT MEAN MAXIMUM NUMBER OF MODULES

Do not create dozens of Gradle modules simply to appear architecturally sophisticated.

A folder/package boundary and a Gradle module boundary are different concepts.

Use separate Gradle modules when isolation provides meaningful benefits such as:

* dependency control
* independent testing
* build scalability
* ownership
* compile-time boundaries
* reduced coupling

The goal is **useful modularity**, not maximum fragmentation.

---

# 11. POLYGLOT CAPABILITIES (MULTI-LANGUAGE SUPPORT)

Capabilities can be implemented using different programming languages tailored to their domain requirements, provided they strictly honor the public contract boundary:

### Supported Language Profiles:
1. **JVM Languages (Kotlin & Java)**:
   * **Full Native Interoperability**: 100% interoperable on the Android Runtime (ART).
   * Feature A can be written in pure Java, Feature B in pure Kotlin.
   * Direct access to Android APIs, Jetpack Compose, Views, and Android lifecycle.

2. **Native Languages (C, C++, Rust)**:
   * **NDK Integration**: Compiled to native `.so` shared libraries.
   * Ideal for high-performance capabilities (audio/video processing, cryptographic ciphers, math engines).
   * Exposed to the application via JNI / UniFFI contracts.

3. **Interpreted / Scripting Languages (Python, JavaScript, Lua)**:
   * **Embedded Runtime Execution**: Android does not have a native Python interpreter in the OS, so Python capabilities execute via an embedded runtime (e.g. Chaquopy, BeeWare Rubicon, Python C-API, or micro-engine).
   * **Domain vs UI Separation**: Python excels at data parsing, mathematical algorithms, ML, or backend scripting. The Android UI (Views/Compose) and public contract remain in Java/Kotlin, with Python powering the internal domain/data layer.

### The Golden Polyglot Law:
> **The language of a capability is an internal implementation detail. Its public API must expose a standard Android/JVM contract (or clean IPC contract) that the App shell can invoke without language-specific leakage.**

```text
features/my-capability/
├── contract/             # Java/Kotlin interface exposed to App
├── presentation/         # Android UI (Java / Kotlin Compose/View)
├── bridge/               # Language bridge (JNI / Chaquopy / IPC)
└── internal_engine/      # Domain logic in Python / C++ / Rust / Java / Kotlin
```

---

# 12. NEW CAPABILITY PROTOCOL

Whenever a new capability is requested:

1. Define its responsibility.
2. Define what it does NOT own.
3. Identify its dependencies.
4. Identify required shared infrastructure.
5. Keep its implementation inside its own boundary.
6. Define its public entry point/contract.
7. Add it to the application's capability registry/launcher where applicable.
8. Do not modify unrelated capabilities unless technically necessary.
9. Verify that it does not introduce circular dependencies.
10. Document the capability's architectural boundary.

---

# 13. MODIFYING AN EXISTING CAPABILITY

When modifying a capability:

1. Identify its architectural boundary.
2. Inspect its dependencies.
3. Inspect consumers of its public interfaces.
4. Keep implementation changes inside the capability whenever possible.
5. Do not move feature-specific logic into Core for convenience.
6. Do not modify unrelated capabilities unnecessarily.
7. If a shared abstraction must change, analyze every consumer before modifying it.

The preferred principle is:

> **Change the smallest architectural boundary that correctly solves the problem.**

---

# 14. REFACTORING RULE

Never perform a large architectural refactor simply because the code could look cleaner.

Before refactoring, identify:

```text
Current architectural problem
        ↓
Desired architectural property
        ↓
Migration strategy
        ↓
Verification
```

Prefer incremental migration over uncontrolled rewrites when the existing application is already functional.

---

# 15. ARCHITECTURAL DRIFT

Continuously watch for architectural degradation.

Examples:

```text
Feature A imports Feature B internals
Core contains feature-specific business logic
Global utilities contain unrelated functionality
Features share mutable global state
Circular dependencies appear
One module becomes responsible for everything
```

When such drift is detected, report it and recommend a correction.

Do not silently continue making the architecture worse.

---

# 16. FUTURE EXTENSIBILITY

The architecture must make future capabilities easy to introduce.

Ideally, adding:

```text
features/notes/
```

should not require rewriting:

```text
features/calculator/
features/routine/
features/cleaner/
```

Similarly, removing one capability should not destabilize unrelated capabilities.

Design for **controlled evolution**, not hypothetical complexity.

---

# 17. THE CORE QUESTION

Before making any architectural decision, ask:

> **"Who owns this responsibility?"**

Then ask:

> **"Does this responsibility belong to the application, Core, or a specific capability?"**

Then:

> **"What is the smallest dependency required to connect it to the rest of the system?"**

These questions should guide every architectural change.

---

# 18. FINAL ARCHITECTURAL LAW

The application must continuously move toward this model:

```text
                         APPLICATION
                              |
              ┌───────────────┴───────────────┐
              │                               │
             CORE                         CAPABILITIES
              │                               │
      Shared stable infrastructure      Independent domains
              │                               │
              │               ┌───────────────┼───────────────┐
              │               │               │               │
              │          Calculator        Routine         Cleaner
              │
              │               ┌───────────────┼───────────────┐
              │               │               │               │
              │          Wellbeing        Pomodoro       Downloader
              │
              └───────────────────────────────────────────────
```

The guiding principles are:

> **Capabilities own their domains.**

> **Core owns genuinely shared infrastructure.**

> **Capabilities must not depend on each other's internals.**

> **Dependencies must be explicit and intentional.**

> **Circular dependencies are architectural defects.**

> **Shared abstractions must be justified, not assumed.**

> **The number of modules should reflect meaningful boundaries, not architectural vanity.**

> **Every future change must preserve these boundaries.**

The ultimate goal is not merely a clean folder structure.

The goal is a system where **complexity remains contained**.

When the application grows from 5 capabilities to 20 capabilities, the architecture should become larger—but it should not become proportionally more tangled.

A new engineer should be able to understand:

```text
Where does this capability live?
What does it own?
What does it depend on?
What does it expose?
What can safely change without affecting it?
```

If those answers remain clear as the application grows, the modular architecture is succeeding.

**Treat this document as a permanent architectural constitution. Every agentic AI working on this repository must preserve it.**
