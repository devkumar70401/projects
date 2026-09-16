# ⚡ WarpSync (P2_Warpsync)

> **Instant Local Peer-to-Peer File Sharing Application**

---

## 📌 What is WarpSync?

**WarpSync** is a lightweight, high-speed local file and clipboard sharing tool (a replica of ShareIt, LocalSend, or Quick Share). It lets you transfer files, documents, photos, videos, and text snippets back and forth between devices without relying on cloud storage or an internet connection.

---

## 🎯 For What Purpose?

1. **Zero-Internet File Transfer**: Share large files between devices placed on the same table when internet access is unavailable, slow, or restricted.
2. **Zero-Install Convenience**: No app installation is required on recipient/client devices (mobile phones, tablets, or secondary laptops). Any browser on the local network can connect instantly.
3. **Cross-Platform Compatibility**: Connects Linux, Windows, macOS, Android, and iOS seamlessly.
4. **Privacy & Security**: Files stay entirely within your local Wi-Fi or hotspot network; nothing is uploaded to external servers.

---

## ⚡ How to Use WarpSync

### 🖥️ Server Side Setup (Your Primary Laptop / PC)

#### **Option 1: Simple 1-Click Launch (Terminal)**
1. Open your terminal and navigate to the project directory:
   ```bash
   cd /home/dev/SE/P2_Warpsync
   ```
2. Run the launcher script:
   ```bash
   ./run.sh
   ```
   *(Or run `python3 start.py`)*

3. **What happens next:**
   - A virtual environment `.venv` is automatically prepared.
   - Dependencies are verified.
   - An **ASCII QR Code** and your **Local Access URL** (e.g. `http://192.168.1.50:8000`) will print directly in your terminal window, and your web browser will open automatically.

---

#### **Option 2: Docker Launch (Sandboxed / Containerized)**
If you prefer running WarpSync inside a Docker container:
```bash
cd /home/dev/SE/P2_Warpsync
docker compose up -d
```
To view the terminal QR code and connection logs:
```bash
docker compose logs -f
```

---

### 📱 Client Side Setup (Mobile Phones, Tablets, or Secondary Laptops)

**No installation required!**

#### **Step 1: Connect to the Same Network**
Ensure your mobile device or client laptop is connected to the **same Wi-Fi network** or **mobile hotspot** as the server laptop.

#### **Step 2: Open WarpSync on Client**
- **Mobile Phones & Tablets**: Scan the QR Code displayed on the server's terminal screen or web page using your phone camera.
- **Secondary Laptops**: Open your browser and type the Local URL shown on the server screen (e.g. `http://192.168.1.50:8000`).

#### **Step 3: Transfer Files & Text**
- **To Send Files from Client $\rightarrow$ Server**:
  - Tap **Browse Files** or drag-and-drop any file into the upload dropzone.
  - Watch the live progress bar as it transfers directly to the server's `downloads/` folder.
- **To Download Files from Server $\rightarrow$ Client**:
  - Scroll down to **Received Files Library** and tap **Download** next to any file.
- **To Share Text / Links**:
  - Type or paste any message into **Quick Text & Link Share** and tap **Share Text**. It instantly appears on all connected screens.

---

## 💡 Summary Workflow

```
[ Laptop Server ] <==== (Local Wi-Fi / Hotspot) ====> [ Phone / Client Browser ]
  - Runs ./run.sh                                        - Scans QR Code
  - Shows QR Code                                        - Sends / Receives Files
  - Stores in downloads/                                 - Zero App Install Needed
```

---

## 🛠️ Testing & Troubleshooting

- **Check server status with Pytest**:
  ```bash
  source .venv/bin/activate
  PYTHONPATH=. pytest -v
  ```
- **Connection Issue?**
  - Verify both devices are on the exact same Wi-Fi router or Hotspot.
  - Make sure firewall permits port `8000`.

---

## 📄 License
MIT License • Part of the `SE` Workspace.
