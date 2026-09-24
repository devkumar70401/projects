"""
yt_downloader.py - Core Engine for Capability-Oriented Media Downloader
Modeled directly on /home/dev/SE/Library/Scripts/YouTube_Ingestion/yt_downloader_cli.py

Features:
  1. Original Format MP4: Extracts & downloads highest available video & audio combined without lossy re-encoding.
  2. Automatic Node.js runtime detection for YouTube n-sig / decipher execution.
  3. Structured extraction of formats, resolutions, and direct stream URLs for Android UI.
  4. Real-time progress callbacks for integration with CLI and Android Download Queue.
"""

import os
import sys
import json
import shutil
from pathlib import Path
from typing import Dict, Any, List, Optional, Callable
import yt_dlp


def get_default_download_dir() -> Path:
    """Returns default destination directory, prioritizing ~/Music or Movies."""
    dest = Path.home() / "Music"
    dest.mkdir(parents=True, exist_ok=True)
    return dest


def get_node_runtime() -> dict:
    """Finds the available Node.js binary to handle YouTube n-sig / cipher execution."""
    candidates = [
        shutil.which("node"),
        shutil.which("nodejs"),
        str(Path.home() / "SE/Projects/.venv/bin/node"),
        str(Path.home() / "SE/Library/.venv/bin/node"),
        str(Path.home() / "SE/Notes/.venv/bin/node"),
        "/data/data/com.termux/files/usr/bin/node",
        "/system/bin/node",
    ]
    for candidate in candidates:
        if candidate and (os.path.exists(candidate) or shutil.which(candidate)):
            return {"node": {"path": candidate}}
    return {}


def build_ydl_options(
    output_dir: Optional[Path] = None,
    format_spec: Optional[str] = None,
    progress_hook: Optional[Callable[[Dict[str, Any]], None]] = None,
) -> dict:
    """Constructs yt-dlp configuration matching yt_downloader_cli.py."""
    if output_dir is None:
        output_dir = get_default_download_dir()
    else:
        output_dir = Path(output_dir).expanduser().resolve()
        output_dir.mkdir(parents=True, exist_ok=True)

    ydl_opts = {
        "format": format_spec or "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
        "outtmpl": str(output_dir / "%(title)s.%(ext)s"),
        "merge_output_format": "mp4",
        "noplaylist": True,
        "writethumbnail": False,
        "socket_timeout": 30,
        "retries": 10,
        "fragment_retries": 10,
        "concurrent_fragment_downloads": 8,
        "buffersize": 1048576,
        "http_chunk_size": 10485760,
        "quiet": True,
        "no_warnings": True,
        "postprocessors": [
            {
                "key": "FFmpegMetadata",
                "add_metadata": True,
            }
        ],
    }

    node_rt = get_node_runtime()
    if node_rt:
        ydl_opts["js_runtimes"] = node_rt

    if progress_hook:
        ydl_opts["progress_hooks"] = [progress_hook]

    return ydl_opts


def extract_media_info(url: str) -> Dict[str, Any]:
    """
    Probes remote YouTube streams and formats, returning structured JSON
    compatible with both Android ExtractedMedia UI and CLI output.
    """
    ydl_opts = {
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "skip_download": True,
    }
    node_rt = get_node_runtime()
    if node_rt:
        ydl_opts["js_runtimes"] = node_rt

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=False)
        if not info:
            raise ValueError(f"Could not extract info from URL: {url}")

        video_id = info.get("id", "")
        title = info.get("title", "Unknown Title")
        thumbnail = info.get("thumbnail", "")
        uploader = info.get("uploader", info.get("channel", "YouTube"))
        duration = info.get("duration", 0)

        # Parse streams into structured formats
        formats_raw = info.get("formats", [])
        stream_options: List[Dict[str, Any]] = []

        # 1. Flagship Original Format (Highest Quality MP4)
        stream_options.append({
            "id": "best_original_mp4",
            "extension": "mp4",
            "resolution": "Original Best MP4 (Combined)",
            "bitrate": 0,
            "isAudioOnly": False,
            "videoUrl": url,
            "audioUrl": "",
            "requiresMuxing": True,
            "approximateBytes": info.get("filesize") or info.get("filesize_approx") or 0,
            "mimeType": "video/mp4",
            "isOriginalFormat": True,
        })

        # 2. Extract best audio stream (M4A)
        best_audio = None
        for f in reversed(formats_raw):
            if f.get("vcodec") == "none" and (f.get("acodec") != "none"):
                ext = f.get("ext", "m4a")
                if ext in ("m4a", "mp4", "aac", "mp3", "opus"):
                    best_audio = f
                    break

        if best_audio:
            abr = best_audio.get("abr", 128)
            stream_options.append({
                "id": "best_audio_m4a",
                "extension": "m4a",
                "resolution": f"HQ Audio ({int(abr)} kbps)",
                "bitrate": int(abr * 1000),
                "isAudioOnly": True,
                "videoUrl": "",
                "audioUrl": best_audio.get("url", ""),
                "requiresMuxing": False,
                "approximateBytes": best_audio.get("filesize") or best_audio.get("filesize_approx") or 0,
                "mimeType": "audio/mp4",
                "isOriginalFormat": False,
            })

        # 3. Progressive and adaptive video resolutions
        seen_res = set()
        for f in reversed(formats_raw):
            height = f.get("height")
            if not height or height in seen_res:
                continue
            seen_res.add(height)

            ext = f.get("ext", "mp4")
            v_url = f.get("url", "")
            has_audio = f.get("acodec") != "none"
            a_url = best_audio.get("url", "") if best_audio and not has_audio else ""

            stream_options.append({
                "id": f"video_{height}p_{f.get('format_id')}",
                "extension": "mp4" if ext == "mp4" else ext,
                "resolution": f"{height}p HD" if height >= 720 else f"{height}p",
                "bitrate": int(f.get("tbr", 0) * 1000),
                "isAudioOnly": False,
                "videoUrl": v_url,
                "audioUrl": a_url,
                "requiresMuxing": not has_audio,
                "approximateBytes": f.get("filesize") or f.get("filesize_approx") or 0,
                "mimeType": "video/mp4",
                "isOriginalFormat": False,
            })

        return {
            "id": video_id,
            "title": title,
            "thumbnailUrl": thumbnail,
            "author": uploader,
            "durationSeconds": duration,
            "source": "YOUTUBE",
            "streamOptions": stream_options,
        }


def download_media_file(
    url: str,
    output_dir: Optional[Path] = None,
    format_spec: Optional[str] = None,
    progress_hook: Optional[Callable[[Dict[str, Any]], None]] = None,
) -> Dict[str, Any]:
    """
    Executes a high-fidelity download matching yt_downloader_cli.py.
    """
    if output_dir is None:
        output_dir = get_default_download_dir()
    else:
        output_dir = Path(output_dir).expanduser().resolve()
        output_dir.mkdir(parents=True, exist_ok=True)

    ydl_opts = build_ydl_options(output_dir, format_spec, progress_hook)

    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        title = info.get("title", "Unknown Title")
        ext = info.get("ext", "mp4")
        out_file = output_dir / f"{title}.{ext}"

        return {
            "status": "success",
            "title": title,
            "filePath": str(out_file),
            "extension": ext,
            "duration": info.get("duration", 0),
            "videoCodec": info.get("vcodec", "Native"),
            "audioCodec": info.get("acodec", "Native"),
        }
