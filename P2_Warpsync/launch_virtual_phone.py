import os
import sys
import time
import subprocess
from pathlib import Path

BASE_DIR = Path("/home/dev/SE/P2_Warpsync")
ANDROID_HOME = Path("/home/dev/.local/android-sdk")
JAVA_HOME = Path("/home/dev/jdk-17")
APK_PATH = BASE_DIR / "dist" / "WarpSync_v2.0.0_aligned.apk"

def setup_env():
    env = os.environ.copy()
    env["JAVA_HOME"] = str(JAVA_HOME)
    env["ANDROID_HOME"] = str(ANDROID_HOME)
    env["PATH"] = f"{JAVA_HOME}/bin:{ANDROID_HOME}/emulator:{ANDROID_HOME}/platform-tools:{ANDROID_HOME}/cmdline-tools/latest/bin:" + env.get("PATH", "")
    return env

def launch_virtual_phone():
    print("\n" + "📱 " * 15)
    print("    WarpSync - Launching Android Virtual Phone (AVD)")
    print("📱 " * 15 + "\n")
    
    env = setup_env()
    
    avdmanager_bin = ANDROID_HOME / "cmdline-tools" / "latest" / "bin" / "avdmanager"
    emulator_bin = ANDROID_HOME / "emulator" / "emulator"
    adb_bin = ANDROID_HOME / "platform-tools" / "adb"
    
    # 1. Check if VirtualPhone AVD exists
    print("🔍 Checking Android Virtual Device (AVD) templates...")
    res_list = subprocess.run([str(avdmanager_bin), "list", "avd"], capture_output=True, text=True, env=env)
    
    if "VirtualPhone" not in res_list.stdout:
        print("⚙️ Creating 'VirtualPhone' AVD instance...")
        create_cmd = f"echo no | {avdmanager_bin} create avd -n VirtualPhone -k 'system-images;android-34;google_apis;x86_64' --force"
        subprocess.run(create_cmd, shell=True, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        print("✅ 'VirtualPhone' AVD created successfully!")
    else:
        print("✅ 'VirtualPhone' AVD is ready!")

    # 2. Launch Emulator Window
    print("\n🚀 Starting Virtual Phone GUI Window...")
    emu_cmd = [str(emulator_bin), "-avd", "VirtualPhone", "-gpu", "host", "-no-snapshot-load"]
    emu_proc = subprocess.Popen(emu_cmd, env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    
    print("⏳ Waiting for Virtual Phone to boot...")
    subprocess.run([str(adb_bin), "wait-for-device"], env=env)
    
    # Wait for package manager to be ready
    boot_completed = False
    start_time = time.time()
    while time.time() - start_time < 60:
        res = subprocess.run([str(adb_bin), "shell", "getprop", "sys.boot_completed"], capture_output=True, text=True, env=env)
        if res.stdout.strip() == "1":
            boot_completed = True
            break
        time.sleep(2)
        
    if boot_completed:
        print("🎉 Virtual Phone Boot Complete!")
        
        # Install WarpSync APK
        if APK_PATH.exists():
            print(f"📦 Installing WarpSync APK ({APK_PATH.name}) onto Virtual Phone...")
            subprocess.run([str(adb_bin), "install", "-r", str(APK_PATH)], env=env)
            print("✅ WarpSync APK installed on Virtual Phone!")
            
            # Launch WarpSync on Virtual Phone
            print("🚀 Launching WarpSync on Virtual Phone screen...")
            subprocess.run([
                str(adb_bin), "shell", "am", "start",
                "-n", "com.devendra.warpsync/.MainActivity"
            ], env=env)
    else:
        print("⚠️ Virtual Phone booting in background window. You can interact with it on your screen!")

    return emu_proc

if __name__ == "__main__":
    launch_virtual_phone()
