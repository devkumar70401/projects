"""
downloader_ipc.py - Programmatic JSON IPC Server / CLI Bridge for Android & Core
Provides machine-readable JSON interfaces for:
  1. --ping: Health-check and capability inspection
  2. --extract <url>: Returns full metadata and structured stream options
  3. --download <url> --out <dir> [--format <fmt>]: Streams live JSON progress events and returns file info
"""

import sys
import os
import json
import argparse
from pathlib import Path
from yt_downloader import (
    extract_media_info,
    download_media_file,
    get_node_runtime,
    get_default_download_dir,
)
import yt_dlp


def handle_ping():
    node_info = get_node_runtime()
    res = {
        "status": "ok",
        "engine": "yt-dlp",
        "version": yt_dlp.version.__version__,
        "nodeRuntime": bool(node_info),
        "nodePath": node_info.get("node", {}).get("path", "") if node_info else None,
    }
    print(json.dumps(res))
    sys.exit(0)


def handle_extract(url: str):
    try:
        data = extract_media_info(url)
        print(json.dumps({"status": "success", "data": data}))
        sys.exit(0)
    except Exception as e:
        print(json.dumps({"status": "error", "message": str(e)}))
        sys.exit(1)


def handle_download(url: str, output_dir: str, format_spec: str = None):
    try:
        out_path = Path(output_dir) if output_dir else get_default_download_dir()

        def progress_hook(d):
            if d.get("status") == "downloading":
                total_bytes = d.get("total_bytes") or d.get("total_bytes_estimate") or 0
                downloaded = d.get("downloaded_bytes") or 0
                percent = (downloaded / total_bytes * 100) if total_bytes > 0 else 0.0
                event = {
                    "type": "progress",
                    "status": "downloading",
                    "percent": round(percent, 2),
                    "downloadedBytes": downloaded,
                    "totalBytes": total_bytes,
                    "speed": d.get("_speed_str", "").strip(),
                    "eta": d.get("_eta_str", "").strip(),
                }
                # Emit line-delimited JSON
                sys.stdout.write(json.dumps(event) + "\n")
                sys.stdout.flush()
            elif d.get("status") == "finished":
                event = {
                    "type": "finished",
                    "status": "muxing",
                    "message": "Download finished, merging original MP4 streams...",
                }
                sys.stdout.write(json.dumps(event) + "\n")
                sys.stdout.flush()

        res = download_media_file(
            url=url,
            output_dir=out_path,
            format_spec=format_spec,
            progress_hook=progress_hook,
        )

        completed_event = {
            "type": "completed",
            "status": "success",
            "result": res,
        }
        print(json.dumps(completed_event))
        sys.exit(0)
    except Exception as e:
        err_event = {
            "type": "error",
            "status": "error",
            "message": str(e),
        }
        print(json.dumps(err_event))
        sys.exit(1)


def main():
    parser = argparse.ArgumentParser(description="Downloader IPC Interface")
    parser.add_argument("--ping", action="store_true", help="Health check")
    parser.add_argument("--extract", type=str, help="Extract media stream options from URL")
    parser.add_argument("--download", type=str, help="Download media from URL")
    parser.add_argument("--out", type=str, default=None, help="Output destination folder")
    parser.add_argument("--format", type=str, default=None, help="Optional format specification")

    args = parser.parse_args()

    if args.ping:
        handle_ping()
    elif args.extract:
        handle_extract(args.extract)
    elif args.download:
        handle_download(args.download, args.out, args.format)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
