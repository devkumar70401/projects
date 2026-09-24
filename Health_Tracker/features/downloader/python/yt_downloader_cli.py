"""
yt_downloader_cli.py - High-Fidelity YouTube Media Downloader (CLI Edition)
Capability Engine for HealthTracker / Modular Android Architecture
Reference: /home/dev/SE/Library/Scripts/YouTube_Ingestion/yt_downloader_cli.py

Features:
  1. Original Format MP4: Downloads highest available video & audio combined without lossy re-encoding.
  2. Zero-Quote Interactive Mode: Type "yt" and paste your link directly without escaping special characters.
  3. Direct CLI Mode: "yt <url>" downloads directly to ~/Music or ~/Movies and exits cleanly.
"""

import sys
import os
import shutil
import argparse
from pathlib import Path
from yt_downloader import download_media_file, get_default_download_dir


def download_media(urls: list[str], output_dir: Path = None) -> int:
    """Downloads YouTube media in original high quality MP4 format."""
    if output_dir is None:
        output_dir = get_default_download_dir()
    else:
        output_dir = Path(output_dir).expanduser().resolve()
        output_dir.mkdir(parents=True, exist_ok=True)

    print("\n" + "=" * 65)
    print("🎬  \033[1;36mYouTube Original Format MP4 Downloader\033[0m")
    print(f"📁  Destination: \033[1;33m{output_dir}\033[0m")
    print(f"⚡  Format:      \033[1;32mOriginal Highest Quality MP4 (Native Streams)\033[0m")
    print("=" * 65 + "\n")

    def progress_callback(d):
        if d.get("status") == "downloading":
            pct = d.get("_percent_str", "0.0%").strip()
            spd = d.get("_speed_str", "N/A").strip()
            eta = d.get("_eta_str", "N/A").strip()
            sys.stdout.write(f"\r⏳ Progress: {pct} | Speed: {spd} | ETA: {eta}    ")
            sys.stdout.flush()
        elif d.get("status") == "finished":
            sys.stdout.write("\n✨ Download finished, merging streams...\n")
            sys.stdout.flush()

    success_count = 0
    for idx, url in enumerate(urls, start=1):
        url = url.strip()
        if not url:
            continue
        print(f"\033[1;35m[{idx}/{len(urls)}]\033[0m Connecting: \033[4m{url}\033[0m")
        try:
            res = download_media_file(
                url=url,
                output_dir=output_dir,
                progress_hook=progress_callback
            )
            title = res.get("title", "Unknown Title")
            out_file = res.get("filePath", "")
            ext = res.get("extension", "mp4")
            vcodec = res.get("videoCodec", "Native")
            acodec = res.get("audioCodec", "Native")

            print(f"\n\033[1;32m✅ Successfully Downloaded:\033[0m {title}")
            print(f"   • File:     \033[1;34m{title}.{ext}\033[0m (Video: {vcodec}, Audio: {acodec})")
            print(f"   • Saved To: \033[1;33m{out_file}\033[0m\n")
            success_count += 1
        except Exception as e:
            print(f"\n\033[1;31m❌ Error downloading {url}:\033[0m {e}")
            if "403" in str(e) or "Forbidden" in str(e):
                print("   \033[1;33m💡 Tip: Run 'pip install -U yt-dlp' to update decipher keys.\033[0m\n")

    print("=" * 65)
    print(f"🎉  \033[1;32mFinished: {success_count}/{len(urls)} downloaded successfully.\033[0m")
    print("=" * 65 + "\n")
    return 0 if success_count == len(urls) else 1


def main():
    parser = argparse.ArgumentParser(
        description="yt - High-Speed Pure Original MP4 Media Downloader",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="Examples:\n  yt https://youtu.be/xyz\n  yt (prompts for link without quotes)"
    )
    parser.add_argument("urls", nargs="*", help="YouTube video URLs to download")
    parser.add_argument("--dir", "-d", type=str, default=None, help="Custom download directory (default: ~/Music)")

    args = parser.parse_args()

    if not args.urls:
        print("\n\033[1;36m🎵 YouTube Downloader (Interactive Mode)\033[0m")
        print("💡 \033[90mTip: You can paste any link here without using quotes.\033[0m")
        try:
            raw_url = input("\n🔗 Paste YouTube URL: ").strip()
        except (KeyboardInterrupt, EOFError):
            print("\n\033[1;33mCancelled by user.\033[0m")
            sys.exit(0)

        if not raw_url:
            print("\033[1;31mNo URL provided. Exiting.\033[0m")
            sys.exit(1)
        args.urls = [raw_url]

    sys.exit(download_media(urls=args.urls, output_dir=args.dir))


if __name__ == "__main__":
    main()
