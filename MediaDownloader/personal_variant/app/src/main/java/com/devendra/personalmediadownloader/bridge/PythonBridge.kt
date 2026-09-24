package com.devendra.personalmediadownloader.bridge

import android.content.Context
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

object PythonBridge {

    private var initialized = false

    fun init(context: Context) {
        if (!initialized && !Python.isStarted()) {
            Python.start(AndroidPlatform(context.applicationContext))
            initialized = true
        }
    }

    suspend fun getMediaInfo(url: String): Result<MediaInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val py = Python.getInstance()
            val module = py.getModule("downloader")
            val pyResult: PyObject = module.callAttr("get_media_info", url)
            val map = pyResult.asMap()

            fun getVal(key: String): String {
                for ((k, v) in map) {
                    if (k.toString() == key) return v.toString()
                }
                return ""
            }

            val title = getVal("title")
            val thumbnail = getVal("thumbnail")
            val duration = getVal("duration").toLongOrNull() ?: 0L
            val uploader = getVal("uploader")
            val extractor = getVal("extractor")
            val formatsJson = getVal("formats_json")

            val jsonArray = JSONArray(if (formatsJson.isNotEmpty()) formatsJson else "[]")
            val formatsList = mutableListOf<VideoFormat>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                formatsList.add(
                    VideoFormat(
                        formatId = item.optString("format_id"),
                        resolution = item.optString("resolution"),
                        ext = item.optString("ext"),
                        vcodec = item.optString("vcodec"),
                        acodec = item.optString("acodec"),
                        filesizeBytes = item.optLong("filesize_bytes"),
                        filesizeStr = item.optString("filesize_str"),
                        isAudioOnly = item.optBoolean("is_audio_only"),
                        isVideo = item.optBoolean("is_video"),
                        requiresMux = item.optBoolean("requires_mux"),
                        note = item.optString("note")
                    )
                )
            }

            MediaInfo(
                title = title.ifEmpty { "Media" },
                thumbnail = thumbnail,
                durationSeconds = duration,
                uploader = uploader,
                extractor = extractor,
                url = url,
                formats = formatsList
            )
        }
    }

    suspend fun downloadMedia(
        url: String,
        formatId: String,
        outputDir: File,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val py = Python.getInstance()
            val module = py.getModule("downloader")

            val callback = object : PythonProgressCallback {
                override fun onProgress(
                    downloaded: Long,
                    total: Long,
                    speed: Double,
                    eta: Long,
                    status: String,
                    filename: String
                ) {
                    onProgress(
                        DownloadProgress(
                            downloadedBytes = downloaded,
                            totalBytes = total,
                            speedBytesPerSec = speed,
                            etaSeconds = eta,
                            status = status,
                            filename = filename
                        )
                    )
                }
            }

            val resultPath = module.callAttr(
                "download_media",
                url,
                formatId,
                outputDir.absolutePath,
                callback
            ).toString()

            val file = File(resultPath)
            if (!file.exists()) {
                throw IllegalStateException("Downloaded file not found at: $resultPath")
            }
            file
        }
    }

    suspend fun downloadInstagram(
        url: String,
        outputDir: File,
        sessionCookie: String?,
        onProgress: (DownloadProgress) -> Unit
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        runCatching {
            val py = Python.getInstance()
            val module = py.getModule("downloader")

            val callback = object : PythonProgressCallback {
                override fun onProgress(
                    downloaded: Long,
                    total: Long,
                    speed: Double,
                    eta: Long,
                    status: String,
                    filename: String
                ) {
                    onProgress(
                        DownloadProgress(
                            downloadedBytes = downloaded,
                            totalBytes = total,
                            speedBytesPerSec = speed,
                            etaSeconds = eta,
                            status = status,
                            filename = filename
                        )
                    )
                }
            }

            val pyList = module.callAttr(
                "download_instagram",
                url,
                outputDir.absolutePath,
                sessionCookie,
                null,
                null,
                callback
            ).asList()

            pyList.map { File(it.toString()) }.filter { it.exists() }
        }
    }
}
