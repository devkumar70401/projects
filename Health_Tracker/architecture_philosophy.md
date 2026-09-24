# MASTER ARCHITECTURAL CONSTITUTION

## Capability-Oriented, Modular, Scalable Android Application Philosophy

**Document Type:** Permanent Engineering Constitution / Agent Operating Specification
**Target:** Agentic AI software engineers, coding agents, autonomous refactoring agents, debugging agents, code-generation agents, review agents, and future maintainers
**Primary Platform:** Android
**Preferred Language:** Kotlin
**Preferred UI:** Jetpack Compose
**Architectural Philosophy:** Capability-Oriented Modular Architecture
**Quality Target:** Production-grade, maintainable, testable, secure, scalable, observable, evolvable software
**Status:** Permanent project-level engineering law

---

# 0. EXECUTIVE DIRECTIVE

You are not being asked merely to write Android code.

You are being asked to participate in the construction and long-term evolution of a **large, high-quality, capability-oriented Android software system**.

This repository must be treated as a **software platform containing multiple independent capabilities**, not as a collection of unrelated screens and not as one monolithic application whose functionality happens to be divided into folders.

The distinction is fundamental.

The application may contain capabilities such as:

* Routine
* Calculator
* Cleaner
* Digital Wellbeing
* Pomodoro
* Downloader
* Keyboard Lock
* Unit Conversion
* Future capabilities not yet known

Each capability must have a clearly defined responsibility, internal implementation boundary, lifecycle, state model, tests, and user-facing experience.

At the same time, capabilities must share a carefully designed platform layer for genuinely cross-cutting concerns such as:

* Design system
* Navigation infrastructure
* Persistence
* Database
* Logging
* Security
* Permissions
* Notifications
* Background execution
* Networking
* Error handling
* Common domain models
* Configuration
* Application lifecycle
* Testing infrastructure
* Accessibility infrastructure
* Observability
* Feature registration

The goal is to achieve the following property:

> **A new capability should be possible to add without destabilizing unrelated capabilities, and a change to one capability should have the smallest reasonably possible blast radius.**

The architecture must therefore optimize for:

1. Separation of responsibility
2. Explicit dependencies
3. Stable interfaces
4. Low coupling
5. High cohesion
6. Testability
7. Replaceability
8. Evolvability
9. Security
10. Reliability
11. Accessibility
12. Performance
13. Observability
14. Predictable behavior
15. Long-term maintainability

This document is not optional guidance.

Treat it as the **architectural constitution of the repository**.

Whenever you modify this project, you MUST reason according to this constitution.

---

# 1. CORE PHILOSOPHY

The fundamental mental model is:

```text
                    APPLICATION
                         |
            +------------+------------+
            |                         |
        PLATFORM                 CAPABILITIES
            |                         |
     Shared infrastructure      Independent products
            |                         |
     +------+------+       +---------+---------+
     |      |      |       |         |         |
    UI    Data   Security Routine Calculator Cleaner
                              |
                        Digital Wellbeing
                              |
                           Pomodoro
                              |
                          Downloader
```

The application is therefore composed of two conceptual worlds:

## 1.1 PLATFORM

The platform provides infrastructure that multiple capabilities genuinely need.

Examples:

```text
core/
    common/
    model/
    ui/
    design-system/
    navigation/
    datastore/
    database/
    network/
    security/
    permissions/
    notifications/
    background/
    logging/
    observability/
    testing/
```

The platform must remain deliberately small.

It must not become a dumping ground.

---

## 1.2 CAPABILITIES

A capability is a coherent area of product functionality.

Examples:

```text
features/
    routine/
    calculator/
    cleaner/
    digital-wellbeing/
    pomodoro/
    downloader/
    keyboard-lock/
```

Each capability should own its own implementation.

For example:

```text
feature/calculator/
    presentation/
    domain/
    data/
    infrastructure/
    tests/
```

and:

```text
feature/routine/
    presentation/
    domain/
    data/
    infrastructure/
    tests/
```

The exact directory organization may evolve, but the **architectural boundary must remain**.

---

# 2. THE MOST IMPORTANT RULE

## NEVER CONFUSE FOLDER STRUCTURE WITH ARCHITECTURE

Creating:

```text
calculator/
routine/
cleaner/
```

does not automatically create modular architecture.

True modularity requires:

* dependency boundaries
* ownership boundaries
* API boundaries
* state boundaries
* data boundaries
* testing boundaries
* lifecycle boundaries
* security boundaries
* build boundaries where appropriate
* clear contracts
* controlled communication

The agent must therefore inspect both:

1. Physical project structure
2. Logical dependency structure

A beautiful folder tree with unrestricted dependencies is still a monolith.

---

# 3. CAPABILITY DEFINITION

A capability is a product-level unit of responsibility.

A capability should answer:

> "What meaningful user problem does this subsystem solve?"

Examples:

### Calculator

Responsible for:

* expression evaluation
* mathematical operations
* scientific calculations
* programming calculations
* unit calculations if explicitly part of the calculator domain
* calculator history
* calculator-specific settings

It should NOT own:

* global application theme
* global navigation infrastructure
* unrelated notification infrastructure
* another feature's database schema
* application-wide logging implementation

### Routine

Responsible for:

* routine creation
* scheduling
* routine state
* routine completion
* routine-specific reminders
* routine history

It should NOT directly manipulate calculator state.

### Digital Wellbeing

Responsible for:

* wellbeing-related functionality
* usage information relevant to the capability
* user-defined limits/settings
* capability-specific reports

It should not become a general-purpose system manager.

---

# 4. CAPABILITY BOUNDARIES

Every capability must have a clearly defined boundary.

The agent MUST be able to answer:

1. What does this capability own?
2. What does it not own?
3. What data does it own?
4. Which dependencies does it require?
5. Which external interfaces does it expose?
6. Which capabilities can depend on it?
7. Which capabilities must not depend on it?
8. Which platform modules does it consume?
9. What lifecycle does it have?
10. What happens when it fails?
11. What happens when it is unavailable?
12. What permissions does it require?
13. What background work does it perform?
14. What state does it persist?
15. How is that state migrated?
16. How is the capability tested independently?

If these answers are unclear, the capability boundary is not sufficiently defined.

---

# 5. HIGH COHESION, LOW COUPLING

The architectural target is:

> **High cohesion inside capabilities, low coupling between capabilities.**

A capability should contain things that naturally belong together.

Bad:

```text
calculator/
    random_global_helper.kt
    notification_manager.kt
    routine_repository.kt
    theme_manager.kt
    calculator.kt
```

Good:

```text
calculator/
    presentation/
    domain/
    data/
    infrastructure/
```

The exact names can differ, but responsibility must remain coherent.

---

# 6. DEPENDENCY DIRECTION

Dependencies must be deliberate.

Prefer:

```text
Feature
   |
   +----> Core
```

Avoid:

```text
Feature A
   |
   +----> Feature B
              |
              +----> Feature C
```

unless the dependency is explicitly justified.

Avoid:

```text
Feature A <----> Feature B
```

especially mutual dependencies.

Avoid:

```text
Core ---> Feature
```

because this reverses the intended ownership model and causes architectural contamination.

The preferred relationship is:

```text
Application
    |
    +----> Features
    |
    +----> Core

Features
    |
    +----> Core

Core
    |
    +----> Platform/external dependencies
```

The agent must continuously watch for dependency inversion violations.

---

# 7. FEATURE-TO-FEATURE COMMUNICATION

Features will occasionally need to communicate.

Do NOT solve this by allowing arbitrary direct access.

Bad:

```kotlin
calculatorRepository.internalDatabase.query(...)
```

from another feature.

Bad:

```kotlin
routineViewModel.calculatorSomething(...)
```

Bad:

```kotlin
cleaner.internalManager.resetRoutineState()
```

Instead, use explicit contracts.

Possible approaches include:

```text
interfaces
events
navigation contracts
application-level coordinators
shared domain contracts
well-defined service abstractions
```

The appropriate mechanism depends on the use case.

The principle is:

> **A capability may consume a capability's public contract, but must never depend on another capability's private implementation.**

---

# 8. PUBLIC API VS INTERNAL IMPLEMENTATION

Each capability should conceptually have:

```text
PUBLIC
    contracts
    entry points
    navigation destination
    required interfaces

PRIVATE
    ViewModels
    repositories
    database implementation
    internal services
    implementation details
```

The agent must avoid exposing internal classes unnecessarily.

Prefer:

```kotlin
interface CalculatorLauncher
```

over exposing:

```kotlin
InternalCalculatorRepositoryImpl
```

to the entire application.

Use the narrowest useful interface.

---

# 9. DO NOT CREATE "GOD SERVICES"

A common architectural failure is a class such as:

```text
AppManager
ApplicationManager
UtilityManager
SystemManager
FeatureManager
CommonManager
GlobalManager
```

containing hundreds of unrelated responsibilities.

This is forbidden unless the class genuinely represents one coherent abstraction.

Instead of:

```text
AppManager
    calculate()
    clean()
    download()
    schedule()
    notify()
    saveRoutine()
    changeTheme()
    lockKeyboard()
```

separate responsibilities.

The system should have focused components.

---

# 10. THE CORE MODULE MUST NOT BECOME A GARBAGE DRAWER

This is one of the highest-risk architectural pitfalls.

Developers often begin with:

```text
core/
    Utils.kt
```

Then:

```text
core/
    Utils.kt
    Helpers.kt
    Common.kt
    Manager.kt
```

Eventually:

```text
core/
    800 files
```

and every feature depends on it.

This creates a hidden monolith.

Therefore:

> A component belongs in `core` only if multiple capabilities genuinely require the same abstraction and the abstraction is stable enough to justify centralization.

If only one feature uses something, keep it inside that feature.

Do not centralize merely because something "might be useful later."

---

# 11. AVOID PREMATURE ABSTRACTION

Do not create:

```text
BaseCalculatorRepository
AbstractFeatureManager
GenericCapabilityEngine
UniversalDataProvider
GenericUseCase
BaseViewModelFactory
```

simply because abstraction appears sophisticated.

Abstraction must solve a demonstrated problem.

Prefer:

> Simple and correct first.

Then:

> Abstract where repetition, variation, or dependency boundaries justify abstraction.

A high-end architecture is not one containing the maximum number of interfaces.

It is one containing the **right abstractions**.

---

# 12. SHARED DESIGN SYSTEM

All capabilities must visually belong to the same product.

Create a centralized design system.

Conceptually:

```text
core/design-system/
    typography/
    colors/
    spacing/
    shapes/
    elevation/
    components/
    icons/
    motion/
    interaction/
```

The design system should define:

* typography
* spacing
* colors
* semantic colors
* shapes
* corner radii
* elevation
* borders
* icon sizing
* touch targets
* animations
* interaction states
* buttons
* toggles
* cards
* dialogs
* sheets
* fields
* segmented controls
* loading states
* error states

Capabilities may compose these components but should not independently reinvent the application's visual language.

---

# 13. DESIGN SYSTEM DOES NOT MEAN VISUAL UNIFORMITY

The goal is not for every capability to look identical.

The goal is:

```text
same product language
+
capability-specific expression
```

For example:

Calculator may require:

* dense keypad
* numeric display
* precision controls

Routine may require:

* scheduling interfaces
* timeline components

Digital Wellbeing may require:

* charts
* summaries

All can still share:

* typography
* semantic colors
* spacing
* touch behavior
* animation principles
* accessibility standards
* button language

---

# 14. THEME ARCHITECTURE

Light and dark mode must be centralized.

Capabilities must not independently decide:

```text
if dark -> black
else -> white
```

Instead use semantic tokens.

For example:

```text
backgroundPrimary
backgroundSecondary
surface
surfaceElevated
textPrimary
textSecondary
border
accent
success
warning
error
```

The exact implementation may use Compose Material theming or another suitable design-system mechanism.

The important rule:

> **Capabilities consume semantic design tokens. They do not hard-code application-wide colors.**

Dark mode must be tested across every capability.

---

# 15. ICONOGRAPHY

Icons should be part of the design system or feature registry.

Each capability should have:

* stable icon identity
* accessible content description
* appropriate visual weight
* consistent sizing
* light/dark compatibility

Avoid random icon selection.

Avoid mixing incompatible icon styles.

Avoid using icons merely because they "look cool."

Icons must communicate function.

---

# 16. MAIN APPLICATION HOME

The main screen should be treated as a **capability launcher**, not as a container of capability implementations.

Conceptually:

```text
Home
    |
    +---- Capability Registry
             |
             +---- Calculator
             +---- Routine
             +---- Cleaner
             +---- Digital Wellbeing
             +---- Pomodoro
             +---- Downloader
```

The home screen should know:

* capability ID
* display name
* icon
* destination
* availability
* ordering
* visibility

It should NOT know:

* calculator algorithms
* routine database implementation
* downloader internals
* cleaner implementation details

---

# 17. CAPABILITY REGISTRY

Where appropriate, maintain a centralized capability registry.

Example conceptual model:

```kotlin
data class CapabilityDescriptor(
    val id: CapabilityId,
    val title: String,
    val icon: CapabilityIcon,
    val destination: CapabilityDestination,
    val defaultPriority: Int,
    val availability: CapabilityAvailability
)
```

The registry must use **stable IDs**.

Never use:

* array position
* display name
* icon name
* localized string

as the persistent identity of a capability.

Good:

```text
calculator
routine
cleaner
digital_wellbeing
pomodoro
downloader
```

The IDs must remain stable across releases.

---

# 18. USER-ORDERABLE CAPABILITIES

If users can rearrange capabilities on the home screen, persist their ordering using stable IDs.

Bad:

```text
position = 0 means Calculator
```

Good:

```text
[
    "routine",
    "calculator",
    "pomodoro",
    "cleaner"
]
```

The system must handle:

* new capabilities
* removed capabilities
* renamed capabilities
* disabled capabilities
* corrupted ordering
* duplicate IDs
* missing IDs
* old persisted data
* future migrations

When a new capability is introduced:

1. Preserve existing user order.
2. Add the new capability using a deterministic fallback priority.
3. Never unexpectedly reshuffle everything.

---

# 19. CAPABILITY LIFECYCLE

Every capability should have a predictable lifecycle.

Consider:

```text
NotInitialized
      |
      v
Initializing
      |
      v
Ready
      |
      +----> Running
      |
      +----> Error
      |
      +----> Disabled
```

The actual states may differ.

The important thing is that lifecycle behavior is explicit.

Do not allow lifecycle behavior to emerge accidentally from random callbacks.

---

# 20. STATE MANAGEMENT

State should have a clear owner.

Ask:

> Who owns this state?

Examples:

Application state:

```text
theme
global settings
capability order
```

Capability state:

```text
calculator expression
routine completion state
pomodoro timer state
```

Temporary UI state:

```text
dialog open
selected tab
expanded section
```

Do not put everything into one global state object.

Do not put application state inside a random feature ViewModel.

Do not duplicate the same state across multiple layers without a synchronization strategy.

---

# 21. SINGLE SOURCE OF TRUTH

When state represents persistent domain truth, there should be a clear authoritative source.

Avoid:

```text
Database says A
DataStore says B
ViewModel says C
UI says D
```

without explicit synchronization rules.

Prefer:

```text
Persistent source
      |
Repository
      |
Domain
      |
UI state
      |
UI
```

The exact architecture may differ, but truth ownership must be explicit.

---

# 22. DATA OWNERSHIP

Each capability should own its domain data whenever possible.

Example:

```text
Calculator
    calculator_history

Routine
    routines
    routine_completion

Digital Wellbeing
    wellbeing_settings
    wellbeing_records
```

Shared database infrastructure may exist, but schema ownership should remain understandable.

Avoid one gigantic database repository:

```text
UniversalRepository
```

that knows every table in the application.

---

# 23. DATABASE DESIGN

If Room or another persistence system is used:

* separate entities by responsibility
* keep migrations explicit
* test migrations
* avoid destructive migrations in production unless intentional
* validate schema changes
* consider transaction boundaries
* avoid leaking DAO implementation into unrelated capabilities

Every schema change must trigger:

1. migration analysis
2. backward compatibility analysis
3. data-loss analysis
4. migration testing
5. upgrade testing

Never casually delete or rename a persisted field.

---

# 24. PERSISTENCE IS A CONTRACT

Persistent data survives code.

Therefore:

> Changing code is easy. Changing stored user data is dangerous.

Any agent modifying persistence must ask:

* What versions already exist?
* What happens to existing users?
* Is migration required?
* Can old data be read?
* What if migration fails?
* Can migration be retried?
* Is data loss possible?
* Is rollback possible?
* Are there tests?

---

# 25. NAVIGATION

Navigation should be treated as infrastructure.

Features should define their entry points without exposing internal UI implementation unnecessarily.

Avoid scattering raw navigation route strings everywhere.

Prefer stable destinations/contracts.

Navigation changes must be checked for:

* deep links
* back stack
* process recreation
* configuration changes
* state restoration
* accessibility
* external entry
* unavailable capability
* authentication/permission requirements if applicable

---

# 26. BACKGROUND WORK

Capabilities such as:

* Pomodoro
* Downloader
* Cleaner
* Digital Wellbeing
* scheduled routines

may require background execution.

Do not implement background work merely by launching arbitrary threads.

Use appropriate Android mechanisms.

Evaluate:

* lifecycle
* WorkManager
* foreground services where genuinely necessary
* alarms where appropriate
* notifications
* battery constraints
* OS restrictions
* process death
* reboot behavior
* cancellation
* retry policies
* persistence

Never assume:

> "The app process will remain alive."

It will not.

---

# 27. TIMER ARCHITECTURE

Timers must never depend purely on UI recomposition or counting loop iterations.

Bad:

```text
while(true) {
    delay(1000)
    remaining--
}
```

as the authoritative time source.

Instead reason from actual time.

Conceptually:

```text
targetEndTime - currentTime = remainingTime
```

This makes the timer resilient to:

* app backgrounding
* process interruption
* delayed execution
* device sleep
* scheduling jitter
* UI recreation

The same principle applies to Pomodoro.

---

# 28. NEVER TRUST WALL CLOCK BLINDLY

For timing-sensitive systems, distinguish between:

* wall-clock time
* elapsed/monotonic time

The agent must choose the appropriate clock.

Wall clock can change because of:

* user modification
* network synchronization
* timezone changes
* daylight-saving transitions
* system adjustments

Timer correctness must therefore be reasoned about explicitly.

---

# 29. SECURITY ARCHITECTURE

Security is not a feature that gets added at the end.

Each capability must be threat-modeled where appropriate.

Consider:

* untrusted input
* malicious files
* malformed URLs
* permissions
* exported Android components
* intents
* deep links
* IPC
* content providers
* storage
* network communication
* secrets
* credentials
* logs
* clipboard
* temporary files
* downloaded files
* path traversal
* injection
* denial of service
* excessive resource consumption

---

# 30. CAPABILITY SECURITY BOUNDARIES

A capability should request only permissions it genuinely requires.

Never request broad permissions merely because they might be useful later.

For every permission ask:

1. Why is it necessary?
2. Which capability needs it?
3. When is it requested?
4. Can functionality work without it?
5. What happens if denied?
6. What happens if revoked later?
7. Is there a safer alternative?

Permissions must be capability-specific whenever possible.

---

# 31. STORAGE SECURITY

Never trust filenames or paths originating from:

* URLs
* users
* downloaded content
* external applications
* metadata

Sanitize appropriately.

Prevent:

* path traversal
* illegal filenames
* collisions
* unexpected extensions
* MIME confusion

Do not load enormous files entirely into memory.

Use streaming for large media.

---

# 32. NETWORKING

Network operations must be isolated from UI.

Avoid:

```text
Composable
    |
    +----> HTTP request
```

Prefer:

```text
UI
 |
ViewModel
 |
Use Case
 |
Repository
 |
Network/Data Source
```

The exact layers can be simplified where justified.

Networking must account for:

* timeout
* cancellation
* retry
* backoff
* offline state
* malformed response
* partial response
* HTTP errors
* TLS
* redirects
* large payloads
* resource exhaustion

---

# 33. DOWNLOADER CAPABILITY

A downloader is particularly security-sensitive.

Treat remote media as untrusted.

Do not assume:

```text
URL extension == actual file type
```

Validate content appropriately.

Use:

* streaming
* bounded resources
* safe temporary files
* atomic finalization
* cancellation
* resume where technically supported
* integrity checks where appropriate
* clear state transitions

Never implement DRM, authentication, paywall, or access-control bypasses.

The architecture should support lawful and authorized downloads.

---

# 34. CLEANER CAPABILITY

A "cleaner" must not be given unrestricted authority simply because it is called Cleaner.

The agent must distinguish between:

* safe temporary data
* cache
* user-created files
* application-owned files
* system-owned files
* protected data

Never delete something merely because it looks old or large.

Deletion must be:

* explicit
* explainable
* scoped
* reversible where practical
* permission-aware
* user-confirmed when destructive

---

# 35. DIGITAL WELLBEING

Digital Wellbeing functionality may require sensitive system APIs and permissions.

The capability must:

* request minimum necessary access
* clearly explain why
* degrade gracefully if access is unavailable
* avoid pretending to have data it cannot obtain
* respect Android platform restrictions
* avoid hidden collection

The capability must not become a global permission owner.

---

# 36. CALCULATOR CAPABILITY

The calculator should remain internally sophisticated without contaminating the rest of the application.

A high-end calculator may contain:

```text
Expression Parser
Numerical Engine
Scientific Functions
Complex Numbers
Matrices
Vectors
Statistics
Programming Arithmetic
Unit Conversion
Constants
Precision Management
Formatting
History
```

These should be internally organized.

Do not put calculator algorithms into `core/common`.

---

# 37. COMMON FUNCTIONALITY

Only move functionality to shared modules when it is genuinely shared.

Good candidates:

```text
Result<T>
Error model
Logger
Date/time abstraction
Design tokens
Navigation contracts
Permission abstractions
Database infrastructure
Network infrastructure
Security utilities
```

Bad candidates:

```text
CalculatorUtils
RoutineUtils
FeatureUtils
UniversalHelper
Misc
```

---

# 38. ERROR HANDLING

Errors must have ownership.

Do not throw arbitrary exceptions from deep layers and expect UI to interpret them.

Establish a coherent strategy.

For example:

```text
Infrastructure error
       ↓
Domain-relevant error
       ↓
Presentation state
       ↓
User-facing message
```

Do not expose:

* stack traces
* internal SQL errors
* server internals
* filesystem paths
* secrets

to users.

Logs may contain diagnostic details, but must also be privacy-conscious.

---

# 39. RESULT TYPES

Where appropriate, prefer explicit outcomes.

Conceptually:

```kotlin
sealed interface OperationResult
```

with states such as:

```text
Success
Failure
Cancelled
Unavailable
PermissionRequired
```

Do not overuse sealed classes where ordinary exceptions or return values are more appropriate.

Use explicit state where the state is meaningful.

---

# 40. OBSERVABILITY

High-end software must be diagnosable.

Provide appropriate:

* structured logging
* error reporting
* performance measurement
* state transition visibility
* background-work diagnostics
* crash diagnostics

But never log:

* passwords
* tokens
* private user content
* unnecessary personal data
* authentication credentials

Observability must respect privacy.

---

# 41. TESTABILITY

Every capability must be independently testable.

At minimum consider:

```text
Unit tests
Integration tests
UI tests
Navigation tests
Persistence tests
Migration tests
Permission behavior tests
Failure tests
Lifecycle tests
```

For mathematically intensive capabilities:

```text
property-based tests
invariant tests
reference-value tests
boundary tests
precision tests
```

For timers:

```text
state transition tests
time calculation tests
pause/resume tests
process recreation tests
clock-change tests
```

For downloader:

```text
network failure
partial download
cancel
resume
retry
invalid response
large file
storage failure
```

---

# 42. TEST THE ARCHITECTURE ITSELF

Do not merely test functionality.

Test architectural assumptions.

Examples:

* feature A cannot access feature B internals
* core cannot depend on features
* capability IDs remain stable
* migrations preserve existing data
* disabled capabilities do not crash the home screen
* corrupted ordering is recovered safely
* theme applies globally
* new capabilities do not destroy user ordering

Architecture should have enforceable rules where possible.

---

# 43. DEPENDENCY RULES SHOULD BE MACHINE-CHECKABLE

If practical, introduce build checks or dependency-analysis mechanisms.

The goal is to make violations fail early.

For example:

```text
core -> feature
```

should be rejected.

Likewise, cyclic feature dependencies should be detected.

Do not rely entirely on developers remembering architectural rules.

Automate what can reasonably be automated.

---

# 44. CYCLIC DEPENDENCIES

Cycles are architectural warning signs.

Bad:

```text
Routine -> Pomodoro
Pomodoro -> Routine
```

If this happens, stop.

Determine the underlying shared abstraction.

Perhaps:

```text
Routine -> Core Scheduling Contract
Pomodoro -> Core Scheduling Contract
```

or an application-level coordinator is appropriate.

Do not solve cycles by creating an enormous "Common" module.

---

# 45. MODULE DEPENDENCY GRAPH

Maintain a mental or documented graph.

Ideal:

```text
                 :app
                /    \
               /      \
        :feature:*    :core:*
             \          /
              \        /
               \      /
               platform
```

Avoid:

```text
Feature A ---> Feature B ---> Feature C ---> Feature A
```

The dependency graph should remain understandable by a new engineer.

If the graph cannot be explained simply, architecture may be becoming too complicated.

---

# 46. MODULARIZATION HAS A COST

Do not create 100 Gradle modules merely to claim modularity.

Every module creates:

* build configuration
* dependency management
* API surface
* maintenance
* cognitive overhead
* build complexity

Use module boundaries where they provide meaningful value.

A capability should become a separate Gradle module when its isolation, build independence, ownership, testing, or scalability justify it.

The architecture should optimize for **useful modularity**, not maximum module count.

---

# 47. FOLDER-LEVEL VS GRADLE-MODULE-LEVEL

The agent must distinguish:

```text
folder organization
```

from:

```text
Gradle module isolation
```

A feature can initially be organized by package/folder.

As the system grows, it can become:

```text
:feature:calculator
:feature:routine
```

The transition should be deliberate.

Do not perform massive modularization without analyzing:

* dependency graph
* build impact
* API boundaries
* testing
* migration risk

---

# 48. PACKAGE NAMING

Use predictable package names.

For example:

```text
com.example.app
com.example.app.core
com.example.app.feature.calculator
com.example.app.feature.routine
```

Avoid meaningless packages such as:

```text
misc
stuff
temp
helpers
new
old
final
final2
```

Names communicate architecture.

---

# 49. DOMAIN LAYER

A domain layer should contain actual business rules where those rules are meaningful.

Do not create a domain layer merely because a textbook says every feature needs one.

For a sophisticated calculator, domain separation is valuable.

For a trivial static settings screen, it may be unnecessary.

Use architecture proportionally.

---

# 50. VIEWMODEL RESPONSIBILITY

ViewModels should coordinate presentation state.

Avoid putting:

* SQL logic
* HTTP implementation
* complex mathematical algorithms
* filesystem manipulation
* arbitrary global state

directly into ViewModels.

The ViewModel should not become the feature's entire backend.

---

# 51. COMPOSABLE RESPONSIBILITY

Compose UI should primarily:

* render state
* emit user events
* display loading/error/success
* manage appropriate local UI state

Avoid putting significant business logic inside composables.

Bad:

```text
Composable
    -> database
    -> network
    -> file system
    -> business algorithm
```

Good:

```text
Composable
    -> event
ViewModel
    -> use case
Domain/data
    -> result
UI state
    -> Composable
```

---

# 52. UNIDIRECTIONAL DATA FLOW

Prefer predictable flow:

```text
User Action
    ↓
Event
    ↓
ViewModel / State Holder
    ↓
Use Case
    ↓
Repository
    ↓
Data Source
    ↓
Result
    ↓
State
    ↓
UI
```

The architecture should avoid circular hidden state updates.

---

# 53. REACTIVE STATE

Use appropriate reactive mechanisms such as Kotlin Flow/StateFlow where justified.

Do not create endless observers.

Every collector must have an appropriate lifecycle.

Avoid memory leaks.

Avoid collecting application-wide streams from short-lived UI objects without lifecycle awareness.

---

# 54. COROUTINES

Coroutines must have explicit ownership.

Avoid:

```kotlin
GlobalScope.launch
```

or equivalent uncontrolled background work.

Prefer structured concurrency.

Every coroutine should have a meaningful lifecycle.

Cancellation must propagate correctly.

The agent must test cancellation paths.

---

# 55. THREADING

Never assume code runs on the correct thread.

Reason about:

* main thread
* IO
* CPU-intensive work
* database thread
* network
* background work

Do not move everything to background threads blindly.

Do not perform expensive work on the main thread.

Do not use background threads merely for trivial operations.

---

# 56. RESOURCE OWNERSHIP

Every resource should have an owner and cleanup strategy.

Resources include:

* streams
* files
* database transactions
* sockets
* workers
* callbacks
* listeners
* receivers
* services
* coroutine scopes

Ask:

> Who creates it?

> Who owns it?

> Who cancels it?

> Who closes it?

> What happens if the owner dies?

---

# 57. MEMORY MANAGEMENT

Large capabilities such as Downloader, Cleaner, Calculator graphing, or data analysis must avoid unnecessary memory usage.

Never load huge resources entirely into RAM unless there is a proven reason.

Consider:

* streaming
* pagination
* lazy loading
* bounded caches
* lifecycle-aware caches
* cancellation

Watch for:

* retained Activity references
* long-lived Context references
* static mutable objects
* unbounded collections
* coroutine leaks
* image memory leaks

---

# 58. PERFORMANCE

Performance must be measured, not guessed.

Avoid premature optimization.

But do not knowingly introduce:

* O(n²) algorithms for huge datasets
* blocking IO on UI
* unnecessary recomposition
* repeated expensive calculations
* unbounded database queries
* excessive object creation
* unnecessary network requests

For the calculator, numerical correctness takes priority over superficial micro-optimizations.

---

# 59. OFFLINE-FIRST THINKING

Where practical, capabilities should degrade gracefully without connectivity.

For example:

Calculator:

```text
fully offline
```

Routine:

```text
mostly offline
```

Downloader:

```text
network required for retrieval
```

Do not make the entire application dependent on a network connection if the capability doesn't require it.

---

# 60. CAPABILITY AVAILABILITY

A capability may be:

```text
Available
Unavailable
PermissionRequired
PlatformUnsupported
Disabled
TemporarilyUnavailable
```

The home screen must handle these states safely.

A capability should not crash the application because an optional dependency is unavailable.

---

# 61. OPTIONAL CAPABILITIES

Future architecture may allow optional capabilities.

The system should ideally support:

```text
Capability A installed
Capability B installed
Capability C disabled
```

without breaking the launcher.

Do not assume all capabilities always exist.

---

# 62. FEATURE FLAGS

If feature flags are used, define ownership and lifecycle.

Avoid permanent flags such as:

```text
enableNewCalculator2
```

that survive for years.

Flags must have:

* owner
* purpose
* default
* rollout strategy
* removal plan

---

# 63. VERSIONING

Architectural contracts should be treated as versioned assets.

When changing a public capability contract, ask:

* Who consumes it?
* Is the change breaking?
* Can it be backward compatible?
* Does navigation change?
* Does persisted data change?
* Does deep linking change?

Never modify public APIs casually.

---

# 64. BACKWARD COMPATIBILITY

The agent must consider users who are upgrading from previous versions.

Never assume a clean installation.

Real users may have:

* old database schemas
* old settings
* old capability order
* deleted permissions
* interrupted downloads
* stale files
* partially completed operations

Every change must consider migration.

---

# 65. MIGRATION PHILOSOPHY

For every migration:

```text
Old State
   ↓
Migration
   ↓
New State
```

The transformation must be:

* deterministic
* testable
* repeatable where appropriate
* safe
* observable

If migration fails, define the recovery strategy.

---

# 66. USER DATA IS SACRED

Never silently destroy user data during:

* refactoring
* schema changes
* feature removal
* reinstall-like operations
* cleanup
* migration

If data deletion is intentional, make it explicit.

---

# 67. REMOVING A CAPABILITY

Removing a capability requires more than deleting its UI.

The agent must inspect:

* navigation
* registry
* database
* migrations
* settings
* notifications
* background jobs
* workers
* permissions
* deep links
* tests
* analytics
* references
* dependency graph

Existing user data must be handled deliberately.

---

# 68. RENAMING A CAPABILITY

Display names may change.

Stable IDs generally should not.

For example:

```text
ID:
calculator

Display:
Calculate
```

Changing:

```text
Calculator -> Calculate
```

must not cause persisted ordering to break.

---

# 69. USER CUSTOMIZATION

User customization must not modify architectural identity.

The user may change:

* order
* visibility
* theme
* preferences

but must not alter internal capability contracts.

Persistent preferences should be versioned where necessary.

---

# 70. ACCESSIBILITY

Every capability must support:

* screen readers
* semantic labels
* sufficient contrast
* keyboard navigation where relevant
* large text
* touch targets
* focus states
* reduced motion where applicable

Accessibility is not an optional polish stage.

---

# 71. TOUCH TARGETS

Every interactive target must be comfortably usable.

Do not make visually minimal controls so small that they become difficult to tap.

Premium UI means:

```text
minimal visual footprint
+
comfortable interaction area
```

not:

```text
tiny controls
```

---

# 72. MOTION

Animations must communicate:

* transition
* hierarchy
* feedback
* state change

Avoid animation for decoration alone.

Animations must respect:

* reduced motion preferences
* performance
* lifecycle
* interrupted transitions

---

# 73. UI STATE COMPLETENESS

Every important screen should consider:

```text
Loading
Content
Empty
Error
Unavailable
Permission required
Success
Refreshing
Offline
```

Do not design only the happy path.

---

# 74. EMPTY STATES

An empty state must explain:

1. What is empty?
2. Why?
3. What can the user do next?

Avoid blank screens.

---

# 75. ERROR STATES

Errors must be:

* understandable
* actionable
* non-destructive
* honest

Never display:

```text
Something went wrong!!!
```

when a useful explanation is available.

---

# 76. NO FAKE FUNCTIONALITY

The agent must never create a UI that appears functional when the backend capability does not exist.

Forbidden patterns:

```text
button -> fake success
download -> fake progress
clean -> pretend deletion
wellbeing -> fabricated statistics
calculator -> approximate result presented as exact
```

If something is not implemented, say so.

---

# 77. SCIENTIFIC CORRECTNESS

For mathematically intensive capabilities, correctness is more important than visual polish.

Do not invent formulas.

Use:

* established mathematical definitions
* reference implementations
* high-precision test vectors
* boundary tests
* invariants
* property-based testing

Floating-point behavior must be understood.

Do not claim arbitrary precision if the implementation is not arbitrary precision.

---

# 78. NUMERICAL PRECISION

The calculator must distinguish between:

```text
display precision
```

and:

```text
internal computation precision
```

Do not round intermediate values unnecessarily.

For example:

```text
display = 2.35
internal = higher precision
```

where appropriate.

Document rounding behavior.

---

# 79. SECURITY VS CONVENIENCE

When security and convenience conflict, do not silently weaken security.

Examples:

* broad permissions
* unsafe file access
* arbitrary code execution
* insecure downloads
* plaintext secrets
* unrestricted intents

The agent must document the tradeoff.

---

# 80. NO ARBITRARY CODE EXECUTION

Expression evaluation must not be implemented through unsafe dynamic code execution.

Do not take user input and execute it as:

```text
JavaScript
Kotlin
Java
Python
shell
```

unless there is a carefully sandboxed, explicitly intended execution architecture.

For a calculator, use a safe parser/interpreter.

---

# 81. DEPENDENCY MANAGEMENT

Every third-party dependency creates:

* security surface
* maintenance burden
* licensing considerations
* upgrade risk
* build complexity

Before adding a dependency, ask:

1. Is it necessary?
2. Can the platform already solve this?
3. Is it actively maintained?
4. Is its license acceptable?
5. Is it secure?
6. Is its binary size acceptable?
7. Does it introduce transitive dependencies?
8. Can we remove it later?

Do not add libraries simply because they are popular.

---

# 82. DEPENDENCY UPDATES

When updating dependencies:

* inspect release notes
* inspect breaking changes
* run tests
* check security advisories
* check API changes
* verify runtime behavior

Never blindly update every dependency.

---

# 83. BUILD SYSTEM

Gradle configuration is part of the architecture.

Keep:

* plugin versions
* dependency versions
* compiler configuration
* convention plugins
* build logic

organized.

If the project becomes sufficiently large, centralized Gradle convention plugins may be appropriate.

Avoid duplicating configuration across every feature.

---

# 84. BUILD LOGIC

If many modules repeat the same configuration, consider:

```text
build-logic/
```

or another appropriate Gradle convention mechanism.

For example:

```text
android-feature
android-library
android-compose
android-test
```

But do not abstract Gradle configuration before repetition justifies it.

---

# 85. RELEASE ENGINEERING

Every architectural change must eventually be release-safe.

Consider:

* debug
* release
* signing
* shrinking
* obfuscation
* ProGuard/R8
* resources
* permissions
* background restrictions
* startup
* migration

Test release builds, not only debug builds.

---

# 86. BACKUP BEFORE MAJOR CHANGES

Before major architectural changes:

1. Create a complete backup.
2. Verify the backup exists.
3. Verify it is readable.
4. Record its location.
5. Record repository state.
6. Record current build status.
7. Record current test status.

Do not assume Git alone is a complete backup strategy.

---

# 87. GIT DISCIPLINE

Before modifying code:

```text
git status
```

must be understood.

Inspect:

```text
current branch
uncommitted changes
recent commits
```

Never destroy user changes.

Never reset, checkout, clean, or rewrite history destructively without explicit authorization.

---

# 88. CHANGE ISOLATION

Large changes should be decomposed.

Prefer:

```text
Architecture preparation
    ↓
Core infrastructure
    ↓
One capability migration
    ↓
Tests
    ↓
Next capability
```

over:

```text
rewrite entire application
```

unless a full rewrite is explicitly authorized.

---

# 89. BIG-BANG REFACTORING

Avoid changing:

* architecture
* database
* navigation
* UI
* theme
* downloader
* calculator

all at once.

The larger the change surface, the harder it becomes to identify regressions.

---

# 90. STRANGLER MIGRATION

When converting an existing monolith to capability architecture, prefer gradual migration where feasible.

Example:

```text
Old application
       |
       +---- Calculator migrated
       |
       +---- Routine migrated
       |
       +---- Cleaner migrated
       |
       +---- Remaining legacy
```

Eventually:

```text
Legacy
   ↓
removed
```

This reduces risk.

---

# 91. LEGACY CODE

Legacy code should not automatically be rewritten.

Classify it:

```text
Keep
Refactor
Wrap
Migrate
Replace
Remove
```

The agent must identify why a change is needed.

---

# 92. DO NOT MIX ARCHITECTURES RANDOMLY

A repository may temporarily contain multiple architectural styles during migration.

This is acceptable.

But new code should follow the target architecture.

Do not create:

```text
Feature A -> MVVM
Feature B -> MVC
Feature C -> random callbacks
Feature D -> Redux
Feature E -> custom architecture
```

unless there is a documented reason.

---

# 93. ARCHITECTURAL CONSISTENCY

Consistency does not mean identical implementation.

The agent should standardize:

* naming
* state conventions
* error conventions
* dependency injection
* navigation contracts
* repository patterns where appropriate
* testing conventions
* UI patterns

But permit justified deviations.

---

# 94. DEPENDENCY INJECTION

Use a consistent DI approach.

Whether the project chooses:

* Hilt
* manual dependency injection
* another mechanism

is less important than:

* explicit ownership
* lifecycle correctness
* testability
* predictable construction

Avoid service locators disguised as dependency injection.

---

# 95. GLOBAL SINGLETONS

Singletons are not automatically bad.

But global mutable state is dangerous.

Before introducing a singleton, ask:

* What owns it?
* What state does it contain?
* Is it thread-safe?
* Is it lifecycle-independent?
* Is it testable?
* Does it create hidden coupling?

Prefer dependency injection where appropriate.

---

# 96. CONTEXT USAGE

Android `Context` must not be passed everywhere casually.

Ask:

> Does this component actually require Android Context?

If not, don't give it one.

This keeps domain code more portable and testable.

---

# 97. PLATFORM BOUNDARIES

Android-specific APIs should remain near Android-specific infrastructure.

Prefer:

```text
domain
    ↓
abstract interface
    ↓
Android implementation
```

where platform independence provides meaningful value.

Do not force every class to become platform-agnostic.

---

# 98. ANDROID PERMISSION BOUNDARY

Permission requests belong near the capability that needs the permission.

The application shell should not request every permission on startup.

Bad:

```text
Startup
    -> request 17 permissions
```

Good:

```text
User opens Cleaner
    -> explain permission
    -> request relevant permission
```

---

# 99. LIFECYCLE ROBUSTNESS

Every capability must survive:

* rotation/configuration changes
* backgrounding
* foregrounding
* process death where applicable
* recreation
* navigation away/back
* interrupted operations

Do not rely solely on in-memory state for important information.

---

# 100. PROCESS DEATH

Android can kill the process.

The agent must assume:

> Memory can disappear at any time when the application is not active.

For important operations:

```text
state -> persistent representation
```

where appropriate.

Do not attempt to preserve everything.

Persist what actually needs recovery.

---

# 101. RECOVERY DESIGN

Every long-running capability should define:

```text
Normal
Interrupted
Failed
Cancelled
Recovering
Recovered
```

where relevant.

For example:

Downloader:

```text
Queued
Preparing
Downloading
Paused
Cancelling
Cancelled
Failed
Completed
```

Pomodoro:

```text
Idle
Running
Paused
Completed
Interrupted
```

Explicit state machines are strongly preferred over scattered booleans.

---

# 102. BOOLEAN EXPLOSION

Avoid:

```text
isLoading
isPaused
isDownloading
isCancelled
isCompleted
isError
```

when mutually exclusive states exist.

This can create impossible combinations:

```text
isLoading = true
isCompleted = true
isCancelled = true
```

Prefer a finite state model.

---

# 103. STATE MACHINES

For complex capabilities, explicitly model:

```text
states
events
transitions
invariants
side effects
```

For each transition, ask:

* Is it valid?
* What happens if repeated?
* What happens if interrupted?
* What happens if delayed?
* What happens if the user leaves?
* What happens if the process dies?

---

# 104. IDEMPOTENCY

Important operations should be idempotent where practical.

For example:

```text
Cancel download
```

called twice should not corrupt state.

Likewise:

```text
Complete routine
```

should have defined duplicate semantics.

---

# 105. RETRIES

Retries must be bounded.

Avoid:

```text
retry forever
```

Use:

* maximum attempts
* exponential backoff where appropriate
* cancellation
* permanent failure classification

---

# 106. FAILURE INJECTION

For important capabilities, deliberately test failure.

Simulate:

* network unavailable
* storage unavailable
* permission denied
* database failure
* malformed data
* process recreation
* cancellation
* timeout
* corrupted state

A system that works only when everything goes correctly is not production-grade.

---

# 107. LOGGING

Logs should be:

* structured
* useful
* appropriately leveled
* privacy-conscious

Avoid:

```text
println("HERE")
println("WHY")
println("AAAA")
```

Use meaningful diagnostics.

---

# 108. TELEMETRY

If telemetry exists, treat it as a privacy-sensitive subsystem.

Collect only what is necessary.

Document:

* what is collected
* why
* retention
* anonymization
* opt-out requirements

Do not add telemetry merely because it is technically possible.

---

# 109. USER PRIVACY

The application should follow data minimization.

If a feature can work without collecting something, prefer not collecting it.

Do not send local data to servers without an explicit architectural and product reason.

---

# 110. NETWORK TRUST

Never trust:

* URLs
* server metadata
* downloaded filenames
* MIME declarations
* response sizes
* redirects

Validate assumptions.

---

# 111. INPUT VALIDATION

All external inputs must be treated as untrusted.

Sources include:

* text fields
* intents
* deep links
* clipboard
* files
* network
* system APIs
* imported configuration

Validate at boundaries.

---

# 112. OUTPUT VALIDATION

Do not assume data is valid simply because it came from your own database.

Database corruption, migration errors, older versions, or bugs can create invalid states.

Validate important invariants.

---

# 113. INVARIANTS

Each capability should define important invariants.

Example:

Calculator:

```text
No invalid expression reaches evaluator.
```

Downloader:

```text
Completed file exists before state becomes Completed.
```

Routine:

```text
Completion cannot reference a nonexistent routine.
```

Capability registry:

```text
Every visible capability ID is unique.
```

Theme:

```text
Every theme state maps to a valid theme.
```

---

# 114. DATA VALIDATION

Do not assume persisted user data is clean.

On reading:

```text
validate
normalize
recover if safe
otherwise report controlled failure
```

Do not crash because one preference contains an unexpected value.

---

# 115. CORRUPTION RECOVERY

Persistent user preferences such as capability ordering may become malformed.

Example:

```text
["calculator", "calculator", "unknown", null]
```

The system should recover deterministically.

A robust strategy might:

1. remove invalid IDs
2. remove duplicates
3. preserve valid known ordering
4. append missing capabilities according to default order
5. persist repaired state

The exact policy must be documented.

---

# 116. LOCALIZATION

Display names must not be used as IDs.

Never:

```text
id = "Calculator"
```

because localization changes it.

Use:

```text
id = "calculator"
```

and localize the display title separately.

---

# 117. INTERNATIONALIZATION

Design for:

* longer translations
* right-to-left layouts where appropriate
* number formats
* date formats
* decimal separators
* units
* pluralization

Do not hard-code English assumptions into domain logic.

---

# 118. TIME AND DATE

Time-related capabilities must distinguish:

* instant
* local date
* local time
* timezone
* duration
* elapsed time

Do not represent every concept as a `String`.

---

# 119. UNIT SYSTEMS

For calculator/unit conversion functionality:

* distinguish dimensions from units
* validate dimensional compatibility
* avoid accidental unit mixing
* document conversion constants
* test conversion invariants

For example:

```text
meters -> feet
```

is valid.

```text
meters -> kilograms
```

is not.

---

# 120. CONFIGURATION

Central application configuration should not become a global mutable object containing everything.

Separate:

```text
build configuration
runtime configuration
user preferences
feature configuration
secrets
```

---

# 121. SECRETS

Never hard-code:

* API keys
* private tokens
* passwords
* signing credentials

into source code.

Do not commit secrets to Git.

---

# 122. BUILD-TIME SECRETS

Even build-time secrets must be handled carefully.

Do not expose secrets in:

* source
* public repository
* logs
* APK resources
* screenshots
* test fixtures

Remember:

> Anything shipped inside an APK should generally be considered recoverable by an attacker.

---

# 123. TEST DATA

Test data must not contain real user information.

Use synthetic fixtures.

---

# 124. UI PREVIEW DATA

Compose previews must use safe deterministic sample data.

Do not connect previews to live services.

---

# 125. TEST DETERMINISM

Tests must not randomly fail due to:

* actual network
* actual current time
* actual filesystem state
* actual device locale
* actual device timezone

Inject controllable abstractions where required.

---

# 126. CLOCK ABSTRACTION

For time-dependent domain logic, consider injecting a clock.

This makes tests deterministic.

For example:

```text
ProductionClock
TestClock
```

But do not abstract time everywhere unnecessarily.

---

# 127. RANDOMNESS

If functionality requires randomness, use injectable/testable randomness where reproducibility matters.

---

# 128. FILESYSTEM TESTING

File operations should be testable against controlled directories.

Never let automated tests accidentally operate on user files.

---

# 129. DESTRUCTIVE OPERATIONS

Destructive capabilities require additional safeguards.

Before deletion:

* determine scope
* display what will happen
* ensure correct permission
* support cancellation where possible
* avoid partial corruption

---

# 130. SAFE DEFAULTS

When uncertain, prefer the safer default.

Examples:

* deny permission
* don't delete
* don't overwrite
* don't expose
* don't upload
* don't execute
* don't assume success

---

# 131. PRINCIPLE OF LEAST AUTHORITY

Every capability should have the smallest authority required.

This applies to:

* permissions
* filesystem
* network
* database
* intents
* background execution

---

# 132. EXPORTS

Review Android components:

* Activities
* Services
* BroadcastReceivers
* ContentProviders

Ask whether each must be externally accessible.

Unnecessary exported components increase attack surface.

---

# 133. INTENTS

Treat incoming intents as untrusted.

Validate:

* action
* URI
* extras
* MIME
* source assumptions

Do not blindly trust external applications.

---

# 134. DEEP LINKS

Deep links must validate:

* destination
* parameters
* authentication state
* permissions
* malformed input

Never assume a deep link originated from your own UI.

---

# 135. NOTIFICATIONS

Notifications are part of the capability boundary.

A capability creating notifications should define:

* channel
* importance
* content
* lifecycle
* cancellation
* permissions
* privacy

Do not leave orphaned notifications.

---

# 136. BACKGROUND NOTIFICATION STATE

If a notification represents a long-running operation, the notification state and operation state must not diverge.

Example:

```text
Downloader says Completed
notification says Downloading
```

This is a consistency bug.

---

# 137. APP STARTUP

Startup should remain lightweight.

Do not initialize every capability fully at application launch.

Prefer lazy initialization where appropriate.

A calculator shouldn't initialize a downloader networking stack if the downloader hasn't been used.

---

# 138. LAZY LOADING

Capabilities that are expensive should be initialized only when required.

But don't over-engineer lazy loading to the point where behavior becomes unpredictable.

---

# 139. STARTUP DEPENDENCY GRAPH

Keep startup dependencies minimal.

The application should be able to launch even if an optional capability has a problem, when technically feasible.

---

# 140. CRASH ISOLATION

One capability's failure should not unnecessarily bring down unrelated capabilities.

Where possible:

```text
Calculator failure
    !=
Entire launcher failure
```

The exact level of isolation is constrained by Android process architecture, but the software design should minimize shared failure points.

---

# 141. SHARED INFRASTRUCTURE FAILURE

Core infrastructure is naturally high-impact.

Therefore core modules require stricter testing.

A bug in:

```text
core/design-system
```

may affect everything.

A bug in:

```text
feature/calculator
```

should ideally affect calculator functionality only.

---

# 142. CORE CHANGE POLICY

Changes to core modules must receive greater scrutiny than ordinary feature changes.

Before changing core:

1. Identify all consumers.
2. Identify compatibility risk.
3. Run broad tests.
4. Inspect UI impact.
5. Inspect performance.
6. Inspect security implications.
7. Inspect migration implications.
8. Verify unrelated capabilities.

---

# 143. FEATURE CHANGE POLICY

When modifying one feature:

1. Inspect its public contract.
2. Inspect dependencies.
3. Inspect data ownership.
4. Inspect navigation.
5. Inspect tests.
6. Search for consumers.
7. Identify shared components affected.
8. Make the smallest safe change.
9. Run feature tests.
10. Run affected integration tests.
11. Run broader regression tests when necessary.

---

# 144. DO NOT TOUCH UNRELATED FEATURES

An agent must not refactor unrelated capabilities merely because it notices imperfections.

If unrelated improvements are valuable, report them separately.

Do not silently expand scope.

---

# 145. SCOPE CONTROL

Every task must have:

```text
requested scope
architecturally necessary scope
optional improvements
```

Implement the first two.

Do not implement the third without authorization unless it is required to prevent a defect.

---

# 146. CHANGE IMPACT ANALYSIS

Before coding, produce a mental or explicit impact analysis.

Example:

```text
Requested:
Add dark mode toggle.

Potential impact:
- design system
- theme
- settings
- persistence
- home screen
- all feature screens
- previews
- tests
```

Then determine the minimum correct change.

---

# 147. SEARCH BEFORE MODIFY

Never assume the codebase works as expected.

Before modifying:

* inspect files
* search references
* understand current architecture
* inspect tests
* inspect build configuration
* inspect dependencies

---

# 148. NEVER REWRITE BLINDLY

Do not replace large files because they "look old."

Understand them first.

Preserve:

* working functionality
* edge-case fixes
* platform compatibility
* existing tests
* user data behavior

---

# 149. EXISTING BUGS

If you encounter an unrelated existing bug:

* document it
* assess whether it blocks the requested work
* fix only if necessary or authorized
* do not pretend it was introduced by the current change

---

# 150. AGENTIC AI MUST NOT TRUST ITS OWN PREVIOUS CODE

This is critical.

When an agent sees code previously generated or modified by another AI agent:

> Treat it as untrusted until verified.

Do not reason:

```text
Previous agent implemented it.
Therefore it works.
```

Instead:

```text
Inspect
Test
Verify
Challenge assumptions
```

---

# 151. AGENT HANDOFFS

If multiple agents work on the project, every significant change should leave behind:

* architectural rationale
* changed modules
* migration details
* test results
* known limitations
* unresolved risks

Do not rely on conversational memory.

---

# 152. DOCUMENTATION

Important architectural decisions should be documented.

Maintain something such as:

```text
docs/
    architecture/
    decisions/
    capabilities/
    security/
    testing/
```

Use Architecture Decision Records where appropriate.

---

# 153. ARCHITECTURE DECISION RECORDS

For significant decisions, document:

```text
Context
Decision
Alternatives
Reasoning
Consequences
```

Examples:

* why Compose
* why Room
* why WorkManager
* why a particular downloader architecture
* why a feature became a separate module
* why a shared abstraction exists

---

# 154. NO CARGO-CULT ARCHITECTURE

Do not add architecture because:

> "Large companies use it."

Architecture must serve this application's actual requirements.

Names such as:

```text
Clean Architecture
Hexagonal
MVVM
MVI
DDD
Modular Architecture
```

are tools, not religions.

Use principles intelligently.

---

# 155. COMBINATION OF PATTERNS

The project may combine:

```text
Capability-oriented modularization
+
Clean Architecture principles
+
MVVM/MVI-style presentation
+
Unidirectional Data Flow
+
Repository abstraction
+
Dependency Injection
```

The combination must remain understandable.

---

# 156. DO NOT OVER-LAYER

Bad:

```text
UI
ViewModel
Interactor
UseCase
Service
Manager
Repository
Provider
Gateway
Adapter
Factory
FactoryProvider
```

for a simple boolean.

Use layers proportional to complexity.

---

# 157. ARCHITECTURAL ECONOMY

Every abstraction must pay rent.

Ask:

> Does this abstraction reduce complexity more than it introduces?

If no, don't create it.

---

# 158. CAPABILITY TEMPLATE

New capabilities should follow a standard template.

Conceptually:

```text
feature/<name>/
    presentation/
    domain/
    data/
    infrastructure/
    navigation/
    tests/
    README.md
```

Not every folder must exist if unnecessary.

The README should explain:

```text
Purpose
Ownership
Dependencies
Public API
Persistence
Permissions
Background work
Known limitations
Testing
```

---

# 159. NEW CAPABILITY CHECKLIST

Before adding a capability, answer:

### Product

* What user problem does it solve?
* Why is it a separate capability?
* What is its scope?

### Architecture

* What does it own?
* What does it consume?
* What does it expose?

### Data

* Does it persist data?
* Where?
* Migration?

### Security

* What permissions?
* What untrusted inputs?

### Lifecycle

* Does it run in background?
* What happens after process death?

### UI

* What screens?
* What states?
* Accessibility?

### Performance

* Expensive operations?
* Memory?

### Testing

* Unit?
* Integration?
* UI?
* Failure paths?

---

# 160. CAPABILITY README

Every sufficiently complex capability should document:

```text
# Capability Name

## Purpose

## Responsibilities

## Non-responsibilities

## Architecture

## Dependencies

## Data

## State machine

## Permissions

## Background work

## Security considerations

## Testing

## Known limitations
```

---

# 161. CAPABILITY NON-RESPONSIBILITIES

This section is especially important.

Example:

```text
Calculator does not own:
- global theme
- global navigation
- downloader storage
- routine scheduling
```

Explicit non-responsibilities prevent architectural drift.

---

# 162. ARCHITECTURAL DRIFT

Over time, architecture degrades.

Examples:

```text
Feature A starts importing Feature B.
```

Then:

```text
Feature B imports Feature A.
```

Then:

```text
Core contains feature-specific logic.
```

Then:

```text
Everything depends on everything.
```

The agent must actively detect this.

---

# 163. ARCHITECTURAL DEBT

Track debt explicitly.

Classify:

```text
Low
Medium
High
Critical
```

Do not hide architectural debt.

---

# 164. DEBT MUST NOT BECOME EXCUSE

Architecture debt should not be used as justification to stop development.

Instead:

```text
identify
contain
document
schedule
reduce
```

---

# 165. BOOTSTRAPPING A NEW FEATURE

When creating a new feature:

1. Inspect existing capability conventions.
2. Copy only the architectural pattern, not arbitrary implementation.
3. Define ownership.
4. Define public boundary.
5. Define dependencies.
6. Add tests.
7. Register capability.
8. Add navigation.
9. Add UI.
10. Validate integration.

---

# 166. FEATURE REGISTRATION

Registration should be centralized enough to avoid scattered configuration, but not so centralized that the registry contains implementation logic.

Registry:

```text
identity
metadata
destination
availability
default order
```

Feature implementation:

```text
actual functionality
```

---

# 167. HOME SCREEN SHOULD REMAIN DUMB

The home screen should be a renderer and launcher.

It should not become:

```text
UniversalFeatureManager
```

It should consume capability descriptors.

---

# 168. USER PRIORITY

If capabilities are reorderable, treat user ordering as user data.

Never reset ordering after an unrelated update.

Never silently reorder because the developer changed the default ordering.

Default ordering applies only when no user preference exists or when introducing a new capability.

---

# 169. CAPABILITY VISIBILITY

If capabilities can be hidden:

```text
visible IDs
hidden IDs
```

must be represented safely.

A hidden capability must remain reachable through appropriate settings if product design requires it.

Do not delete capability state merely because its launcher button is hidden.

---

# 170. FEATURE DISCOVERY

If future capabilities can be dynamically enabled, architecture should distinguish:

```text
known capability
installed capability
enabled capability
visible capability
available capability
```

Do not collapse all of these concepts into one boolean.

---

# 171. PLUGIN-LIKE THINKING

The system may borrow ideas from plugin architecture:

```text
Capability
    -> descriptor
    -> contract
    -> implementation
```

But do not build a full plugin framework unless there is a real requirement.

The goal is modularity, not unnecessary dynamism.

---

# 172. DYNAMIC FEATURE LOADING

Do not introduce dynamic code loading simply because capabilities are modular.

Dynamic loading introduces:

* security
* deployment
* compatibility
* debugging
* lifecycle
* signing

complexity.

Static modularization is sufficient for most applications.

---

# 173. SHARED EVENTS

Application-wide events should be rare.

An event bus can easily become hidden global coupling.

Avoid:

```text
Everything publishes everything.
```

If events are used:

* define ownership
* define payload
* define lifetime
* define delivery guarantees
* define failure behavior

---

# 174. EVENT BUS WARNING

Do not use an event bus to solve poor architecture.

If:

```text
Feature A
```

needs to tell:

```text
Feature B
```

something, first ask why.

Then choose:

* explicit interface
* repository/shared state
* navigation
* application coordinator
* event

based on actual semantics.

---

# 175. SHARED DOMAIN MODELS

Shared models can create coupling.

Do not put every model into:

```text
core/model
```

just because multiple features currently reference it.

A model belongs in core when it represents genuinely shared application-level meaning.

Otherwise keep it feature-local.

---

# 176. MODEL TRANSLATION

Sometimes different capabilities use different representations of similar concepts.

Do not force one universal model if their semantics differ.

Duplication can be healthier than inappropriate coupling.

---

# 177. DATABASE SHARING

Sharing one database does not mean sharing every DAO.

Keep access boundaries.

For example:

```text
CalculatorRepository -> calculator DAO
RoutineRepository -> routine DAO
```

not:

```text
Everything -> UniversalDAO
```

---

# 178. REPOSITORY OWNERSHIP

Repositories should have clear responsibility.

A repository should not become:

```text
CalculatorRepository
    + routine data
    + network downloads
    + theme settings
```

---

# 179. NETWORK REPOSITORIES

Separate remote data sources from domain semantics where appropriate.

Do not leak HTTP-specific details throughout the application.

Avoid spreading:

```text
Response<T>
HTTP status code
Retrofit details
```

through domain logic unless there is a strong reason.

---

# 180. STORAGE REPOSITORIES

Likewise, avoid leaking:

```text
Cursor
SQLite implementation
Room DAO
FileDescriptor
```

into unrelated UI code.

---

# 181. FILE OWNERSHIP

Every capability that creates files must define:

* directory
* naming
* cleanup
* ownership
* retention
* migration
* deletion behavior

---

# 182. CACHE VS USER DATA

Never confuse:

```text
cache
```

with:

```text
user data
```

Cache can often be recreated.

User data cannot.

Cleaner must respect this distinction.

---

# 183. BACKGROUND JOB OWNERSHIP

Every worker/job should have a clear capability owner.

Bad:

```text
UniversalBackgroundWorker
```

doing everything.

Better:

```text
DownloaderWorker
RoutineReminderWorker
```

when those responsibilities are genuinely distinct.

Shared infrastructure may provide scheduling helpers.

---

# 184. CANCELLATION

Every asynchronous operation should define cancellation behavior.

Ask:

> If the user presses Cancel right now, what exactly happens?

Cancellation should not leave:

* corrupt files
* inconsistent state
* orphaned jobs
* misleading UI

---

# 185. TIMEOUTS

Network and other external operations should have appropriate timeouts.

Never allow indefinite waits.

---

# 186. RESOURCE LIMITS

Bound:

* download sizes where appropriate
* retry counts
* queue sizes
* memory use
* concurrent jobs
* database query size

Unbounded systems eventually fail.

---

# 187. CONCURRENCY

Concurrent operations must have defined semantics.

Example:

Can two downloader jobs modify the same file?

Can a routine be completed twice simultaneously?

Can a timer receive pause and complete events simultaneously?

The agent must reason about races.

---

# 188. ATOMICITY

Important state changes should be atomic where required.

Example:

Downloader:

```text
temporary file complete
        ↓
atomic move
        ↓
mark Completed
```

not:

```text
mark Completed
        ↓
hope file finishes
```

---

# 189. TRANSACTIONS

Use database transactions where multiple related writes must succeed or fail together.

Do not use transactions blindly for everything.

---

# 190. CONSISTENCY

For every persistent operation ask:

> What states are impossible?

Then enforce them.

---

# 191. EVENTUAL CONSISTENCY

If eventual consistency is acceptable, document it.

Do not accidentally create it.

---

# 192. UI OPTIMISM

Optimistic UI updates can be useful.

But if used, define rollback behavior.

Bad:

```text
UI says deleted
server/database says failed
UI remains deleted
```

Good:

```text
Optimistic state
   ↓
operation
   ↓
success -> confirm
failure -> restore/report
```

---

# 193. ACCESSIBILITY AND ARCHITECTURE

Accessibility must not be added as an afterthought.

Shared components should provide accessible defaults.

Capabilities should supply meaningful semantics.

---

# 194. TEST ACCESSIBILITY

Where practical, test:

* content descriptions
* semantics
* focus order
* touch target sizes
* contrast
* text scaling

---

# 195. UI TESTING

Do not test implementation details unnecessarily.

Prefer testing observable behavior:

```text
User taps Calculator
→ Calculator opens.
```

rather than:

```text
InternalClassXYZ.method123 called.
```

---

# 196. DOMAIN TESTING

Domain tests should be deterministic and independent of Android UI where practical.

This is particularly important for:

* calculator
* unit conversion
* scheduling
* timer calculations
* data transformations

---

# 197. INTEGRATION TESTING

Integration tests should verify boundaries:

```text
Repository + database
Navigation + feature
Worker + persistence
Permission + capability
```

---

# 198. END-TO-END TESTING

Use selectively.

End-to-end tests are valuable for critical flows but expensive to maintain.

Prioritize:

* startup
* navigation
* core capability flows
* critical persistence
* destructive operations
* background recovery

---

# 199. REGRESSION TESTING

Every bug that reaches a verified root cause should ideally gain a regression test.

Never merely patch the symptom if a test can prevent recurrence.

---

# 200. PROPERTY-BASED TESTING

For mathematical or state-machine capabilities, property-based testing can reveal bugs that example-based tests miss.

Examples:

Unit conversion:

```text
convert(A -> B -> A) ≈ A
```

depending on expected precision.

State machine:

```text
invalid transition must never occur
```

---

# 201. FUZZ TESTING

Where appropriate, fuzz:

* calculator expressions
* imported data
* filenames
* URLs
* network payloads
* persisted state

The goal is to discover crashes and unexpected states.

---

# 202. SECURITY TESTING

Test:

* malformed intents
* malformed URLs
* path traversal
* oversized input
* permission denial
* unauthorized access
* exported components
* insecure storage

---

# 203. PERFORMANCE TESTING

Measure:

* startup
* navigation
* scrolling
* calculation
* downloads
* database operations
* memory
* battery impact

Do not optimize solely based on intuition.

---

# 204. BATTERY

Background capabilities can consume battery.

Avoid:

```text
continuous polling
```

unless absolutely necessary.

Prefer platform-aware scheduling.

---

# 205. ANDROID OS RESTRICTIONS

Never assume desktop-like process behavior.

Android may restrict:

* background execution
* storage
* notifications
* services
* alarms
* permissions

The implementation must follow the platform's actual contracts.

---

# 206. API LEVELS

When using Android APIs, consider supported API levels.

Do not blindly use the newest API.

Provide compatibility where required.

---

# 207. DEPRECATIONS

Do not ignore deprecation warnings indefinitely.

Evaluate whether:

* migration is required
* compatibility wrapper is needed
* old API is still acceptable

---

# 208. THIRD-PARTY API INSTABILITY

External APIs may change.

Isolate them behind boundaries.

This is especially important for:

* media services
* download sources
* remote APIs
* system integrations

---

# 209. NO HARD-CODED ASSUMPTIONS

Do not assume:

* screen size
* locale
* timezone
* network
* storage availability
* battery state
* permission state
* process lifetime
* device manufacturer behavior

unless explicitly required.

---

# 210. RESPONSIVE UI

The application must adapt to:

* small phones
* large phones
* tablets
* landscape
* large fonts
* foldables where applicable

The 3-column launcher should not blindly remain 3 columns on every screen size if that harms usability.

The product requirement may define default 3 columns, but responsive adaptation should be considered.

---

# 211. MAIN GRID

The main launcher should remain:

* compact
* polished
* accessible
* responsive
* reorderable
* persistent
* visually coherent

Each capability tile/button should have:

```text
icon
short name
semantic role
press state
selected/reorder state if applicable
accessibility semantics
```

---

# 212. TOUCH INTERACTION

Every interactive surface should have clear:

* pressed
* focused
* selected
* disabled
* loading
* error
* success

states where applicable.

Do not use color alone to communicate state.

---

# 213. REORDER MODE

If drag-and-drop ordering exists:

* provide clear affordance
* support accessibility alternative
* prevent accidental rearrangement
* persist changes safely
* animate position changes
* handle cancellation
* handle invalid IDs

---

# 214. SETTINGS ARCHITECTURE

Settings should be divided conceptually into:

```text
Application settings
Capability settings
```

Application settings:

```text
theme
launcher order
global appearance
```

Capability settings:

```text
calculator precision
pomodoro duration
routine preferences
downloader settings
```

Do not put every setting into one enormous object.

---

# 215. SETTINGS OWNERSHIP

Each capability owns its own settings.

Global settings belong to application/core configuration.

---

# 216. DEFAULTS

Defaults should be deterministic and documented.

Changing defaults in a later version should not necessarily overwrite existing user preferences.

---

# 217. USER PREFERENCES

Persist user preferences safely.

Validate values when loading.

For example:

```text
negative timer duration
invalid theme enum
unknown capability ID
```

must not crash the app.

---

# 218. SECURITY REVIEW AFTER ARCHITECTURAL CHANGES

Every major architecture change must trigger a security review.

Moving a component can accidentally change:

* visibility
* exported status
* storage access
* dependency trust
* network exposure

---

# 219. PERFORMANCE REVIEW AFTER MODULARIZATION

Modularization can affect:

* build time
* startup
* dependency graph
* generated code
* APK size

Measure where meaningful.

---

# 220. BUILD PERFORMANCE

Avoid unnecessarily huge dependency graphs.

Keep feature dependencies minimal.

---

# 221. APK SIZE

Do not introduce large libraries for trivial functionality.

Consider:

* binary size
* resources
* native libraries
* duplicate dependencies

---

# 222. RESOURCE OWNERSHIP

Resources such as:

* strings
* icons
* images
* animations

should have ownership.

Shared resources belong to the design system.

Capability-specific resources belong to the capability.

---

# 223. STRING OWNERSHIP

Do not place every string into one global strings file.

Keep capability-specific strings near the capability where platform conventions permit.

Shared strings should genuinely be shared.

---

# 224. COLOR OWNERSHIP

Semantic design colors belong to the design system.

Capability-specific semantic states can exist if justified, but must integrate with global accessibility requirements.

---

# 225. FEATURE-SPECIFIC THEMING

Avoid creating completely separate themes for every capability.

Instead:

```text
Global theme
   +
Capability-specific components
```

---

# 226. DOCUMENTATION AS CODE

Architecture documentation must evolve with the implementation.

When an agent makes a major architectural change, it should update relevant documentation.

---

# 227. CODE COMMENTS

Comments should explain:

* why
* constraints
* non-obvious tradeoffs
* invariants

Avoid comments that merely restate code.

Bad:

```kotlin
// Increment count
count++
```

Good:

```text
Use monotonic elapsed time here because wall-clock changes can invalidate timer duration.
```

---

# 228. TODO POLICY

TODOs must be meaningful.

Prefer:

```text
TODO(username): migrate legacy persistence after API X is removed.
```

over:

```text
TODO fix later
```

---

# 229. DEAD CODE

Remove dead code when confidently identified.

But do not delete seemingly unused code before checking:

* reflection
* navigation
* DI
* Android manifest
* generated code
* deep links
* external entry points

---

# 230. REFLECTION

Reflection can obscure dependencies.

Use only when justified.

Document important reflective behavior.

---

# 231. GENERATED CODE

Do not manually modify generated code unless the tool explicitly requires it.

Fix the generator/input instead.

---

# 232. CODE GENERATION

When using code generation:

* keep generated artifacts predictable
* test generated outputs
* document generation steps
* avoid hidden magic

---

# 233. AGENT MUST VERIFY BUILD

After significant changes:

```text
compile
test
lint
```

as appropriate.

A successful code edit is not a successful implementation.

---

# 234. AGENT MUST VERIFY INSTALLATION

When feasible, build/install/run the application.

Verify actual runtime behavior.

---

# 235. AGENT MUST VERIFY RELEASE BUILD

A debug build passing does not prove release correctness.

For release-impacting changes, verify release configuration when feasible.

---

# 236. STATIC ANALYSIS

Use available:

* Kotlin compiler
* Android lint
* detekt or equivalent if configured
* dependency analysis
* security scanning
* formatting checks

Do not introduce tools unnecessarily, but use configured tools consistently.

---

# 237. FORMATTING

Follow project formatting.

Do not create style wars during unrelated feature work.

---

# 238. NAMING

Names should describe responsibility.

Prefer:

```text
CalculatorRepository
RoutineScheduler
DownloadManager
CapabilityRegistry
```

over:

```text
ManagerThing
Utils2
DataHelper
```

---

# 239. MANAGER CLASS WARNING

"Manager" is often a smell.

Before creating a manager, define exactly what it manages.

Prefer domain-specific names.

---

# 240. UTIL CLASS WARNING

Before creating:

```text
Utils.kt
```

ask:

> What coherent abstraction does this represent?

If there is no answer, do not create it.

---

# 241. FACTORY WARNING

Factories should exist when object creation genuinely has complexity or variation.

Don't create factories merely to make architecture look enterprise-grade.

---

# 242. SINGLE RESPONSIBILITY

Classes should have focused responsibilities.

But don't interpret this as:

> One method per class.

Use judgment.

---

# 243. CAPABILITY RESPONSIBILITY

Likewise, one capability should solve a coherent user problem.

Do not create:

```text
Feature: Everything
```

---

# 244. DOMAIN LANGUAGE

Use consistent terminology.

If the product calls it:

```text
Capability
```

do not randomly call it:

```text
Module
Tool
Plugin
Widget
Feature
Service
```

unless those terms represent distinct concepts.

---

# 245. FEATURE VS MODULE

These are different concepts.

A:

```text
capability
```

is a product concept.

A:

```text
Gradle module
```

is an implementation/build concept.

One capability may contain multiple modules if justified.

One module should not necessarily contain multiple unrelated capabilities.

---

# 246. CORE VS PLATFORM

Use:

```text
core
```

for shared application infrastructure.

Use:

```text
platform
```

conceptually for Android/external system integrations when appropriate.

Do not create both unless the distinction provides value.

---

# 247. ARCHITECTURE EVOLUTION

The architecture must be allowed to evolve.

Today's:

```text
feature/calculator
```

may become tomorrow's:

```text
feature/calculator/
    engine/
    scientific/
    programming/
    graphing/
```

Do not prematurely create this complexity.

---

# 248. SCALABILITY

Scalability means more than performance.

It means the architecture can absorb:

* more features
* more developers
* more tests
* more users
* more device types
* more data
* more integrations

without exponential complexity.

---

# 249. TEAM SCALABILITY

A good capability boundary allows separate engineers to work on:

```text
Calculator
```

and:

```text
Routine
```

without constantly editing the same files.

This reduces merge conflicts and cognitive load.

---

# 250. OWNERSHIP

Where the project grows, each capability can have an owner or maintainer.

Ownership should be explicit where useful.

---

# 251. CODE REVIEW

Changes should be reviewable.

A massive 10,000-line change is difficult to verify.

Prefer focused commits.

---

# 252. COMMIT DISCIPLINE

Use commits that communicate intent.

Examples:

```text
feat(calculator): add matrix operations
fix(timer): derive remaining time from monotonic clock
refactor(core): isolate navigation contracts
feat(home): persist capability ordering
```

Avoid:

```text
changes
fix stuff
final
final final
```

---

# 253. ATOMIC COMMITS

Where practical, keep commits logically coherent.

This improves:

* review
* rollback
* debugging
* bisecting

---

# 254. ROLLBACK

For risky changes, define how rollback would work.

Especially:

* database migrations
* persistent settings
* background operations
* architecture migrations

---

# 255. DATABASE ROLLBACK WARNING

Database schema changes are not always safely reversible.

Therefore migration design must be conservative.

---

# 256. RELEASE MIGRATION STRATEGY

For major changes:

```text
backup
→ migrate
→ validate
→ run
→ verify
```

Do not assume migration succeeded merely because no exception occurred.

---

# 257. OBSERVABLE MIGRATIONS

Where appropriate, migration failures should be diagnosable without exposing user data.

---

# 258. FEATURE REMOVAL DATA POLICY

Before removing persistent capability data, determine:

```text
delete
archive
migrate
retain
```

Do not choose automatically.

---

# 259. CACHE CLEANUP

Cleaner must distinguish application caches from essential data.

The application should never silently delete files simply because a filename looks temporary.

---

# 260. EXTERNAL STORAGE

Respect Android's scoped storage and platform storage model.

Do not rely on old unrestricted filesystem assumptions.

---

# 261. MEDIASTORE

Media-related capabilities should use appropriate Android media/storage APIs rather than bypassing platform contracts.

---

# 262. KEYBOARD LOCK

System-level capabilities require special scrutiny.

A keyboard-lock capability must explicitly define:

* what is locked
* how it is locked
* how the user exits
* what happens after crash
* what happens after reboot
* what happens after permission loss
* emergency recovery path
* accessibility implications

Never create an unrecoverable lock state.

---

# 263. FAIL-SAFE DESIGN

For destructive or system-integrated capabilities:

> Failure should degrade toward a safe state.

For example:

```text
keyboard lock failure
    -> unlock safely
```

rather than:

```text
keyboard lock failure
    -> user permanently trapped
```

---

# 264. RECOVERY PATHS

Every system-level feature must have a recovery strategy.

Ask:

> What happens if this feature crashes at the worst possible moment?

---

# 265. USER CONTROL

High-end software must preserve user agency.

Users should be able to:

* cancel
* undo where possible
* disable
* revoke permissions
* reset settings
* recover from failures

---

# 266. NO DARK PATTERNS

Do not:

* hide important controls
* make destructive actions deceptively easy
* obscure permission requirements
* fabricate success
* prevent legitimate exit

---

# 267. TRANSPARENCY

If a capability cannot perform an operation because Android restricts it, tell the user honestly.

Do not create fake workarounds.

---

# 268. CAPABILITY CONTRACT

Each capability should have an explicit contract.

Conceptually:

```text
CapabilityContract
    identity
    display metadata
    availability
    navigation entry
    permissions
    lifecycle requirements
```

Do not necessarily implement one giant interface if it creates unnecessary coupling.

---

# 269. CAPABILITY INTERFACE DESIGN

Use narrow interfaces.

Bad:

```kotlin
interface EverythingService {
    fun calculate()
    fun clean()
    fun download()
    fun schedule()
}
```

Good:

```text
Calculator API
Routine API
Downloader API
```

where actual cross-feature interaction is necessary.

---

# 270. APPLICATION COORDINATION

Some behavior genuinely belongs at application level.

Examples:

* global navigation
* capability registration
* global theme
* launcher ordering
* application lifecycle

Do not force these into arbitrary features.

---

# 271. GLOBAL STATE MINIMIZATION

Global state should be:

```text
minimal
explicit
persistent where required
observable
testable
```

---

# 272. FEATURE STATE MINIMIZATION

Likewise, feature state should not leak into global state unless required.

---

# 273. EVENTUAL DECOMPOSITION

If a capability becomes huge, it may itself be decomposed internally.

Example:

```text
Calculator
    |
    +-- Expression
    +-- Scientific
    +-- Programming
    +-- Matrix
    +-- Units
```

But retain one product-level capability if the user perceives it as one coherent tool.

---

# 274. INTERNAL MODULES

A large capability may become:

```text
:feature:calculator:api
:feature:calculator:impl
```

only when justified.

Do not introduce this structure prematurely.

---

# 275. API/IMPLEMENTATION SPLIT

An API/implementation split is useful when:

* multiple consumers exist
* implementation is large
* dependency isolation matters
* compile-time boundaries matter

Otherwise, it may add unnecessary complexity.

---

# 276. ARCHITECTURAL TEST

At any point, a new engineer should be able to answer:

> "Where would I go to change Calculator?"

If the answer is unclear, architecture needs improvement.

Likewise:

> "Where would I change global theme?"

> "Where would I change capability ordering?"

> "Where does Downloader own its state?"

If these questions cannot be answered quickly, the architecture is becoming opaque.

---

# 277. SEARCHABILITY

Architecture should make code easy to locate.

Naming should support:

```text
search
navigation
debugging
review
```

---

# 278. DEBUGGING

When debugging a bug:

1. Identify affected capability.
2. Identify layer.
3. Reproduce.
4. Trace state.
5. Identify invariant violation.
6. Find root cause.
7. Fix root cause.
8. Add regression test.
9. Check adjacent capability boundaries.
10. Re-run regression suite.

---

# 279. DO NOT PATCH SYMPTOMS

Example:

If timer displays wrong remaining time:

Do not simply modify:

```text
Text formatting
```

until it looks right.

Find whether the underlying time model is wrong.

---

# 280. ROOT-CAUSE ENGINEERING

For every significant bug, document:

```text
Observed behavior
Expected behavior
Root cause
Why existing tests missed it
Fix
Regression test
Potential related locations
```

---

# 281. SECURITY ROOT CAUSE

If a security flaw is found:

Do not patch only the exact input.

Search for the same vulnerable pattern throughout the repository.

---

# 282. BUG PATTERN SEARCH

If one instance of:

```text
unsafe file path handling
```

is found, search all file-handling capabilities.

If one:

```text
GlobalScope
```

is found, search all modules.

If one:

```text
hard-coded color
```

violates design architecture, search other features.

---

# 283. ARCHITECTURAL PATTERN SEARCH

When fixing an architectural violation, search for the pattern globally.

Do not assume there is only one occurrence.

---

# 284. REGRESSION SURFACE

After a fix, identify:

```text
direct impact
indirect impact
shared infrastructure impact
```

Test accordingly.

---

# 285. AGENT MUST NOT CLAIM PERFECTION

Never state:

```text
100% bug-free
100% secure
perfect
unbreakable
mathematically guaranteed correct
```

unless a claim is specifically and rigorously limited to something that can actually be proven.

Use evidence-based statements:

```text
Verified by tests
Verified by static analysis
Partially verified
Not verified
Unknown
```

---

# 286. EVIDENCE-BASED ENGINEERING

For every important claim:

```text
Claim
↓
Evidence
↓
Verification
```

Examples:

> "Migration is safe."

Evidence:

```text
Migration tests + old fixture database + upgrade test
```

---

# 287. FINAL REVIEW REQUIREMENT

After implementation, perform an independent mental review.

Ask:

* Did I preserve capability boundaries?
* Did I introduce coupling?
* Did I move feature-specific code into core?
* Did I create hidden global state?
* Did I break persistence?
* Did I break navigation?
* Did I introduce a lifecycle bug?
* Did I introduce a race?
* Did I introduce a security vulnerability?
* Did I create a performance regression?
* Did I break accessibility?
* Did I break dark mode?
* Did I break existing capabilities?

---

# 288. SECOND-PASS REVIEW

For high-risk changes, perform a second pass as if another engineer had written the code.

Do not defend your implementation.

Attack it.

Try to find:

* edge cases
* race conditions
* invalid states
* migration bugs
* permission failures
* lifecycle failures
* architecture violations

---

# 289. CLEAN ARCHITECTURE PRINCIPLES

Where useful, follow:

```text
Presentation
    ↓
Domain
    ↓
Data abstractions
    ↓
Infrastructure
```

But do not force every feature into identical layers.

---

# 290. DEPENDENCY RULE

Inner layers should not know unnecessary outer-layer implementation details.

For example, domain logic should not know:

```text
Compose
Activity
View
Retrofit
Room
```

unless the capability's nature genuinely requires platform integration.

---

# 291. DOMAIN PURITY

Keep core business rules as platform-independent as reasonably practical.

This improves:

* testing
* reasoning
* correctness
* reuse

---

# 292. ANDROID-SPECIFIC LOGIC

Do not pretend Android doesn't exist.

Some capabilities inherently require Android APIs.

Examples:

* notifications
* storage
* system usage APIs
* keyboard/system integrations

Keep those dependencies localized rather than contaminating everything.

---

# 293. TESTING ANDROID-SPECIFIC CODE

Android-specific behavior requires appropriate instrumentation or integration testing.

Do not assume JVM unit tests prove Android behavior.

---

# 294. PLATFORM LIMITATIONS

If Android prevents a desired behavior, the agent must:

1. identify the limitation
2. verify it
3. explain it
4. implement the closest legitimate behavior
5. avoid fake functionality

---

# 295. USER EXPECTATIONS

A polished application must distinguish:

```text
Implemented
Partially implemented
Unsupported
Permission unavailable
Platform restricted
```

---

# 296. FEATURE ROADMAP

The architecture should make future capabilities easy to add.

For example:

```text
features/
    finance/
    notes/
    habit/
    file-tools/
```

can be added without rewriting:

```text
core
home
calculator
routine
```

---

# 297. FUTURE-PROOFING WARNING

Do not design for hypothetical features forever.

Build extension points only where there is credible future value.

---

# 298. COMPLEXITY BUDGET

Every architectural decision adds complexity.

The agent must continuously ask:

> Is this complexity justified by current or highly probable requirements?

---

# 299. SIMPLE CORE

The more capabilities the application gains, the more important it becomes for core infrastructure to remain boring.

Core should be:

```text
stable
predictable
well-tested
small
boring
```

This is a compliment.

---

# 300. SOPHISTICATED FEATURES, SIMPLE BOUNDARIES

Individual capabilities can be highly sophisticated.

The boundaries between them should remain simple.

For example:

```text
Calculator
     |
     +---- public contract
     |
     +---- isolated implementation
```

rather than exposing 30 internal interfaces.

---

# 301. ARCHITECTURAL CONSTITUTION OVERRIDE RULE

When a task appears to conflict with this constitution, do not silently violate it.

Instead:

1. Identify the conflict.
2. Explain it.
3. Determine whether the requested behavior genuinely requires an exception.
4. Propose the smallest architectural deviation.
5. Obtain authorization when required.
6. Document the exception.

---

# 302. EXCEPTION REGISTER

If an exception is approved, document:

```text
Exception
Reason
Affected modules
Risk
Duration
Removal condition
```

Do not allow undocumented exceptions.

---

# 303. TEMPORARY COMPATIBILITY CODE

Temporary code must have an explicit reason.

Example:

```text
// Compatibility layer for legacy version <= X.
// Remove after migration Y.
```

Do not allow temporary code to become permanent accidentally.

---

# 304. ARCHITECTURE REVIEW TRIGGERS

An architecture review is required when changes involve:

* core modules
* dependency graph
* database schema
* capability boundaries
* navigation architecture
* global state
* security architecture
* background execution
* permissions
* application lifecycle
* major UI design system changes
* build architecture

---

# 305. FEATURE REVIEW TRIGGERS

Feature-level review is required when changes involve:

* state machine
* persistence
* external network
* destructive operations
* system permissions
* long-running background work
* sensitive information

---

# 306. CHANGE SIZE

Prefer the smallest change that correctly solves the problem.

But do not make changes artificially tiny if that leaves architectural inconsistency.

Correctness comes first.

---

# 307. ARCHITECTURAL INTEGRITY OVER SHORT-TERM SPEED

Never choose:

```text
quick hack
```

when it permanently damages the architecture.

If a temporary shortcut is genuinely necessary, isolate and document it.

---

# 308. NO COPY-PASTE ARCHITECTURE

Do not duplicate entire feature implementations simply because two features appear similar.

First determine whether they share:

* domain semantics
* infrastructure
* UI component
* state behavior

Reuse only the appropriate layer.

---

# 309. DUPLICATION VS COUPLING

Remember:

> Some duplication is cheaper than incorrect coupling.

Do not extract a shared abstraction merely because two classes currently contain similar code.

---

# 310. SHARED COMPONENTS

Shared UI components should be extracted when:

* visually identical
* behaviorally consistent
* reused
* stable enough

A shared component should have a clear API.

---

# 311. COMPONENT API

Avoid enormous reusable components with 30 parameters.

Prefer composable APIs with sensible defaults.

---

# 312. DESIGN TOKEN DISCIPLINE

Do not hard-code:

```text
padding = 17.dp
cornerRadius = 13.dp
```

everywhere if the design system defines standardized values.

---

# 313. PREMIUM UI WITHOUT DESIGN CHAOS

Premium does not mean:

* gradients everywhere
* blur everywhere
* excessive shadows
* excessive animation
* excessive colors

Premium means:

```text
consistency
hierarchy
restraint
precision
feedback
accessibility
```

---

# 314. PLATFORM RESPECT

The application may be iOS-inspired visually where appropriate, but it remains an Android application.

Do not imitate another operating system so literally that Android usability suffers.

---

# 315. DESIGN SYSTEM EVOLUTION

When a component changes globally:

1. Identify consumers.
2. Verify screenshots/previews.
3. Test accessibility.
4. Test dark mode.
5. Check capability-specific edge cases.

---

# 316. ICON/NAME CHANGES

Changing a capability's display icon or title must not change its persistent identity.

---

# 317. USER ORDER MIGRATION

When capability metadata changes:

```text
stable ID
+
updated metadata
```

must preserve user ordering.

---

# 318. NEW CAPABILITY INTRODUCTION

When adding a new capability:

```text
1. implement
2. test
3. register
4. provide default position
5. preserve existing user order
6. provide icon/title
7. verify navigation
8. verify accessibility
```

---

# 319. CAPABILITY DISABLEMENT

If a capability becomes unavailable:

The launcher should gracefully represent it.

Do not crash because:

```text
destination == unavailable
```

---

# 320. FEATURE AVAILABILITY

Availability can depend on:

* Android version
* permissions
* hardware
* configuration
* network

The architecture must represent this explicitly where necessary.

---

# 321. HARDWARE

If future capabilities depend on:

* camera
* microphone
* sensors
* biometrics
* storage

handle absence gracefully.

---

# 322. DEVICE MANUFACTURER DIFFERENCES

Android devices vary.

Avoid assuming one vendor's behavior is universal.

Test on multiple API levels where practical.

---

# 323. TEST MATRIX

For important features, consider:

```text
minimum supported API
current API
latest API
small screen
large screen
light mode
dark mode
permission granted
permission denied
offline
online
cold start
warm start
process recreation
```

---

# 324. AUTOMATION

The repository should automate:

* build
* tests
* lint
* static checks
* formatting
* architecture checks

where practical.

---

# 325. CI

Continuous integration should catch:

* compilation errors
* unit test failures
* lint errors
* architecture violations
* migration failures

---

# 326. PRE-COMMIT QUALITY

Before declaring a task complete, run the narrowest sufficient validation.

For major changes, broaden validation.

---

# 327. NO "IT SHOULD WORK"

The agent must not report:

> "It should work."

Prefer:

> "The feature compiles, tests X/Y/Z pass, and the following scenarios were manually verified."

---

# 328. UNKNOWN IS VALID

If something cannot be verified:

```text
UNKNOWN
```

is a legitimate engineering status.

Never fabricate confidence.

---

# 329. FINAL IMPLEMENTATION REPORT

After significant work, report:

```text
Changed
Architecture impact
Affected capabilities
Core impact
Database impact
Security impact
Testing
Build status
Known limitations
Remaining risks
```

---

# 330. FINAL ARCHITECTURE REPORT

For major architectural work, include:

```text
Before
After
Dependency changes
New modules
Removed modules
New contracts
Migration strategy
Testing
Risks
```

---

# 331. AGENT EXECUTION PROTOCOL

Whenever you receive a task in this repository, follow this sequence.

## PHASE 1 — UNDERSTAND

Read:

* architecture constitution
* relevant documentation
* project structure
* build files
* affected code
* tests

---

## PHASE 2 — MAP

Determine:

```text
Which capability?
Which core modules?
Which dependencies?
Which state?
Which data?
Which permissions?
Which lifecycle?
Which UI?
```

---

## PHASE 3 — IMPACT ANALYSIS

Identify:

```text
direct impact
indirect impact
cross-capability impact
core impact
migration impact
security impact
performance impact
```

---

## PHASE 4 — PLAN

Create a minimal safe implementation plan.

Do not code until the architecture is understood.

---

## PHASE 5 — IMPLEMENT

Implement within the correct ownership boundary.

Do not move functionality into core unless justified.

Do not reach into another capability's internals.

---

## PHASE 6 — TEST

Run:

```text
unit tests
integration tests
UI tests
architecture checks
lint
build
```

as appropriate.

---

## PHASE 7 — ADVERSARIAL REVIEW

Try to break your own implementation.

---

## PHASE 8 — DOCUMENT

Update documentation when architecture changed.

---

## PHASE 9 — REPORT

State:

```text
What changed
Why
Evidence
Known limitations
```

---

# 332. WHEN USER SAYS "ADD A FEATURE"

Interpret:

> Add one new capability or extend an existing capability without violating architectural boundaries.

Do not automatically put everything into `app`.

---

# 333. WHEN USER SAYS "CHANGE A FEATURE"

First identify the feature boundary.

Modify the smallest appropriate scope.

---

# 334. WHEN USER SAYS "CHANGE UI"

Determine whether the UI component is:

```text
feature-specific
```

or:

```text
shared design-system component
```

before editing it.

---

# 335. WHEN USER SAYS "CHANGE THEME"

Treat it as a core/design-system change.

Audit all capabilities.

---

# 336. WHEN USER SAYS "CHANGE HOME SCREEN"

Treat it as launcher/capability-registry work.

Do not move feature logic into the home screen.

---

# 337. WHEN USER SAYS "ADD A NEW CAPABILITY"

Use the capability checklist.

Do not duplicate unrelated code.

---

# 338. WHEN USER SAYS "REFACTOR"

Do not refactor merely for aesthetic reasons.

Define:

```text
current problem
desired property
migration path
verification
```

---

# 339. WHEN USER SAYS "OPTIMIZE"

Measure first where practical.

Identify bottleneck.

Then optimize.

Do not rewrite everything.

---

# 340. WHEN USER SAYS "FIX BUG"

Reproduce first where possible.

Find root cause.

Fix root cause.

Add regression test.

Search for related instances.

---

# 341. WHEN USER SAYS "MAKE IT HIGH-END"

Interpret this as:

```text
correctness
reliability
security
performance
accessibility
maintainability
polish
```

not merely:

```text
more animations
more colors
more code
```

---

# 342. WHEN USER SAYS "MAKE IT LIKE X"

Extract principles from the reference.

Do not blindly copy implementation.

---

# 343. ARCHITECTURAL PHILOSOPHY IN ONE SENTENCE

The entire system should follow:

> **Independent capabilities, shared stable infrastructure, explicit contracts, controlled dependencies, predictable state, durable user data, safe platform integration, and continuous verification.**

---

# 344. THE CAPABILITY PRINCIPLE

Every capability should feel like:

> **A small product living inside a larger product.**

It should have:

```text
purpose
boundary
state
data
UI
tests
security model
lifecycle
public entry point
```

without becoming isolated from the common design language.

---

# 345. THE PLATFORM PRINCIPLE

The platform should feel like:

> **A stable foundation that capabilities consume, not a place where capabilities are implemented.**

---

# 346. THE HOME PRINCIPLE

The home screen should feel like:

> **A launcher and organizer of capabilities, not the brain of the application.**

---

# 347. THE DATA PRINCIPLE

User data should feel like:

> **A durable contract between application versions.**

---

# 348. THE SECURITY PRINCIPLE

Every boundary should be treated as a potential trust boundary.

---

# 349. THE FAILURE PRINCIPLE

A failure in one capability should cause the smallest reasonably possible failure elsewhere.

---

# 350. THE CHANGE PRINCIPLE

Every change should minimize:

```text
coupling
blast radius
migration risk
regression risk
security risk
```

while preserving correctness.

---

# 351. THE SIMPLICITY PRINCIPLE

The architecture should be sophisticated internally but simple to understand externally.

---

# 352. THE HONESTY PRINCIPLE

Never claim functionality, security, performance, or correctness that has not been verified.

---

# 353. THE EVOLUTION PRINCIPLE

The architecture must support change without requiring constant rewrites.

---

# 354. THE ANTI-MONOLITH PRINCIPLE

A monolith can exist physically while remaining modular logically.

But unrestricted cross-feature access must not gradually recreate a monolith.

Continuously monitor:

```text
dependency graph
shared state
shared data
shared services
core size
```

---

# 355. THE ANTI-FRAGMENTATION PRINCIPLE

Do not create so many modules and abstractions that the system becomes harder to understand than the problem itself.

The objective is neither:

```text
monolith
```

nor:

```text
microservice-like chaos
```

The objective is:

> **cohesive modularity.**

---

# 356. THE BLAST-RADIUS PRINCIPLE

Before changing anything, ask:

> "If this breaks, what else breaks?"

The smaller the answer, the healthier the architecture.

---

# 357. THE REVERSIBILITY PRINCIPLE

For risky changes, prefer approaches that are easier to roll back.

---

# 358. THE OBSERVABILITY PRINCIPLE

Important behavior must be diagnosable.

---

# 359. THE RECOVERY PRINCIPLE

Important operations must have recovery paths.

---

# 360. THE USER-TRUST PRINCIPLE

Never sacrifice user data, privacy, safety, or control for architectural convenience.

---

# 361. THE ENGINEERING MATURITY PRINCIPLE

High-end engineering means anticipating:

```text
failure
change
scale
attack
migration
maintenance
human error
```

not merely implementing the happy path.

---

# 362. AGENT MUST CHALLENGE BAD REQUESTS

If a requested implementation would clearly violate the architecture, do not blindly execute it.

Explain:

```text
Why it violates architecture
What risk it introduces
What alternative preserves the intent
```

Then proceed with the safer design when authorized.

---

# 363. AGENT MUST ASK WHEN NECESSARY

Do not ask unnecessary questions.

But ask when:

* requirements conflict
* destructive behavior is ambiguous
* data-loss risk exists
* security implications are unclear
* architecture requires an irreversible decision

---

# 364. AGENT MUST NOT ASK FOR THINGS IT CAN INSPECT

Before asking:

> "Where is the calculator?"

search the repository.

Before asking:

> "What database do we use?"

inspect the build and source.

Before asking:

> "How does navigation work?"

inspect navigation.

Use repository evidence first.

---

# 365. REPOSITORY-FIRST REASONING

The actual codebase is the source of truth for implementation.

This constitution is the source of truth for architectural principles.

When they conflict:

1. Understand why the existing code differs.
2. Determine whether it is intentional legacy behavior.
3. Avoid blindly assuming the constitution has already been implemented.
4. Migrate carefully.

---

# 366. NEVER ASSUME IDEAL ARCHITECTURE ALREADY EXISTS

The agent must recognize that an existing project may be:

```text
partially modular
legacy
inconsistent
mid-migration
```

Do not pretend it is already perfect.

---

# 367. ARCHITECTURAL BASELINE

Before major modernization, establish:

```text
current structure
current dependency graph
current tests
current build status
current known defects
```

---

# 368. MIGRATION BASELINE

If tests are weak, strengthen tests before performing dangerous refactoring where practical.

Otherwise you are changing the architecture without safety rails.

---

# 369. GOLDEN RULE OF REFACTORING

> **First make behavior observable. Then make behavior safe. Then change structure.**

---

# 370. GOLDEN RULE OF FEATURE DEVELOPMENT

> **First define ownership. Then define contracts. Then implement behavior. Then integrate UI. Then verify the system.**

---

# 371. GOLDEN RULE OF SHARED CODE

> **Share stable abstractions, not merely similar code.**

---

# 372. GOLDEN RULE OF CORE

> **Core should contain infrastructure, not feature business logic.**

---

# 373. GOLDEN RULE OF FEATURES

> **Features own their domain.**

---

# 374. GOLDEN RULE OF UI

> **UI renders state; it should not secretly own the application architecture.**

---

# 375. GOLDEN RULE OF STATE

> **Every important piece of state has one clear owner.**

---

# 376. GOLDEN RULE OF PERSISTENCE

> **Persist only what must survive, and treat persisted data as a compatibility contract.**

---

# 377. GOLDEN RULE OF SECURITY

> **Everything crossing a trust boundary is untrusted until validated.**

---

# 378. GOLDEN RULE OF TESTING

> **Every important invariant should have a way to be verified.**

---

# 379. GOLDEN RULE OF AGENTIC DEVELOPMENT

> **Never trust generated code merely because it was generated. Verify it.**

---

# 380. MANDATORY FINAL CHECKLIST FOR EVERY CHANGE

Before declaring any task complete, verify:

### Architecture

* [ ] Correct capability identified
* [ ] Ownership preserved
* [ ] No unnecessary coupling
* [ ] No circular dependencies
* [ ] No feature logic leaked into core
* [ ] No core garbage-drawer additions
* [ ] No unnecessary abstraction

### UI

* [ ] Design system followed
* [ ] Light mode checked
* [ ] Dark mode checked
* [ ] Accessibility considered
* [ ] Responsive behavior considered
* [ ] Interactive states handled

### State

* [ ] State owner identified
* [ ] Impossible states prevented
* [ ] Lifecycle handled
* [ ] Cancellation handled
* [ ] Process recreation considered

### Data

* [ ] Persistence impact analyzed
* [ ] Migration considered
* [ ] Existing user data preserved
* [ ] Corrupt data handled
* [ ] Stable IDs preserved

### Security

* [ ] Inputs validated
* [ ] Permissions minimized
* [ ] Storage safe
* [ ] Secrets protected
* [ ] External intents validated
* [ ] Network behavior reviewed
* [ ] Sensitive data not unnecessarily logged

### Performance

* [ ] No blocking UI operations
* [ ] Memory use reasonable
* [ ] Background work bounded
* [ ] Network requests controlled
* [ ] Startup impact considered

### Testing

* [ ] Unit tests
* [ ] Integration tests where needed
* [ ] UI tests where needed
* [ ] Regression test for bug fixes
* [ ] Failure paths considered
* [ ] Architecture checks
* [ ] Build successful
* [ ] Lint/static analysis successful where configured

### Documentation

* [ ] Architecture documentation updated if necessary
* [ ] Capability README updated if necessary
* [ ] Migration documented
* [ ] Known limitations documented
* [ ] Exceptions documented

---

# 381. MANDATORY REVIEW QUESTIONS

Before finishing, ask yourself:

### Question 1

> Did I accidentally make one capability dependent on another capability's internal implementation?

If yes, fix it.

### Question 2

> Did I put feature-specific code into core because it was convenient?

If yes, move it back unless there is a strong architectural reason.

### Question 3

> Did I create global state merely because it was easy?

If yes, reconsider ownership.

### Question 4

> Did I introduce a new abstraction without a real need?

If yes, simplify.

### Question 5

> Did I change persisted data?

If yes, verify migration.

### Question 6

> Can Android kill this process while the operation is running?

If yes, design recovery.

### Question 7

> What happens if permission is denied?

There must be a defined behavior.

### Question 8

> What happens if the network disappears?

There must be a defined behavior where networking is involved.

### Question 9

> What happens if input is malicious?

There must be validation.

### Question 10

> What happens if the user performs actions in an unexpected order?

The state machine must remain valid.

---

# 382. FINAL PHILOSOPHICAL MODEL

Think about the application as a **city**, not a building.

The city has:

```text
roads
utilities
security
traffic rules
public infrastructure
```

These correspond to:

```text
core
navigation
storage
security
design system
background infrastructure
```

Then there are neighborhoods:

```text
Calculator
Routine
Cleaner
Digital Wellbeing
Pomodoro
Downloader
```

Each neighborhood has:

```text
its own buildings
its own internal organization
its own responsibilities
```

But they share:

```text
roads
utilities
rules
```

A good city does not give every neighborhood its own sewer system.

But it also does not put every household's furniture into the public infrastructure.

That is exactly the balance required here.

---

# 383. FINAL ARCHITECTURAL VISION

The long-term application should evolve toward something conceptually similar to:

```text
                         ┌─────────────────────┐
                         │      ANDROID APP     │
                         └──────────┬──────────┘
                                    │
                 ┌──────────────────┴──────────────────┐
                 │                                     │
                 │              PLATFORM               │
                 │                                     │
                 │  Design System                      │
                 │  Navigation                         │
                 │  Persistence                         │
                 │  Database                            │
                 │  Security                            │
                 │  Permissions                         │
                 │  Networking                          │
                 │  Background Work                     │
                 │  Notifications                       │
                 │  Logging                             │
                 │  Testing                             │
                 │  Common Infrastructure               │
                 │                                     │
                 └──────────────────┬──────────────────┘
                                    │
        ┌───────────────────────────┼────────────────────────────┐
        │                           │                            │
        ▼                           ▼                            ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│   ROUTINE     │           │  CALCULATOR   │           │    CLEANER    │
├───────────────┤           ├───────────────┤           ├───────────────┤
│ Presentation  │           │ Presentation  │           │ Presentation  │
│ Domain        │           │ Domain        │           │ Domain        │
│ Data          │           │ Data          │           │ Data          │
│ Infrastructure│           │ Infrastructure│           │ Infrastructure│
│ Tests         │           │ Tests         │           │ Tests         │
└───────────────┘           └───────────────┘           └───────────────┘

        ┌───────────────────────┐       ┌────────────────────────┐
        │ DIGITAL WELLBEING    │       │       POMODORO         │
        ├───────────────────────┤       ├────────────────────────┤
        │ Presentation          │       │ Presentation            │
        │ Domain                │       │ Domain                  │
        │ Data                  │       │ Data                    │
        │ Android Integration   │       │ Background/Timer        │
        │ Tests                 │       │ Tests                   │
        └───────────────────────┘       └────────────────────────┘

                         ┌─────────────────────┐
                         │      DOWNLOADER     │
                         ├─────────────────────┤
                         │ Presentation        │
                         │ Domain              │
                         │ Download Engine     │
                         │ Storage             │
                         │ Network             │
                         │ Background Work     │
                         │ Security            │
                         │ Tests               │
                         └─────────────────────┘
```

The exact implementation may differ.

The **principles must not**.

---

# 384. THE MOST IMPORTANT RULE OF ALL

Whenever you modify this repository, do not ask only:

> "How can I make this code work?"

Ask:

> "How can I make this code work while preserving the capability boundary, minimizing coupling, protecting user data, respecting Android's lifecycle and security model, maintaining the shared design system, and making future changes safer rather than harder?"

That is the engineering standard.

---

# 385. AGENT ACKNOWLEDGEMENT

Before beginning major implementation work, internally verify that you understand:

```text
This is a capability-oriented application.

Capabilities own their domains.

Core owns shared infrastructure.

Features must not reach into other features' internals.

Stable contracts are preferred over hidden coupling.

Persistent user data is a compatibility contract.

Android lifecycle must be assumed hostile to in-memory assumptions.

Security must be designed at boundaries.

UI must follow the shared design system.

Testing must verify both behavior and architecture.

Generated code is not automatically trustworthy.

Architecture must evolve deliberately.

The goal is long-term software quality, not merely immediate task completion.
```

---

# 386. FINAL DIRECTIVE TO THE AGENT

You are an engineering agent working inside a **long-lived, high-end Android application**.

Your responsibility is not simply to satisfy the current prompt.

Your responsibility is to leave the repository **better architecturally than you found it**, without introducing unnecessary scope or breaking existing functionality.

For every task:

```text
UNDERSTAND
    ↓
INSPECT
    ↓
MAP
    ↓
ANALYZE IMPACT
    ↓
DESIGN
    ↓
IMPLEMENT
    ↓
TEST
    ↓
ATTACK YOUR OWN IMPLEMENTATION
    ↓
VERIFY
    ↓
DOCUMENT
    ↓
REPORT
```

Never skip directly from:

```text
USER REQUEST
```

to:

```text
CODE
```

without understanding the architecture.

Never optimize for:

```text
shortest code
fastest implementation
most abstractions
most modules
most features
```

Optimize for:

```text
correctness
clarity
safety
maintainability
modularity
testability
security
performance
accessibility
recoverability
evolvability
```

The application should eventually become a system where:

> **New capabilities can be introduced independently. Existing capabilities can evolve independently. Shared infrastructure remains stable. User data survives versions. Failures remain contained. Security boundaries remain explicit. The UI remains coherent. The dependency graph remains understandable. And every future agent can continue the work without having to rediscover the entire architecture from scratch.**

That is the objective.

That is the architecture.

That is the philosophy.

**Treat this document as a permanent engineering constitution for the project.**
