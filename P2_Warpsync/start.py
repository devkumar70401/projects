#!/usr/bin/env python3
"""
WarpSync - Instant P2P File & Text Share
One-Click Launcher with Automatic Hotspot & Public Tunnel Support
"""

import os
import sys
import webbrowser
import subprocess
import time
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
VENV_DIR = BASE_DIR / ".venv"
REQUIREMENTS_FILE = BASE_DIR / "requirements.txt"

def main():
    print("\n" + "⚡ " * 15)
    print("    WarpSync v2.0 - Instant P2P File Transfer Server")
    print("⚡ " * 15 + "\n")

    sys.path.insert(0, str(BASE_DIR))
    
    from app.network import get_local_ip, print_terminal_qr
    from app.tunnel import start_public_tunnel
    import uvicorn

    host_ip = "0.0.0.0"
    port = 8000
    local_ip = get_local_ip()
    local_access_url = f"http://{local_ip}:{port}"

    print(f"📡 Local Network Address: {local_access_url}")
    print("🌐 Connecting secure public tunnel for phone hotspot / 5G / 4G access...")
    
    tunnel_url, tunnel_proc = start_public_tunnel(port)
    
    if tunnel_url:
        print("\n" + "🚀 " * 15)
        print("✅ PUBLIC HTTPS TUNNEL ACTIVE (Works on Mobile Hotspot / 5G / 4G!):")
        print(f"🔗 Mobile Access URL: {tunnel_url}")
        print("🚀 " * 15 + "\n")
        print_terminal_qr(tunnel_url)
    else:
        print(f"\n📱 Mobile QR Code (Local Hotspot): {local_access_url}")
        print_terminal_qr(local_access_url)

    # Open local browser
    try:
        webbrowser.open(f"http://localhost:{port}")
    except Exception:
        pass

    # Launch Uvicorn Server
    try:
        uvicorn.run("app.main:app", host=host_ip, port=port, reload=False, log_level="info")
    finally:
        if tunnel_proc:
            tunnel_proc.terminate()

if __name__ == "__main__":
    main()
