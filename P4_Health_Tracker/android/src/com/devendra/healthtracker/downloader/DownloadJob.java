package com.devendra.healthtracker.downloader;

import android.net.Uri;

public class DownloadJob {
    public enum State {
        QUEUED,
        DOWNLOADING,
        MUXING,
        COMPLETED,
        PAUSED,
        ERROR,
        CANCELED
    }

    public final String id;
    public final String title;
    public final MediaSource source;
    public final StreamFormat format;
    public final String destinationSubdir; // "Movies/RoseMedia", "Music/RoseMedia", "Pictures/RoseMedia"

    public volatile State state;
    public volatile float progress;          // 0.0 to 1.0
    public volatile long downloadedBytes;
    public volatile long totalBytes;
    public volatile long speedBytesPerSec;
    public volatile String errorMessage;
    public volatile Uri savedMediaUri;        // MediaStore content URI once completed
    public volatile String savedFilePath;    // Local path / displayName

    public DownloadJob(String id, String title, MediaSource source, StreamFormat format, String destinationSubdir) {
        this.id = id;
        this.title = title;
        this.source = source;
        this.format = format;
        this.destinationSubdir = destinationSubdir;
        this.state = State.QUEUED;
        this.progress = 0f;
    }

    public int getProgressPercent() {
        return Math.min(100, Math.max(0, (int) (progress * 100)));
    }

    public String getFormattedSpeed() {
        if (state != State.DOWNLOADING || speedBytesPerSec <= 0) return "";
        if (speedBytesPerSec < 1024 * 1024) {
            return String.format("%.1f KB/s", speedBytesPerSec / 1024.0);
        }
        return String.format("%.2f MB/s", speedBytesPerSec / (1024.0 * 1024.0));
    }

    public String getFormattedProgressBytes() {
        String downStr = formatBytes(downloadedBytes);
        if (totalBytes > 0) {
            return downStr + " / " + formatBytes(totalBytes);
        }
        return downStr;
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
