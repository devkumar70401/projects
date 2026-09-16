#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BIN_DIR="${DIR}/../bin"
mkdir -p "${BIN_DIR}"

echo "========================================================"
echo "    Running Keyboard-Lock Test Matrix & Verification    "
echo "========================================================"

CFLAGS="-Wall -Wextra -O2 -pthread -D_GNU_SOURCE"
SRC_DIR="${DIR}/../src"

echo "[1/4] Running State Machine Unit Tests..."
gcc ${CFLAGS} "${SRC_DIR}/state_machine.c" "${DIR}/test_state_machine.c" -o "${BIN_DIR}/test_state_machine"
"${BIN_DIR}/test_state_machine"
echo ">> State Machine Tests: PASSED"
echo ""

echo "[2/4] Running Device Classification & Mouse Isolation Tests..."
gcc ${CFLAGS} "${DIR}/test_device_filter.c" -o "${BIN_DIR}/test_device_filter"
"${BIN_DIR}/test_device_filter"
echo ">> Device Classification Tests: PASSED"
echo ""

echo "[3/4] Running Hotkey Parsing & Lookup Tests..."
gcc ${CFLAGS} "${SRC_DIR}/hotkey.c" "${DIR}/test_hotkey.c" -o "${BIN_DIR}/test_hotkey"
"${BIN_DIR}/test_hotkey"
echo ">> Hotkey Tests: PASSED"
echo ""

echo "[4/4] Running End-to-End Mock Evdev Simulation Tests..."
gcc ${CFLAGS} "${SRC_DIR}/state_machine.c" "${DIR}/test_mock_evdev.c" -o "${BIN_DIR}/test_mock_evdev"
"${BIN_DIR}/test_mock_evdev"
echo ">> Mock Evdev Simulation Tests: PASSED"
echo ""

echo "========================================================"
echo "    ALL TEST SUITES PASSED (100% SUCCESS)              "
echo "========================================================"
