"""
downloader.py - Python Download Layer for Personal Media Downloader Android App
Integrates yt-dlp and instaloader via Chaquopy Java/Kotlin bridge.
Ports logic from yt_downloader_cli.py with non-blocking progress hooks and format inspection.
"""

import os
import sys
import json
import re
import traceback
from typing import Optional, Dict, Any, List

# Core extraction engines
import yt_dlp

try:
    import instaloader
except ImportError:
    instaloader = None


def sanitize_filename(name: str) -> str:
    """Sanitizes strings to be filesystem and MediaStore friendly."""
    if not name:
        return "media_download"
    clean = re.sub(r'[\\/*?:"<>|]', "", name)
    clean = re.sub(r'[\s]+', " ", clean).strip()
    return clean[:120] if len(clean) > 120 else clean


def format_bytes(byte_count: Optional[int]) -> str:
    """Formats raw bytes into human-readable string."""
    if not byte_count or byte_count <= 0:
        return "Unknown"
    for unit in ['B', 'KB', 'MB', 'GB']:
        if byte_count < 1024.0:
            return f"{byte_count:.1f} {unit}"
        byte_count /= 1024.0
    return f"{byte_count:.2f} TB"


def get_media_info(url: str) -> Dict[str, Any]:
    """
    Extracts metadata and lists available stream formats without downloading.
    Equivalent to yt-dlp --dump-json / extract_info(download=False).
    """
    url = url.strip()
    if not url:
        raise ValueError("URL cannot be empty")

    ydl_opts = {
        "extract_flat": False,
        "skip_download": True,
        "noplaylist": True,
        "socket_timeout": 20,
        "retries": 5,
        "no_warnings": True,
        "quiet": True,
    }

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        try:
            info = ydl.extract_info(url, download=False)
        except Exception as e:
            err_msg = str(e)
            if "403" in err_msg or "Forbidden" in err_msg:
                raise RuntimeError(f"HTTP 403 Forbidden: Extractor decipher key expired. Error: {err_msg}")
            raise RuntimeError(f"Failed to extract info: {err_msg}")

        if not info:
            raise RuntimeError("No media streams resolved from link")

        title = info.get("title", "Unknown Title")
        thumbnail = info.get("thumbnail", "")
        duration = info.get("duration", 0)
        uploader = info.get("uploader", info.get("channel", "Unknown Uploader"))
        extractor = info.get("extractor_key", "Generic")

        formats_list = []
        raw_formats = info.get("formats", [])

        # Filter and summarize formats for user selection
        seen_res = set()

        # Check for best combined presets first
        formats_list.append({
            "format_id": "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
            "resolution": "Highest Quality (Original Native)",
            "ext": "mp4",
            "vcodec": "Native Best",
            "acodec": "Native Best",
            "filesize_bytes": info.get("filesize", 0) or info.get("filesize_approx", 0) or 0,
            "filesize_str": format_bytes(info.get("filesize") or info.get("filesize_approx")),
            "is_audio_only": False,
            "is_video": True,
            "requires_mux": True,
            "note": "Combined optimal video & audio streams"
        })

        # Add pure audio extraction preset
        formats_list.append({
            "format_id": "bestaudio[ext=m4a]/bestaudio/best",
            "resolution": "Audio Only (HQ M4A / MP3)",
            "ext": "m4a",
            "vcodec": "none",
            "acodec": "AAC / MP3",
            "filesize_bytes": 0,
            "filesize_str": "Adaptive",
            "is_audio_only": True,
            "is_video": False,
            "requires_mux": False,
            "note": "Extracted high-fidelity audio track"
        })

        # Parse granular video formats
        for fmt in raw_formats:
            fmt_id = fmt.get("format_id", "")
            height = fmt.get("height")
            vcodec = fmt.get("vcodec", "none")
            acodec = fmt.get("acodec", "none")
            ext = fmt.get("ext", "mp4")
            filesize = fmt.get("filesize") or fmt.get("filesize_approx") or 0

            is_video = vcodec != "none" and vcodec is not None
            is_audio = acodec != "none" and acodec is not None

            if is_video and height:
                res_key = f"{height}p"
                if res_key not in seen_res and height in [2160, 1440, 1080, 720, 480, 360]:
                    seen_res.add(res_key)
                    needs_mux = not is_audio
                    actual_format_id = f"{fmt_id}+bestaudio[ext=m4a]/bestaudio" if needs_mux else fmt_id

                    formats_list.append({
                        "format_id": actual_format_id,
                        "resolution": f"{height}p HD",
                        "ext": ext if ext in ["mp4", "webm"] else "mp4",
                        "vcodec": vcodec[:15] if vcodec else "H.264",
                        "acodec": acodec[:15] if is_audio else "AAC (Merged)",
                        "filesize_bytes": filesize,
                        "filesize_str": format_bytes(filesize),
                        "is_audio_only": False,
                        "is_video": True,
                        "requires_mux": needs_mux,
                        "note": "DASH Video + High Bitrate Audio" if needs_mux else "Direct Stream"
                    })

        return {
            "title": title,
            "thumbnail": thumbnail,
            "duration": duration,
            "uploader": uploader,
            "extractor": extractor,
            "url": url,
            "formats_json": json.dumps(formats_list)
        }


def download_media(url: str,
                   format_id: str,
                   output_dir: str,
                   progress_callback: Any = None) -> str:
    """
    Downloads media using yt-dlp with live progress hook reporting back to Kotlin.
    Returns the absolute path to the downloaded/muxed file on disk.
    """
    url = url.strip()
    if not url:
        raise ValueError("URL cannot be empty")

    os.makedirs(output_dir, exist_ok=True)

    def hook(d: Dict[str, Any]):
        if progress_callback is None:
            return

        status = d.get("status", "downloading")
        if status == "downloading":
            downloaded = d.get("downloaded_bytes", 0)
            total = d.get("total_bytes") or d.get("total_bytes_estimate", 0)
            speed = d.get("speed", 0) or 0
            eta = d.get("eta", 0) or 0
            filename = os.path.basename(d.get("filename", ""))

            try:
                progress_callback.onProgress(
                    int(downloaded),
                    int(total),
                    float(speed),
                    int(eta),
                    "DOWNLOADING",
                    filename
                )
            except Exception as cb_err:
                print(f"[downloader.py] Callback error: {cb_err}")

        elif status == "finished":
            filename = os.path.basename(d.get("filename", ""))
            try:
                progress_callback.onProgress(
                    100, 100, 0.0, 0, "PROCESSING", filename
                )
            except Exception:
                pass

    ydl_opts = {
        "format": format_id or "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
        "outtmpl": os.path.join(output_dir, "%(title)s.%(ext)s"),
        "merge_output_format": "mp4",
        "noplaylist": True,
        "writethumbnail": False,
        "socket_timeout": 30,
        "retries": 10,
        "fragment_retries": 10,
        "quiet": False,
        "no_warnings": True,
        "progress_hooks": [hook],
        "postprocessors": [
            {
                "key": "FFmpegMetadata",
                "add_metadata": True,
            }
        ],
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            if not info:
                raise RuntimeError("Extraction failed")

            title = info.get("title", "download")
            ext = info.get("ext", "mp4")
            
            # Find output file
            expected_name = f"{title}.{ext}"
            expected_path = os.path.join(output_dir, expected_name)

            if os.path.exists(expected_path):
                return expected_path

            # Search in output_dir for recently created file
            files = [os.path.join(output_dir, f) for f in os.listdir(output_dir)]
            if files:
                newest = max(files, key=os.path.getmtime)
                return newest

            return expected_path

    except Exception as e:
        err_str = str(e)
        if "403" in err_str or "Forbidden" in err_str:
            raise RuntimeError(f"HTTP 403 Forbidden: YouTube cipher blocked. Update yt-dlp wheel. Details: {err_str}")
        raise RuntimeError(f"Download error: {err_str}")


def download_instagram(url: str,
                       output_dir: str,
                       session_cookie: Optional[str] = None,
                       username: Optional[str] = None,
                       password: Optional[str] = None,
                       progress_callback: Any = None) -> List[str]:
    """
    Downloads Instagram Reels, Posts, and Stories using instaloader or yt-dlp fallback.
    Returns list of downloaded media file paths.
    """
    os.makedirs(output_dir, exist_ok=True)
    downloaded_files = []

    # 1. Try Instaloader if available
    if instaloader is not None:
        try:
            L = instaloader.Instaloader(
                dirname_pattern=output_dir,
                filename_pattern="{date_utc}_UTC_{target}",
                download_pictures=True,
                download_videos=True,
                download_video_thumbnails=False,
                download_geotags=False,
                download_comments=False,
                save_metadata=False,
                compress_json=False
            )

            if session_cookie:
                L.context._session.cookies.set("sessionid", session_cookie, domain=".instagram.com")
            elif username and password:
                L.login(username, password)

            shortcode_match = re.search(r'(?:p|reel|tv)\/([a-zA-Z0-9_-]+)', url)
            if shortcode_match:
                shortcode = shortcode_match.group(1)
                post = instaloader.Post.from_shortcode(L.context, shortcode)
                L.download_post(post, target="Instagram")

                # Scan output directory for new media
                for fname in os.listdir(output_dir):
                    if fname.endswith(('.mp4', '.jpg', '.jpeg', '.png')):
                        downloaded_files.append(os.path.join(output_dir, fname))

                if downloaded_files:
                    return downloaded_files
        except Exception as e:
            print(f"[downloader.py] Instaloader attempt failed: {e}. Falling back to yt-dlp...")

    # 2. Universal fallback using yt-dlp
    file_path = download_media(url, "best", output_dir, progress_callback)
    if file_path and os.path.exists(file_path):
        return [file_path]

    return downloaded_files


def download_facebook(url: str,
                      output_dir: str,
                      progress_callback: Any = None) -> str:
    """
    Downloads public Facebook videos using yt-dlp.
    """
    return download_media(url, "best", output_dir, progress_callback)
