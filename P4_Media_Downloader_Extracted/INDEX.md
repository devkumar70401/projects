# P4 Health Tracker - Media Downloader Raw Code Extract

This archive contains raw, uncompiled source code files extracted directly from:
`/home/dev/SE/Projects/P4_Health_Tracker`

---

## 📂 Contents

1. **`PROJECT_TREE.txt`**:
   The full tree structure of the entire `P4_Health_Tracker` project.

2. **`raw_code/`**:
   * **`YouTubeExtractorEngine.java`**: Multi-client YouTube stream extractor (TVHTML5 Embedded bypass, VisionOS, Web Embedded, Android 21.x, iOS, Invidious/Piped fallbacks, DASH deciphering).
   * **`InstagramExtractorEngine.java`**: Direct JSON endpoint and OpenGraph fallback for Reels, Posts, and Stories.
   * **`UniversalWebExtractor.java`**: Deep extractor for Twitter/X (twimg syndication), TikTok (clean video), Reddit (`v.redd.it`), Facebook, Pinterest, and HTML5 video/audio streams.
   * **`WhatsAppStatusHarvester.java`**: Scans cached statuses from direct paths and Android 11+ SAF tree (`.Statuses`), and exports directly to Gallery.
   * **`MediaResolverRegistry.java`**: URL router, platform detection, and direct stream resolver.
   * **`MediaSource.java`**: Supported platform enum and URL classifier.
   * **`StreamFormat.java`**: Stream metadata model (bitrate, resolution, muxing flag, file size).
   * **`ExtractedMedia.java`**: Media item model containing metadata and stream options.
   * **`DownloadJob.java`**: State machine model (QUEUED, DOWNLOADING, MUXING, COMPLETED, PAUSED, ERROR).
   * **`MediaDownloadManager.java`**: Concurrent download queue worker engine, HTTP byte-range resume support, progress and speed calculations.
   * **`MediaDownloadService.java`**: Android foreground service for background downloading with notification progress.
   * **`NativeMediaMuxer.java`**: Android hardware `MediaMuxer` combiner for separate DASH video and audio tracks (zero FFmpeg dependency).
   * **`MediaStoreExporter.java`**: Scoped Storage exporter to system galleries (`Movies/RoseMedia`, `Music/RoseMedia`, `Pictures/RoseMedia`).
   * **`AsyncThumbnailLoader.java`**: LruCache image loader for thumbnail previews.
   * **`MediaDownloaderEngineTest.java`**: Unit test suite for ID extraction, platform detection, format parsing, and download job lifecycles.
   * **`MainActivity_MediaDownloader_UI_Snippet.java`**: Exact lines 4040 to 4853 from `MainActivity.java` containing the UI card layout, video preview, format options, WhatsApp harvester UI, download queue list, and incoming share intent handler (`android.intent.action.SEND`).
   * **`AndroidManifest_Media_Snippet.xml`**: Relevant permissions, service declaration, and share intent filter.
   * **`AndroidManifest_P4_Original.xml`**: Full original `AndroidManifest.xml` from `P4_Health_Tracker`.
