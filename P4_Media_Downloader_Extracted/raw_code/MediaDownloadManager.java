package com.devendra.healthtracker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MediaDownloadManager {
    private static final String TAG = "MediaDownloadManager";
    private static MediaDownloadManager instance;

    public interface DownloadQueueListener {
        void onQueueChanged();
    }

    private final Context appContext;
    private final List<DownloadJob> jobs = new ArrayList<>();
    private final ConcurrentHashMap<String, Future<?>> activeFutures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> pausedFlags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> canceledFlags = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<DownloadQueueListener> listeners = new ArrayList<>();

    public static synchronized MediaDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new MediaDownloadManager(context.getApplicationContext());
        }
        return instance;
    }

    private MediaDownloadManager(Context context) {
        this.appContext = context;
    }

    public synchronized void registerListener(DownloadQueueListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void unregisterListener(DownloadQueueListener listener) {
        listeners.remove(listener);
    }

    public synchronized List<DownloadJob> getJobs() {
        return Collections.unmodifiableList(new ArrayList<>(jobs));
    }

    public synchronized void enqueueJob(DownloadJob job) {
        jobs.add(0, job);
        notifyQueueChanged();
        startServiceIfActive();
        executeJob(job);
    }

    public synchronized void pauseJob(String jobId) {
        pausedFlags.put(jobId, true);
        Future<?> f = activeFutures.get(jobId);
        if (f != null) {
            f.cancel(true);
        }
        for (DownloadJob j : jobs) {
            if (j.id.equals(jobId)) {
                j.state = DownloadJob.State.PAUSED;
                break;
            }
        }
        notifyQueueChanged();
        checkActiveCountAndStopService();
    }

    public synchronized void resumeJob(String jobId) {
        pausedFlags.remove(jobId);
        for (DownloadJob j : jobs) {
            if (j.id.equals(jobId) && j.state == DownloadJob.State.PAUSED) {
                j.state = DownloadJob.State.QUEUED;
                executeJob(j);
                break;
            }
        }
        notifyQueueChanged();
        startServiceIfActive();
    }

    public synchronized void cancelJob(String jobId) {
        canceledFlags.put(jobId, true);
        Future<?> f = activeFutures.get(jobId);
        if (f != null) {
            f.cancel(true);
        }
        for (DownloadJob j : jobs) {
            if (j.id.equals(jobId)) {
                j.state = DownloadJob.State.CANCELED;
                break;
            }
        }
        notifyQueueChanged();
        checkActiveCountAndStopService();
    }

    public synchronized void dismissJob(String jobId) {
        cancelJob(jobId);
        jobs.removeIf(j -> j.id.equals(jobId));
        notifyQueueChanged();
    }

    private void executeJob(DownloadJob job) {
        Future<?> future = executor.submit(() -> {
            job.state = DownloadJob.State.DOWNLOADING;
            notifyQueueChanged();

            try {
                if (job.format.requiresMuxing && job.format.audioUrl != null && !job.format.audioUrl.isEmpty()) {
                    // Muxed Download Workflow: Video DASH + Audio DASH -> NativeMediaMuxer -> MediaStore
                    downloadAndMuxWorkflow(job);
                } else {
                    // Direct Download Workflow
                    downloadDirectWorkflow(job);
                }
                job.state = DownloadJob.State.COMPLETED;
                job.progress = 1.0f;
            } catch (Exception e) {
                Log.e(TAG, "Download execution failed for " + job.title, e);
                if (Boolean.TRUE.equals(pausedFlags.get(job.id))) {
                    job.state = DownloadJob.State.PAUSED;
                } else if (Boolean.TRUE.equals(canceledFlags.get(job.id))) {
                    job.state = DownloadJob.State.CANCELED;
                } else {
                    job.state = DownloadJob.State.ERROR;
                    job.errorMessage = e.getMessage() != null ? e.getMessage() : "Network download failed";
                }
            } finally {
                activeFutures.remove(job.id);
                notifyQueueChanged();
                checkActiveCountAndStopService();
            }
        });
        activeFutures.put(job.id, future);
    }

    private void downloadDirectWorkflow(DownloadJob job) throws Exception {
        String targetUrl = job.format.videoUrl != null ? job.format.videoUrl : job.format.audioUrl;
        if (targetUrl == null) {
            throw new IllegalArgumentException("No valid stream URL found for " + job.title);
        }

        File cacheDir = appContext.getCacheDir();
        String safeName = sanitizeFilename(job.title) + "_" + job.id + "." + job.format.extension;
        File tempFile = new File(cacheDir, "temp_" + safeName);

        downloadFileWithProgress(job, targetUrl, tempFile, 0f, 1.0f);

        // Export to MediaStore
        String mime = job.format.isAudioOnly ? "audio/mp4" : "video/mp4";
        if ("jpg".equalsIgnoreCase(job.format.extension) || "jpeg".equalsIgnoreCase(job.format.extension)) {
            mime = "image/jpeg";
        }
        Uri exportedUri = MediaStoreExporter.exportFileToGallery(
                appContext,
                tempFile,
                job.title,
                mime,
                job.destinationSubdir
        );
        job.savedMediaUri = exportedUri;
        job.savedFilePath = safeName;
    }

    private void downloadAndMuxWorkflow(DownloadJob job) throws Exception {
        File cacheDir = appContext.getCacheDir();
        String baseName = sanitizeFilename(job.title) + "_" + job.id;
        File tempVideo = new File(cacheDir, "v_" + baseName + ".mp4");
        File tempAudio = new File(cacheDir, "a_" + baseName + ".m4a");
        File tempMuxed = new File(cacheDir, "muxed_" + baseName + ".mp4");

        // 1. Download Video Track (Progress 0% - 60%)
        downloadFileWithProgress(job, job.format.videoUrl, tempVideo, 0.0f, 0.60f);

        // 2. Download Audio Track (Progress 60% - 85%)
        downloadFileWithProgress(job, job.format.audioUrl, tempAudio, 0.60f, 0.85f);

        // 3. Mux Video + Audio using Native Android MediaMuxer (Progress 85% - 98%)
        job.state = DownloadJob.State.MUXING;
        notifyQueueChanged();
        NativeMediaMuxer.muxVideoAndAudio(tempVideo, tempAudio, tempMuxed, muxProgress -> {
            job.progress = 0.85f + (0.13f * muxProgress);
            notifyQueueChangedThrottled();
        });

        // 4. Export final muxed video to MediaStore System Gallery
        Uri exportedUri = MediaStoreExporter.exportFileToGallery(
                appContext,
                tempMuxed,
                job.title,
                "video/mp4",
                job.destinationSubdir
        );
        job.savedMediaUri = exportedUri;
        job.savedFilePath = baseName + ".mp4";
    }

    private void downloadFileWithProgress(DownloadJob job, String fileUrl, File outputFile,
                                          float progressStart, float progressEnd) throws Exception {
        HttpURLConnection conn = null;
        String currentUrl = fileUrl;
        int redirectCount = 0;
        long existingLength = outputFile.exists() ? outputFile.length() : 0;
        boolean isPartial = false;

        try {
            while (redirectCount < 5) {
                URL url = new URL(currentUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);

                // Resume support
                if (existingLength > 0) {
                    conn.setRequestProperty("Range", "bytes=" + existingLength + "-");
                }

                int responseCode = conn.getResponseCode();

                // Handle HTTP 3xx Redirects
                if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                    String location = conn.getHeaderField("Location");
                    conn.disconnect();
                    if (location != null && !location.isEmpty()) {
                        currentUrl = location;
                        redirectCount++;
                        continue;
                    }
                }

                isPartial = (responseCode == 206);
                if (responseCode != 200 && !isPartial) {
                    throw new IllegalStateException("HTTP " + responseCode + ": " + conn.getResponseMessage());
                }
                break;
            }

            long contentLength = conn.getContentLengthLong();
            long totalExpected = isPartial ? (existingLength + contentLength) : contentLength;
            job.totalBytes = totalExpected;

            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(outputFile, isPartial)) {

                byte[] buffer = new byte[16384]; // 16 KB chunks
                long currentBytes = existingLength;
                long lastSpeedCalcTime = System.currentTimeMillis();
                long bytesSinceCalc = 0;
                long lastUiUpdateTime = 0;

                int read;
                while ((read = is.read(buffer)) != -1) {
                    if (Boolean.TRUE.equals(canceledFlags.get(job.id)) || Boolean.TRUE.equals(pausedFlags.get(job.id))) {
                        throw new InterruptedException("Download canceled or paused");
                    }

                    fos.write(buffer, 0, read);
                    currentBytes += read;
                    bytesSinceCalc += read;
                    job.downloadedBytes = currentBytes;

                    long now = System.currentTimeMillis();
                    if (now - lastSpeedCalcTime >= 1000) {
                        job.speedBytesPerSec = (bytesSinceCalc * 1000) / (now - lastSpeedCalcTime);
                        bytesSinceCalc = 0;
                        lastSpeedCalcTime = now;
                    }

                    if (totalExpected > 0) {
                        float chunkFraction = (float) currentBytes / totalExpected;
                        job.progress = progressStart + chunkFraction * (progressEnd - progressStart);
                    }

                    // Throttle UI updates to maximum once every 250ms
                    if (now - lastUiUpdateTime > 250) {
                        lastUiUpdateTime = now;
                        notifyQueueChanged();
                    }
                }
                fos.flush();
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void notifyQueueChanged() {
        mainHandler.post(() -> {
            for (DownloadQueueListener l : listeners) {
                try {
                    l.onQueueChanged();
                } catch (Exception ignored) {}
            }
        });
    }

    private long lastThrottledTime = 0;
    private void notifyQueueChangedThrottled() {
        long now = System.currentTimeMillis();
        if (now - lastThrottledTime > 250) {
            lastThrottledTime = now;
            notifyQueueChanged();
        }
    }

    private void startServiceIfActive() {
        try {
            Intent intent = new Intent(appContext, MediaDownloadService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent);
            } else {
                appContext.startService(intent);
            }
        } catch (Exception ignored) {}
    }

    private void checkActiveCountAndStopService() {
        boolean hasActive = false;
        for (DownloadJob j : jobs) {
            if (j.state == DownloadJob.State.DOWNLOADING || j.state == DownloadJob.State.MUXING || j.state == DownloadJob.State.QUEUED) {
                hasActive = true;
                break;
            }
        }
        if (!hasActive) {
            try {
                Intent intent = new Intent(appContext, MediaDownloadService.class);
                appContext.stopService(intent);
            } catch (Exception ignored) {}
        }
    }

    private static String sanitizeFilename(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "media_" + System.currentTimeMillis();
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
