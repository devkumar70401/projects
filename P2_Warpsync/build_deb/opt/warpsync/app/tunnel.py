import subprocess
import re
import time
import qrcode

def start_public_tunnel(port: int = 8000):
    """
    Launches a zero-configuration public HTTPS tunnel via localhost.run.
    Returns the public HTTPS URL and QR code string.
    """
    cmd = [
        "ssh", "-o", "StrictHostKeyChecking=no",
        "-o", "ServerAliveInterval=30",
        "-R", f"80:localhost:{port}",
        "nokey@localhost.run"
    ]
    
    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1
        )
        
        tunnel_url = None
        start_time = time.time()
        
        while time.time() - start_time < 15:
            line = proc.stdout.readline()
            if not line:
                break
            # Match https://xxx.lhr.life or https://xxx.serveo.net
            match = re.search(r'https://[a-zA-Z0-9-]+\.lhr\.life', line)
            if match:
                tunnel_url = match.group(0)
                break
                
        return tunnel_url, proc
    except Exception as e:
        print(f"Tunnel creation warning: {e}")
        return None, None
