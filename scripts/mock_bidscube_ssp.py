#!/usr/bin/env python3
"""
Mock Bidscube SSP: HTTP GET /sdk?... → JSON { "adm", "position" }.

Android SDK завжди використовує HTTPS для запитів. Щоб дістатися з будь-якого девайса,
потрібен публічний HTTPS URL — скрипт може сам підняти тунель (cloudflared або ngrok).

Запуск:
  python3 scripts/mock_bidscube_ssp.py              # сервер + авто-тунель (cloudflared → ngrok)
  python3 scripts/mock_bidscube_ssp.py --no-tunnel # лише локально :8787 (далі ngrok вручну)
  python3 scripts/mock_bidscube_ssp.py --tunnel cloudflared
  python3 scripts/mock_bidscube_ssp.py --tunnel ngrok

Потрібні в PATH (хоча б один):
  - cloudflared  (https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/)
  - ngrok        (https://ngrok.com/download) — може знадобитись ngrok config add-authtoken

Скопіюйте надрукований adRequestAuthority у bidscube-testapp-android/gradle.properties:
  bidcube.testSspAuthority=<host без https://>
"""
from __future__ import annotations

import argparse
import json
import os
import re
import signal
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, HTTPServer

DEFAULT_PORT = 8787

MOCK_ADM = """<!DOCTYPE html><html><head><meta charset="utf-8"/><meta name="viewport" content="width=device-width"/></head>
<body style="margin:0;background:#16213e;color:#e94560;font-family:system-ui,sans-serif;text-align:center;padding:32px;">
<h2>Mock Bidscube SSP</h2><p>Custom <code>adRequestAuthority</code> works.</p></body></html>"""

# Inline SVG data URIs — via.placeholder.com often fails SSL in WebView on real devices.
_QA_BANNER_SVG = (
    "data:image/svg+xml,%3Csvg%20xmlns%3D%27http%3A//www.w3.org/2000/svg%27%20width%3D%27320%27%20"
    "height%3D%2750%27%3E%3Crect%20width%3D%27320%27%20height%3D%2750%27%20fill%3D%27%2316213e%27/%3E%3C"
    "text%20x%3D%2750%25%27%20y%3D%2750%25%27%20dominant-baseline%3D%27middle%27%20text-anchor%3D%27"
    "middle%27%20fill%3D%27%23e94560%27%20font-family%3D%27sans-serif%27%20font-size%3D%2714%27%3E"
    "Mock%20Banner%3C/text%3E%3C/svg%3E"
)
_DOCWRITE_BANNER_SVG = (
    "data:image/svg+xml,%3Csvg%20xmlns%3D%27http%3A//www.w3.org/2000/svg%27%20width%3D%27320%27%20"
    "height%3D%2750%27%3E%3Crect%20width%3D%27320%27%20height%3D%2750%27%20fill%3D%27%230f3460%27/%3E%3C"
    "text%20x%3D%2750%25%27%20y%3D%2750%25%27%20dominant-baseline%3D%27middle%27%20text-anchor%3D%27"
    "middle%27%20fill%3D%27%2353d8fb%27%20font-family%3D%27sans-serif%27%20font-size%3D%2714%27%3E"
    "DocWrite%3C/text%3E%3C/svg%3E"
)

BANNER_IMP_ADM = (
    "<div id=\"wrapper_mock\">"
    "<a href=\"https://click.example/mock\"><img width=\"320\" height=\"50\" "
    f"src=\"{_QA_BANNER_SVG}\" alt=\"ad\"></a>"
    "</div>"
    "<img width=\"1\" height=\"1\" src=\"https://ssp-bcc-ads.com/sdk?c=b&amp;m=i&amp;h=mock123token\">"
    "<img width=\"1\" height=\"1\" src=\"https://tracking.onaudience.com/pixel/mock\">"
)

BANNER_DOCWRITE_ADM = (
    "document.write('<span id=\"pos\"></span>');"
    "<img width=\"1\" height=\"1\" src=\"https://ssp-bcc-ads.com/sdk?c=b&amp;m=i&amp;h=mockPresWrapper\">"
    "<div id=\"wrapper_docwrite\">"
    "<a href=\"https://click.example/docwrite\"><img width=\"320\" height=\"50\" "
    f"src=\"{_DOCWRITE_BANNER_SVG}\" alt=\"ad\"></a>"
    "</div>"
    "<img width=\"1\" height=\"1\" src=\"https://ssp-bcc-ads.com/sdk?c=b&amp;m=i&amp;h=mockPostWrapper\">"
)

PRESETS = {
    "default": MOCK_ADM,
    "banner_imp": BANNER_IMP_ADM,
    "banner_docwrite": BANNER_DOCWRITE_ADM,
}


def parse_query(path: str) -> dict[str, str]:
    from urllib.parse import parse_qs, urlparse

    q = urlparse(path).query
    parsed = parse_qs(q, keep_blank_values=True)
    return {k: (v[0] if v else "") for k, v in parsed.items()}


def log_request_params(path: str) -> None:
    params = parse_query(path)
    sys.stderr.write(
        "[mock-ssp] placementId=%s bundle=%s c=%s m=%s preset=%s\n"
        % (
            params.get("placementId", params.get("id", "-")),
            params.get("bundle", "-"),
            params.get("c", "-"),
            params.get("m", "-"),
            params.get("preset", "default"),
        )
    )


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args: object) -> None:
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))

    def do_GET(self) -> None:
        path = self.path.split("?", 1)[0]
        if path == "/sdk" or path.startswith("/sdk/"):
            log_request_params(self.path)
            params = parse_query(self.path)
            preset = params.get("preset", "default")
            adm = PRESETS.get(preset, PRESETS["default"])
            payload = {"adm": adm, "position": 0}
            body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        self.send_response(404)
        self.end_headers()


def authority_from_https_url(url: str) -> str:
    u = url.strip()
    if u.lower().startswith("https://"):
        u = u[8:]
    elif u.lower().startswith("http://"):
        u = u[7:]
    slash = u.find("/")
    if slash > 0:
        u = u[:slash]
    return u.strip()


def which(cmd: str) -> str | None:
    from shutil import which as sh_which

    return sh_which(cmd)


def find_tunnel_executable(cmd: str) -> str | None:
    """
    shutil.which + типові шляхи Homebrew на macOS (у зменшеному PATH скрипт часто не бачить brew).
    """
    path = which(cmd)
    if path:
        return path
    if sys.platform == "darwin":
        for prefix in ("/opt/homebrew/bin", "/usr/local/bin"):
            candidate = os.path.join(prefix, cmd)
            if os.path.isfile(candidate) and os.access(candidate, os.X_OK):
                return candidate
    return None


def print_tunnel_install_help(port: int) -> None:
    has_cf = find_tunnel_executable("cloudflared") is not None
    has_ng = find_tunnel_executable("ngrok") is not None
    print("\nДіагностика PATH:")
    print("  cloudflared: %s" % ("знайдено" if has_cf else "немає у PATH (і не в /opt/homebrew/bin, /usr/local/bin)"))
    print("  ngrok:       %s" % ("знайдено" if has_ng else "немає у PATH (і не в /opt/homebrew/bin, /usr/local/bin)"))
    print("\nВстановлення (macOS + Homebrew):")
    print("  brew install cloudflared")
    print("  # або")
    print("  brew install ngrok/ngrok/ngrok && ngrok config add-authtoken <TOKEN>")
    print("\nПісля встановлення перезапустіть скрипт, або в окремому терміналі:")
    print("  cloudflared tunnel --url http://127.0.0.1:%d" % port)
    print("  ngrok http %d" % port)
    print("\nДокументація:")
    print("  https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/install-and-setup/installation/")
    print("  https://ngrok.com/download")


def try_cloudflared(port: int, timeout_sec: float = 45.0) -> tuple[subprocess.Popen[str], str] | tuple[None, None]:
    exe = find_tunnel_executable("cloudflared")
    if not exe:
        return None, None
    cmd = [exe, "tunnel", "--url", "http://127.0.0.1:%d" % port]
    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
    except OSError:
        return None, None

    url_re = re.compile(r"https://[a-zA-Z0-9.-]+\.trycloudflare\.com/?")
    deadline = time.monotonic() + timeout_sec
    assert proc.stdout is not None
    while time.monotonic() < deadline:
        line = proc.stdout.readline()
        if not line:
            if proc.poll() is not None:
                break
            time.sleep(0.05)
            continue
        sys.stderr.write(line)
        m = url_re.search(line)
        if m:
            host = authority_from_https_url(m.group(0))
            return proc, host
    proc.terminate()
    try:
        proc.wait(timeout=3)
    except subprocess.TimeoutExpired:
        proc.kill()
    return None, None


def try_ngrok(port: int, timeout_sec: float = 25.0) -> tuple[subprocess.Popen[str], str] | tuple[None, None]:
    exe = find_tunnel_executable("ngrok")
    if not exe:
        return None, None
    cmd = [exe, "http", str(port), "--log=stdout"]
    try:
        proc = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
    except OSError:
        return None, None

    # Читаємо лог ngrok і паралельно опитуємо локальний API (надійніше для v2/v3).
    assert proc.stdout is not None
    deadline = time.monotonic() + timeout_sec
    host: str | None = None

    def reader() -> None:
        nonlocal host
        for line in proc.stdout:
            sys.stderr.write(line)
            if host:
                continue
            # Інколи URL з’являється прямо в логу
            m = re.search(r"https://[a-zA-Z0-9.-]+\.ngrok[^\\s\"']+", line)
            if m:
                cand = authority_from_https_url(m.group(0))
                if cand:
                    host = cand

    t = threading.Thread(target=reader, daemon=True)
    t.start()

    api_url = "http://127.0.0.1:4040/api/tunnels"
    while time.monotonic() < deadline and host is None:
        try:
            with urllib.request.urlopen(api_url, timeout=1.0) as resp:
                data = json.loads(resp.read().decode())
            for tun in data.get("tunnels") or []:
                pub = tun.get("public_url") or ""
                if pub.startswith("https://"):
                    host = authority_from_https_url(pub)
                    break
        except (urllib.error.URLError, json.JSONDecodeError, ValueError):
            pass
        if proc.poll() is not None:
            break
        time.sleep(0.35)

    if not host:
        proc.terminate()
        try:
            proc.wait(timeout=3)
        except subprocess.TimeoutExpired:
            proc.kill()
        return None, None
    return proc, host


def run_http_server(host: str, port: int, stop_event: threading.Event) -> None:
    try:
        httpd = HTTPServer((host, port), Handler)
    except OSError as e:
        sys.stderr.write("Помилка: не вдалося слухати %s:%d — %s\n" % (host, port, e))
        stop_event.set()
        return
    httpd.timeout = 0.5
    try:
        while not stop_event.is_set():
            httpd.handle_request()
    finally:
        httpd.server_close()


def main() -> int:
    p = argparse.ArgumentParser(description="Mock Bidscube SSP + optional public HTTPS tunnel.")
    p.add_argument("--port", type=int, default=DEFAULT_PORT, help="Local port (default %d)" % DEFAULT_PORT)
    p.add_argument(
        "--tunnel",
        choices=("auto", "cloudflared", "ngrok", "none"),
        default="auto",
        help="Public HTTPS tunnel: auto tries cloudflared then ngrok; none = local only",
    )
    p.add_argument("--no-tunnel", action="store_true", help="Same as --tunnel none")
    args = p.parse_args()
    port = args.port
    mode = "none" if args.no_tunnel else args.tunnel

    bind = "0.0.0.0"
    stop_event = threading.Event()
    server_thread = threading.Thread(
        target=run_http_server,
        args=(bind, port, stop_event),
        daemon=True,
    )
    server_thread.start()
    time.sleep(0.4)  # дати потоку відкрити порт
    if stop_event.is_set():
        sys.stderr.write("Вихід: локальний HTTP-сервер не запущено.\n")
        return 1

    print("Mock SSP: http://127.0.0.1:%d/sdk?…  (JSON adm+position)" % port)
    print("          http://%s:%d/ — слухає на всіх інтерфейсах" % (bind, port))

    tunnel_proc: subprocess.Popen[str] | None = None
    public_host: str | None = None

    if mode != "none":
        if mode in ("auto", "cloudflared"):
            tunnel_proc, public_host = try_cloudflared(port)
            if public_host:
                print("\n--- cloudflared quick tunnel ---")
        if not public_host and mode in ("auto", "ngrok"):
            if tunnel_proc:
                tunnel_proc.terminate()
                tunnel_proc = None
            tunnel_proc, public_host = try_ngrok(port)
            if public_host:
                print("\n--- ngrok tunnel ---")

    if public_host:
        print("\n  Публічний HTTPS хост для Android SDK (лише authority, без шляху):")
        print("\n    %s\n" % public_host)
        print("  gradle.properties (bidscube-testapp-android):")
        print('    bidcube.testSspAuthority=%s\n' % public_host)
        print("  Після зміни — rebuild debug APK. Перевірка: logcat HttpProvider → Base URL https://%s/sdk\n" % public_host)
    elif mode != "none":
        print("\nНе вдалося підняти публічний HTTPS-тунель (або cloudflared/ngrok не встановлені).")
        print_tunnel_install_help(port)

    def shutdown(_sig=None, _frame=None) -> None:
        stop_event.set()
        if tunnel_proc is not None:
            tunnel_proc.terminate()
            try:
                tunnel_proc.wait(timeout=4)
            except subprocess.TimeoutExpired:
                tunnel_proc.kill()
        sys.exit(0)

    signal.signal(signal.SIGINT, shutdown)
    signal.signal(signal.SIGTERM, shutdown)

    try:
        while True:
            time.sleep(1.0)
    except KeyboardInterrupt:
        shutdown()
    return 0


if __name__ == "__main__":
    sys.exit(main())
