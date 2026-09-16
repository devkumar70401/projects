# Downloader Capability (Capability-Oriented Architecture)

**Capability Name:** Downloader  
**Primary Engine:** Python (`yt-dlp` based on `/home/dev/SE/Library/Scripts/YouTube_Ingestion/yt_downloader_cli.py`)  
**Public Contract:** `com.devendra.healthtracker.downloader.DownloaderContract`  
**Bridge Implementation:** `com.devendra.healthtracker.downloader.PythonDownloaderBridge`  
**Architecture Mandate:** [MODULAR_ARCHITECTURE_CONSTITUTION.md](../../MODULAR_ARCHITECTURE_CONSTITUTION.md) (Rule 9 & Rule 11)

---

## 1. Responsibilities & Boundaries

### Owned Responsibilities:
* High-fidelity original MP4 extraction and downloading (`bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best`).
* Node.js cipher & n-sig resolution.
* Zero-re-encoding stream muxing.
* Format resolution and metadata parsing for YouTube media streams.
* Independent CLI execution (`yt_downloader_cli.py`).
* Real-time line-delimited JSON IPC streaming (`downloader_ipc.py`).

### Non-Responsibilities:
* Does NOT own the Android global UI theme or shell navigation (owned by `app` and `core/design-system`).
* Does NOT own other platform media extraction (Instagram, WhatsApp, etc. owned by their respective modules).
* Does NOT persist or leak mutable state into other capabilities.

---

## 2. Directory Layout

```text
features/downloader/
├── README.md                      # Capability architectural specification
├── contract/
│   └── DownloaderContract.java    # Public Java API contract
├── bridge/
│   └── PythonDownloaderBridge.java# Android bridge connecting ART to Python engine
└── python/
    ├── __init__.py                # Package initializer
    ├── yt_downloader.py          # Core yt-dlp downloading and stream extraction engine
    ├── yt_downloader_cli.py      # Direct CLI interface matching yt_downloader_cli.py
    ├── downloader_ipc.py         # JSON IPC server for programmatic Android execution
    └── requirements.txt          # Python dependencies (yt-dlp)
```

---

## 3. Execution Modes

### A. Standalone CLI Mode:
Can be executed directly from terminal just like `yt_downloader_cli.py`:

```bash
# Interactive zero-quote mode:
python3 features/downloader/python/yt_downloader_cli.py

# Direct CLI download:
python3 features/downloader/python/yt_downloader_cli.py "https://youtu.be/..."

# Custom output directory:
python3 features/downloader/python/yt_downloader_cli.py "https://youtu.be/..." --dir ~/Movies
```

### B. Programmatic JSON IPC Mode:
```bash
# Ping / Health check:
python3 features/downloader/python/downloader_ipc.py --ping

# Extract media metadata & stream options:
python3 features/downloader/python/downloader_ipc.py --extract "https://youtu.be/..."

# Download media with live JSON progress events:
python3 features/downloader/python/downloader_ipc.py --download "https://youtu.be/..." --out /tmp
```

### C. Android App Integration:
The Android app consumes the capability via `DownloaderContract`:
```java
PythonDownloaderBridge bridge = PythonDownloaderBridge.getInstance(context);
bridge.extractMediaAsync(url, new DownloaderContract.ExtractionListener() {
    @Override
    public void onSuccess(ExtractedMedia media) { ... }
    @Override
    public void onError(Exception error) { ... }
});
```
If Python is available on the host/device, it leverages the full power of `yt-dlp`. If Python is not present, it automatically and gracefully falls back to native Java extraction.
