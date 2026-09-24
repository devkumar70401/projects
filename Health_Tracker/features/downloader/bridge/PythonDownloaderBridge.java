package com.devendra.healthtracker.downloader;

import android.content.Context;
import android.net.Uri;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PythonDownloaderBridge - Capability Bridge connecting Android to the Python yt-dlp Engine
 * Modeled on /home/dev/SE/Library/Scripts/YouTube_Ingestion/yt_downloader_cli.py
 *
 * Conforms to Modular Architecture Constitution Rule 11 (Polyglot Capabilities).
 * Exposes a standard Java contract to the Android application while driving extraction
 * and downloading through the high-fidelity Python yt-dlp engine.
 */
public class PythonDownloaderBridge implements DownloaderContract {
    private static final String TAG = "PythonDownloaderBridge";
    private static PythonDownloaderBridge instance;

    private final Context appContext;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private String detectedPythonBinary = null;
    private String detectedIpcScript = null;
    private Boolean pythonAvailable = null;

    public static synchronized PythonDownloaderBridge getInstance(Context context) {
        if (instance == null) {
            instance = new PythonDownloaderBridge(context != null ? context.getApplicationContext() : null);
        } else if (instance.appContext == null && context != null) {
            instance = new PythonDownloaderBridge(context.getApplicationContext());
        }
        return instance;
    }

    private PythonDownloaderBridge(Context context) {
        this.appContext = context;
        initEngineDetection();
    }

    private synchronized void initEngineDetection() {
        String[] binaryCandidates = new String[]{
                "/home/dev/SE/Projects/.venv/bin/python3",
                "/home/dev/SE/Library/.venv/bin/python3",
                "/home/dev/SE/Notes/.venv/bin/python3",
                "/usr/bin/python3",
                "/system/bin/python3",
                "/data/data/com.termux/files/usr/bin/python3",
                "/apex/com.android.runtime/bin/python3",
                "python3"
        };

        List<String> scriptCandidates = new ArrayList<>();
        scriptCandidates.add("/home/dev/SE/Projects/P4_Health_Tracker/features/downloader/python/downloader_ipc.py");
        scriptCandidates.add("/home/dev/SE/Library/Scripts/YouTube_Ingestion/yt_downloader_cli.py");
        if (appContext != null) {
            File internalDir = new File(appContext.getFilesDir(), "python");
            File scriptInInternal = new File(internalDir, "downloader_ipc.py");
            if (!scriptInInternal.exists()) {
                unpackAssetsPython(internalDir);
            }
            if (scriptInInternal.exists()) {
                scriptCandidates.add(scriptInInternal.getAbsolutePath());
            }
        }

        for (String b : binaryCandidates) {
            File bf = new File(b);
            if (bf.exists() && bf.canExecute()) {
                detectedPythonBinary = b;
                break;
            }
        }

        for (String s : scriptCandidates) {
            File sf = new File(s);
            if (sf.exists()) {
                detectedIpcScript = s;
                break;
            }
        }

        if (detectedPythonBinary != null && detectedIpcScript != null) {
            try {
                ProcessBuilder pb = new ProcessBuilder(detectedPythonBinary, detectedIpcScript, "--ping");
                Process p = pb.start();
                int exit = p.waitFor();
                pythonAvailable = (exit == 0);
                if (pythonAvailable) {
                    Log.i(TAG, "High-fidelity Python yt-dlp engine active: " + detectedPythonBinary);
                }
            } catch (Exception e) {
                Log.w(TAG, "Python engine probe failed: " + e.getMessage());
                pythonAvailable = false;
            }
        } else {
            pythonAvailable = false;
        }
    }

    private void unpackAssetsPython(File destDir) {
        if (appContext == null) return;
        try {
            destDir.mkdirs();
            String[] files = new String[]{"__init__.py", "downloader_ipc.py", "yt_downloader.py", "yt_downloader_cli.py", "requirements.txt"};
            for (String fName : files) {
                File target = new File(destDir, fName);
                try (java.io.InputStream in = appContext.getAssets().open("python/" + fName);
                     java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isPythonEngineAvailable() {
        if (pythonAvailable == null) {
            initEngineDetection();
        }
        return Boolean.TRUE.equals(pythonAvailable);
    }

    public ExtractedMedia extractMedia(String url) throws Exception {
        if (isPythonEngineAvailable()) {
            try {
                return extractViaPython(url);
            } catch (Exception e) {
                Log.w(TAG, "Python extraction failed, falling back to Java engine: " + e.getMessage());
            }
        }
        // Graceful fallback to pure Java engine
        return YouTubeExtractorEngine.extract(url);
    }

    @Override
    public void extractMediaAsync(String url, ExtractionListener listener) {
        executor.submit(() -> {
            try {
                ExtractedMedia media = extractMedia(url);
                listener.onSuccess(media);
            } catch (Exception e) {
                listener.onError(e);
            }
        });
    }

    private ExtractedMedia extractViaPython(String url) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(detectedPythonBinary, detectedIpcScript, "--extract", url);
        Process p = pb.start();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        int code = p.waitFor();
        if (code != 0) {
            throw new RuntimeException("Python engine exited with code " + code);
        }

        JSONObject root = new JSONObject(sb.toString());
        if (!"success".equalsIgnoreCase(root.optString("status"))) {
            throw new RuntimeException(root.optString("message", "Python extraction failed"));
        }

        JSONObject data = root.getJSONObject("data");
        String id = data.optString("id");
        String title = data.optString("title", "Unknown");
        String thumb = data.optString("thumbnailUrl", "");
        String author = data.optString("author", "YouTube");
        long duration = data.optLong("durationSeconds", 0);

        List<StreamFormat> formats = new ArrayList<>();
        JSONArray arr = data.optJSONArray("streamOptions");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject f = arr.getJSONObject(i);
                formats.add(new StreamFormat(
                        f.optString("id", "opt_" + i),
                        f.optString("extension", "mp4"),
                        f.optString("resolution", "HD"),
                        f.optLong("bitrate", 0),
                        f.optBoolean("isAudioOnly", false),
                        f.optString("videoUrl", null),
                        f.optString("audioUrl", null),
                        f.optBoolean("requiresMuxing", false),
                        f.optLong("approximateBytes", 0),
                        f.optString("mimeType", "video/mp4")
                ));
            }
        }

        return new ExtractedMedia(
                id,
                title,
                thumb,
                MediaSource.YOUTUBE,
                author,
                duration,
                null,
                formats
        );
    }

    @Override
    public void executeDownloadAsync(DownloadJob job, DownloadProgressListener listener) {
        executor.submit(() -> {
            try {
                if (isPythonEngineAvailable()) {
                    executePythonDownload(job, listener);
                } else {
                    throw new UnsupportedOperationException("Python engine not available; use Java MediaDownloadManager");
                }
            } catch (Exception e) {
                listener.onError(e);
            }
        });
    }

    private void executePythonDownload(DownloadJob job, DownloadProgressListener listener) throws Exception {
        File cacheDir = (appContext != null) ? appContext.getCacheDir() : new File(System.getProperty("java.io.tmpdir", "/tmp"));
        String targetUrl = (job.format != null && job.format.videoUrl != null && !job.format.videoUrl.isEmpty())
                ? job.format.videoUrl
                : job.title;

        List<String> cmd = new ArrayList<>();
        cmd.add(detectedPythonBinary);
        cmd.add(detectedIpcScript);
        cmd.add("--download");
        cmd.add(targetUrl);
        cmd.add("--out");
        cmd.add(cacheDir.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();

        String finalSavedPath = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    JSONObject event = new JSONObject(line);
                    String type = event.optString("type");
                    if ("progress".equals(type)) {
                        float pct = (float) event.optDouble("percent", 0.0) / 100.0f;
                        long dl = event.optLong("downloadedBytes", 0);
                        long tot = event.optLong("totalBytes", 0);
                        String spd = event.optString("speed", "");
                        String eta = event.optString("eta", "");
                        if (listener != null) {
                            listener.onProgress(pct, dl, tot, spd, eta);
                        }
                    } else if ("finished".equals(type)) {
                        if (listener != null) {
                            listener.onMuxing();
                        }
                    } else if ("completed".equals(type)) {
                        JSONObject res = event.optJSONObject("result");
                        if (res != null) {
                            finalSavedPath = res.optString("filePath", null);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python downloader exited with code: " + exitCode);
        }

        if (finalSavedPath != null && listener != null) {
            File downloadedFile = new File(finalSavedPath);
            if (downloadedFile.exists()) {
                String mime = (job.format != null && job.format.isAudioOnly) ? "audio/mp4" : "video/mp4";
                if (appContext != null) {
                    Uri uri = MediaStoreExporter.exportFileToGallery(
                            appContext,
                            downloadedFile,
                            job.title,
                            mime,
                            job.destinationSubdir
                    );
                    job.savedMediaUri = uri;
                }
                job.savedFilePath = downloadedFile.getName();
            }
            listener.onComplete(finalSavedPath);
        }
    }
}
