package com.devendra.mediadownloader;

import android.net.Uri;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MediaResolverRegistry {

    public static ExtractedMedia resolve(String rawUrl) throws Exception {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("URL is empty. Please enter a valid media link.");
        }

        String cleanUrl = rawUrl.trim();
        MediaSource source = MediaSource.detectFromUrl(cleanUrl);

        switch (source) {
            case YOUTUBE:
                return YouTubeExtractorEngine.extract(cleanUrl);

            case INSTAGRAM:
                return InstagramExtractorEngine.extract(cleanUrl);

            case TWITTER:
                return UniversalWebExtractor.extractTwitter(cleanUrl);

            case TIKTOK:
                return UniversalWebExtractor.extractTikTok(cleanUrl);

            case REDDIT:
                return UniversalWebExtractor.extractReddit(cleanUrl);

            case FACEBOOK:
                return UniversalWebExtractor.extractFacebook(cleanUrl);

            case PINTEREST:
                return UniversalWebExtractor.extractPinterest(cleanUrl);

            case GENERIC_WEB:
            case WHATSAPP:
            default:
                return UniversalWebExtractor.extractGeneric(cleanUrl);
        }
    }

    private static ExtractedMedia resolveDirectWebMedia(String cleanUrl) throws Exception {
        URL u = new URL(cleanUrl);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod("HEAD");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");

        int code = conn.getResponseCode();
        if (code >= 400 || code == -1) {
            // Try GET with Range 0-0 if HEAD not allowed
            conn.disconnect();
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Range", "bytes=0-0");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
            code = conn.getResponseCode();
        }

        String contentType = conn.getContentType();
        long contentLength = conn.getContentLengthLong();
        conn.disconnect();

        String filename = extractFilename(cleanUrl, contentType);
        String ext = getExtension(filename);
        boolean isAudio = contentType != null && contentType.startsWith("audio/");
        boolean isVideo = contentType != null && contentType.startsWith("video/");
        boolean isImage = contentType != null && contentType.startsWith("image/");

        String label = (isVideo ? "Direct Video" : (isAudio ? "Direct Audio" : (isImage ? "Direct Image" : "Web File")));
        List<StreamFormat> options = new ArrayList<>();
        options.add(new StreamFormat(
                "direct_file",
                ext,
                label,
                0,
                isAudio,
                cleanUrl,
                null,
                false,
                contentLength,
                contentType != null ? contentType : "application/octet-stream"
        ));

        return new ExtractedMedia(
                cleanUrl,
                filename,
                "",
                MediaSource.GENERIC_WEB,
                u.getHost(),
                0,
                null,
                options
        );
    }

    public static String extractUrl(String text) {
        if (text == null) return null;
        String[] tokens = text.split("\\s+");
        for (String t : tokens) {
            if (t.startsWith("http://") || t.startsWith("https://")) {
                return t.trim();
            }
        }
        return null;
    }

    private static String extractFilename(String url, String contentType) {
        try {
            Uri uri = Uri.parse(url);
            String last = uri.getLastPathSegment();
            if (last != null && last.contains(".")) {
                return last;
            }
        } catch (Exception ignored) {}

        String ext = "bin";
        if (contentType != null) {
            if (contentType.contains("video/mp4")) ext = "mp4";
            else if (contentType.contains("audio/mpeg")) ext = "mp3";
            else if (contentType.contains("audio/mp4")) ext = "m4a";
            else if (contentType.contains("image/jpeg")) ext = "jpg";
            else if (contentType.contains("image/png")) ext = "png";
        }
        return "download_" + System.currentTimeMillis() + "." + ext;
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0 && dot < filename.length() - 1) {
            return filename.substring(dot + 1).toLowerCase();
        }
        return "mp4";
    }
}
