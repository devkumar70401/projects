package com.devendra.healthtracker;

public enum MediaSource {
    YOUTUBE("YouTube", "#EF4444", "🎥"),
    INSTAGRAM("Instagram", "#EC4899", "📸"),
    TWITTER("Twitter / X", "#1DA1F2", "𝕏"),
    TIKTOK("TikTok", "#00F2FE", "🎵"),
    REDDIT("Reddit", "#FF4500", "🤖"),
    FACEBOOK("Facebook", "#1877F2", "👥"),
    PINTEREST("Pinterest", "#E60023", "📌"),
    WHATSAPP("WhatsApp Status", "#10B981", "💬"),
    GENERIC_WEB("Direct / Web", "#6366F1", "🌐");

    public final String displayName;
    public final String colorHex;
    public final String icon;

    MediaSource(String displayName, String colorHex, String icon) {
        this.displayName = displayName;
        this.colorHex = colorHex;
        this.icon = icon;
    }

    public static MediaSource detectFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) return GENERIC_WEB;
        String lower = url.toLowerCase().trim();
        if (lower.contains("youtube.com") || lower.contains("youtu.be")) {
            return YOUTUBE;
        } else if (lower.contains("instagram.com") || lower.contains("instagr.am")) {
            return INSTAGRAM;
        } else if (lower.contains("twitter.com") || lower.contains("x.com/") || lower.contains("//x.com") || lower.contains("//t.co/") || lower.startsWith("https://t.co/") || lower.startsWith("http://t.co/")) {
            return TWITTER;
        } else if (lower.contains("tiktok.com") || lower.contains("douyin.com")) {
            return TIKTOK;
        } else if (lower.contains("reddit.com") || lower.contains("redd.it") || lower.contains("v.redd.it")) {
            return REDDIT;
        } else if (lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com/")) {
            return FACEBOOK;
        } else if (lower.contains("pinterest.com") || lower.contains("pin.it")) {
            return PINTEREST;
        } else if (lower.startsWith("whatsapp://") || lower.contains(".statuses") || lower.contains("whatsapp")) {
            return WHATSAPP;
        }
        return GENERIC_WEB;
    }
}
