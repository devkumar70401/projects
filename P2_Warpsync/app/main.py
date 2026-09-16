import os
import json
import time
import asyncio
from pathlib import Path
from typing import List, Dict, Any
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request, File, UploadFile, WebSocket, WebSocketDisconnect, HTTPException, Form
from fastapi.responses import HTMLResponse, FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

from app.network import get_local_ip, generate_qr_base64
from app.storage import (
    ensure_downloads_dir, save_uploaded_file, get_shared_files, 
    delete_shared_file, DOWNLOADS_DIR, format_file_size
)

SERVER_START_TIME = time.time()

@asynccontextmanager
async def lifespan(app: FastAPI):
    ensure_downloads_dir()
    yield

app = FastAPI(title="WarpSync - Instant P2P Local File Share", version="2.0.0", lifespan=lifespan)

BASE_DIR = Path(__file__).resolve().parent
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))
app.mount("/static", StaticFiles(directory=str(BASE_DIR / "static")), name="static")

# Shared state
clipboards: List[Dict[str, Any]] = []

class ConnectionManager:
    def __init__(self):
        self.active_connections: List[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.active_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        if websocket in self.active_connections:
            self.active_connections.remove(websocket)

    async def broadcast(self, message: dict):
        for connection in list(self.active_connections):
            try:
                await connection.send_json(message)
            except Exception:
                self.disconnect(connection)

manager = ConnectionManager()

@app.get("/", response_class=HTMLResponse)
async def get_index(request: Request):
    local_ip = get_local_ip()
    port = request.url.port or 8000
    access_url = f"http://{local_ip}:{port}"
    qr_b64 = generate_qr_base64(access_url)
    
    return templates.TemplateResponse(
        request=request,
        name="index.html",
        context={
            "access_url": access_url,
            "local_ip": local_ip,
            "port": port,
            "qr_b64": qr_b64
        }
    )

@app.get("/api/info")
async def get_server_info(request: Request):
    local_ip = get_local_ip()
    port = request.url.port or 8000
    access_url = f"http://{local_ip}:{port}"
    uptime_sec = int(time.time() - SERVER_START_TIME)
    
    return {
        "status": "healthy",
        "version": "2.0.0",
        "local_ip": local_ip,
        "port": port,
        "access_url": access_url,
        "qr_b64": generate_qr_base64(access_url),
        "files_count": len(get_shared_files()),
        "clipboards_count": len(clipboards),
        "peers_count": len(manager.active_connections),
        "uptime_seconds": uptime_sec
    }

@app.get("/api/files")
async def list_files():
    return get_shared_files()

@app.post("/api/upload")
async def upload_files(files: List[UploadFile] = File(...)):
    saved_results = []
    for file in files:
        result = save_uploaded_file(file, file.filename)
        saved_results.append(result)
        
        # Broadcast file update event over WebSockets
        await manager.broadcast({
            "type": "file_received",
            "file": result
        })
        
    return {"message": "Files uploaded successfully", "files": saved_results}

@app.delete("/api/files/{filename}")
async def delete_file(filename: str):
    success = delete_shared_file(filename)
    if not success:
        raise HTTPException(status_code=404, detail="File not found")
        
    await manager.broadcast({
        "type": "file_deleted",
        "filename": filename
    })
    return {"status": "success", "message": f"Deleted {filename}"}

@app.get("/api/download/{filename}")
async def download_file(filename: str):
    file_path = DOWNLOADS_DIR / Path(filename).name
    if not file_path.exists() or not file_path.is_file():
        raise HTTPException(status_code=404, detail="File not found")
    return FileResponse(path=file_path, filename=filename, media_type="application/octet-stream")

@app.get("/api/clipboard")
async def get_clipboards():
    return clipboards

@app.post("/api/clipboard")
async def add_clipboard(content: str = Form(...)):
    item = {
        "id": int(time.time() * 1000),
        "content": content,
        "timestamp": time.strftime("%H:%M:%S")
    }
    clipboards.insert(0, item)
    if len(clipboards) > 30:
        clipboards.pop()
        
    await manager.broadcast({
        "type": "clipboard_added",
        "item": item
    })
    return {"status": "success", "item": item}

@app.delete("/api/clipboard/{item_id}")
async def delete_clipboard_item(item_id: int):
    global clipboards
    clipboards = [c for c in clipboards if c["id"] != item_id]
    await manager.broadcast({
        "type": "clipboard_deleted",
        "id": item_id
    })
    return {"status": "success"}

@app.delete("/api/clipboard")
async def clear_all_clipboard():
    global clipboards
    clipboards.clear()
    await manager.broadcast({
        "type": "clipboard_cleared"
    })
    return {"status": "success"}

@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await manager.connect(websocket)
    try:
        await manager.broadcast({
            "type": "peer_count",
            "count": len(manager.active_connections)
        })
        
        while True:
            data = await websocket.receive_text()
            message = json.loads(data)
            if message.get("type") == "ping":
                await websocket.send_json({"type": "pong", "timestamp": time.time()})
            elif message.get("type") in ["webrtc_offer", "webrtc_answer", "webrtc_ice"]:
                await manager.broadcast(message)
    except WebSocketDisconnect:
        manager.disconnect(websocket)
        await manager.broadcast({
            "type": "peer_count",
            "count": len(manager.active_connections)
        })
