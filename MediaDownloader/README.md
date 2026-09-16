# Universal Media Downloader & WhatsApp Status Harvester

Standalone, high-performance, unrestricted Android media extraction and harvesting engine extracted from Health Tracker (`P4_Health_Tracker`).

---

## 🌟 Key Features

1. **Universal Stream Extraction Engines**:
   * **YouTube HD**: Multi-client extraction pipeline (TVHTML5 Embedded bypass, VisionOS, Web Embedded, Android 21.x, iOS clients, and Invidious/Piped public mirror fallback). Extracts both combined 720p/360p streams and separate high-bitrate 1080p video + 128kbps audio DASH streams.
   * **Instagram**: Reels, posts, and IGTV extraction via direct mobile JSON API endpoints with OpenGraph fallback.
   * **Twitter / X**: Twimg public syndication API integration for multi-bitrate MP4 streams and audio extraction.
   * **TikTok**: Watermark-free stream parsing and video resolution.
   * **Reddit**: Video and audio DASH extraction from `v.redd.it` posts with automatic hardware muxing.
   * **Facebook & Pinterest**: Public video stream sniffers.
   * **Direct Web / HTML5**: Deep HTML sniffing for embedded MP4, M3U8, WebM, MOV, and MP3 media links.

2. **Native Hardware Media Muxing (`NativeMediaMuxer`)**:
   * Synchronizes separate video-only (1080p DASH) and audio-only (M4A/AAC) streams into a single MP4 container using Android's native hardware `MediaMuxer` and `MediaExtractor`.
   * Requires **zero external FFmpeg binaries** or native C++ dependencies, keeping the APK lightweight (~726 KB).

3. **Android Scoped Storage Integration (`MediaStoreExporter`)**:
   * Saves completed media directly to system-indexed public folders:
     * Videos: `/storage/emulated/0/Movies/RoseMedia/`
     * Audio: `/storage/emulated/0/Music/RoseMedia/`
     * Photos: `/storage/emulated/0/Pictures/RoseMedia/`
   * Complies with Android 10+ (Q) through Android 14+ Scoped Storage guidelines (`MediaStore.MediaColumns.IS_PENDING`).

4. **WhatsApp Status Harvester (`WhatsAppStatusHarvester`)**:
   * Offline status viewer and 1-tap saver to system gallery.
   * Scans both direct filesystem paths and Android 11+ Storage Access Framework (SAF) folder tree (`Android/media/com.whatsapp/WhatsApp/Media/.Statuses`).

5. **Download Manager & Foreground Service**:
   * Thread pool executor with pause, resume (HTTP `Range: bytes=X-` support), cancel, and dismiss actions.
   * Android `dataSync` Foreground Service with low-priority non-intrusive ongoing notifications displaying live download speed (KB/s, MB/s) and progress percentages.

6. **System Share Intent Target**:
   * Configured with `android.intent.action.SEND` for `text/plain` MIME types.
   * Tap "Share" from YouTube, Twitter, Instagram, TikTok, Reddit, or Chrome and select **Media Downloader** to automatically load and analyze the link.

7. **Theme Engine**:
   * Instant toggle between Slate Light and VS Code-inspired dark theme.

---

## 📁 Project Structure

```text
MediaDownloader/
├── AndroidManifest.xml           # Permissions, activities, and foreground service
├── build_apk.sh                  # Standalone SDK compiler toolchain (aapt2, javac, d8, apksigner)
├── debug.keystore                # Android debug signing keystore
├── README.md                     # Documentation
├── dist/
│   └── MediaDownloader.apk       # Ready-to-install signed APK
├── res/                          # Drawables, mipmaps, strings, colors
└── src/
    └── com/devendra/mediadownloader/
        ├── MainActivity.java             # Standalone UI & share handler
        ├── MediaDownloadManager.java     # Concurrent download queue & worker engine
        ├── MediaDownloadService.java     # Foreground service for downloads
        ├── DownloadJob.java              # Download state machine model
        ├── ExtractedMedia.java           # Unified media container model
        ├── StreamFormat.java             # Media stream metadata
        ├── MediaSource.java              # Platform detection & branding
        ├── MediaResolverRegistry.java    # Central URL router & web parser
        ├── MediaStoreExporter.java       # Scoped Storage gallery exporter
        ├── NativeMediaMuxer.java         # Hardware MediaMuxer video/audio combiner
        ├── AsyncThumbnailLoader.java     # LruCache image loader for preview cards
        ├── YouTubeExtractorEngine.java   # Multi-client YouTube extractor
        ├── InstagramExtractorEngine.java # Instagram Reels/Post extractor
        ├── UniversalWebExtractor.java    # Twitter, TikTok, Reddit, FB, Web extractor
        ├── WhatsAppStatusHarvester.java  # WhatsApp status scanner & exporter
        ├── theme/
        │   └── ThemeManager.java         # Dynamic theme switcher (Dark / Light)
        └── ui/
            ├── components/
            │   ├── PremiumButton.java    # Soft-tactile spring buttons
            │   ├── PremiumCard.java      # Elevated surface cards
            │   └── PremiumIconButton.java# Touch-accessible icon buttons
            └── design/
                ├── DesignTokens.java     # Colors, elevation, radius scales
                └── TouchFeedback.java    # iOS-style tactile touch interactions
```

---

## 🛠️ Build & Install

### Requirements
* Android SDK Platform 34 (`android-34`)
* Android Build Tools 34.0.0 (`aapt2`, `d8`, `zipalign`, `apksigner`)
* JDK 17+ (`javac`)

### Compiling
Run the standalone build script:
```bash
cd /home/dev/SE/Projects/MediaDownloader
./build_apk.sh
```

### Installing on Device
```bash
adb install dist/MediaDownloader.apk
```
