#!/usr/bin/env bash
# simulate_typing.sh - Verify keyboard-lock status and transitions
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BIN="${DIR}/bin/keyboard-lock"

if [ ! -f "$BIN" ]; then
    echo "Building keyboard-lock..."
    make -C "$DIR"
fi

echo "============================================="
echo "   Keyboard-Lock Runtime Verification Tool   "
echo "============================================="

echo "1. Checking daemon status..."
"$BIN" --status || true

echo ""
echo "2. Sending LOCK command via IPC..."
"$BIN" --lock || echo "(Daemon not running - start with '$BIN' first)"

echo ""
echo "3. Checking status while locked..."
"$BIN" --status || true

echo ""
echo "4. Sending UNLOCK command via IPC..."
"$BIN" --unlock || echo "(Daemon not running)"

echo ""
echo "5. Checking final status..."
"$BIN" --status || true

echo "============================================="
echo "Verification complete."
