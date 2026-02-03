import { getStoredAuth } from "./storage";

type MessageHandler = (body: any) => void;

type Subscription = {
  id: string;
  destination: string;
  handler: MessageHandler;
};

let socket: WebSocket | null = null;
let connected = false;
let connecting = false;
let reconnectTimer: number | null = null;
let subscriptions: Subscription[] = [];
let subCounter = 0;
let activeToken: string | null = null;

const heartbeatIntervalMs = 15000;
let heartbeatTimer: number | null = null;

function getWsUrl() {
  // Prefer explicit override in env, fallback to backend on 8080
  const env = (import.meta as any).env ?? {};
  const envUrl = env.VITE_WS_URL as string | undefined;
  if (envUrl) return envUrl;
  const apiBase = env.VITE_API_BASE_URL as string | undefined;
  if (apiBase) {
    const url = new URL(apiBase);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    url.pathname = "/ws-chat";
    url.search = "";
    url.hash = "";
    return url.toString();
  }
  const protocol = window.location.protocol === "https:" ? "wss" : "ws";
  const host = window.location.port === "5173" ? "localhost:8080" : window.location.host;
  return `${protocol}://${host}/ws-chat`;
}

function frame(command: string, headers: Record<string, string>, body = "") {
  const headerLines = Object.entries(headers)
    .map(([k, v]) => `${k}:${v}`)
    .join("\n");
  return `${command}\n${headerLines}\n\n${body}\0`;
}

function parseFrames(data: string) {
  return data.split("\0").filter(Boolean);
}

function parseMessageFrame(raw: string) {
  const [head, ...bodyParts] = raw.split("\n\n");
  const lines = head.split("\n");
  const command = lines[0];
  const headers: Record<string, string> = {};
  for (let i = 1; i < lines.length; i += 1) {
    const [key, ...rest] = lines[i].split(":");
    headers[key] = rest.join(":");
  }
  const body = bodyParts.join("\n\n");
  return { command, headers, body };
}

function startHeartbeat() {
  if (heartbeatTimer) window.clearInterval(heartbeatTimer);
  heartbeatTimer = window.setInterval(() => {
    if (socket && connected) {
      socket.send("\n");
    }
  }, heartbeatIntervalMs);
}

function stopHeartbeat() {
  if (heartbeatTimer) window.clearInterval(heartbeatTimer);
  heartbeatTimer = null;
}

function scheduleReconnect() {
  if (reconnectTimer) return;
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null;
    connectWs();
  }, 2000);
}

export function connectWs(token?: string) {
  const authToken = token ?? getStoredAuth()?.accessToken ?? null;
  if (!authToken) return;

  if (connected && socket && activeToken === authToken) return;
  if (connecting) return;

  activeToken = authToken;
  connecting = true;

  if (socket) {
    socket.close();
    socket = null;
  }

  socket = new WebSocket(getWsUrl());

  socket.onopen = () => {
    if (!socket) return;
    const headers = {
      "accept-version": "1.2",
      Authorization: `Bearer ${authToken}`,
      host: "localhost"
    };
    socket.send(frame("CONNECT", headers));
  };

  socket.onmessage = (event) => {
    const frames = parseFrames(event.data);
    frames.forEach((raw) => {
      const parsed = parseMessageFrame(raw);
      if (parsed.command === "CONNECTED") {
        connected = true;
        connecting = false;
        startHeartbeat();
        subscriptions.forEach((sub) => {
          socket?.send(
            frame("SUBSCRIBE", { id: sub.id, destination: sub.destination })
          );
        });
      } else if (parsed.command === "MESSAGE") {
        const destination = parsed.headers.destination;
        const payload = parsed.body ? JSON.parse(parsed.body) : null;
        subscriptions.forEach((sub) => {
          if (sub.destination === destination) {
            sub.handler(payload);
          }
        });
      } else if (parsed.command === "ERROR") {
        console.warn("WS error frame", parsed.body);
      }
    });
  };

  socket.onclose = () => {
    connected = false;
    connecting = false;
    stopHeartbeat();
    scheduleReconnect();
  };

  socket.onerror = () => {
    connected = false;
    connecting = false;
    stopHeartbeat();
    scheduleReconnect();
  };
}

export function subscribeWs(destination: string, handler: MessageHandler) {
  connectWs();
  const id = `sub-${subCounter++}`;
  const subscription: Subscription = { id, destination, handler };
  subscriptions.push(subscription);

  if (socket && connected) {
    socket.send(frame("SUBSCRIBE", { id, destination }));
  }

  return () => {
    subscriptions = subscriptions.filter((s) => s.id !== id);
    if (socket && connected) {
      socket.send(frame("UNSUBSCRIBE", { id }));
    }
  };
}

export function publishWs(destination: string, body: unknown) {
  connectWs();
  if (!socket || !connected) return false;
  const json = JSON.stringify(body ?? {});
  socket.send(
    frame("SEND", {
      destination,
      "content-type": "application/json",
      "content-length": String(new TextEncoder().encode(json).length)
    }, json)
  );
  return true;
}

export function disconnectWs() {
  subscriptions = [];
  if (socket && connected) {
    socket.send(frame("DISCONNECT", {}, ""));
  }
  socket?.close();
  socket = null;
  connected = false;
  connecting = false;
  stopHeartbeat();
}
