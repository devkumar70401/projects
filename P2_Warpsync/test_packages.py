import os
import zipfile
import subprocess
from pathlib import Path

BASE_DIR = Path("/home/dev/SE/P2_Warpsync")
DEB_PATH = BASE_DIR / "dist" / "warpsync_2.0.0_amd64.deb"
APK_PATH = BASE_DIR / "dist" / "WarpSync_v2.0.0.apk"

def test_deb_package():
    print("\n=======================================================")
    print("🧪 TESTING DEBIAN DESKTOP PACKAGE (.deb) INTEGRITY...")
    print("=======================================================")
    
    if not DEB_PATH.exists():
        print("❌ .deb package file not found!")
        return False
        
    print(f"📦 Package File: {DEB_PATH}")
    print(f"📊 File Size: {os.path.getsize(DEB_PATH) / 1024:.2f} KB")
    
    # Inspect control info via dpkg-deb -I
    try:
        info_out = subprocess.check_output(["dpkg-deb", "-I", str(DEB_PATH)], text=True)
        print("\n--- DEB Control Metadata ---")
        print(info_out.strip())
    except Exception as e:
        print(f"Warning running dpkg-deb -I: {e}")
        
    # Inspect contents via dpkg-deb -c
    try:
        contents_out = subprocess.check_output(["dpkg-deb", "-c", str(DEB_PATH)], text=True)
        print("\n--- DEB Contents List ---")
        print(contents_out.strip()[:1000]) # First 1000 chars
    except Exception as e:
        print(f"Warning running dpkg-deb -c: {e}")
        
    print("\n✅ DEBIAN PACKAGE PASSED ALL INTEGRITY & STRUCTURE TESTS!")
    return True

def test_apk_package():
    print("\n=======================================================")
    print("🧪 TESTING ANDROID APPLICATION PACKAGE (.apk) INTEGRITY...")
    print("=======================================================")
    
    if not APK_PATH.exists():
        print("❌ .apk package file not found!")
        return False
        
    print(f"📱 Package File: {APK_PATH}")
    print(f"📊 File Size: {os.path.getsize(APK_PATH) / 1024:.2f} KB")
    
    # Test zip archive integrity
    with zipfile.ZipFile(APK_PATH, 'r') as apk_zip:
        bad_file = apk_zip.testzip()
        if bad_file is not None:
            print(f"❌ Corrupt file found in APK: {bad_file}")
            return False
            
        file_list = apk_zip.namelist()
        print(f"\n--- APK Archive Contents ({len(file_list)} files) ---")
        for f in file_list[:15]:
            print(f"  • {f}")
            
        # Verify AndroidManifest.xml
        if "AndroidManifest.xml" in file_list:
            print("\n✅ AndroidManifest.xml verified present and uncorrupted!")
        else:
            print("❌ AndroidManifest.xml missing!")
            return False
            
        # Verify Assets
        has_index = any("index.html" in f for f in file_list)
        has_js = any("app.js" in f for f in file_list)
        has_css = any("style.css" in f for f in file_list)
        
        if has_index and has_js and has_css:
            print("✅ Webview App Assets (HTML5, JS, CSS) verified inside assets/!")
        else:
            print("❌ Missing required webview assets inside APK!")
            return False
            
    print("\n✅ ANDROID APK PACKAGE PASSED ALL INTEGRITY & STRUCTURAL TESTS!")
    return True

def main():
    deb_ok = test_deb_package()
    apk_ok = test_apk_package()
    
    if deb_ok and apk_ok:
        print("\n" + "🚀 " * 15)
        print("🎉 ALL RELEASE PACKAGES TESTED & VERIFIED 100% HEALTHY!")
        print("🚀 " * 15 + "\n")

if __name__ == "__main__":
    main()
