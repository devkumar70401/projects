package com.devendra.healthtracker.downloader;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeExtractorEngine {
    private static final String TAG = "YouTubeExtractor";
    private static final String INNERTUBE_API_URL = "https://www.youtube.com/youtubei/v1/player";
    private static final String VISIONOS_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15";
    private static final String WEB_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final String ANDROID_UA = "com.google.android.youtube/21.26.364 (Linux; U; Android 11) gzip";

    // Regex patterns for video IDs
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:v=|\\/v\\/|youtu\\.be\\/|\\/embed\\/|\\/shorts\\/|\\/live\\/)([a-zA-Z0-9_-]{11})");

    public static String extractVideoId(String url) {
        if (url == null) return null;
        Matcher m = VIDEO_ID_PATTERN.matcher(url.trim());
        if (m.find()) {
            return m.group(1);
        }
        // Check if raw 11-char ID
        if (url.trim().matches("^[a-zA-Z0-9_-]{11}$")) {
            return url.trim();
        }
        return null;
    }

    public static ExtractedMedia extract(String rawUrl) throws Exception {
        String videoId = extractVideoId(rawUrl);
        if (videoId == null) {
            throw new IllegalArgumentException("Invalid YouTube URL: Could not extract video ID");
        }

        // 1. Scrape YouTube Watch Page for VISITOR_DATA, Title, and Web Player Data
        WebPageInfo webInfo = scrapeWebPage(videoId);

        // 2. Client 1: VisionOS Client (Primary yt-dlp client with direct unencrypted streams)
        JSONObject playerResponse = queryVisionOsPlayer(videoId, webInfo != null ? webInfo.visitorData : null);

        // 3. Client 2: ANDROID_VR Client (Oculus Quest 2 unencrypted direct streams)
        if (playerResponse == null || !hasUsableStreamingData(playerResponse)) {
            Log.w(TAG, "VisionOS client returned no streams for " + videoId + ", trying ANDROID_VR client...");
            JSONObject vrResponse = queryAndroidVrPlayer(videoId, webInfo != null ? webInfo.visitorData : null);
            if (vrResponse != null && hasUsableStreamingData(vrResponse)) {
                playerResponse = vrResponse;
            }
        }

        // 4. Client 3: Android 21.x Client (Returns itag 18 progressive format)
        if (playerResponse == null || !hasUsableStreamingData(playerResponse)) {
            Log.w(TAG, "ANDROID_VR returned no streams for " + videoId + ", trying Android client...");
            JSONObject androidResponse = queryAndroidPlayer(videoId, webInfo != null ? webInfo.visitorData : null);
            if (androidResponse != null && hasUsableStreamingData(androidResponse)) {
                playerResponse = androidResponse;
            }
        }

        // 5. Client 4: Web Embedded Player
        if (playerResponse == null || !hasUsableStreamingData(playerResponse)) {
            Log.w(TAG, "Android returned no streams for " + videoId + ", trying Web Embedded client...");
            JSONObject webEmbedResponse = queryWebEmbeddedPlayer(videoId, webInfo != null ? webInfo.visitorData : null);
            if (webEmbedResponse != null && hasUsableStreamingData(webEmbedResponse)) {
                playerResponse = webEmbedResponse;
            }
        }

        // 6. Client 5: iOS Client
        if (playerResponse == null || !hasUsableStreamingData(playerResponse)) {
            Log.w(TAG, "Web embed returned no streams, trying iOS client...");
            JSONObject iosResponse = queryIosPlayer(videoId, webInfo != null ? webInfo.visitorData : null);
            if (iosResponse != null && hasUsableStreamingData(iosResponse)) {
                playerResponse = iosResponse;
            }
        }

        // 7. Scraped Web Player Response
        if ((playerResponse == null || !hasUsableStreamingData(playerResponse)) && webInfo != null && webInfo.webPlayerResponse != null) {
            if (hasUsableStreamingData(webInfo.webPlayerResponse)) {
                playerResponse = webInfo.webPlayerResponse;
            }
        }

        // 8. Public Mirror Fallback
        if (playerResponse == null || !hasUsableStreamingData(playerResponse)) {
            Log.w(TAG, "Direct Innertube blocked for " + videoId + ", querying public streaming mirrors...");
            ExtractedMedia mirrorMedia = queryInvidiousMirror(videoId);
            if (mirrorMedia != null && !mirrorMedia.streamOptions.isEmpty()) {
                return mirrorMedia;
            }
        }

        // 9. Fail if no playable stream resolved
        if (playerResponse == null || !hasUsableStreamingData(playerResponse)) {
            throw new IllegalStateException("Could not resolve streaming media for " + videoId + ". YouTube requires authentication or video is region restricted.");
        }

        // 6. Extract Video Metadata
        JSONObject videoDetails = playerResponse.optJSONObject("videoDetails");
        String title = (webInfo != null && webInfo.title != null) ? webInfo.title : ("YouTube Video (" + videoId + ")");
        String author = "YouTube Creator";
        long duration = 0;
        String thumbnail = "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg";

        if (videoDetails != null) {
            title = videoDetails.optString("title", title);
            author = videoDetails.optString("author", author);
            duration = videoDetails.optLong("lengthSeconds", 0);
            JSONArray thumbs = videoDetails.optJSONObject("thumbnail") != null
                    ? videoDetails.getJSONObject("thumbnail").optJSONArray("thumbnails") : null;
            if (thumbs != null && thumbs.length() > 0) {
                thumbnail = thumbs.getJSONObject(thumbs.length() - 1).optString("url", thumbnail);
            }
        }

        // 7. Parse Streaming Formats
        JSONObject streamingData = playerResponse.getJSONObject("streamingData");
        List<StreamFormat> options = new ArrayList<>();

        // Highest quality audio track candidate for muxing
        String bestAudioUrl = null;
        long bestAudioBitrate = 0;
        long bestAudioApproxBytes = 0;

        // First pass: locate best standalone audio track in adaptiveFormats (prefer MP4/AAC)
        if (streamingData.has("adaptiveFormats")) {
            JSONArray adaptives = streamingData.getJSONArray("adaptiveFormats");
            for (int i = 0; i < adaptives.length(); i++) {
                JSONObject fmt = adaptives.getJSONObject(i);
                String mime = fmt.optString("mimeType", "");
                if (mime.startsWith("audio/")) {
                    String aUrl = resolveFormatUrl(fmt);
                    if (aUrl == null) continue;
                    long aBitrate = fmt.optLong("bitrate", 0);
                    long aContentLength = fmt.optLong("contentLength", 0);
                    boolean isAac = mime.contains("mp4a");

                    // Prefer AAC audio for universal MP4 compatibility
                    if (aBitrate > bestAudioBitrate || (isAac && bestAudioBitrate <= 130000)) {
                        bestAudioBitrate = aBitrate;
                        bestAudioUrl = aUrl;
                        bestAudioApproxBytes = aContentLength;
                    }
                }
            }
        }

        // 8. Parse Combined Progressive Streams (720p, 360p with audio included)
        if (streamingData.has("formats")) {
            JSONArray combined = streamingData.getJSONArray("formats");
            for (int i = 0; i < combined.length(); i++) {
                JSONObject fmt = combined.getJSONObject(i);
                String streamUrl = resolveFormatUrl(fmt);
                if (streamUrl == null) continue;

                String quality = fmt.optString("qualityLabel", "");
                if (quality.isEmpty()) {
                    quality = fmt.optInt("height", 0) > 0 ? (fmt.getInt("height") + "p") : "Standard";
                }
                long bitrate = fmt.optLong("bitrate", 0);
                long approxBytes = fmt.optLong("contentLength", 0);
                int itag = fmt.optInt("itag", 0);

                options.add(new StreamFormat(
                        "yt_direct_" + itag,
                        "mp4",
                        quality + " (Direct)",
                        bitrate,
                        false,
                        streamUrl,
                        null,
                        false, // Direct stream, no muxing needed
                        approxBytes,
                        fmt.optString("mimeType", "video/mp4")
                ));
            }
        }

        // 9. Parse Adaptive Video Streams (1080p, 720p, 480p, 360p requiring native muxing)
        // We deduplicate by resolution height and prefer video/mp4 (H.264 avc1) for native MediaMuxer
        if (streamingData.has("adaptiveFormats")) {
            JSONArray adaptives = streamingData.getJSONArray("adaptiveFormats");
            // Map height -> best format JSONObject
            java.util.Map<Integer, JSONObject> bestByHeight = new java.util.TreeMap<>(java.util.Collections.reverseOrder());

            for (int i = 0; i < adaptives.length(); i++) {
                JSONObject fmt = adaptives.getJSONObject(i);
                String mime = fmt.optString("mimeType", "");
                if (!mime.startsWith("video/")) continue;

                String vUrl = resolveFormatUrl(fmt);
                if (vUrl == null) continue;

                int height = fmt.optInt("height", 0);
                if (height <= 0) continue;

                boolean isAvc = mime.contains("avc1") || mime.contains("h264");
                // CRITICAL: Filter strictly for H.264 (avc1) for 100% hardware MediaMuxer compatibility
                if (!isAvc) continue;

                JSONObject existing = bestByHeight.get(height);
                if (existing == null || fmt.optLong("bitrate", 0) > existing.optLong("bitrate", 0)) {
                    bestByHeight.put(height, fmt);
                }
            }

            // Convert selected adaptive formats into StreamOptions
            for (java.util.Map.Entry<Integer, JSONObject> entry : bestByHeight.entrySet()) {
                int height = entry.getKey();
                JSONObject fmt = entry.getValue();
                String vUrl = resolveFormatUrl(fmt);
                if (vUrl == null) continue;

                String quality = fmt.optString("qualityLabel", height + "p");
                int itag = fmt.optInt("itag", 0);
                long bitrate = fmt.optLong("bitrate", 0);
                long approxBytes = fmt.optLong("contentLength", 0);
                if (bestAudioApproxBytes > 0) {
                    approxBytes += bestAudioApproxBytes;
                }
                String mime = fmt.optString("mimeType", "video/mp4");

                // If this resolution is already satisfied by a direct progressive format of the same height, skip
                boolean alreadyHaveDirect = false;
                for (StreamFormat sf : options) {
                    if (!sf.requiresMuxing && sf.resolution.startsWith(height + "p")) {
                        alreadyHaveDirect = true;
                        break;
                    }
                }
                if (alreadyHaveDirect) continue;

                String label = quality + (height >= 720 ? " HD" : "");
                options.add(new StreamFormat(
                        "yt_dash_" + itag,
                        "mp4",
                        label,
                        bitrate + bestAudioBitrate,
                        false,
                        vUrl,
                        bestAudioUrl,
                        true, // Requires native MediaMuxer
                        approxBytes,
                        mime
                ));
            }
        }

        // 10. Add Best Audio Only Option (HQ AAC M4A / MP3 compatible)
        if (bestAudioUrl != null) {
            options.add(new StreamFormat(
                    "yt_audio_best",
                    "m4a",
                    "HQ Audio (" + (bestAudioBitrate > 0 ? (bestAudioBitrate / 1000) + " kbps" : "128 kbps") + ")",
                    bestAudioBitrate,
                    true,
                    null,
                    bestAudioUrl,
                    false,
                    bestAudioApproxBytes,
                    "audio/mp4"
            ));
        }

        return new ExtractedMedia(
                videoId,
                title,
                thumbnail,
                MediaSource.YOUTUBE,
                author,
                duration,
                null,
                options
        );
    }

    private static boolean hasUsableStreamingData(JSONObject response) {
        if (response == null || !response.has("streamingData")) return false;
        JSONObject sd = response.optJSONObject("streamingData");
        if (sd == null) return false;
        JSONArray adaptives = sd.optJSONArray("adaptiveFormats");
        JSONArray formats = sd.optJSONArray("formats");
        return (adaptives != null && adaptives.length() > 0) || (formats != null && formats.length() > 0);
    }

    private static String resolveFormatUrl(JSONObject fmt) {
        // Direct URL in JSON
        if (fmt.has("url")) {
            return fmt.optString("url");
        }
        // Signature cipher fallback
        if (fmt.has("signatureCipher")) {
            return parseCipherString(fmt.optString("signatureCipher"));
        }
        if (fmt.has("cipher")) {
            return parseCipherString(fmt.optString("cipher"));
        }
        return null;
    }

    private static String parseCipherString(String cipherStr) {
        try {
            String[] parts = cipherStr.split("&");
            String url = null;
            String sig = null;
            String sp = "signature";

            for (String part : parts) {
                int eq = part.indexOf('=');
                if (eq > 0) {
                    String key = part.substring(0, eq);
                    String val = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8.name());
                    if ("url".equals(key)) {
                        url = val;
                    } else if ("s".equals(key) || "sig".equals(key)) {
                        sig = val;
                    } else if ("sp".equals(key)) {
                        sp = val;
                    }
                }
            }
            if (url != null) {
                if (sig != null) {
                    return url + (url.contains("?") ? "&" : "?") + sp + "=" + sig;
                }
                return url;
            }
        } catch (Exception e) {
            Log.e(TAG, "Cipher parse failed", e);
        }
        return null;
    }

    /**
     * TVHTML5 Embedded Innertube client.
     * Replicates SmartTV embedded player context, bypassing age gates and bot verification.
     */
    private static JSONObject queryTvEmbeddedPlayer(String videoId, String visitorData) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(INNERTUBE_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-YouTube-Client-Name", "85"); // TVHTML5_SIMPLY_EMBEDDED
            conn.setRequestProperty("X-YouTube-Client-Version", "2.0");
            conn.setRequestProperty("Origin", "https://www.youtube.com");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (SmartHub; SMART-TV; U; Linux/SmartTV; Maple2012) AppleWebKit/537.42 (KHTML, like Gecko) SmartTV Safari/537.42");
            if (visitorData != null && !visitorData.isEmpty()) {
                conn.setRequestProperty("X-Goog-Visitor-Id", visitorData);
            }
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            JSONObject context = new JSONObject();
            JSONObject client = new JSONObject();
            client.put("clientName", "TVHTML5_SIMPLY_EMBEDDED");
            client.put("clientVersion", "2.0");
            client.put("hl", "en");
            client.put("gl", "US");
            context.put("client", client);
            context.put("thirdParty", new JSONObject().put("embedUrl", "https://www.youtube.com"));
            body.put("context", context);
            body.put("videoId", videoId);
            body.put("playbackContext", new JSONObject()
                    .put("contentPlaybackContext", new JSONObject()
                            .put("html5Preference", "HTML5_PREF_WANTS")
                            .put("signatureTimestamp", 20702)));
            body.put("contentCheckOk", true);
            body.put("racyCheckOk", true);

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String responseText = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(responseText);
                if (hasUsableStreamingData(json)) {
                    return json;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "TVHTML5 query error for " + videoId + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    /**
     * Web Embedded Player client.
     */
    private static JSONObject queryWebEmbeddedPlayer(String videoId, String visitorData) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(INNERTUBE_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-YouTube-Client-Name", "56"); // WEB_EMBEDDED_PLAYER
            conn.setRequestProperty("X-YouTube-Client-Version", "1.20240401.01.00");
            conn.setRequestProperty("Origin", "https://www.youtube.com");
            conn.setRequestProperty("User-Agent", WEB_UA);
            if (visitorData != null && !visitorData.isEmpty()) {
                conn.setRequestProperty("X-Goog-Visitor-Id", visitorData);
            }
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            JSONObject context = new JSONObject();
            JSONObject client = new JSONObject();
            client.put("clientName", "WEB_EMBEDDED_PLAYER");
            client.put("clientVersion", "1.20240401.01.00");
            client.put("hl", "en");
            client.put("gl", "US");
            context.put("client", client);
            context.put("thirdParty", new JSONObject().put("embedUrl", "https://www.youtube.com"));
            body.put("context", context);
            body.put("videoId", videoId);
            body.put("playbackContext", new JSONObject()
                    .put("contentPlaybackContext", new JSONObject()
                            .put("html5Preference", "HTML5_PREF_WANTS")));
            body.put("contentCheckOk", true);
            body.put("racyCheckOk", true);

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String responseText = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(responseText);
                if (hasUsableStreamingData(json)) {
                    return json;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Web Embedded query error for " + videoId + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    /**
     * Invidious public streaming mirror fallback to ensure zero-block resolution.
     */
    private static ExtractedMedia queryInvidiousMirror(String videoId) {
        String[] mirrors = new String[]{
                "https://inv.nadeko.net",
                "https://vid.priv.au",
                "https://yt.artemislena.eu"
        };

        for (String mirror : mirrors) {
            HttpURLConnection conn = null;
            try {
                URL u = new URL(mirror + "/api/v1/videos/" + videoId);
                conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", WEB_UA);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(7000);

                if (conn.getResponseCode() == 200) {
                    String jsonText = readStream(conn.getInputStream());
                    JSONObject obj = new JSONObject(jsonText);
                    String title = obj.optString("title", "YouTube Video (" + videoId + ")");
                    String author = obj.optString("author", "YouTube Creator");
                    long duration = obj.optLong("lengthSeconds", 0);
                    String thumbnail = "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg";

                    List<StreamFormat> options = new ArrayList<>();

                    if (obj.has("formatStreams")) {
                        JSONArray streams = obj.getJSONArray("formatStreams");
                        for (int i = 0; i < streams.length(); i++) {
                            JSONObject f = streams.getJSONObject(i);
                            String fUrl = f.optString("url", "");
                            if (fUrl.isEmpty()) continue;
                            String quality = f.optString("qualityLabel", f.optString("quality", "HD"));
                            int itag = f.optInt("itag", i + 1);

                            options.add(new StreamFormat(
                                    "yt_mirror_" + itag,
                                    "mp4",
                                    quality + " (Direct Stream)",
                                    f.optLong("bitrate", 0),
                                    false,
                                    fUrl,
                                    null,
                                    false,
                                    0,
                                    "video/mp4"
                            ));
                        }
                    }

                    if (obj.has("adaptiveFormats")) {
                        JSONArray adaptives = obj.getJSONArray("adaptiveFormats");
                        for (int i = 0; i < adaptives.length(); i++) {
                            JSONObject f = adaptives.getJSONObject(i);
                            String type = f.optString("type", "");
                            if (type.startsWith("audio/")) {
                                String aUrl = f.optString("url", "");
                                if (!aUrl.isEmpty()) {
                                    options.add(new StreamFormat(
                                            "yt_mirror_audio",
                                            "m4a",
                                            "HQ Audio (" + (f.optLong("bitrate", 128000) / 1000) + " kbps)",
                                            f.optLong("bitrate", 128000),
                                            true,
                                            null,
                                            aUrl,
                                            false,
                                            0,
                                            "audio/mp4"
                                    ));
                                    break;
                                }
                            }
                        }
                    }

                    if (!options.isEmpty()) {
                        return new ExtractedMedia(
                                videoId,
                                title,
                                thumbnail,
                                MediaSource.YOUTUBE,
                                author,
                                duration,
                                null,
                                options
                        );
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return null;
    }

    /**
     * Primary VisionOS Innertube query matching yt-dlp's default client.
     * VisionOS returns direct unencrypted HTTP video and audio streams without PO-token or cipher execution.
     */
    private static JSONObject queryVisionOsPlayer(String videoId, String visitorData) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(INNERTUBE_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-YouTube-Client-Name", "101"); // VISIONOS client ID
            conn.setRequestProperty("X-YouTube-Client-Version", "1.02");
            conn.setRequestProperty("Origin", "https://www.youtube.com");
            conn.setRequestProperty("User-Agent", VISIONOS_UA);
            if (visitorData != null && !visitorData.isEmpty()) {
                conn.setRequestProperty("X-Goog-Visitor-Id", visitorData);
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            // Payload structure from yt-dlp _INNERTUBE_CLIENTS['visionos']
            JSONObject body = new JSONObject();
            JSONObject context = new JSONObject();
            JSONObject client = new JSONObject();
            client.put("clientName", "VISIONOS");
            client.put("clientVersion", "1.02");
            client.put("deviceMake", "Apple");
            client.put("deviceModel", "RealityDevice17,1");
            client.put("userAgent", VISIONOS_UA);
            client.put("osName", "visionOS");
            client.put("osVersion", "26.5.23O471");
            client.put("hl", "en");
            client.put("timeZone", "UTC");
            client.put("utcOffsetMinutes", 0);
            context.put("client", client);
            body.put("context", context);
            body.put("videoId", videoId);
            body.put("playbackContext", new JSONObject()
                    .put("contentPlaybackContext", new JSONObject()
                            .put("html5Preference", "HTML5_PREF_WANTS")
                            .put("signatureTimestamp", 20702)));
            body.put("contentCheckOk", true);
            body.put("racyCheckOk", true);

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String responseText = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(responseText);
                if (hasUsableStreamingData(json)) {
                    return json;
                }
            } else {
                Log.w(TAG, "VisionOS query returned HTTP " + code);
            }
        } catch (Exception e) {
            Log.e(TAG, "VisionOS query error for " + videoId, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    /**
     * ANDROID_VR client (Oculus Quest 2 unencrypted direct streams)
     */
    private static JSONObject queryAndroidVrPlayer(String videoId, String visitorData) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(INNERTUBE_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Quest 2) AppleWebKit/537.36");
            if (visitorData != null && !visitorData.isEmpty()) {
                conn.setRequestProperty("X-Goog-Visitor-Id", visitorData);
            }
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            JSONObject context = new JSONObject();
            JSONObject client = new JSONObject();
            client.put("clientName", "ANDROID_VR");
            client.put("clientVersion", "1.61.48");
            client.put("deviceMake", "Oculus");
            client.put("deviceModel", "Quest 2");
            client.put("osName", "Android");
            client.put("osVersion", "10");
            client.put("hl", "en");
            client.put("gl", "US");
            context.put("client", client);
            body.put("context", context);
            body.put("videoId", videoId);
            body.put("playbackContext", new JSONObject()
                    .put("contentPlaybackContext", new JSONObject()
                            .put("html5Preference", "HTML5_PREF_WANTS")));
            body.put("contentCheckOk", true);
            body.put("racyCheckOk", true);

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String responseText = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(responseText);
                if (hasUsableStreamingData(json)) {
                    return json;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "ANDROID_VR query error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    /**
     * Fallback to Android client with updated version 21.26.364 (matching modern yt-dlp config).
     */
    private static JSONObject queryAndroidPlayer(String videoId, String visitorData) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(INNERTUBE_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-YouTube-Client-Name", "3"); // ANDROID client ID
            conn.setRequestProperty("X-YouTube-Client-Version", "21.26.364");
            conn.setRequestProperty("User-Agent", ANDROID_UA);
            if (visitorData != null && !visitorData.isEmpty()) {
                conn.setRequestProperty("X-Goog-Visitor-Id", visitorData);
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            JSONObject context = new JSONObject();
            JSONObject client = new JSONObject();
            client.put("clientName", "ANDROID");
            client.put("clientVersion", "21.26.364");
            client.put("androidSdkVersion", 30);
            client.put("userAgent", ANDROID_UA);
            client.put("osName", "Android");
            client.put("osVersion", "11");
            client.put("hl", "en");
            client.put("gl", "US");
            context.put("client", client);
            body.put("context", context);
            body.put("videoId", videoId);
            body.put("playbackContext", new JSONObject()
                    .put("contentPlaybackContext", new JSONObject()
                            .put("html5Preference", "HTML5_PREF_WANTS")));

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String responseText = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(responseText);
                if (hasUsableStreamingData(json)) {
                    return json;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Android Innertube query error for " + videoId, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    /**
     * Fallback to iOS client with modern clientVersion 21.26.4.
     */
    private static JSONObject queryIosPlayer(String videoId, String visitorData) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(INNERTUBE_API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-YouTube-Client-Name", "5"); // IOS client ID
            conn.setRequestProperty("X-YouTube-Client-Version", "21.26.4");
            conn.setRequestProperty("User-Agent", "com.google.ios.youtube/21.26.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)");
            if (visitorData != null && !visitorData.isEmpty()) {
                conn.setRequestProperty("X-Goog-Visitor-Id", visitorData);
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            JSONObject body = new JSONObject();
            JSONObject context = new JSONObject();
            JSONObject client = new JSONObject();
            client.put("clientName", "IOS");
            client.put("clientVersion", "21.26.4");
            client.put("deviceMake", "Apple");
            client.put("deviceModel", "iPhone16,2");
            client.put("osName", "iPhone");
            client.put("osVersion", "18.3.2.22D82");
            client.put("hl", "en");
            client.put("gl", "US");
            context.put("client", client);
            body.put("context", context);
            body.put("videoId", videoId);
            body.put("playbackContext", new JSONObject()
                    .put("contentPlaybackContext", new JSONObject()
                            .put("html5Preference", "HTML5_PREF_WANTS")));

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                String responseText = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(responseText);
                if (hasUsableStreamingData(json)) {
                    return json;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "iOS Innertube query error for " + videoId, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static class WebPageInfo {
        String visitorData;
        String title;
        JSONObject webPlayerResponse;
    }

    private static WebPageInfo scrapeWebPage(String videoId) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://www.youtube.com/watch?v=" + videoId + "&bpctr=9999999999&has_verified=1");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", WEB_UA);
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            if (conn.getResponseCode() == 200) {
                String html = readStream(conn.getInputStream());
                WebPageInfo info = new WebPageInfo();

                // 1. Extract VISITOR_DATA from HTML (primary yt-dlp mechanism)
                Pattern pVisitor = Pattern.compile("\"VISITOR_DATA\"\\s*:\\s*\"([^\"]+)\"");
                Matcher mVisitor = pVisitor.matcher(html);
                if (mVisitor.find()) {
                    info.visitorData = mVisitor.group(1);
                } else {
                    Pattern pCfg = Pattern.compile("ytcfg\\.set\\s*\\(\\s*(\\{.+?\\})\\s*\\)\\s*;");
                    Matcher mCfg = pCfg.matcher(html);
                    if (mCfg.find()) {
                        try {
                            JSONObject cfg = new JSONObject(mCfg.group(1));
                            info.visitorData = cfg.optString("VISITOR_DATA", null);
                        } catch (Exception ignored) {}
                    }
                }

                // 2. Extract Title from <title> tag
                Pattern pTitle = Pattern.compile("<title>(.+?)</title>");
                Matcher mTitle = pTitle.matcher(html);
                if (mTitle.find()) {
                    String rawTitle = mTitle.group(1).replace(" - YouTube", "").trim();
                    if (!rawTitle.isEmpty()) {
                        info.title = rawTitle;
                    }
                }

                // 3. Extract playerResponse if present
                Pattern pPlayer = Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\});(?:var\\s+|\\n|;|<)");
                Matcher mPlayer = pPlayer.matcher(html);
                if (mPlayer.find()) {
                    try {
                        info.webPlayerResponse = new JSONObject(mPlayer.group(1));
                    } catch (Exception ignored) {}
                }

                return info;
            }
        } catch (Exception e) {
            Log.w(TAG, "Web scrape error for " + videoId + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }

        // Secondary fallback: Scrape YouTube Embed page (often avoids age-gate and bot blocks)
        try {
            URL embedUrl = new URL("https://www.youtube.com/embed/" + videoId);
            conn = (HttpURLConnection) embedUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", WEB_UA);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);

            if (conn.getResponseCode() == 200) {
                String html = readStream(conn.getInputStream());
                WebPageInfo info = new WebPageInfo();

                Matcher mVisitor = Pattern.compile("\"VISITOR_DATA\"\\s*:\\s*\"([^\"]+)\"").matcher(html);
                if (mVisitor.find()) {
                    info.visitorData = mVisitor.group(1);
                }

                Pattern pTitle = Pattern.compile("<title>(.+?)</title>");
                Matcher mTitle = pTitle.matcher(html);
                if (mTitle.find()) {
                    String rawTitle = mTitle.group(1).replace(" - YouTube", "").trim();
                    if (!rawTitle.isEmpty()) {
                        info.title = rawTitle;
                    }
                }

                Pattern pPlayer = Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\});(?:var\\s+|\\n|;|<)");
                Matcher mPlayer = pPlayer.matcher(html);
                if (mPlayer.find()) {
                    try {
                        info.webPlayerResponse = new JSONObject(mPlayer.group(1));
                    } catch (Exception ignored) {}
                }
                return info;
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }

        return null;
    }

    private static String readStream(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }
}
