# 1. Patch UseCases.kt
with open("/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app/domain/usecase/UseCases.kt", "r") as f:
    uc = f.read()

uc = uc.replace("""        val pageId = pageRepository.insertPage(
            sectionId = sectionId,
            title = copyResult.suggestedTitle,
            format = verifiedFormat,
            internalFilePath = copyResult.internalPath,
            originalFileName = copyResult.originalFileName,
            fileSizeBytes = copyResult.sizeBytes,
            pageOrCellCount = metadata.pageOrCellCount,
            charset = metadata.detectedCharset
        )""", """        val pageId = pageRepository.insertPage(
            sectionId = sectionId,
            title = copyResult.suggestedTitle,
            originalFileName = copyResult.originalFileName,
            format = verifiedFormat,
            filePath = copyResult.internalPath,
            thumbnailPath = null,
            fileSizeBytes = copyResult.sizeBytes,
            pageOrCellCount = metadata.pageOrCellCount,
            charset = metadata.detectedCharset
        )""")

with open("/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app/domain/usecase/UseCases.kt", "w") as f:
    f.write(uc)

# 2. Patch ReaderViewModel.kt
with open("/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app/ui/reader/ReaderViewModel.kt", "r") as f:
    rvm = f.read()

rvm = rvm.replace("private val saveProgressUseCase = app.saveProgressUseCase", "private val saveProgressUseCase = app.saveReadingProgressUseCase")
rvm = rvm.replace("val page = pageRepo.getPage(pageId)", "val page = pageRepo.getPageById(pageId)")

with open("/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app/ui/reader/ReaderViewModel.kt", "w") as f:
    f.write(rvm)

print("Patched UseCases.kt and ReaderViewModel.kt")
