import os
import shutil
import subprocess
from pathlib import Path

BASE_DIR = Path("/home/dev/SE/P2_Warpsync")
BUILD_DIR = BASE_DIR / "build_deb"
DIST_DIR = BASE_DIR / "dist"

def create_deb_package():
    print("📦 Building Native Linux Debian Package (.deb) for WarpSync v2.0...")
    
    if BUILD_DIR.exists():
        shutil.rmtree(BUILD_DIR)
    DIST_DIR.mkdir(parents=True, exist_ok=True)
    
    # 1. Directory Structure
    opt_app_dir = BUILD_DIR / "opt" / "warpsync"
    bin_dir = BUILD_DIR / "usr" / "bin"
    apps_dir = BUILD_DIR / "usr" / "share" / "applications"
    debian_dir = BUILD_DIR / "DEBIAN"
    
    opt_app_dir.mkdir(parents=True, exist_ok=True)
    bin_dir.mkdir(parents=True, exist_ok=True)
    apps_dir.mkdir(parents=True, exist_ok=True)
    debian_dir.mkdir(parents=True, exist_ok=True)
    
    # 2. Copy application source
    for item in ["app", "requirements.txt", "start.py"]:
        src = BASE_DIR / item
        dst = opt_app_dir / item
        if src.is_dir():
            shutil.copytree(src, dst)
        elif src.is_file():
            shutil.copy2(src, dst)
            
    # 3. Create /usr/bin/warpsync launcher script
    launcher_path = bin_dir / "warpsync"
    launcher_content = """#!/bin/bash
export PYTHONPATH=/opt/warpsync
python3 /opt/warpsync/start.py "$@"
"""
    with open(launcher_path, "w") as f:
        f.write(launcher_content)
    os.chmod(launcher_path, 0o755)
    
    # 4. Create Desktop Entry /usr/share/applications/warpsync.desktop
    desktop_path = apps_dir / "warpsync.desktop"
    desktop_content = """[Desktop Entry]
Name=WarpSync P2P File Share
Comment=Instant P2P Local File & Text Sharing Application (ShareIt Alternative)
Exec=/usr/bin/warpsync
Icon=network-transmit-receive
Terminal=true
Type=Application
Categories=Network;FileTransfer;Utility;
Keywords=shareit;airdrop;file transfer;p2p;
"""
    with open(desktop_path, "w") as f:
        f.write(desktop_content)
    os.chmod(desktop_path, 0o644)
    
    # 5. Create DEBIAN/control metadata
    control_path = debian_dir / "control"
    control_content = """Package: warpsync
Version: 2.0.0
Architecture: amd64
Maintainer: Devendra Kumar <devkumar70401@gmail.com>
Section: net
Priority: optional
Depends: python3, python3-fastapi, python3-uvicorn, python3-qrcode, python3-jinja2, python3-pil
Description: Instant P2P Local File & Text Sharing Application
 WarpSync is a high-speed, zero-trust local peer-to-peer file and clipboard
 text sharing application. Features WebSockets signaling, Web Crypto AES-256
 encryption, public HTTPS hotspot tunneling, and modern glassmorphism UI.
"""
    with open(control_path, "w") as f:
        f.write(control_content)
        
    # 6. Compile DEB package via dpkg-deb --root-owner-group
    deb_filename = DIST_DIR / "warpsync_2.0.0_amd64.deb"
    cmd = f"dpkg-deb --root-owner-group --build '{BUILD_DIR}' '{deb_filename}'"
    res = os.system(cmd)
    
    if res == 0:
        print(f"✅ Native Debian Package compiled successfully at: {deb_filename}")
        print(f"📦 Size: {os.path.getsize(deb_filename) / 1024:.2f} KB")
    else:
        print(f"❌ Error compiling .deb package: {res}")

if __name__ == "__main__":
    create_deb_package()
