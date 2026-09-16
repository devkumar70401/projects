import unittest
import sys
import os
import json

sys.path.insert(0, os.path.abspath("/home/dev/SE/Projects/PersonalMediaDownloader/app/src/main/python"))
import downloader

class TestDownloaderLayer(unittest.TestCase):

    def test_sanitize_filename(self):
        dirty = 'My Video: Episode 1 / "Special" <HD>?*'
        clean = downloader.sanitize_filename(dirty)
        self.assertNotIn(":", clean)
        self.assertNotIn("/", clean)
        self.assertNotIn('"', clean)
        self.assertNotIn("<", clean)
        self.assertNotIn(">", clean)
        self.assertNotIn("?", clean)
        self.assertNotIn("*", clean)
        self.assertEqual(clean, "My Video Episode 1 Special HD")

    def test_format_bytes(self):
        self.assertEqual(downloader.format_bytes(0), "Unknown")
        self.assertEqual(downloader.format_bytes(None), "Unknown")
        self.assertEqual(downloader.format_bytes(1024), "1.0 KB")
        self.assertEqual(downloader.format_bytes(1024 * 1024 * 50), "50.0 MB")
        self.assertEqual(downloader.format_bytes(1024 * 1024 * 1024 * 2), "2.0 GB")

    def test_progress_callback_mock(self):
        class MockProgressCallback:
            def __init__(self):
                self.calls = []

            def onProgress(self, downloaded, total, speed, eta, status, filename):
                self.calls.append({
                    "downloaded": downloaded,
                    "total": total,
                    "speed": speed,
                    "eta": eta,
                    "status": status,
                    "filename": filename
                })

        cb = MockProgressCallback()
        cb.onProgress(5000, 10000, 1024.0, 5, "DOWNLOADING", "test.mp4")
        self.assertEqual(len(cb.calls), 1)
        self.assertEqual(cb.calls[0]["downloaded"], 5000)
        self.assertEqual(cb.calls[0]["total"], 10000)
        self.assertEqual(cb.calls[0]["status"], "DOWNLOADING")

    def test_empty_url_handling(self):
        with self.assertRaises(ValueError):
            downloader.get_media_info("")
        with self.assertRaises(ValueError):
            downloader.download_media("", "best", "/tmp")

if __name__ == "__main__":
    unittest.main()
