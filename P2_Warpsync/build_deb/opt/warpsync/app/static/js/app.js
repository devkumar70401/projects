// WarpSync 2.0 State-of-the-Art P2P Sharing Logic with Multi-Theme Engine

let ws;
let pingInterval;

// Theme Switcher Engine
function changeTheme(themeName) {
    if (themeName === 'midnight') {
        document.documentElement.removeAttribute('data-theme');
    } else {
        document.documentElement.setAttribute('data-theme', themeName);
    }
    localStorage.setItem('warpsync_theme', themeName);
    const selector = document.getElementById('theme-selector');
    if (selector) selector.value = themeName;
}

function initTheme() {
    const savedTheme = localStorage.getItem('warpsync_theme') || 'midnight';
    changeTheme(savedTheme);
}

function initWebSocket() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${protocol}//${window.location.host}/ws`;
    
    ws = new WebSocket(wsUrl);

    ws.onopen = () => {
        console.log("⚡ Connected to WarpSync WebSocket Stream");
        const statusDot = document.getElementById("status-dot");
        if (statusDot) statusDot.style.background = "#10b981";
        
        // Heartbeat ping
        clearInterval(pingInterval);
        pingInterval = setInterval(() => {
            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({ type: "ping" }));
            }
        }, 10000);
    };

    ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === "peer_count") {
            const peerStatus = document.getElementById("peer-status");
            if (peerStatus) {
                peerStatus.textContent = `${data.count} Connected Device${data.count > 1 ? 's' : ''}`;
            }
        } else if (data.type === "file_received") {
            loadFilesList();
            showNotification(`📥 Received file: ${data.file.filename}`);
        } else if (data.type === "file_deleted") {
            loadFilesList();
            showNotification(`🗑️ File deleted: ${data.filename}`);
        } else if (data.type === "clipboard_added") {
            loadClipboards();
            showNotification(`📋 New text snippet shared`);
        } else if (data.type === "clipboard_deleted" || data.type === "clipboard_cleared") {
            loadClipboards();
        }
    };

    ws.onclose = () => {
        console.log("WebSocket disconnected, reconnecting in 3s...");
        const statusDot = document.getElementById("status-dot");
        if (statusDot) statusDot.style.background = "#ef4444";
        setTimeout(initWebSocket, 3000);
    };
}

// Setup Drag & Drop Upload Handlers
const dropzone = document.getElementById("dropzone");
const fileInput = document.getElementById("file-input");

if (dropzone && fileInput) {
    ['dragenter', 'dragover'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropzone.classList.add("dragover");
        });
    });

    ['dragleave', 'drop'].forEach(eventName => {
        dropzone.addEventListener(eventName, (e) => {
            e.preventDefault();
            dropzone.classList.remove("dragover");
        });
    });

    dropzone.addEventListener('drop', (e) => {
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            uploadFiles(files);
        }
    });

    fileInput.addEventListener('change', (e) => {
        if (fileInput.files.length > 0) {
            uploadFiles(fileInput.files);
        }
    });
}

function uploadFiles(files) {
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
        formData.append("files", files[i]);
    }

    const progressContainer = document.getElementById("progress-container");
    const progressBar = document.getElementById("progress-bar");
    const uploadStatus = document.getElementById("upload-status");

    progressContainer.style.display = "block";
    uploadStatus.style.display = "block";
    uploadStatus.textContent = "Uploading file(s)...";

    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/api/upload", true);

    xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
            const percent = Math.round((e.loaded / e.total) * 100);
            progressBar.style.width = percent + "%";
            uploadStatus.textContent = `⚡ Uploading: ${percent}%`;
        }
    };

    xhr.onload = () => {
        if (xhr.status === 200) {
            uploadStatus.textContent = "✅ Upload complete!";
            setTimeout(() => {
                progressContainer.style.display = "none";
                uploadStatus.style.display = "none";
                progressBar.style.width = "0%";
            }, 2500);
            loadFilesList();
        } else {
            uploadStatus.textContent = "❌ Upload failed!";
        }
    };

    xhr.send(formData);
}

function loadFilesList() {
    fetch("/api/files")
        .then(res => res.json())
        .then(files => {
            const fileListEl = document.getElementById("file-list");
            const fileCountBadge = document.getElementById("file-count-badge");
            
            if (fileCountBadge) fileCountBadge.textContent = files.length;

            if (!files || files.length === 0) {
                fileListEl.innerHTML = `
                    <div style="text-align: center; padding: 2.5rem 1rem; color: var(--text-muted);">
                        <div style="font-size: 2.5rem; margin-bottom: 0.5rem;">📁</div>
                        <p>No files shared yet. Drag & drop files above to transfer instantly!</p>
                    </div>`;
                return;
            }

            fileListEl.innerHTML = files.map(file => {
                const icon = getCategoryIcon(file.category);
                return `
                <div class="file-item">
                    <div class="file-info">
                        <span class="file-icon">${icon}</span>
                        <div>
                            <div class="file-name">${escapeHtml(file.filename)}</div>
                            <div class="file-meta">${file.formatted_size} • ${file.modified_str}</div>
                        </div>
                    </div>
                    <div class="file-actions">
                        <a href="/api/download/${encodeURIComponent(file.filename)}" class="btn-primary" style="padding: 0.4rem 0.9rem; font-size: 0.8rem; text-decoration: none;">
                            📥 Download
                        </a>
                        <button class="btn-danger" style="padding: 0.4rem 0.7rem; font-size: 0.8rem;" onclick="deleteFile('${escapeHtml(file.filename)}')">
                            🗑️
                        </button>
                    </div>
                </div>
            `;
            }).join('');
        })
        .catch(err => console.error("Error loading files:", err));
}

function deleteFile(filename) {
    if (!confirm(`Are you sure you want to delete "${filename}"?`)) return;
    
    fetch(`/api/files/${encodeURIComponent(filename)}`, { method: "DELETE" })
        .then(res => res.json())
        .then(data => {
            loadFilesList();
            showNotification(`🗑️ Deleted ${filename}`);
        });
}

function sendClipboard() {
    const input = document.getElementById("clip-input");
    const text = input.value.trim();
    if (!text) return;

    const formData = new FormData();
    formData.append("content", text);

    fetch("/api/clipboard", {
        method: "POST",
        body: formData
    })
    .then(res => res.json())
    .then(data => {
        input.value = "";
        loadClipboards();
        showNotification("🚀 Text snippet shared across devices!");
    });
}

function loadClipboards() {
    fetch("/api/clipboard")
        .then(res => res.json())
        .then(items => {
            const clipListEl = document.getElementById("clip-list");
            const clipCountBadge = document.getElementById("clip-count-badge");
            
            if (clipCountBadge) clipCountBadge.textContent = items.length;

            if (!items || items.length === 0) {
                clipListEl.innerHTML = `<p style="color: var(--text-muted); font-size: 0.85rem; padding: 1rem 0;">No text snippets shared yet.</p>`;
                return;
            }

            clipListEl.innerHTML = items.map(item => `
                <div class="clip-item">
                    <div style="flex: 1; min-width: 0; padding-right: 1rem;">
                        <div class="clip-content">${escapeHtml(item.content)}</div>
                        <div class="clip-time">Shared at ${item.timestamp}</div>
                    </div>
                    <div style="display: flex; gap: 0.5rem; align-items: center;">
                        <button class="btn-primary" style="padding: 0.35rem 0.75rem; font-size: 0.75rem;" onclick="copyText('${escapeHtml(item.content)}')">
                            📋 Copy
                        </button>
                        <button class="btn-danger" style="padding: 0.35rem 0.6rem; font-size: 0.75rem;" onclick="deleteClipboard(${item.id})">
                            🗑️
                        </button>
                    </div>
                </div>
            `).join('');
        });
}

function deleteClipboard(id) {
    fetch(`/api/clipboard/${id}`, { method: "DELETE" })
        .then(res => res.json())
        .then(data => loadClipboards());
}

function clearAllClipboard() {
    if (!confirm("Clear all shared text snippets?")) return;
    fetch("/api/clipboard", { method: "DELETE" })
        .then(res => res.json())
        .then(data => loadClipboards());
}

function copyText(text) {
    navigator.clipboard.writeText(text).then(() => {
        showNotification("📋 Copied to clipboard!");
    });
}

function copyAccessUrl() {
    const urlText = document.getElementById("url-text").textContent;
    navigator.clipboard.writeText(urlText).then(() => {
        showNotification("🔗 Copied Local Network URL!");
    });
}

function getCategoryIcon(cat) {
    switch (cat) {
        case 'image': return '🖼️';
        case 'video': return '🎥';
        case 'pdf': return '📕';
        case 'audio': return '🎵';
        case 'archive': return '📦';
        case 'code': return '💻';
        default: return '📄';
    }
}

function showNotification(msg) {
    const toast = document.createElement("div");
    toast.className = "toast-notification";
    toast.textContent = msg;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return String(text).replace(/[&<>"']/g, m => map[m]);
}

// Initialize on DOM load
document.addEventListener("DOMContentLoaded", () => {
    initTheme();
    initWebSocket();
    loadFilesList();
    loadClipboards();
});
