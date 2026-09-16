package com.devendra.healthtracker;

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
        String cleanUrl = rawUrl.trim();
        int queryIdx = cleanUrl.indexOf('?');
        String basePostUrl = (queryIdx > 0) ? cleanUrl.substring(0, queryIdx) : cleanUrl;
        if (!basePostUrl.endsWith("/")) basePostUrl += "/";

        String shortcode = extractShortcode(cleanUrl);
        if (shortcode == null) {
            shortcode = "insta_" + System.currentTimeMillis();
        }

        // 1. Try Instagram Mobile Endpoint with ?__a=1&__d=dis
        ExtractedMedia media = tryJsonEndpoint(basePostUrl, shortcode);
        if (media != null && !media.streamOptions.isEmpty()) {
            return media;
        }

        // 2. Fallback to OpenGraph HTML scraping
        media = scrapeHtmlOpenGraph(basePostUrl, shortcode);
        if (media != null && !media.streamOptions.isEmpty()) {
            return media;
        }

        throw new IllegalStateException("Instagram media could not be resolved. The post might be private or rate-limited.");
    }

    private static ExtractedMedia tryJsonEndpoint(String basePostUrl, String shortcode) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(basePostUrl + "?__a=1&__d=dis");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", IG_USER_AGENT);
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            conn.setRequestProperty("X-IG-App-ID", "936619743392459");
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

    private static ExtractedMedia scrapeHtmlOpenGraph(String basePostUrl, String shortcode) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(basePostUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
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
