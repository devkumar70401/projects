package com.notereader.app.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "notebooks")
data class NotebookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val colorHex: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "sections",
    foreignKeys = [
        ForeignKey(
            entity = NotebookEntity::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notebookId")]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notebookId: Long,
    val title: String,
    val colorHex: String,
    val sortOrder: Int,
    val createdAt: Long
)

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sectionId")]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sectionId: Long,
    val title: String,
    val format: String,              // "PDF" | "TXT" | "MARKDOWN" | "IPYNB"
    val internalFilePath: String,    // path inside filesDir where copied file lives
    val thumbnailPath: String?,
    val originalFileName: String,
    val fileSizeBytes: Long,
    val pageOrCellCount: Int?,
    val sortOrder: Int,
    val importedAt: Long,
    val lastOpenedAt: Long?,
    val charset: String = "UTF-8"
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val pageId: Long,
    val progressFraction: Float,
    val positionToken: String,
    val updatedAt: Long
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pageId")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageId: Long,
    val positionToken: String,
    val label: String?,
    val createdAt: Long
)

@Entity(
    tableName = "annotations",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("pageId")]
)
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageId: Long,
    val type: String,                 // "HIGHLIGHT" | "INK"
    val positionToken: String,
    val colorHex: String,
    val textSnippet: String? = null,
    val inkPathData: String? = null,
    val createdAt: Long
)
