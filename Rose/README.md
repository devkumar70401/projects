# 🌹 ROSE — Modular Voice-Activated System (v0.1 Timer Milestone)

ROSE is a modular, event-driven personal AI architecture implemented in **pure C** following the **Lego Brick Contract**.

---

## 🧱 Architecture & Project Layout

```
Rose/
├── core/
│   ├── main.c                  # Kernel: boots bricks, owns main loop
│   ├── event_bus.c / .h        # Thread-safe Publish/Subscribe engine with deep-copy lifecycle
│   ├── events.h                # Event types and payload struct definitions
│   └── brick.h                 # Universal Brick interface contract
│
├── bricks/
│   ├── audio_capture/          # Brick 1: Always-on microphone stream
│   ├── transcription/          # Brick 2: Offline speech-to-text (Vosk C API)
│   ├── wake_word/              # Brick 3: Wake-word detection ("Rose")
│   ├── command_router/         # Brick 4: Natural phrasing command matcher
│   └── tone_player/            # Brick 5: Tone and notification player
│
├── subsystems/
│   └── timer/
│       ├── duration_parser.c   # Parses "10 minutes" -> 600 seconds
│       ├── countdown_engine.c  # Monotonic countdown worker thread
│       └── timer_subsystem.c   # Subsystem wiring
│
├── third_party/
│   └── vosk/                   # Offline Vosk STT shared library & C header
│
├── models/
│   └── vosk-model-small-en-us/ # Offline acoustic model (resolved relative to binary)
│
├── tests/                      # Isolated test drivers (Steps 1 through 8)
├── bin/                        # Compiled binaries (rose, rose_sim, test drivers)
└── Makefile                    # Unified build & test orchestrator
```

---

## ⚡ Quick Start

### 1. Build the Binary
Compile the complete system and test suite with zero warnings:
```bash
make clean && make
```

### 2. Run All 8 Isolated Brick Tests
Verify all architectural milestones in sequence:
```bash
make test
```

### 3. Run ROSE Live Voice System
ROSE resolves its offline Vosk acoustic model path dynamically relative to the executable (`bin/../models/vosk-model-small-en-us`), meaning you can run it from any directory:
```bash
./bin/rose
```
Say: **"Rose, start timer of 10 seconds"** (or any duration like `1 minute`, `5 minutes`).

### 4. Interactive REPL Simulator (No Mic Needed)
Test voice commands via keyboard input:
```bash
./bin/rose_sim
```

---

## 🔍 Diagnostic Logs & Audio Troubleshooting

- **Audio Subsystem Error Logs**: If audio capture or playback is silent, check `/tmp/rose_audio_errors.log` for hardware driver / pipe diagnostic messages:
  ```bash
  cat /tmp/rose_audio_errors.log
  ```
- **Bluetooth Neckbands**: Ensure your Bluetooth audio device is set to **Handsfree / Headset (HSP/HFP)** profile so the microphone stream is exposed to Linux ALSA / PipeWire.
