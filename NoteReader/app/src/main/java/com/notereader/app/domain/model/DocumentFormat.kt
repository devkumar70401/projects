package com.notereader.app.domain.model

enum class DocumentFormat(val extension: String, val displayName: String) {
    PDF("pdf", "PDF Document"),
    TXT("txt", "Plain Text"),
    MARKDOWN("md", "Markdown"),
    IPYNB("ipynb", "Jupyter Notebook");

    companion object {
        fun fromExtension(ext: String): DocumentFormat? = when (ext.lowercase().removePrefix(".")) {
            "pdf" -> PDF
            "txt" -> TXT
            "md", "markdown" -> MARKDOWN
            "ipynb" -> IPYNB
            else -> null
        }
    }
}
