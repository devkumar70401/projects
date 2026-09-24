#!/usr/bin/env bash
# WarpSync Shell Runner

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

if [ ! -d ".venv" ]; then
    echo "⚡ Initializing virtual environment..."
    python3 -m venv .venv
fi

source .venv/bin/activate
pip install -q -r requirements.txt

python3 start.py
