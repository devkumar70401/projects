package com.devendra.personalmediadownloader.bridge

data class DownloadProgress(
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val speedBytesPerSec: Double = 0.0,
    val etaSeconds: Long = 0,
    val status: String = "QUEUED",
    val filename: String = "",
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
) {
    val progressFraction: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f

    val percent: Int
        get() = (progressFraction * 100).toInt()

    val formattedSpeed: String
        get() {
            if (speedBytesPerSec <= 0) return ""
            return if (speedBytesPerSec < 1024 * 1024) {
                String.format("%.1f KB/s", speedBytesPerSec / 1024.0)
            } else {
                String.format("%.2f MB/s", speedBytesPerSec / (1024.0 * 1024.0))
            }
        }

    val formattedProgress: String
        get() {
            val down = formatBytes(downloadedBytes)
            return if (totalBytes > 0) {
                "$down / ${formatBytes(totalBytes)}"
            } else {
                down
            }
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            if (bytes < 1024) return "$bytes B"
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0)
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
}

interface PythonProgressCallback {
    fun onProgress(
        downloaded: Long,
        total: Long,
        speed: Double,
        eta: Long,
        status: String,
        filename: String
    )
}
