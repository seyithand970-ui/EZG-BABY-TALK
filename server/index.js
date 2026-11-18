const http = require("http");
const { WebSocketServer, WebSocket } = require("ws");

const PORT = process.env.PORT || 10000;
const rooms = new Map();
const attempts = new Map();

const server = http.createServer((req, res) => {
  if (req.url === "/health") {
    res.writeHead(200, { "content-type": "application/json" });
    return res.end(JSON.stringify({ ok: true, service: "EZG BABY TALK signaling" }));
  }
  res.writeHead(200, { "content-type": "text/plain" });
  res.end("EZG BABY TALK signaling server");
});

const wss = new WebSocketServer({ noServer: true, maxPayload: 32 * 1024 });
server.on("upgrade", (req, socket, head) => {
  if (req.url !== "/") { socket.destroy(); return; }
  wss.handleUpgrade(req, socket, head, ws => wss.emit("connection", ws, req));
});

function send(ws, payload) {
  if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(payload));
}
function detach(ws) {
  const room = ws.room;
  const role = ws.role;
  if (!room || !role) return;
  const members = rooms.get(room);
  if (!members) return;
  if (members[role] === ws) delete members[role];
  const other = members[role === "baby" ? "parent" : "baby"];
  send(other, { type: "peer-left" });
  if (!members.baby && !members.parent) rooms.delete(room);
  ws.room = null;
  ws.role = null;
}

wss.on("connection", ws => {
  ws.on("message", raw => {
    let msg;
    try { msg = JSON.parse(raw.toString()); } catch { return send(ws, { type: "error", message: "Geçersiz mesaj" }); }

    if (msg.type === "join") {
      const room = String(msg.room || "");
      const role = String(msg.role || "");
      if (!/^\d{6}$/.test(room) || !["baby", "parent"].includes(role)) {
        return send(ws, { type: "error", message: "Oda kodu veya rol geçersiz" });
      }
      const now = Date.now();
      const ip = ws._socket?.remoteAddress || "unknown";
      const recent = (attempts.get(ip) || []).filter(t => now - t < 60_000);
      if (recent.length >= 30) return send(ws, { type: "error", message: "Çok fazla deneme. Bir dakika bekleyin." });
      recent.push(now); attempts.set(ip, recent);

      if (ws.room) detach(ws);
      let members = rooms.get(room);
      if (!members) { members = {}; rooms.set(room, members); }
      if (members[role] && members[role] !== ws) {
        return send(ws, { type: "error", message: "Bu rolde başka bir telefon zaten bağlı" });
      }
      members[role] = ws;
      ws.room = room; ws.role = role;
      send(ws, { type: "joined", room });
      const other = members[role === "baby" ? "parent" : "baby"];
      if (other) {
        send(ws, { type: "peer-joined" });
        send(other, { type: "peer-joined" });
      }
      return;
    }

    if (msg.type === "signal") {
      if (!ws.room || msg.room !== ws.room) return;
      const members = rooms.get(ws.room);
      const other = members && members[ws.role === "baby" ? "parent" : "baby"];
      if (other) send(other, { type: "signal", data: msg.data });
    }
  });
  ws.on("close", () => detach(ws));
  ws.on("error", () => detach(ws));
});

server.listen(PORT, "0.0.0.0", () => console.log("EZG signaling listening on " + PORT));
