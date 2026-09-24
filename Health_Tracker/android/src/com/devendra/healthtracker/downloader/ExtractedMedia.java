package com.devendra.healthtracker.downloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtractedMedia {
    public final String id;
    public final String title;
    public final String thumbnailUrl;
    public final MediaSource source;
    public final String author;
    public final long durationSeconds;
    public final String restrictionNotice;
    public final List<StreamFormat> streamOptions;

    public ExtractedMedia(String id, String title, String thumbnailUrl, MediaSource source,
                          String author, long durationSeconds, String restrictionNotice,
                          List<StreamFormat> streamOptions) {
        this.id = id != null ? id : "";
        this.title = (title != null && !title.trim().isEmpty()) ? title.trim() : "Unknown Title";
        this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl : "";
        this.source = source != null ? source : MediaSource.GENERIC_WEB;
        this.author = author != null ? author : "";
        this.durationSeconds = Math.max(0, durationSeconds);
        this.restrictionNotice = restrictionNotice;
        this.streamOptions = streamOptions != null ? Collections.unmodifiableList(streamOptions) : Collections.emptyList();
    }

    public String getFormattedDuration() {
        if (durationSeconds <= 0) return "";
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        if (minutes >= 60) {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    public List<StreamFormat> getVideoOptions() {
        List<StreamFormat> list = new ArrayList<>();
        for (StreamFormat f : streamOptions) {
            if (!f.isAudioOnly) list.add(f);
        }
        return list;
    }

    public List<StreamFormat> getAudioOptions() {
        List<StreamFormat> list = new ArrayList<>();
        for (StreamFormat f : streamOptions) {
            if (f.isAudioOnly) list.add(f);
        }
        return list;
    }
}
