package com.devendra.healthtracker;

import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UniversalWebExtractor: Unrestricted universal media resolution engine.
 * Inspired by Snaptube and yt-dlp architecture with zero artificial guardrails.
 * Resolves Twitter/X, TikTok, Reddit, Facebook, Pinterest, and any arbitrary HTML5 webpage.
 */
public class UniversalWebExtractor {
    private static final String TAG = "UniversalWebExtractor";
    private static final String DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final String MOBILE_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

    // Twitter / X Status pattern
    private static final Pattern TWITTER_STATUS_PATTERN = Pattern.compile(
            "(?:twitter\\.com|x\\.com)\\/[a-zA-Z0-9_]+\\/status(?:es)?\\/(\\d+)");

    // TikTok video ID pattern
    private static final Pattern TIKTOK_VIDEO_PATTERN = Pattern.compile(
            "(?:tiktok\\.com\\/@[a-zA-Z0-9_.-]+\\/video\\/|v\\.douyin\\.com\\/|video\\/)(\\d+)");

    // Reddit post or v.redd.it pattern
    private static final Pattern REDDIT_V_PATTERN = Pattern.compile(
            "v\\.redd\\.it\\/([a-zA-Z0-9_-]+)");

    // Generic Media Stream Pattern for deep HTML sniffing
    private static final Pattern MEDIA_STREAM_REGEX = Pattern.compile(
            "(https?:\\\\?/\\\\?/[^\"'\\s<>]+\\.(?:mp4|m3u8|webm|mov|m4a|mp3)(?:\\?[^\"'\\s<>]*)?)",
            Pattern.CASE_INSENSITIVE);

    // =========================================================================
    // 1. TWITTER / X EXTRACTION
    // =========================================================================
    public static ExtractedMedia extractTwitter(String rawUrl) throws Exception {
        String finalUrl = followRedirects(rawUrl);
        String tweetId = null;
        Matcher m = TWITTER_STATUS_PATTERN.matcher(finalUrl);
        if (m.find()) {
            tweetId = m.group(1);
        } else {
            // Check if URL ends with digits
            Matcher numM = Pattern.compile("\\/(\\d{10,25})(?:\\?|$)").matcher(finalUrl);
            if (numM.find()) {
                tweetId = numM.group(1);
            }
        }

        if (tweetId == null) {
            return extractGeneric(finalUrl);
        }

        // Method A: Query Twimg Public Syndication API (No Auth / No Limits)
        try {
            String syndicationUrl = "https://cdn.syndication.twimg.com/tweet-result?id=" + tweetId + "&lang=en&token=4";
            String jsonStr = fetchHttpText(syndicationUrl, DESKTOP_UA);
            if (jsonStr != null && !jsonStr.trim().isEmpty()) {
                JSONObject json = new JSONObject(jsonStr);
                String text = json.optString("text", "Twitter / X Post");
                String title = text.length() > 60 ? (text.substring(0, 57) + "...") : text;
                String author = "@" + tweetId;
                if (json.has("user")) {
                    JSONObject user = json.getJSONObject("user");
                    author = "@" + user.optString("screen_name", user.optString("name", "twitter"));
                }

                String thumbnail = "";
                List<StreamFormat> options = new ArrayList<>();

                if (json.has("mediaDetails")) {
                    JSONArray mediaList = json.getJSONArray("mediaDetails");
                    for (int i = 0; i < mediaList.length(); i++) {
                        JSONObject mediaObj = mediaList.getJSONObject(i);
                        String type = mediaObj.optString("type", "");

                        if (thumbnail.isEmpty() && mediaObj.has("media_url_https")) {
                            thumbnail = mediaObj.getString("media_url_https");
                        }

                        if ("video".equals(type) || "animated_gif".equals(type)) {
                            JSONObject vInfo = mediaObj.optJSONObject("video_info");
                            if (vInfo != null && vInfo.has("variants")) {
                                JSONArray variants = vInfo.getJSONArray("variants");
                                // Collect all MP4 variants
                                List<JSONObject> mp4List = new ArrayList<>();
                                for (int j = 0; j < variants.length(); j++) {
                                    JSONObject var = variants.getJSONObject(j);
                                    String ct = var.optString("content_type", "");
                                    if ("video/mp4".equalsIgnoreCase(ct) && var.has("url")) {
                                        mp4List.add(var);
                                    }
                                }

                                // Sort by bitrate descending
                                Collections.sort(mp4List, (a, b) ->
                                        Long.compare(b.optLong("bitrate", 0), a.optLong("bitrate", 0)));

                                for (JSONObject var : mp4List) {
                                    String vUrl = var.getString("url");
                                    long bitrate = var.optLong("bitrate", 0);
                                    String qualityLabel = formatBitrateLabel(bitrate);

                                    options.add(new StreamFormat(
                                            "tw_mp4_" + (options.size() + 1),
                                            "mp4",
                                            qualityLabel,
                                            bitrate,
                                            false,
                                            vUrl,
                                            null,
                                            false,
                                            0,
                                            "video/mp4"
                                    ));
                                }

                                // Audio track option
                                if (!mp4List.isEmpty()) {
                                    options.add(new StreamFormat(
                                            "tw_audio_best",
                                            "m4a",
                                            "HQ Audio Stream",
                                            128000,
                                            true,
                                            null,
                                            mp4List.get(0).getString("url"),
                                            false,
                                            0,
                                            "audio/mp4"
                                    ));
                                }
                            }
                        } else if ("photo".equals(type) && mediaObj.has("media_url_https")) {
                            String pUrl = mediaObj.getString("media_url_https");
                            options.add(new StreamFormat(
                                    "tw_photo_" + (i + 1),
                                    "jpg",
                                    "Original Photo",
                                    0,
                                    false,
                                    pUrl + "?name=orig",
                                    null,
                                    false,
                                    0,
                                    "image/jpeg"
                            ));
                        }
                    }
                }

                if (!options.isEmpty()) {
                    return new ExtractedMedia(
                            tweetId,
                            title,
                            thumbnail,
                            MediaSource.TWITTER,
                            author,
                            0,
                            null,
                            options
                    );
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Twitter syndication query failed for " + tweetId + ", falling back to HTML sniff: " + e.getMessage());
        }

        // Method B: HTML OpenGraph and universal stream sniffer
        return extractGenericWithSource(finalUrl, MediaSource.TWITTER);
    }

    // =========================================================================
    // 2. TIKTOK EXTRACTION
    // =========================================================================
    public static ExtractedMedia extractTikTok(String rawUrl) throws Exception {
        String finalUrl = followRedirects(rawUrl);
        String videoId = "tiktok_" + System.currentTimeMillis();
        Matcher m = TIKTOK_VIDEO_PATTERN.matcher(finalUrl);
        if (m.find()) {
            videoId = m.group(1);
        }

        String html = fetchHttpText(finalUrl, DESKTOP_UA);
        if (html != null && !html.isEmpty()) {
            String title = extractMetaTag(html, "property=\"og:title\"");
            if (title == null || title.isEmpty()) {
                title = extractMetaTag(html, "name=\"twitter:title\"");
            }
            if (title == null || title.isEmpty()) {
                title = "TikTok Video (" + videoId + ")";
            }

            String thumbnail = extractMetaTag(html, "property=\"og:image\"");
            if (thumbnail == null) thumbnail = "";

            String author = "@tiktok_creator";
            Matcher authorM = Pattern.compile("\"author\":\\{\"uniqueId\":\"([^\"]+)\"").matcher(html);
            if (authorM.find()) {
                author = "@" + authorM.group(1);
            }

            List<StreamFormat> options = new ArrayList<>();

            // 1. Check __UNIVERSAL_DATA_FOR_REHYDRATION__ JSON
            Matcher uDataM = Pattern.compile("<script\\s+id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"[^>]*>([\\s\\S]*?)<\\/script>").matcher(html);
            if (uDataM.find()) {
                try {
                    String jsonText = uDataM.group(1);
                    Matcher playM = Pattern.compile("\"playAddr\":\"([^\"]+)\"").matcher(jsonText);
                    if (playM.find()) {
                        String playAddr = unescapeJsonString(playM.group(1));
                        options.add(new StreamFormat(
                                "tt_hd_nowm",
                                "mp4",
                                "HD Video (No Watermark)",
                                0,
                                false,
                                playAddr,
                                null,
                                false,
                                0,
                                "video/mp4"
                        ));
                    }
                } catch (Exception ignored) {}
            }

            // 2. OpenGraph / Twitter video tags
            String ogVideo = extractMetaTag(html, "property=\"og:video\"");
            if (ogVideo == null) ogVideo = extractMetaTag(html, "property=\"og:video:secure_url\"");
            if (ogVideo == null) ogVideo = extractMetaTag(html, "name=\"twitter:player:stream\"");

            if (ogVideo != null && !ogVideo.isEmpty()) {
                ogVideo = ogVideo.replace("&amp;", "&");
                boolean alreadyAdded = false;
                for (StreamFormat sf : options) {
                    if (sf.videoUrl != null && sf.videoUrl.equals(ogVideo)) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) {
                    options.add(new StreamFormat(
                            "tt_direct_mp4",
                            "mp4",
                            "Standard MP4 Video",
                            0,
                            false,
                            ogVideo,
                            null,
                            false,
                            0,
                            "video/mp4"
                    ));
                }
            }

            // 3. Audio stream option
            if (!options.isEmpty()) {
                options.add(new StreamFormat(
                        "tt_audio_original",
                        "m4a",
                        "Original Sound (Audio)",
                        128000,
                        true,
                        null,
                        options.get(0).videoUrl,
                        false,
                        0,
                        "audio/mp4"
                ));

                return new ExtractedMedia(
                        videoId,
                        title,
                        thumbnail,
                        MediaSource.TIKTOK,
                        author,
                        0,
                        null,
                        options
                );
            }
        }

        return extractGenericWithSource(finalUrl, MediaSource.TIKTOK);
    }

    // =========================================================================
    // 3. REDDIT EXTRACTION
    // =========================================================================
    public static ExtractedMedia extractReddit(String rawUrl) throws Exception {
        String finalUrl = followRedirects(rawUrl);

        // Case A: v.redd.it direct link
        Matcher vm = REDDIT_V_PATTERN.matcher(finalUrl);
        if (vm.find()) {
            String redditId = vm.group(1);
            List<StreamFormat> options = new ArrayList<>();
            String audioUrl = "https://v.redd.it/" + redditId + "/DASH_AUDIO_128.mp4";

            // Add HD muxed formats
            options.add(new StreamFormat(
                    "reddit_1080p", "mp4", "1080p Full HD", 4500000, false,
                    "https://v.redd.it/" + redditId + "/DASH_1080.mp4", audioUrl, true, 0, "video/mp4"
            ));
            options.add(new StreamFormat(
                    "reddit_720p", "mp4", "720p HD", 2500000, false,
                    "https://v.redd.it/" + redditId + "/DASH_720.mp4", audioUrl, true, 0, "video/mp4"
            ));
            options.add(new StreamFormat(
                    "reddit_480p", "mp4", "480p Standard", 1200000, false,
                    "https://v.redd.it/" + redditId + "/DASH_480.mp4", audioUrl, true, 0, "video/mp4"
            ));
            options.add(new StreamFormat(
                    "reddit_audio", "m4a", "HQ Audio Only", 128000, true,
                    null, audioUrl, false, 0, "audio/mp4"
            ));

            return new ExtractedMedia(
                    redditId,
                    "Reddit Video (" + redditId + ")",
                    "",
                    MediaSource.REDDIT,
                    "Reddit",
                    0,
                    null,
                    options
            );
        }

        // Case B: Reddit post comments link -> Query public Reddit JSON
        try {
            String jsonUrl = finalUrl;
            int qIdx = jsonUrl.indexOf('?');
            if (qIdx > 0) jsonUrl = jsonUrl.substring(0, qIdx);
            if (!jsonUrl.endsWith(".json")) {
                if (!jsonUrl.endsWith("/")) jsonUrl += "/";
                jsonUrl += ".json?raw_json=1";
            }

            String jsonText = fetchHttpText(jsonUrl, MOBILE_UA);
            if (jsonText != null && jsonText.startsWith("[")) {
                JSONArray root = new JSONArray(jsonText);
                if (root.length() > 0) {
                    JSONObject listing = root.getJSONObject(0);
                    JSONObject post = listing.getJSONObject("data").getJSONArray("children").getJSONObject(0).getJSONObject("data");

                    String title = post.optString("title", "Reddit Post");
                    String author = "u/" + post.optString("author", "redditor");
                    String thumbnail = post.optString("thumbnail", "");
                    if ("default".equals(thumbnail) || "self".equals(thumbnail) || "nsfw".equals(thumbnail)) {
                        thumbnail = "";
                    }

                    List<StreamFormat> options = new ArrayList<>();

                    // Inspect secure_media / media
                    JSONObject mediaObj = post.optJSONObject("secure_media");
                    if (mediaObj == null) mediaObj = post.optJSONObject("media");

                    if (mediaObj != null && mediaObj.has("reddit_video")) {
                        JSONObject rVid = mediaObj.getJSONObject("reddit_video");
                        String fallbackUrl = rVid.optString("fallback_url", "");
                        int height = rVid.optInt("height", 720);

                        if (!fallbackUrl.isEmpty()) {
                            // Deduce DASH audio URL
                            String audioUrl = fallbackUrl.replaceAll("DASH_.*", "DASH_AUDIO_128.mp4");
                            if (audioUrl.contains("?")) audioUrl = audioUrl.substring(0, audioUrl.indexOf('?'));

                            // Add Muxed option (Video + Audio combined)
                            options.add(new StreamFormat(
                                    "reddit_muxed_" + height,
                                    "mp4",
                                    height + "p (HD Audio Muxed)",
                                    rVid.optLong("bitrate_kbps", 2500) * 1000,
                                    false,
                                    fallbackUrl,
                                    audioUrl,
                                    true,
                                    0,
                                    "video/mp4"
                            ));

                            // Add Direct fallback URL
                            options.add(new StreamFormat(
                                    "reddit_direct_" + height,
                                    "mp4",
                                    height + "p (Direct Video)",
                                    0,
                                    false,
                                    fallbackUrl,
                                    null,
                                    false,
                                    0,
                                    "video/mp4"
                            ));

                            // Add Audio Only
                            options.add(new StreamFormat(
                                    "reddit_audio",
                                    "m4a",
                                    "Audio Only",
                                    128000,
                                    true,
                                    null,
                                    audioUrl,
                                    false,
                                    0,
                                    "audio/mp4"
                            ));
                        }
                    }

                    // Check if direct post URL is image or video
                    String postUrl = post.optString("url", "");
                    if (options.isEmpty() && (postUrl.endsWith(".jpg") || postUrl.endsWith(".png") || postUrl.endsWith(".mp4") || postUrl.endsWith(".gif"))) {
                        String ext = postUrl.substring(postUrl.lastIndexOf('.') + 1);
                        options.add(new StreamFormat(
                                "reddit_media",
                                ext,
                                "Direct Media (" + ext.toUpperCase() + ")",
                                0,
                                false,
                                postUrl,
                                null,
                                false,
                                0,
                                ext.equals("mp4") ? "video/mp4" : "image/" + ext
                        ));
                    }

                    if (!options.isEmpty()) {
                        return new ExtractedMedia(
                                post.optString("id", "reddit_" + System.currentTimeMillis()),
                                title,
                                thumbnail,
                                MediaSource.REDDIT,
                                author,
                                0,
                                null,
                                options
                        );
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Reddit JSON API probe failed, falling back to HTML sniff: " + e.getMessage());
        }

        return extractGenericWithSource(finalUrl, MediaSource.REDDIT);
    }

    // =========================================================================
    // 4. FACEBOOK EXTRACTION
    // =========================================================================
    public static ExtractedMedia extractFacebook(String rawUrl) throws Exception {
        String finalUrl = followRedirects(rawUrl);
        String html = fetchHttpText(finalUrl, MOBILE_UA);
        if (html == null || html.isEmpty()) {
            return extractGenericWithSource(finalUrl, MediaSource.FACEBOOK);
        }

        String title = extractMetaTag(html, "property=\"og:title\"");
        if (title == null) title = "Facebook Video";

        String thumbnail = extractMetaTag(html, "property=\"og:image\"");
        if (thumbnail == null) thumbnail = "";

        List<StreamFormat> options = new ArrayList<>();

        // Regex patterns for Facebook video sources
        String hdUrl = extractRegexGroup(html, "(?:browser_native_hd_url|playable_url_quality_hd|\"hd_src\")\\s*:\\s*\"([^\"]+)\"");
        String sdUrl = extractRegexGroup(html, "(?:browser_native_sd_url|playable_url|\"sd_src\")\\s*:\\s*\"([^\"]+)\"");

        if (hdUrl != null) {
            hdUrl = unescapeJsonString(hdUrl);
            options.add(new StreamFormat(
                    "fb_hd",
                    "mp4",
                    "HD 720p Video",
                    0,
                    false,
                    hdUrl,
                    null,
                    false,
                    0,
                    "video/mp4"
            ));
        }

        if (sdUrl != null) {
            sdUrl = unescapeJsonString(sdUrl);
            options.add(new StreamFormat(
                    "fb_sd",
                    "mp4",
                    "SD 360p Video",
                    0,
                    false,
                    sdUrl,
                    null,
                    false,
                    0,
                    "video/mp4"
            ));
        }

        // OpenGraph fallback
        if (options.isEmpty()) {
            String ogVideo = extractMetaTag(html, "property=\"og:video\"");
            if (ogVideo == null) ogVideo = extractMetaTag(html, "property=\"og:video:secure_url\"");
            if (ogVideo != null) {
                options.add(new StreamFormat(
                        "fb_og",
                        "mp4",
                        "HD Video",
                        0,
                        false,
                        ogVideo.replace("&amp;", "&"),
                        null,
                        false,
                        0,
                        "video/mp4"
                ));
            }
        }

        if (!options.isEmpty()) {
            // Audio stream option
            options.add(new StreamFormat(
                    "fb_audio",
                    "m4a",
                    "Audio Only (M4A)",
                    128000,
                    true,
                    null,
                    options.get(0).videoUrl,
                    false,
                    0,
                    "audio/mp4"
            ));

            return new ExtractedMedia(
                    "fb_" + System.currentTimeMillis(),
                    title,
                    thumbnail,
                    MediaSource.FACEBOOK,
                    "Facebook Creator",
                    0,
                    null,
                    options
            );
        }

        return extractGenericWithSource(finalUrl, MediaSource.FACEBOOK);
    }

    // =========================================================================
    // 5. PINTEREST EXTRACTION
    // =========================================================================
    public static ExtractedMedia extractPinterest(String rawUrl) throws Exception {
        String finalUrl = followRedirects(rawUrl);
        String html = fetchHttpText(finalUrl, DESKTOP_UA);
        if (html != null && !html.isEmpty()) {
            String title = extractMetaTag(html, "property=\"og:title\"");
            if (title == null) title = "Pinterest Media";
            String thumbnail = extractMetaTag(html, "property=\"og:image\"");

            List<StreamFormat> options = new ArrayList<>();
            String ogVideo = extractMetaTag(html, "property=\"og:video\"");
            if (ogVideo == null) ogVideo = extractMetaTag(html, "property=\"og:video:secure_url\"");

            if (ogVideo != null) {
                options.add(new StreamFormat(
                        "pin_video",
                        "mp4",
                        "HD Video",
                        0,
                        false,
                        ogVideo.replace("&amp;", "&"),
                        null,
                        false,
                        0,
                        "video/mp4"
                ));
            } else if (thumbnail != null && !thumbnail.isEmpty()) {
                options.add(new StreamFormat(
                        "pin_image",
                        "jpg",
                        "Original Image",
                        0,
                        false,
                        thumbnail.replace("&amp;", "&"),
                        null,
                        false,
                        0,
                        "image/jpeg"
                ));
            }

            if (!options.isEmpty()) {
                return new ExtractedMedia(
                        "pin_" + System.currentTimeMillis(),
                        title,
                        thumbnail != null ? thumbnail : "",
                        MediaSource.PINTEREST,
                        "Pinterest",
                        0,
                        null,
                        options
                );
            }
        }
        return extractGenericWithSource(finalUrl, MediaSource.PINTEREST);
    }

    // =========================================================================
    // 6. UNIVERSAL HTML5 MEDIA SNIFFER & DIRECT DOWNLOADER (ZERO RESTRICTIONS)
    // =========================================================================
    public static ExtractedMedia extractGeneric(String rawUrl) throws Exception {
        MediaSource detected = MediaSource.detectFromUrl(rawUrl);
        return extractGenericWithSource(rawUrl, detected != null ? detected : MediaSource.GENERIC_WEB);
    }

    public static ExtractedMedia extractGenericWithSource(String rawUrl, MediaSource source) throws Exception {
        String finalUrl = followRedirects(rawUrl);
        URL parsedUrl = new URL(finalUrl);

        // Step 1: Probe URL headers (HEAD / GET range)
        HttpURLConnection conn = (HttpURLConnection) parsedUrl.openConnection();
        conn.setRequestMethod("HEAD");
        conn.setRequestProperty("User-Agent", DESKTOP_UA);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setInstanceFollowRedirects(true);

        int code = conn.getResponseCode();
        if (code >= 400 || code == -1) {
            conn.disconnect();
            conn = (HttpURLConnection) parsedUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Range", "bytes=0-1024");
            conn.setRequestProperty("User-Agent", DESKTOP_UA);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            code = conn.getResponseCode();
        }

        String contentType = conn.getContentType();
        long contentLength = conn.getContentLengthLong();
        conn.disconnect();

        boolean isDirectMedia = isMediaContentType(contentType) || isMediaExtension(finalUrl);

        // If it's a direct media link (e.g. .mp4, .mp3, .m3u8, video/mp4, audio/mpeg)
        if (isDirectMedia) {
            String filename = extractFilename(finalUrl, contentType);
            String ext = getExtension(filename);
            boolean isAudio = (contentType != null && contentType.startsWith("audio/")) || "mp3".equalsIgnoreCase(ext) || "m4a".equalsIgnoreCase(ext) || "wav".equalsIgnoreCase(ext);
            boolean isVideo = (contentType != null && contentType.startsWith("video/")) || "mp4".equalsIgnoreCase(ext) || "m3u8".equalsIgnoreCase(ext) || "webm".equalsIgnoreCase(ext) || "mkv".equalsIgnoreCase(ext);

            String label = isVideo ? "Direct Video (" + ext.toUpperCase() + ")"
                    : (isAudio ? "Direct Audio (" + ext.toUpperCase() + ")" : "Direct Media (" + ext.toUpperCase() + ")");

            List<StreamFormat> options = new ArrayList<>();
            options.add(new StreamFormat(
                    "direct_stream",
                    ext,
                    label,
                    0,
                    isAudio,
                    finalUrl,
                    null,
                    false,
                    contentLength,
                    contentType != null ? contentType : "video/mp4"
            ));

            if (isVideo && !isAudio) {
                // Add extract audio option
                options.add(new StreamFormat(
                        "direct_audio",
                        "m4a",
                        "Audio Stream (HQ M4A)",
                        128000,
                        true,
                        null,
                        finalUrl,
                        false,
                        0,
                        "audio/mp4"
                ));
            }

            return new ExtractedMedia(
                    "media_" + System.currentTimeMillis(),
                    filename,
                    "",
                    source,
                    parsedUrl.getHost(),
                    0,
                    null,
                    options
            );
        }

        // Step 2: It is an HTML webpage -> Perform deep universal media sniffing
        String html = fetchHttpText(finalUrl, MOBILE_UA);
        if (html != null && !html.isEmpty()) {
            String title = extractMetaTag(html, "property=\"og:title\"");
            if (title == null) title = extractMetaTag(html, "name=\"twitter:title\"");
            if (title == null) title = extractTitleTag(html);
            if (title == null || title.isEmpty()) {
                title = parsedUrl.getHost() + " Media";
            }

            String thumbnail = extractMetaTag(html, "property=\"og:image\"");
            if (thumbnail == null) thumbnail = extractMetaTag(html, "name=\"twitter:image\"");
            if (thumbnail == null) thumbnail = "";

            // Collect media stream candidates (deduplicated)
            Set<String> streamUrls = new LinkedHashSet<>();

            // 1. OpenGraph & Twitter player stream tags
            addIfValidStream(streamUrls, extractMetaTag(html, "property=\"og:video\""), finalUrl);
            addIfValidStream(streamUrls, extractMetaTag(html, "property=\"og:video:url\""), finalUrl);
            addIfValidStream(streamUrls, extractMetaTag(html, "property=\"og:video:secure_url\""), finalUrl);
            addIfValidStream(streamUrls, extractMetaTag(html, "name=\"twitter:player:stream\""), finalUrl);
            addIfValidStream(streamUrls, extractMetaTag(html, "property=\"og:audio\""), finalUrl);

            // 2. HTML5 <video> and <source> tags
            Matcher vTagM = Pattern.compile("<video[^>]*src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
            while (vTagM.find()) {
                addIfValidStream(streamUrls, vTagM.group(1), finalUrl);
            }

            Matcher sTagM = Pattern.compile("<source[^>]*src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
            while (sTagM.find()) {
                addIfValidStream(streamUrls, sTagM.group(1), finalUrl);
            }

            // 3. HTML5 <audio> tags
            Matcher aTagM = Pattern.compile("<audio[^>]*src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(html);
            while (aTagM.find()) {
                addIfValidStream(streamUrls, aTagM.group(1), finalUrl);
            }

            // 4. Deep Regex Sniffing across scripts and embedded player JSON
            Matcher regexM = MEDIA_STREAM_REGEX.matcher(html);
            int regexCount = 0;
            while (regexM.find() && regexCount < 10) {
                String candidate = unescapeJsonString(regexM.group(1));
                addIfValidStream(streamUrls, candidate, finalUrl);
                regexCount++;
            }

            List<StreamFormat> options = new ArrayList<>();
            int idx = 1;
            for (String sUrl : streamUrls) {
                String ext = getExtension(sUrl);
                boolean isAudio = "mp3".equalsIgnoreCase(ext) || "m4a".equalsIgnoreCase(ext) || "wav".equalsIgnoreCase(ext) || "aac".equalsIgnoreCase(ext);
                String label;
                if ("m3u8".equalsIgnoreCase(ext)) {
                    label = "HLS Live Stream (M3U8) #" + idx;
                } else if (isAudio) {
                    label = "HQ Audio Stream (" + ext.toUpperCase() + ") #" + idx;
                } else {
                    label = "Web Video (" + ext.toUpperCase() + ") #" + idx;
                }

                options.add(new StreamFormat(
                        "sniffed_" + idx,
                        ext,
                        label,
                        0,
                        isAudio,
                        sUrl,
                        null,
                        false,
                        0,
                        isAudio ? "audio/mp4" : "video/mp4"
                ));
                idx++;
            }

            // Add audio extraction if videos found
            if (!options.isEmpty() && !options.get(0).isAudioOnly) {
                options.add(new StreamFormat(
                        "sniffed_audio_extracted",
                        "m4a",
                        "Extracted Audio Track (M4A)",
                        128000,
                        true,
                        null,
                        options.get(0).videoUrl,
                        false,
                        0,
                        "audio/mp4"
                ));

                return new ExtractedMedia(
                        "web_" + System.currentTimeMillis(),
                        title,
                        thumbnail,
                        source,
                        parsedUrl.getHost(),
                        0,
                        null,
                        options
                );
            }
        }

        // Step 3: Zero Restrictions Fallback - Allow direct downloading of resource
        String filename = extractFilename(finalUrl, contentType);
        String ext = getExtension(filename);
        List<StreamFormat> fallbackOptions = new ArrayList<>();
        fallbackOptions.add(new StreamFormat(
                "universal_file",
                ext,
                "Universal Media Resource (" + ext.toUpperCase() + ")",
                0,
                false,
                finalUrl,
                null,
                false,
                contentLength,
                contentType != null ? contentType : "application/octet-stream"
        ));

        return new ExtractedMedia(
                "universal_" + System.currentTimeMillis(),
                filename,
                "",
                source,
                parsedUrl.getHost(),
                0,
                null,
                fallbackOptions
        );
    }

    // =========================================================================
    // HELPER UTILITIES
    // =========================================================================
    public static String followRedirects(String initialUrl) {
        String current = initialUrl;
        int redirects = 0;
        while (redirects < 6) {
            HttpURLConnection conn = null;
            try {
                URL u = new URL(current);
                conn = (HttpURLConnection) u.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setRequestProperty("User-Agent", DESKTOP_UA);
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP
                        || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                    String location = conn.getHeaderField("Location");
                    if (location != null && !location.isEmpty()) {
                        if (!location.startsWith("http://") && !location.startsWith("https://")) {
                            location = new URL(u, location).toString();
                        }
                        current = location;
                        redirects++;
                        continue;
                    }
                }
                break;
            } catch (Exception ignored) {
                break;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return current;
    }

    private static String fetchHttpText(String targetUrl, String userAgent) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (InputStream is = conn.getInputStream()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int n;
                    int totalRead = 0;
                    // Limit max read to 1.5 MB to avoid memory exhaustion
                    while ((n = reader.read(buf)) != -1 && totalRead < 1572864) {
                        sb.append(buf, 0, n);
                        totalRead += n;
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "HTTP fetch failed for " + targetUrl + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static void addIfValidStream(Set<String> set, String raw, String baseUrl) {
        if (raw == null) return;
        String url = raw.trim().replace("&amp;", "&");
        if (url.isEmpty()) return;

        // Resolve protocol-relative
        if (url.startsWith("//")) {
            url = "https:" + url;
        } else if (url.startsWith("/")) {
            try {
                URL base = new URL(baseUrl);
                url = base.getProtocol() + "://" + base.getHost() + url;
            } catch (Exception ignored) {}
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return;
        }

        // Avoid common image tracker false positives
        String lower = url.toLowerCase();
        if (lower.contains("favicon") || lower.contains(".svg") || lower.contains("avatar")
                || lower.contains("pixel") || lower.contains("analytics")) {
            return;
        }

        set.add(url);
    }

    private static boolean isMediaContentType(String ct) {
        if (ct == null) return false;
        String lower = ct.toLowerCase();
        return lower.startsWith("video/") || lower.startsWith("audio/")
                || lower.contains("application/x-mpegurl") || lower.contains("application/vnd.apple.mpegurl")
                || lower.startsWith("image/");
    }

    private static boolean isMediaExtension(String url) {
        if (url == null) return false;
        String clean = url.toLowerCase();
        int q = clean.indexOf('?');
        if (q > 0) clean = clean.substring(0, q);
        return clean.endsWith(".mp4") || clean.endsWith(".m3u8") || clean.endsWith(".mp3")
                || clean.endsWith(".m4a") || clean.endsWith(".webm") || clean.endsWith(".mkv")
                || clean.endsWith(".mov") || clean.endsWith(".wav") || clean.endsWith(".aac")
                || clean.endsWith(".flac") || clean.endsWith(".ogg") || clean.endsWith(".jpg")
                || clean.endsWith(".png") || clean.endsWith(".webp");
    }

    private static String extractFilename(String url, String contentType) {
        try {
            Uri uri = Uri.parse(url);
            String last = uri.getLastPathSegment();
            if (last != null && last.contains(".") && last.length() < 100) {
                return last;
            }
        } catch (Exception ignored) {}

        String ext = "mp4";
        if (contentType != null) {
            if (contentType.contains("audio/mpeg")) ext = "mp3";
            else if (contentType.contains("audio/mp4")) ext = "m4a";
            else if (contentType.contains("image/jpeg")) ext = "jpg";
            else if (contentType.contains("image/png")) ext = "png";
            else if (contentType.contains("application/x-mpegurl") || contentType.contains("application/vnd.apple.mpegurl")) ext = "m3u8";
            else if (contentType.contains("video/webm")) ext = "webm";
        }
        return "download_" + System.currentTimeMillis() + "." + ext;
    }

    private static String getExtension(String filenameOrUrl) {
        String clean = filenameOrUrl;
        int q = clean.indexOf('?');
        if (q > 0) clean = clean.substring(0, q);
        int dot = clean.lastIndexOf('.');
        if (dot > 0 && dot < clean.length() - 1) {
            String ext = clean.substring(dot + 1).toLowerCase();
            if (ext.length() <= 5 && ext.matches("^[a-z0-9]+$")) {
                return ext;
            }
        }
        return "mp4";
    }

    private static String extractMetaTag(String html, String attributeMatch) {
        Pattern p = Pattern.compile("<meta\\s+[^>]*" + Pattern.quote(attributeMatch) + "[^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String extractTitleTag(String html) {
        Pattern p = Pattern.compile("<title[^>]*>([\\s\\S]*?)<\\/title>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) {
            return m.group(1).replaceAll("\\s+", " ").trim();
        }
        return null;
    }

    private static String extractRegexGroup(String text, String regex) {
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String unescapeJsonString(String str) {
        if (str == null) return null;
        return str.replace("\\/", "/")
                .replace("\\u0026", "&")
                .replace("\\u002F", "/")
                .replace("&amp;", "&");
    }

    private static String formatBitrateLabel(long bitrate) {
        if (bitrate >= 2000000) return "1080p Full HD (" + String.format("%.1f", bitrate / 1000000.0) + " Mbps)";
        if (bitrate >= 1000000) return "720p HD (" + String.format("%.1f", bitrate / 1000000.0) + " Mbps)";
        if (bitrate >= 500000) return "480p Standard (" + (bitrate / 1000) + " kbps)";
        if (bitrate > 0) return "360p Mobile (" + (bitrate / 1000) + " kbps)";
        return "HD Video Stream";
    }
}
