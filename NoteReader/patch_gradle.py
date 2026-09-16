with open("/home/dev/SE/NoteReader/app/build.gradle.kts", "r") as f:
    text = f.read()

if "core-splashscreen" not in text:
    text = text.replace(
        'implementation("androidx.core:core-ktx:1.12.0")',
        'implementation("androidx.core:core-ktx:1.12.0")\n    implementation("androidx.core:core-splashscreen:1.0.1")'
    )
    with open("/home/dev/SE/NoteReader/app/build.gradle.kts", "w") as f:
        f.write(text)

print("build.gradle.kts updated with core-splashscreen")
