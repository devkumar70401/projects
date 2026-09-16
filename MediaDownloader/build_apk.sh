#!/usr/bin/env bash
set -e

SDK_DIR="/home/dev/Android/Sdk"
BUILD_TOOLS="$SDK_DIR/build-tools/34.0.0"
PLATFORM="$SDK_DIR/platforms/android-34"
APP_DIR="/home/dev/SE/Projects/MediaDownloader"
cd "$APP_DIR"

# Safe relocation complying with Zero-Delete Mandate
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p /home/dev/SE/deleted/build_cache_mediadl_${TIMESTAMP}
for item in bin dist compiled_res.zip unaligned.apk; do
    if [ -e "$item" ]; then
        mv "$item" /home/dev/SE/deleted/build_cache_mediadl_${TIMESTAMP}/ 2>/dev/null || true
    fi
done
mkdir -p bin dist

echo "==> 1. Compiling Android Resources..."
"$BUILD_TOOLS/aapt2" compile --dir res -o compiled_res.zip

echo "==> 2. Linking Resources & Generating R.java..."
"$BUILD_TOOLS/aapt2" link -I "$PLATFORM/android.jar" \
    --manifest AndroidManifest.xml \
    -o unaligned.apk \
    compiled_res.zip \
    --java src

echo "==> 3. Compiling Java Source Files..."
javac -cp "$PLATFORM/android.jar" \
    -source 17 -target 17 \
    -d bin \
    $(find src/com/devendra/mediadownloader -name "*.java")

echo "==> 4. Converting Java Bytecode to Dalvik DEX (D8)..."
"$BUILD_TOOLS/d8" --lib "$PLATFORM/android.jar" $(find bin -name "*.class") --output bin/

echo "==> 5. Packaging classes.dex into APK..."
zip -u -j unaligned.apk bin/classes.dex

echo "==> 6. Zip-aligning APK..."
"$BUILD_TOOLS/zipalign" -p -f 4 unaligned.apk dist/MediaDownloader.apk

echo "==> 7. Signing APK with debug.keystore..."
"$BUILD_TOOLS/apksigner" sign \
    --ks debug.keystore \
    --ks-pass pass:android \
    --key-pass pass:android \
    dist/MediaDownloader.apk

echo "==> 8. Verifying APK Signature..."
"$BUILD_TOOLS/apksigner" verify dist/MediaDownloader.apk

echo "✅ SUCCESS: MediaDownloader.apk built successfully at: $APP_DIR/dist/MediaDownloader.apk"
ls -lh dist/MediaDownloader.apk
