import os

base_pkg = "/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app"

# 1. Update Daos.kt to include observeAll in BookmarkDao
with open(f"{base_pkg}/data/db/dao/Daos.kt", "r") as f:
    daos_content = f.read()

if "fun observeAll(): Flow<List<BookmarkEntity>>" not in daos_content:
    daos_content = daos_content.replace(
        "interface BookmarkDao {",
        "interface BookmarkDao {\n    @Query(\"SELECT * FROM bookmarks ORDER BY createdAt DESC\")\n    fun observeAll(): Flow<List<BookmarkEntity>>\n"
    )
    with open(f"{base_pkg}/data/db/dao/Daos.kt", "w") as f:
        f.write(daos_content)

# 2. Update Repositories.kt for BookmarkRepository
with open(f"{base_pkg}/data/repository/Repositories.kt", "r") as f:
    repos_content = f.read()

if "fun observeAllBookmarks" not in repos_content:
    repos_content = repos_content.replace(
        "class BookmarkRepository(private val db: AppDatabase) {",
        """class BookmarkRepository(private val db: AppDatabase) {
    private val bookmarkDao = db.bookmarkDao()

    fun observeAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.observeAll().map { list ->
            list.map { Bookmark(it.id, it.pageId, it.positionToken, it.label, it.createdAt) }
        }
    }

    suspend fun addBookmark(pageId: Long, positionToken: String, label: String?): Long {
        return bookmarkDao.insert(
            BookmarkEntity(
                pageId = pageId,
                positionToken = positionToken,
                label = label,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBookmark(id: Long) {
        bookmarkDao.deleteById(id)
    }"""
    )
    with open(f"{base_pkg}/data/repository/Repositories.kt", "w") as f:
        f.write(repos_content)

# 3. Update NoteReaderApplication.kt instantiation
with open(f"{base_pkg}/NoteReaderApplication.kt", "r") as f:
    app_content = f.read()

app_content = app_content.replace(
    "notebookRepository = NotebookRepository(database.notebookDao())",
    "notebookRepository = NotebookRepository(database)"
).replace(
    "sectionRepository = SectionRepository(database.sectionDao())",
    "sectionRepository = SectionRepository(database)"
).replace(
    "pageRepository = PageRepository(database.pageDao(), database.readingProgressDao())",
    "pageRepository = PageRepository(database, this)"
).replace(
    "bookmarkRepository = BookmarkRepository(database.bookmarkDao())",
    "bookmarkRepository = BookmarkRepository(database)"
)

with open(f"{base_pkg}/NoteReaderApplication.kt", "w") as f:
    f.write(app_content)

print("Repositories and NoteReaderApplication.kt fixed.")
