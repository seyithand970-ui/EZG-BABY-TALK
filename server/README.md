# EZG BABY TALK signaling server

Deploy this directory as the Render web service described in the repository root `render.yaml`.

- WebSocket endpoint: `wss://ezg-baby-talk-signal.onrender.com`
- Health check: `/health`
- Room codes are six digits; one baby device and one parent device per room.
- This server forwards WebRTC offer/answer/ICE signaling only. It does not relay audio.

Important: this initial version uses public STUN only. Some mobile networks and NATs require a TURN relay for reliable connections. Add a private TURN service and authentication before relying on this for unattended monitoring. Free Render instances may sleep when idle.
