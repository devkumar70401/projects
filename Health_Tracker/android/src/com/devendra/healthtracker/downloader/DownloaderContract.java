package com.devendra.healthtracker.downloader;


/**
 * DownloaderContract - Public Architecture Contract for Media Downloader Capability
 * Mandated by MODULAR ARCHITECTURE CONSTITUTION (Rule 9 & Rule 11).
 *
 * This contract defines the public surface of the Downloader capability.
 * The internal implementation (Python yt-dlp engine, Java fallback, or native muxers)
 * remains strictly isolated behind this interface.
 */
public interface DownloaderContract {

    interface ExtractionListener {
        void onSuccess(ExtractedMedia media);
        void onError(Exception error);
    }

    interface DownloadProgressListener {
        void onProgress(float progress, long downloadedBytes, long totalBytes, String speed, String eta);
        void onMuxing();
        void onComplete(String localPath);
        void onError(Exception error);
    }

    /**
     * Resolves remote media metadata and stream options asynchronously.
     */
    void extractMediaAsync(String url, ExtractionListener listener);

    /**
     * Executes the high-fidelity download asynchronously.
     */
    void executeDownloadAsync(DownloadJob job, DownloadProgressListener listener);

    /**
     * Checks whether the high-performance Python/yt-dlp engine is active on this system.
     */
    boolean isPythonEngineAvailable();
}
