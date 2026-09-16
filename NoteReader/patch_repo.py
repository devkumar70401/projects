with open("/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app/data/repository/Repositories.kt", "r") as f:
    text = f.read()

text = text.replace("notebookDao.rename(id, newTitle.trim())", "notebookDao.rename(id, newTitle.trim(), System.currentTimeMillis())")
text = text.replace("charset = charset\n            )", "charset = charset ?: \"UTF-8\"\n            )")

with open("/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app/data/repository/Repositories.kt", "w") as f:
    f.write(text)

print("Repositories.kt patched.")
