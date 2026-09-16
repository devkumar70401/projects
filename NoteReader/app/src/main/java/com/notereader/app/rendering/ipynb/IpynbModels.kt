package com.notereader.app.rendering.ipynb

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class IpynbNotebook(
    val cells: List<IpynbCell> = emptyList(),
    val metadata: JsonElement? = null,
    val nbformat: Int = 4,
    val nbformat_minor: Int = 0
)

@Serializable
data class IpynbCell(
    val cell_type: String = "code",                 // "markdown" | "code" | "raw"
    val source: JsonElement? = null,
    val execution_count: Int? = null,
    val outputs: List<IpynbOutput>? = null
)

@Serializable
data class IpynbOutput(
    val output_type: String = "stream",             // "stream" | "execute_result" | "display_data" | "error"
    val name: String? = null,                       // "stdout" | "stderr"
    val text: JsonElement? = null,
    val data: Map<String, JsonElement>? = null,
    val ename: String? = null,
    val evalue: String? = null,
    val traceback: List<String>? = null
)

sealed class OutputRenderable {
    object Empty : OutputRenderable()
    data class PlainText(val text: String, val isStderr: Boolean = false) : OutputRenderable()
    data class Base64Image(val base64: String) : OutputRenderable()
    data class ErrorTraceback(val name: String, val message: String, val lines: List<String>) : OutputRenderable()
    data class UnsupportedPlaceholder(val message: String) : OutputRenderable()
}
