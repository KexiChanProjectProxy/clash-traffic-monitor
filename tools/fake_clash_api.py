#!/usr/bin/env python3
"""Minimal fake Clash external controller for testing the app without a real core.

Serves GET /version, GET /connections, and WebSocket /traffic and /connections,
pushing synthetic data once per second. Standard library only.

    python3 tools/fake_clash_api.py --port 9090 [--secret abc]
"""
import argparse, asyncio, base64, hashlib, json, math, random, struct, time, uuid

GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"


class World:
    def __init__(self):
        self.conns = {}
        self.up_total = 0
        self.down_total = 0
        self.t0 = time.time()

    def tick(self):
        t = time.time() - self.t0
        # Close some, open some, with a slow wave in the open rate.
        for cid in [c for c in self.conns if random.random() < 0.12]:
            del self.conns[cid]
        for _ in range(int(max(0, 6 + 5 * math.sin(t / 15) + random.gauss(0, 2)))):
            self.conns[str(uuid.uuid4())] = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
        up = int(abs(200_000 + 150_000 * math.sin(t / 7) + random.gauss(0, 40_000)))
        down = int(abs(3_000_000 + 2_500_000 * math.sin(t / 11) + random.gauss(0, 500_000)))
        self.up_total += up
        self.down_total += down
        return up, down

    def connections_json(self):
        return json.dumps({
            "downloadTotal": self.down_total,
            "uploadTotal": self.up_total,
            "connections": [{"id": cid, "start": st, "upload": 0, "download": 0,
                             "metadata": {"host": "example.com"}, "chains": ["DIRECT"], "rule": "Match"}
                            for cid, st in self.conns.items()],
        })


WORLD = World()
LAST = {"up": 0, "down": 0}


async def ticker():
    while True:
        LAST["up"], LAST["down"] = WORLD.tick()
        await asyncio.sleep(1)


def ws_frame(text: str) -> bytes:
    data = text.encode()
    n = len(data)
    if n < 126:
        hdr = struct.pack("!BB", 0x81, n)
    elif n < 65536:
        hdr = struct.pack("!BBH", 0x81, 126, n)
    else:
        hdr = struct.pack("!BBQ", 0x81, 127, n)
    return hdr + data


async def handle(reader, writer, secret):
    try:
        head = await reader.readuntil(b"\r\n\r\n")
    except Exception:
        writer.close(); return
    lines = head.decode(errors="replace").split("\r\n")
    method, target, _ = lines[0].split(" ", 2)
    headers = {k.strip().lower(): v.strip() for k, v in (l.split(":", 1) for l in lines[1:] if ":" in l)}
    path = target.split("?")[0]
    peer = writer.get_extra_info("peername")
    print(f"{peer[0]} {method} {path} {'(ws)' if headers.get('upgrade','').lower()=='websocket' else ''}")

    def respond(code, body, ctype="application/json"):
        reason = {200: "OK", 401: "Unauthorized", 404: "Not Found"}[code]
        b = body.encode()
        writer.write(f"HTTP/1.1 {code} {reason}\r\nContent-Type: {ctype}\r\nContent-Length: {len(b)}\r\n"
                     f"Connection: close\r\n\r\n".encode() + b)

    token = ""
    if "?" in target:
        for kv in target.split("?", 1)[1].split("&"):
            if kv.startswith("token="):
                token = kv[6:]
    auth = headers.get("authorization", "")
    if secret and auth != f"Bearer {secret}" and token != secret:
        respond(401, '{"message":"Unauthorized"}')
        await writer.drain(); writer.close(); return

    if headers.get("upgrade", "").lower() == "websocket" and path in ("/traffic", "/connections"):
        accept = base64.b64encode(hashlib.sha1((headers["sec-websocket-key"] + GUID).encode()).digest()).decode()
        writer.write(("HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n"
                      f"Sec-WebSocket-Accept: {accept}\r\n\r\n").encode())
        try:
            while True:
                msg = json.dumps(LAST) if path == "/traffic" else WORLD.connections_json()
                writer.write(ws_frame(msg))
                await writer.drain()
                await asyncio.sleep(1)
        except (ConnectionError, asyncio.CancelledError):
            pass
        writer.close(); return

    if path == "/version":
        respond(200, '{"meta":true,"version":"fake-1.0"}')
    elif path == "/connections":
        respond(200, WORLD.connections_json())
    else:
        respond(404, '{"message":"Not Found"}')
    await writer.drain()
    writer.close()


async def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="0.0.0.0")
    ap.add_argument("--port", type=int, default=9090)
    ap.add_argument("--secret", default="")
    a = ap.parse_args()
    asyncio.create_task(ticker())
    srv = await asyncio.start_server(lambda r, w: handle(r, w, a.secret), a.host, a.port)
    print(f"Fake Clash API on http://{a.host}:{a.port}" + (f" (secret '{a.secret}')" if a.secret else ""))
    async with srv:
        await srv.serve_forever()


if __name__ == "__main__":
    asyncio.run(main())
