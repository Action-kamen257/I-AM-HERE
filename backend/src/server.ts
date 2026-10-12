import http from 'http';
import { WebSocketServer, WebSocket } from 'ws';
import { app } from './app';
import { emergencyStore } from './store';

const PORT = process.env.PORT || 4000;

const server = http.createServer(app);
const wss = new WebSocketServer({ server });

// Map token to connected WebSocket clients (mobile app & viewers)
const sessionClients = new Map<string, Set<WebSocket>>();

wss.on('connection', (ws: WebSocket, req) => {
  const url = new URL(req.url || '', `http://${req.headers.host}`);
  const token = url.searchParams.get('token');

  if (!token) {
    ws.close(1008, 'Token required');
    return;
  }

  if (!sessionClients.has(token)) {
    sessionClients.set(token, new Set());
  }
  sessionClients.get(token)!.add(ws);

  // Send initial state
  const session = emergencyStore.getSessionByToken(token);
  if (session) {
    ws.send(JSON.stringify({ type: 'INIT', session }));
  }

  ws.on('close', () => {
    const clients = sessionClients.get(token);
    if (clients) {
      clients.delete(ws);
      if (clients.size === 0) {
        sessionClients.delete(token);
      }
    }
  });

  ws.on('message', (message: string) => {
    try {
      const data = JSON.parse(message.toString());
      // Broadcast to all peers watching this session
      const clients = sessionClients.get(token);
      if (clients) {
        clients.forEach((client) => {
          if (client !== ws && client.readyState === WebSocket.OPEN) {
            client.send(JSON.stringify(data));
          }
        });
      }
    } catch (e) {
      console.error('Error handling WS message:', e);
    }
  });
});

export function broadcastSessionUpdate(token: string, payload: any) {
  const clients = sessionClients.get(token);
  if (clients) {
    const msg = JSON.stringify(payload);
    clients.forEach((client) => {
      if (client.readyState === WebSocket.OPEN) {
        client.send(msg);
      }
    });
  }
}

if (process.env.NODE_ENV !== 'test') {
  server.listen(PORT, () => {
    console.log(`[I AM HERE] Emergency Backend Server running on port ${PORT}`);
    console.log(`[I AM HERE] Live tracking portal: http://localhost:${PORT}/live/:token`);
  });
}

export { server };
