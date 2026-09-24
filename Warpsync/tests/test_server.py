import pytest
import io
from fastapi.testclient import TestClient
from app.main import app
from app.network import get_local_ip, generate_qr_base64

client = TestClient(app)

def test_get_index():
    response = client.get("/")
    assert response.status_code == 200
    assert "WarpSync" in response.text
    assert "Connect Phone / Device" in response.text


def test_get_server_info():
    response = client.get("/api/info")
    assert response.status_code == 200
    data = response.json()
    assert "local_ip" in data
    assert "access_url" in data
    assert data["access_url"].startswith("http://")

def test_file_upload_and_download():
    # Test uploading a file
    file_content = b"Hello WarpSync local file transfer testing content!"
    response = client.post(
        "/api/upload",
        files={"files": ("test_file.txt", io.BytesIO(file_content), "text/plain")}
    )
    assert response.status_code == 200
    data = response.json()
    assert len(data["files"]) > 0
    saved_filename = data["files"][0]["filename"]

    # Test downloading the uploaded file
    dl_response = client.get(f"/api/download/{saved_filename}")
    assert dl_response.status_code == 200
    assert dl_response.content == file_content

def test_clipboard_share():
    response = client.post("/api/clipboard", data={"content": "Test secret token or URL"})
    assert response.status_code == 200
    assert response.json()["item"]["content"] == "Test secret token or URL"

    get_resp = client.get("/api/clipboard")
    assert get_resp.status_code == 200
    items = get_resp.json()
    assert any(item["content"] == "Test secret token or URL" for item in items)
