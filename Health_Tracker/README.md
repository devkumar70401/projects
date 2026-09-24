# P4: Health Tracker & Biological Architecture Cadence

Unified system cadence and biometric tracking suite comprising:
1. **System Cadence & Decoder Matrix**: Pure Roman temporal scheduling (`SCHEDULE.md`), biological decoder (`DECODER.md`), and single-page Typst compiled PDF (`SCHEDULE.pdf`).
2. **Native Android Application (`android/`)**: Standalone APK with exact `AlarmManager` wakeup triggers, foreground audio service (`AlarmAudioService`) playing Stephen Chow's *Li Xianglan*, interactive Roman matrix, CCleaner-style deep cleaner, system telemetry monitor, and Master Decoder key dialog.

---

## Directory Structure

```text
P4_Health_Tracker/
├── MODULAR_ARCHITECTURE_CONSTITUTION.md # Permanent capability architecture law
├── architecture_philosophy.md # Master engineering constitution & guidelines
├── SCHEDULE.md          # 16-phase encrypted Roman temporal timetable
├── DECODER.md           # Biological mechanisms, nutritional ciphers & workout circuits
├── SCHEDULE.pdf         # Typst-compiled single-page reference sheet
├── schedule.toml        # Unified mobile cadence configuration
├── features/            # Independent Capability Domains (Contracts & Specs)
│   ├── calculator/      # Scientific & Programmable Calculator domain
│   ├── routine/         # Circadian cadence & checklist domain
│   ├── cleaner/         # Deep system cache & storage cleaner domain
│   ├── digital-wellbeing/# Screen time & habits analytics domain
│   ├── pomodoro/        # Focus timer & interval countdown domain
│   ├── telemetry/       # Hardware & system vitals monitor domain
│   └── downloader/      # High-Fidelity Python yt-dlp media downloader
├── core/                # Shared Cross-Cutting Platform Infrastructure
│   ├── design-system/   # Design tokens, color palette, typography
│   ├── ui/              # Reusable premium widgets & controls
│   ├── security/        # Biometrics authentication & guards
│   ├── sound/           # Feedback chimes & audio preferences
│   ├── theme/           # VS Code Dark & Slate Light theme engine
│   └── features/        # Capability registry & dynamic tile grid
└── android/             # Native Android Application (ART)
    ├── src/com/devendra/healthtracker/
    │   ├── biometrics/  # Daily hydration & nutrition macro state
    │   ├── calculator/  # 23-file high-precision AST & matrix math suite
    │   ├── cleaner/     # Cache scanner & AccessibilityService automation
    │   ├── downloader/  # 16-file media resolvers, extractors, services
    │   ├── features/    # ReorderableFeatureGrid & FeatureRegistry
    │   ├── pomodoro/    # Focus timer, service, receiver, widget
    │   ├── routine/     # ScheduleManager, Checklist, AlarmAudioService
    │   ├── sound/       # SoundManager feedback engine
    │   ├── telemetry/   # DeviceTelemetryManager hardware stats
    │   ├── theme/       # ThemeManager & ThemeToggleView
    │   ├── ui/          # Premium UI components & design tokens
    │   ├── wellbeing/   # WellbeingManager & multi-day rollover history
    │   ├── MainActivity.java
    │   ├── AppEngineManager.java
    │   └── R.java
    ├── assets/          # Embedded Python capability assets
    ├── res/             # Android layouts, drawables, strings, audio
    ├── tests/           # Automated 10-suite unit test runner
    ├── AndroidManifest.xml # Exact alarm, service & receiver permissions
    ├── build_apk.sh     # Standalone SDK compiler toolchain (aapt2, javac, d8, apksigner)
    └── dist/            # Compiled HealthTracker.apk
```

---

## Audio & Alarm Architecture (Android)

* **Audio Asset:** *Li Xianglan* (李香蘭) performed by Stephen Chow in *From Beijing with Love* (1994), embedded in `android/res/raw/li_xiang_lan.mp3`.
* **Foreground Service (`AlarmAudioService`):**
  * Dedicated `FOREGROUND_SERVICE_MEDIA_PLAYBACK` service ensures Android low-memory killer (LMK) never interrupts playback midway.
  * Requests `AudioManager.AUDIOFOCUS_GAIN` with `USAGE_ALARM` and `setWillPauseWhenDucked(false)`, guaranteeing that pulling down the notification drawer never pauses or ducks the audio.
  * Integrated **`[⏹ Stop Song]`** action both on the heads-up notification and in-app button for instant audio termination.
