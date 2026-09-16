package com.devendra.healthtracker.downloader;

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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstagramExtractorEngine {
    private static final String TAG = "InstagramExtractor";
    private static final String IG_USER_AGENT = "Instagram 315.0.0.29.109 Android (34/14; 480dpi; 1080x2400; realme; RMX5011; RMX5011; mt6835)";

    private static volatile String activeSessionCookie = null;

    public static void setSessionCookie(String cookie) {
        activeSessionCookie = (cookie != null && !cookie.trim().isEmpty()) ? cookie.trim() : null;
    }

    public static String getSessionCookie() {
        return activeSessionCookie;
    }

    private static final Pattern SHORTCODE_PATTERN = Pattern.compile(
            "(?:instagram\\.com|instagr\\.am)\\/(?:p|reel|tv|stories)\\/(?:[a-zA-Z0-9._]+\\/)?([a-zA-Z0-9_-]+)");

    public static String extractShortcode(String url) {
        if (url == null) return null;
        Matcher m = SHORTCODE_PATTERN.matcher(url.trim());
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static ExtractedMedia extract(String rawUrl) throws Exception {
        return extract(rawUrl, activeSessionCookie);
    }

    public static ExtractedMedia extract(String rawUrl, String sessionCookie) throws Exception {
        String cleanUrl = rawUrl.trim();
        int queryIdx = cleanUrl.indexOf('?');
        String basePostUrl = (queryIdx > 0) ? cleanUrl.substring(0, queryIdx) : cleanUrl;
        if (!basePostUrl.endsWith("/")) basePostUrl += "/";

        String shortcode = extractShortcode(cleanUrl);
        if (shortcode == null) {
            shortcode = "insta_" + System.currentTimeMillis();
        }

        String cookieToUse = (sessionCookie != null && !sessionCookie.trim().isEmpty()) ? sessionCookie.trim() : activeSessionCookie;
        if (cookieToUse == null || cookieToUse.isEmpty()) {
            cookieToUse = getAutoDetectedCookie();
        }

        // 1. Try Mobile Media Info if session cookie is present
        long mediaId = shortcodeToMediaId(shortcode);
        if (mediaId > 0 && cookieToUse != null && !cookieToUse.isEmpty()) {
            ExtractedMedia media = tryMobileMediaInfo(mediaId, shortcode, cookieToUse);
            if (media != null && !media.streamOptions.isEmpty()) {
                return media;
            }
        }

        // 2. Try Instagram Mobile Endpoint with ?__a=1&__d=dis
        ExtractedMedia media = tryJsonEndpoint(basePostUrl, shortcode, cookieToUse);
        if (media != null && !media.streamOptions.isEmpty()) {
            return media;
        }

        // 3. Try Public Embed Captioned Page (bypasses login for public reels/posts)
        media = scrapeEmbedCaptioned(shortcode);
        if (media != null && !media.streamOptions.isEmpty()) {
            return media;
        }

        // 4. Fallback to OpenGraph HTML scraping
        media = scrapeHtmlOpenGraph(basePostUrl, shortcode, cookieToUse);
        if (media != null && !media.streamOptions.isEmpty()) {
            return media;
        }

        if (cookieToUse == null || cookieToUse.isEmpty()) {
            throw new IllegalStateException("Instagram media protected by login wall. Tap '🌐 Sniff Web' to capture stream directly, or '🔑 IG Session' for 1-tap browser login.");
        } else {
            throw new IllegalStateException("Instagram media could not be resolved with current session. Tap '🌐 Sniff Web' to stream and download directly.");
        }
    }

    public static String getAutoDetectedCookie() {
        try {
            android.webkit.CookieManager cm = android.webkit.CookieManager.getInstance();
            String c = cm.getCookie("https://www.instagram.com");
            if (c != null && c.contains("sessionid=")) {
                return c;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static long shortcodeToMediaId(String shortcode) {
        if (shortcode == null || shortcode.trim().isEmpty()) return -1;
        String sc = shortcode.trim();
        if (sc.length() > 28) sc = sc.substring(0, sc.length() - 28);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        long id = 0;
        for (int i = 0; i < sc.length(); i++) {
            char c = sc.charAt(i);
            int val = alphabet.indexOf(c);
            if (val < 0) return -1;
            id = id * 64 + val;
        }
        return id;
    }

    private static ExtractedMedia tryMobileMediaInfo(long mediaId, String shortcode, String sessionCookie) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://i.instagram.com/api/v1/media/" + mediaId + "/info/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", IG_USER_AGENT);
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("X-IG-App-ID", "936619743392459");
            if (sessionCookie != null && !sessionCookie.isEmpty()) {
                conn.setRequestProperty("Cookie", sessionCookie.startsWith("sessionid=") ? sessionCookie : ("sessionid=" + sessionCookie));
            }
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);

            if (conn.getResponseCode() == 200) {
                String jsonStr = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(jsonStr);
                JSONArray items = json.optJSONArray("items");
                if (items != null && items.length() > 0) {
                    return parseInstagramItem(items.getJSONObject(0), shortcode);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Mobile media info error for " + mediaId + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static ExtractedMedia scrapeEmbedCaptioned(String shortcode) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://www.instagram.com/p/" + shortcode + "/embed/captioned/");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);

            if (conn.getResponseCode() == 200) {
                String html = readStream(conn.getInputStream());
                List<StreamFormat> options = new ArrayList<>();

                // Match video_url
                Pattern pVid = Pattern.compile("\"video_url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher mVid = pVid.matcher(html);
                if (mVid.find()) {
                    String vUrl = mVid.group(1).replace("\\u0026", "&").replace("\\/", "/");
                    options.add(new StreamFormat(
                            "ig_embed_vid_" + shortcode,
                            "mp4",
                            "HD Video (Direct CDN)",
                            0,
                            false,
                            vUrl,
                            null,
                            false,
                            0,
                            "video/mp4"
                    ));
                }

                // Match display_url
                Pattern pImg = Pattern.compile("\"display_url\"\\s*:\\s*\"([^\"]+)\"");
                Matcher mImg = pImg.matcher(html);
                String thumb = "";
                if (mImg.find()) {
                    thumb = mImg.group(1).replace("\\u0026", "&").replace("\\/", "/");
                    if (options.isEmpty()) {
                        options.add(new StreamFormat(
                                "ig_embed_img_" + shortcode,
                                "jpg",
                                "Original Photo (Direct CDN)",
                                0,
                                false,
                                thumb,
                                null,
                                false,
                                0,
                                "image/jpeg"
                        ));
                    }
                }

                if (!options.isEmpty()) {
                    return new ExtractedMedia(
                            shortcode,
                            "Instagram Media (" + shortcode + ")",
                            thumb,
                            MediaSource.INSTAGRAM,
                            "@instagram",
                            0,
                            null,
                            options
                    );
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Embed captioned scrape error for " + shortcode + ": " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static ExtractedMedia tryJsonEndpoint(String basePostUrl, String shortcode, String sessionCookie) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(basePostUrl + "?__a=1&__d=dis");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", IG_USER_AGENT);
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("X-IG-App-ID", "936619743392459");
            if (sessionCookie != null && !sessionCookie.isEmpty()) {
                conn.setRequestProperty("Cookie", sessionCookie.startsWith("sessionid=") ? sessionCookie : ("sessionid=" + sessionCookie));
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            if (code == 200) {
                String jsonStr = readStream(conn.getInputStream());
                JSONObject json = new JSONObject(jsonStr);

                JSONObject item = null;
                if (json.has("items")) {
                    JSONArray items = json.getJSONArray("items");
                    if (items.length() > 0) item = items.getJSONObject(0);
                } else if (json.has("graphql")) {
                    JSONObject graphql = json.getJSONObject("graphql");
                    if (graphql.has("shortcode_media")) {
                        item = graphql.getJSONObject("shortcode_media");
                    }
                }

                if (item != null) {
                    return parseInstagramItem(item, shortcode);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "IG JSON query failed for " + shortcode, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static ExtractedMedia parseInstagramItem(JSONObject item, String shortcode) {
        try {
            String title = "Instagram Reel (" + shortcode + ")";
            String author = "Instagram Creator";
            String thumbnail = "";
            List<StreamFormat> options = new ArrayList<>();

            if (item.has("user")) {
                JSONObject user = item.getJSONObject("user");
                author = "@" + user.optString("username", "instagram");
            } else if (item.has("owner")) {
                JSONObject owner = item.getJSONObject("owner");
                author = "@" + owner.optString("username", "instagram");
            }

            if (item.has("caption") && !item.isNull("caption")) {
                JSONObject caption = item.optJSONObject("caption");
                if (caption != null && caption.has("text")) {
                    String fullText = caption.getString("text");
                    title = fullText.length() > 50 ? (fullText.substring(0, 47) + "...") : fullText;
                }
            }

            // Image Thumbnail
            if (item.has("image_versions2")) {
                JSONObject imgVer = item.getJSONObject("image_versions2");
                JSONArray cands = imgVer.optJSONArray("candidates");
                if (cands != null && cands.length() > 0) {
                    thumbnail = cands.getJSONObject(0).optString("url");
                }
            } else if (item.has("display_url")) {
                thumbnail = item.getString("display_url");
            }

            // Video Versions
            if (item.has("video_versions")) {
                JSONArray videos = item.getJSONArray("video_versions");
                int bestWidth = 0;
                int bestHeight = 0;
                String bestUrl = null;

                for (int i = 0; i < videos.length(); i++) {
                    JSONObject v = videos.getJSONObject(i);
                    int w = v.optInt("width", 0);
                    int h = v.optInt("height", 0);
                    String vUrl = v.optString("url");

                    if (vUrl != null && !vUrl.isEmpty() && (w * h) >= (bestWidth * bestHeight)) {
                        bestWidth = w;
                        bestHeight = h;
                        bestUrl = vUrl;
                    }
                }

                if (bestUrl != null) {
                    String resLabel = (bestHeight > 0) ? (bestHeight + "p HD") : "HD Video";
                    options.add(new StreamFormat(
                            "ig_video_" + shortcode,
                            "mp4",
                            resLabel,
                            0,
                            false,
                            bestUrl,
                            null,
                            false,
                            0,
                            "video/mp4"
                    ));
                }
            } else if (item.has("video_url")) {
                // Direct video URL
                options.add(new StreamFormat(
                        "ig_video_" + shortcode,
                        "mp4",
                        "HD Video",
                        0,
                        false,
                        item.getString("video_url"),
                        null,
                        false,
                        0,
                        "video/mp4"
                ));
            } else if (!thumbnail.isEmpty()) {
                // Photo post
                options.add(new StreamFormat(
                        "ig_photo_" + shortcode,
                        "jpg",
                        "Original Photo",
                        0,
                        false,
                        thumbnail,
                        null,
                        false,
                        0,
                        "image/jpeg"
                ));
            }

            // Multi-photo / multi-video Carousel Post support
            if (item.has("carousel_media")) {
                JSONArray carousel = item.optJSONArray("carousel_media");
                if (carousel != null) {
                    for (int c = 0; c < carousel.length(); c++) {
                        JSONObject cItem = carousel.optJSONObject(c);
                        if (cItem == null) continue;
                        int part = c + 1;
                        if (cItem.has("video_versions")) {
                            JSONArray cVids = cItem.optJSONArray("video_versions");
                            if (cVids != null && cVids.length() > 0) {
                                String vUrl = cVids.getJSONObject(0).optString("url");
                                if (!vUrl.isEmpty()) {
                                    options.add(new StreamFormat(
                                            "ig_part" + part + "_vid_" + shortcode,
                                            "mp4",
                                            "Part " + part + " • HD Video",
                                            0,
                                            false,
                                            vUrl,
                                            null,
                                            false,
                                            0,
                                            "video/mp4"
                                    ));
                                }
                            }
                        } else if (cItem.has("image_versions2")) {
                            JSONObject cImg = cItem.optJSONObject("image_versions2");
                            if (cImg != null) {
                                JSONArray cands = cImg.optJSONArray("candidates");
                                if (cands != null && cands.length() > 0) {
                                    String iUrl = cands.getJSONObject(0).optString("url");
                                    if (!iUrl.isEmpty()) {
                                        options.add(new StreamFormat(
                                                "ig_part" + part + "_img_" + shortcode,
                                                "jpg",
                                                "Part " + part + " • Photo",
                                                0,
                                                false,
                                                iUrl,
                                                null,
                                                false,
                                                0,
                                                "image/jpeg"
                                        ));
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (item.has("edge_sidecar_to_children")) {
                JSONObject sidecar = item.optJSONObject("edge_sidecar_to_children");
                if (sidecar != null) {
                    JSONArray edges = sidecar.optJSONArray("edges");
                    if (edges != null) {
                        for (int c = 0; c < edges.length(); c++) {
                            JSONObject node = edges.getJSONObject(c).optJSONObject("node");
                            if (node == null) continue;
                            int part = c + 1;
                            boolean isVid = node.optBoolean("is_video", false);
                            String vUrl = node.optString("video_url", "");
                            String dUrl = node.optString("display_url", "");
                            if (isVid && !vUrl.isEmpty()) {
                                options.add(new StreamFormat(
                                        "ig_part" + part + "_vid_" + shortcode,
                                        "mp4",
                                        "Part " + part + " • HD Video",
                                        0,
                                        false,
                                        vUrl,
                                        null,
                                        false,
                                        0,
                                        "video/mp4"
                                ));
                            } else if (!dUrl.isEmpty()) {
                                options.add(new StreamFormat(
                                        "ig_part" + part + "_img_" + shortcode,
                                        "jpg",
                                        "Part " + part + " • Photo",
                                        0,
                                        false,
                                        dUrl,
                                        null,
                                        false,
                                        0,
                                        "image/jpeg"
                                ));
                            }
                        }
                    }
                }
            }

            if (!options.isEmpty()) {
                return new ExtractedMedia(
                        shortcode,
                        title,
                        thumbnail,
                        MediaSource.INSTAGRAM,
                        author,
                        0,
                        null,
                        options
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing IG JSON item", e);
        }
        return null;
    }

    private static ExtractedMedia scrapeHtmlOpenGraph(String basePostUrl, String shortcode, String sessionCookie) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(basePostUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
            if (sessionCookie != null && !sessionCookie.isEmpty()) {
                conn.setRequestProperty("Cookie", sessionCookie.startsWith("sessionid=") ? sessionCookie : ("sessionid=" + sessionCookie));
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            if (conn.getResponseCode() == 200) {
                String html = readStream(conn.getInputStream());

                String videoUrl = extractMetaTag(html, "property=\"og:video\"");
                if (videoUrl == null) videoUrl = extractMetaTag(html, "property=\"og:video:secure_url\"");

                String imageUrl = extractMetaTag(html, "property=\"og:image\"");
                String title = extractMetaTag(html, "property=\"og:title\"");
                if (title == null) title = "Instagram Reel (" + shortcode + ")";

                List<StreamFormat> options = new ArrayList<>();
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    options.add(new StreamFormat(
                            "ig_og_video_" + shortcode,
                            "mp4",
                            "HD Video",
                            0,
                            false,
                            videoUrl.replace("&amp;", "&"),
                            null,
                            false,
                            0,
                            "video/mp4"
                    ));
                } else if (imageUrl != null && !imageUrl.isEmpty()) {
                    options.add(new StreamFormat(
                            "ig_og_photo_" + shortcode,
                            "jpg",
                            "Original Photo",
                            0,
                            false,
                            imageUrl.replace("&amp;", "&"),
                            null,
                            false,
                            0,
                            "image/jpeg"
                    ));
                }

                if (!options.isEmpty()) {
                    return new ExtractedMedia(
                            shortcode,
                            title,
                            imageUrl != null ? imageUrl.replace("&amp;", "&") : "",
                            MediaSource.INSTAGRAM,
                            "@instagram",
                            0,
                            null,
                            options
                    );
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "HTML OpenGraph scrape error for " + shortcode, e);
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private static String extractMetaTag(String html, String attributeMatch) {
        Pattern p = Pattern.compile("<meta\\s+[^>]*" + Pattern.quote(attributeMatch) + "[^>]*content=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) {
            return m.group(1);
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
