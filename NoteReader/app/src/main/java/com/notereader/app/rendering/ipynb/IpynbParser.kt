package com.notereader.app.rendering.ipynb

import kotlinx.serialization.json.*

object IpynbParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(rawJson: String): Result<IpynbNotebook> = runCatching {
        json.decodeFromString<IpynbNotebook>(rawJson)
    }

    fun normalizeSource(source: JsonElement?): String {
        if (source == null) return ""
        return when (source) {
            is JsonArray -> source.joinToString("") { 
                if (it is JsonPrimitive) it.content else ""
            }
            is JsonPrimitive -> source.content
            else -> ""
        }
    }

    fun bestOutputRepresentation(output: IpynbOutput): OutputRenderable {
        val data = output.data
        if (data != null) {
            // Check for image
            data["image/png"]?.let {
                val str = if (it is JsonPrimitive) it.content else normalizeSource(it)
                if (str.isNotBlank()) return OutputRenderable.Base64Image(str.replace("\n", "").trim())
            }
            data["image/jpeg"]?.let {
                val str = if (it is JsonPrimitive) it.content else normalizeSource(it)
                if (str.isNotBlank()) return OutputRenderable.Base64Image(str.replace("\n", "").trim())
            }
            // Check for text/plain
            data["text/plain"]?.let {
                return OutputRenderable.PlainText(normalizeSource(it))
            }
            // Fallback for HTML
            if (data.containsKey("text/html")) {
                return OutputRenderable.UnsupportedPlaceholder("[HTML Table / Chart Output]")
            }
        }

        return when (output.output_type) {
            "stream" -> {
                val text = normalizeSource(output.text)
                OutputRenderable.PlainText(text, isStderr = output.name == "stderr")
            }
            "error" -> {
                OutputRenderable.ErrorTraceback(
                    name = output.ename.orEmpty(),
                    message = output.evalue.orEmpty(),
                    lines = output.traceback ?: emptyList()
                )
            }
            else -> OutputRenderable.Empty
        }
    }
}
