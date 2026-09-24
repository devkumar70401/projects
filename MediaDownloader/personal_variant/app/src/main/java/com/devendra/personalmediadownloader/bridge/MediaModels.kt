package com.devendra.personalmediadownloader.bridge

data class MediaInfo(
    val title: String,
    val thumbnail: String,
    val durationSeconds: Long,
    val uploader: String,
    val extractor: String,
    val url: String,
    val formats: List<VideoFormat>
) {
    fun getFormattedDuration(): String {
        if (durationSeconds <= 0) return ""
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return if (minutes >= 60) {
            val hours = minutes / 60
            val remMin = minutes % 60
            String.format("%d:%02d:%02d", hours, remMin, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}

data class VideoFormat(
    val formatId: String,
    val resolution: String,
    val ext: String,
    val vcodec: String,
    val acodec: String,
    val filesizeBytes: Long,
    val filesizeStr: String,
    val isAudioOnly: Boolean,
    val isVideo: Boolean,
    val requiresMux: Boolean,
    val note: String
)

enum class MediaPlatform(val displayName: String, val icon: String, val colorHex: Long) {
    YOUTUBE("YouTube", "🎥", 0xFFEF4444),
    INSTAGRAM("Instagram", "📸", 0xFFEC4899),
    FACEBOOK("Facebook", "👥", 0xFF1877F2),
    TIKTOK("TikTok", "🎵", 0xFF00F2FE),
    TWITTER("Twitter / X", "𝕏", 0xFF1DA1F2),
    REDDIT("Reddit", "🤖", 0xFFFF4500),
    WHATSAPP("WhatsApp Status", "💬", 0xFF10B981),
    GENERIC("Web Link", "🌐", 0xFF6366F1);

    companion object {
        fun detect(url: String): MediaPlatform {
            val lower = url.lowercase().trim()
            return when {
                lower.contains("youtube.com") || lower.contains("youtu.be") -> YOUTUBE
                lower.contains("instagram.com") || lower.contains("instagr.am") -> INSTAGRAM
                lower.contains("facebook.com") || lower.contains("fb.watch") -> FACEBOOK
                lower.contains("tiktok.com") || lower.contains("douyin.com") -> TIKTOK
                lower.contains("twitter.com") || lower.contains("x.com") -> TWITTER
                lower.contains("reddit.com") || lower.contains("redd.it") -> REDDIT
                lower.contains("whatsapp") -> WHATSAPP
                else -> GENERIC
            }
        }
    }
}
