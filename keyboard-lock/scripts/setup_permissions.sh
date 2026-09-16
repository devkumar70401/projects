#!/usr/bin/env bash
# setup_permissions.sh - Configure Linux input permissions for non-root user execution
set -e

echo "=== Setting up user permissions for keyboard-lock ==="

CURRENT_USER=$(whoami)

if [ "$EUID" -ne 0 ]; then
    echo "This script requires sudo privileges to update group membership or install udev rules."
    echo "Running with sudo..."
    exec sudo bash "$0" "$@"
fi

# Step 1: Add user to 'input' group
echo "1. Adding user '${CURRENT_USER}' to 'input' group..."
usermod -aG input "${CURRENT_USER}"

# Step 2: Install udev rule
echo "2. Installing udev rule to /etc/udev/rules.d/99-keyboard-lock.rules..."
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cp "${DIR}/udev/99-keyboard-lock.rules" /etc/udev/rules.d/99-keyboard-lock.rules
udevadm control --reload-rules && udevadm trigger

echo ""
echo "Permissions successfully configured!"
echo "NOTE: If you just added your user to the 'input' group, please log out and log back in (or run 'newgrp input') for the group membership to take effect."
