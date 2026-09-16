package com.notereader.app.data.db.dao

import androidx.room.*
import com.notereader.app.data.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<NotebookEntity>>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getById(id: Long): NotebookEntity?

    @Query("SELECT MAX(sortOrder) FROM notebooks")
    suspend fun getMaxSortOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notebook: NotebookEntity): Long

    @Update
    suspend fun update(notebook: NotebookEntity)

    @Query("UPDATE notebooks SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun rename(id: Long, title: String, updatedAt: Long)

    @Query("UPDATE notebooks SET colorHex = :colorHex, updatedAt = :updatedAt WHERE id = :id")
    suspend fun recolor(id: Long, colorHex: String, updatedAt: Long)

    @Query("UPDATE notebooks SET sortOrder = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT n.id, COUNT(p.id) as pageCount 
        FROM notebooks n 
        LEFT JOIN sections s ON s.notebookId = n.id 
        LEFT JOIN pages p ON p.sectionId = s.id 
        GROUP BY n.id
    """)
    fun observePageCounts(): Flow<List<NotebookPageCount>>
}

data class NotebookPageCount(
    val id: Long,
    val pageCount: Int
)

@Dao
interface SectionDao {
    @Query("SELECT * FROM sections WHERE notebookId = :notebookId ORDER BY sortOrder ASC, id ASC")
    fun observeByNotebook(notebookId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE id = :id")
    suspend fun getById(id: Long): SectionEntity?

    @Query("SELECT * FROM sections WHERE notebookId = :notebookId")
    suspend fun getAllByNotebook(notebookId: Long): List<SectionEntity>

    @Query("SELECT MAX(sortOrder) FROM sections WHERE notebookId = :notebookId")
    suspend fun getMaxSortOrder(notebookId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: SectionEntity): Long

    @Update
    suspend fun update(section: SectionEntity)

    @Query("UPDATE sections SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE sections SET notebookId = :newNotebookId, sortOrder = :newSortOrder WHERE id = :id")
    suspend fun move(id: Long, newNotebookId: Long, newSortOrder: Int)

    @Query("DELETE FROM sections WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT s.id, COUNT(p.id) as pageCount 
        FROM sections s 
        LEFT JOIN pages p ON p.sectionId = s.id 
        WHERE s.notebookId = :notebookId
        GROUP BY s.id
    """)
    fun observePageCountsForNotebook(notebookId: Long): Flow<List<SectionPageCount>>
}

data class SectionPageCount(
    val id: Long,
    val pageCount: Int
)

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE sectionId = :sectionId ORDER BY sortOrder ASC, id ASC")
    fun observeBySection(sectionId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getById(id: Long): PageEntity?

    @Query("SELECT * FROM pages WHERE id = :id")
    fun observeById(id: Long): Flow<PageEntity?>

    @Query("SELECT * FROM pages WHERE sectionId = :sectionId")
    suspend fun getAllBySection(sectionId: Long): List<PageEntity>

    @Query("""
        SELECT p.* FROM pages p 
        INNER JOIN sections s ON p.sectionId = s.id 
        WHERE s.notebookId = :notebookId
    """)
    suspend fun getAllByNotebook(notebookId: Long): List<PageEntity>

    @Query("SELECT * FROM pages WHERE lastOpenedAt IS NOT NULL ORDER BY lastOpenedAt DESC LIMIT :limit")
    fun observeRecentlyOpened(limit: Int = 10): Flow<List<PageEntity>>

    @Query("SELECT MAX(sortOrder) FROM pages WHERE sectionId = :sectionId")
    suspend fun getMaxSortOrder(sectionId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: PageEntity): Long

    @Update
    suspend fun update(page: PageEntity)

    @Query("UPDATE pages SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("UPDATE pages SET thumbnailPath = :thumbnailPath WHERE id = :id")
    suspend fun updateThumbnail(id: Long, thumbnailPath: String?)

    @Query("UPDATE pages SET lastOpenedAt = :timestamp WHERE id = :id")
    suspend fun updateLastOpened(id: Long, timestamp: Long)

    @Query("UPDATE pages SET sectionId = :newSectionId, sortOrder = :newSortOrder WHERE id = :id")
    suspend fun move(id: Long, newSectionId: Long, newSortOrder: Int)

    @Query("DELETE FROM pages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM pages WHERE title LIKE '%' || :query || '%' OR originalFileName LIKE '%' || :query || '%'")
    suspend fun searchPages(query: String): List<PageEntity>
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE pageId = :pageId")
    suspend fun getByPageId(pageId: Long): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE pageId = :pageId")
    fun observeByPageId(pageId: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress")
    fun observeAll(): Flow<List<ReadingProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE pageId = :pageId")
    suspend fun deleteByPageId(pageId: Long)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE pageId = :pageId ORDER BY createdAt DESC")
    fun observeByPage(pageId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE pageId = :pageId AND positionToken = :token LIMIT 1")
    suspend fun getByToken(pageId: Long, token: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM bookmarks WHERE pageId = :pageId AND positionToken = :token")
    suspend fun deleteByToken(pageId: Long, token: String)
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE pageId = :pageId ORDER BY createdAt ASC")
    fun observeByPage(pageId: Long): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE pageId = :pageId AND type = 'INK' AND positionToken = :token")
    suspend fun getInkStrokesForToken(pageId: Long, token: String): List<AnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: AnnotationEntity): Long

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM annotations WHERE pageId = :pageId AND type = 'INK' AND positionToken = :token")
    suspend fun clearInkForToken(pageId: Long, token: String)
}
