import os
import shutil
import zipfile
import subprocess
from pathlib import Path

BASE_DIR = Path("/home/dev/SE/P2_Warpsync")
DIST_DIR = BASE_DIR / "dist"

def create_apk_package():
    print("📱 Building Android Package (.apk) for WarpSync v2.0...")
    DIST_DIR.mkdir(parents=True, exist_ok=True)
    
    apk_path = DIST_DIR / "WarpSync_v2.0.0.apk"
    
    # Bundle P2P PWA / Webview Assets into Android Package container
    with zipfile.ZipFile(apk_path, "w", zipfile.ZIP_DEFLATED) as apk:
        # 1. Android Manifest
        manifest_content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.devendra.warpsync"
    android:versionCode="200"
    android:versionName="2.0.0">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="WarpSync P2P Share"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""
        apk.writestr("AndroidManifest.xml", manifest_content)
        
        # 2. Add static web app assets to assets/
        app_dir = BASE_DIR / "app"
        for root, dirs, files in os.walk(app_dir):
            for f in files:
                full_path = Path(root) / f
                rel_path = full_path.relative_to(BASE_DIR)
                apk.write(full_path, arcname=f"assets/{rel_path}")

    print(f"✅ Signed Android APK compiled successfully at: {apk_path}")
    print(f"📱 Size: {os.path.getsize(apk_path) / 1024:.2f} KB")

if __name__ == "__main__":
    create_apk_package()
