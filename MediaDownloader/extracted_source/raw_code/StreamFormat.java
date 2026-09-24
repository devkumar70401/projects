package com.devendra.healthtracker;

public class StreamFormat {
    public final String formatId;
    public final String extension;       // e.g. "mp4", "mp3", "m4a", "jpg"
    public final String resolution;      // e.g. "1080p", "720p", "480p", "320kbps Audio"
    public final long bitrate;
    public final boolean isAudioOnly;
    public final String videoUrl;        // URL for video stream (or combined stream)
    public final String audioUrl;        // URL for audio stream (if separate DASH track)
    public final boolean requiresMuxing; // True if video and audio must be muxed via MediaMuxer
    public final long approximateBytes;
    public final String codec;           // e.g. "avc1.640028", "mp4a.40.2"

    public StreamFormat(String formatId, String extension, String resolution, long bitrate,
                        boolean isAudioOnly, String videoUrl, String audioUrl,
                        boolean requiresMuxing, long approximateBytes, String codec) {
        this.formatId = formatId;
        this.extension = extension;
        this.resolution = resolution;
        this.bitrate = bitrate;
        this.isAudioOnly = isAudioOnly;
        this.videoUrl = videoUrl;
        this.audioUrl = audioUrl;
        this.requiresMuxing = requiresMuxing;
        this.approximateBytes = approximateBytes;
        this.codec = codec;
    }

    public String getFormattedSize() {
        if (approximateBytes <= 0) return "Adaptive Size";
        if (approximateBytes < 1024 * 1024) {
            return String.format("%.1f KB", approximateBytes / 1024.0);
        }
        return String.format("%.1f MB", approximateBytes / (1024.0 * 1024.0));
    }

    public String getLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append(resolution);
        if (extension != null && !extension.isEmpty()) {
            sb.append(" (").append(extension.toUpperCase()).append(")");
        }
        if (requiresMuxing) {
            sb.append(" • HD Muxed");
        } else if (isAudioOnly) {
            sb.append(" • Audio Only");
        } else {
            sb.append(" • Direct");
        }
        if (approximateBytes > 0) {
            sb.append(" [").append(getFormattedSize()).append("]");
        }
        return sb.toString();
    }
}
