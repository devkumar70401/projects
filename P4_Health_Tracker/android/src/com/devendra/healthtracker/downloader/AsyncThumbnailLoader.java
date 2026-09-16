package com.devendra.healthtracker.downloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncThumbnailLoader {
    private static final int MAX_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8; // 1/8th of heap
    private static final LruCache<String, Bitmap> memoryCache = new LruCache<String, Bitmap>(MAX_CACHE_SIZE) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    private static final ExecutorService executor = Executors.newFixedThreadPool(3);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void load(String url, ImageView imageView) {
        if (url == null || url.trim().isEmpty() || imageView == null) return;
        final String targetUrl = url.trim();
        imageView.setTag(targetUrl);

        Bitmap cached = memoryCache.get(targetUrl);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        // Placeholder
        imageView.setImageDrawable(null);

        executor.execute(() -> {
            Bitmap bmp = downloadBitmap(targetUrl);
            if (bmp != null) {
                memoryCache.put(targetUrl, bmp);
                mainHandler.post(() -> {
                    if (targetUrl.equals(imageView.getTag())) {
                        imageView.setImageBitmap(bmp);
                    }
                });
            }
        });
    }

    private static Bitmap downloadBitmap(String src) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(src);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Mobile");
            conn.connect();

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return BitmapFactory.decodeStream(is);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }
}
