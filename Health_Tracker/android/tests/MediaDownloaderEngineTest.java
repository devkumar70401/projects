package com.devendra.healthtracker.tests;

import com.devendra.healthtracker.downloader.DownloadJob;
import com.devendra.healthtracker.downloader.ExtractedMedia;
import com.devendra.healthtracker.downloader.InstagramExtractorEngine;
import com.devendra.healthtracker.downloader.MediaResolverRegistry;
import com.devendra.healthtracker.downloader.MediaSource;
import com.devendra.healthtracker.downloader.StreamFormat;
import com.devendra.healthtracker.downloader.WhatsAppStatusHarvester;
import com.devendra.healthtracker.downloader.YouTubeExtractorEngine;

import java.util.ArrayList;
import java.util.List;

public class MediaDownloaderEngineTest {

    private static int assertionsCount = 0;

    private static void assertTrue(boolean condition, String message) {
        assertionsCount++;
        if (!condition) {
            throw new AssertionError("FAILED: " + message);
        }
        System.out.println("  ✓ " + message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        assertionsCount++;
        if (expected == null && actual == null) {
            System.out.println("  ✓ " + message);
            return;
        }
        if (expected != null && expected.equals(actual)) {
            System.out.println("  ✓ " + message);
            return;
        }
        throw new AssertionError("FAILED: " + message + " (Expected: " + expected + ", Got: " + actual + ")");
    }

    public static void run() {
        System.out.println("\n--------------------------------------------------");
        System.out.println("Running MediaDownloaderEngineTest (Native Production Suite)...");
        System.out.println("--------------------------------------------------");

        testYouTubeIdExtraction();
        testInstagramShortcodeExtraction();
        testMediaSourceDetection();
        testUniversalMediaSourcesAndOptions();
        testStreamFormatAndBytesFormatting();
        testDownloadJobStateLifecycle();
        testUrlTokenExtraction();
        testWhatsAppStatusModel();

        System.out.println("✅ All " + assertionsCount + " MediaDownloaderEngineTest assertions PASSED!");
    }

    private static void testYouTubeIdExtraction() {
        assertEquals("dQw4w9WgXcQ", YouTubeExtractorEngine.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"), "Standard YouTube watch link");
        assertEquals("dQw4w9WgXcQ", YouTubeExtractorEngine.extractVideoId("https://youtu.be/dQw4w9WgXcQ?si=abcdef"), "Short YouTube youtu.be link");
        assertEquals("dQw4w9WgXcQ", YouTubeExtractorEngine.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"), "YouTube Shorts link");
        assertEquals("dQw4w9WgXcQ", YouTubeExtractorEngine.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"), "YouTube Embed link");
        assertEquals("dQw4w9WgXcQ", YouTubeExtractorEngine.extractVideoId("dQw4w9WgXcQ"), "Raw 11-char video ID");
        assertEquals(null, YouTubeExtractorEngine.extractVideoId("https://google.com"), "Invalid video URL returns null");
    }

    private static void testInstagramShortcodeExtraction() {
        assertEquals("C_12345Abc", InstagramExtractorEngine.extractShortcode("https://www.instagram.com/reel/C_12345Abc/?igsh=xyz"), "Instagram Reel URL");
        assertEquals("D_98765Xyz", InstagramExtractorEngine.extractShortcode("https://www.instagram.com/p/D_98765Xyz/"), "Instagram Post URL");
        assertEquals("3214567890", InstagramExtractorEngine.extractShortcode("https://www.instagram.com/stories/username/3214567890/"), "Instagram Story URL");
        assertEquals(null, InstagramExtractorEngine.extractShortcode("https://twitter.com/post/123"), "Non-Instagram URL returns null");
    }

    private static void testMediaSourceDetection() {
        assertEquals(MediaSource.YOUTUBE, MediaSource.detectFromUrl("https://www.youtube.com/watch?v=abc"), "Detects YouTube watch");
        assertEquals(MediaSource.YOUTUBE, MediaSource.detectFromUrl("https://youtu.be/abc"), "Detects youtu.be");
        assertEquals(MediaSource.INSTAGRAM, MediaSource.detectFromUrl("https://www.instagram.com/reel/xyz"), "Detects Instagram");
        assertEquals(MediaSource.TWITTER, MediaSource.detectFromUrl("https://twitter.com/user/status/123456789"), "Detects Twitter");
        assertEquals(MediaSource.TWITTER, MediaSource.detectFromUrl("https://x.com/user/status/987654321"), "Detects X.com");
        assertEquals(MediaSource.TIKTOK, MediaSource.detectFromUrl("https://www.tiktok.com/@creator/video/12345"), "Detects TikTok");
        assertEquals(MediaSource.TIKTOK, MediaSource.detectFromUrl("https://vm.tiktok.com/ZM8.../"), "Detects TikTok shortlink");
        assertEquals(MediaSource.REDDIT, MediaSource.detectFromUrl("https://www.reddit.com/r/videos/comments/abc/title/"), "Detects Reddit post");
        assertEquals(MediaSource.REDDIT, MediaSource.detectFromUrl("https://v.redd.it/abcdef123"), "Detects v.redd.it video");
        assertEquals(MediaSource.FACEBOOK, MediaSource.detectFromUrl("https://www.facebook.com/watch/?v=123"), "Detects Facebook watch");
        assertEquals(MediaSource.FACEBOOK, MediaSource.detectFromUrl("https://fb.watch/xyz123"), "Detects fb.watch");
        assertEquals(MediaSource.PINTEREST, MediaSource.detectFromUrl("https://www.pinterest.com/pin/12345/"), "Detects Pinterest");
        assertEquals(MediaSource.WHATSAPP, MediaSource.detectFromUrl("whatsapp://statuses"), "Detects WhatsApp");
        assertEquals(MediaSource.GENERIC_WEB, MediaSource.detectFromUrl("https://example.com/video.mp4"), "Detects Direct Web file");
    }

    private static void testUniversalMediaSourcesAndOptions() {
        List<StreamFormat> options = new ArrayList<>();
        options.add(new StreamFormat("sniffed_1", "mp4", "Web Video (MP4) #1", 2000000, false, "https://cdn.example.com/vid.mp4", null, false, 15000000, "video/mp4"));
        options.add(new StreamFormat("sniffed_2", "m3u8", "HLS Live Stream (M3U8) #2", 0, false, "https://cdn.example.com/master.m3u8", null, false, 0, "application/x-mpegurl"));
        options.add(new StreamFormat("sniffed_audio", "m4a", "Extracted Audio Track (M4A)", 128000, true, null, "https://cdn.example.com/vid.mp4", false, 0, "audio/mp4"));

        ExtractedMedia media = new ExtractedMedia(
                "sniff_01",
                "Sample Web Stream Video",
                "https://cdn.example.com/thumb.jpg",
                MediaSource.GENERIC_WEB,
                "cdn.example.com",
                120,
                null,
                options
        );

        assertEquals(3, media.streamOptions.size(), "Universal media has 3 stream options");
        assertEquals(2, media.getVideoOptions().size(), "Has 2 video options");
        assertEquals(1, media.getAudioOptions().size(), "Has 1 audio option");
        assertEquals("2:00", media.getFormattedDuration(), "Formatted duration 2:00");
    }

    private static void testStreamFormatAndBytesFormatting() {
        StreamFormat fmt = new StreamFormat(
                "yt_dash_137",
                "mp4",
                "1080p",
                4500000,
                false,
                "https://video.url",
                "https://audio.url",
                true,
                52428800, // 50 MB
                "video/mp4"
        );
        assertEquals("1080p", fmt.resolution, "Format resolution");
        assertTrue(fmt.requiresMuxing, "Requires muxing is true for DASH");
        assertEquals("50.0 MB", fmt.getFormattedSize(), "Formats 50 MB correctly");
        assertTrue(fmt.getLabel().contains("HD Muxed"), "Label indicates HD Muxed");
    }

    private static void testDownloadJobStateLifecycle() {
        StreamFormat fmt = new StreamFormat("direct", "mp4", "720p", 2000000, false, "http://a.com", null, false, 10485760, "video/mp4");
        DownloadJob job = new DownloadJob("job_001", "Test Video", MediaSource.YOUTUBE, fmt, "Movies/RoseMedia");

        assertEquals(DownloadJob.State.QUEUED, job.state, "Initial state is QUEUED");
        assertEquals(0, job.getProgressPercent(), "Initial progress is 0%");

        job.state = DownloadJob.State.DOWNLOADING;
        job.progress = 0.55f;
        job.downloadedBytes = 5242880;
        job.totalBytes = 10485760;
        job.speedBytesPerSec = 2097152; // 2 MB/s

        assertEquals(55, job.getProgressPercent(), "Calculates 55% progress");
        assertEquals("2.00 MB/s", job.getFormattedSpeed(), "Calculates formatted speed 2.00 MB/s");
        assertEquals("5.0 MB / 10.0 MB", job.getFormattedProgressBytes(), "Calculates progress bytes text");
    }

    private static void testUrlTokenExtraction() {
        String sharedText = "Check out this reel! https://www.instagram.com/reel/C89abcdef/ it is amazing!";
        String extracted = MediaResolverRegistry.extractUrl(sharedText);
        assertEquals("https://www.instagram.com/reel/C89abcdef/", extracted, "Extracts URL from messy shared text");
    }

    private static void testWhatsAppStatusModel() {
        WhatsAppStatusHarvester.StatusItem item = new WhatsAppStatusHarvester.StatusItem(
                "wa_01",
                "status_video.mp4",
                null,
                null,
                true,
                3145728, // 3 MB
                System.currentTimeMillis()
        );
        assertTrue(item.isVideo, "Status item is video");
        assertEquals("3.0 MB", item.getFormattedSize(), "Formats status item size");
    }
}
