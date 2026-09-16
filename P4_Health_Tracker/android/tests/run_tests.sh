#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BIN_DIR="$SCRIPT_DIR/bin"

mkdir -p "$BIN_DIR"

echo "==> Compiling Automated Unit Tests..."
javac -cp "/home/dev/Android/Sdk/platforms/android-34/android.jar:$SCRIPT_DIR/../src" -d "$BIN_DIR" \
    $(find "$SCRIPT_DIR/../src" -name "*.java") \
    $(find "$SCRIPT_DIR" -maxdepth 1 -name "*.java")

echo "==> Running Automated Unit Tests..."
java -cp "$BIN_DIR:/home/dev/Android/Sdk/platforms/android-34/android.jar" com.devendra.healthtracker.tests.RunAllTests
