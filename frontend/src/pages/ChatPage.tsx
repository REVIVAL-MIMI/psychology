import { useEffect, useMemo, useRef, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { connectWs, publishWs, subscribeWs } from "../lib/ws";

export default function ChatPage() {
  const { auth } = useAuth();
  const [contacts, setContacts] = useState<any[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [messages, setMessages] = useState<any[]>([]);
  const [text, setText] = useState("");
  const [typing, setTyping] = useState(false);
  const [unreadBySender, setUnreadBySender] = useState<Record<number, number>>({});
  const [callState, setCallState] = useState<"idle" | "calling" | "incoming" | "in-call">("idle");
  const [incomingOffer, setIncomingOffer] = useState<any | null>(null);
  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null);
  const typingTimer = useRef<number | null>(null);
  const lastTypingSent = useRef<number>(0);
  const peerRef = useRef<RTCPeerConnection | null>(null);
  const localVideoRef = useRef<HTMLVideoElement | null>(null);
  const remoteVideoRef = useRef<HTMLVideoElement | null>(null);
  const endRef = useRef<HTMLDivElement | null>(null);

  const userId = useMemo(() => auth?.userId ?? null, [auth]);
  const activeContact = useMemo(
    () => contacts.find((c) => c.id === activeId),
    [contacts, activeId]
  );

  const markMessagesRead = async (items: any[]) => {
    if (!auth?.userId) return;
    const unread = items.filter((m) =>
      typeof m.id === "number" && m.receiverId === auth.userId && !m.read
    );
    if (!unread.length) return;

    for (const msg of unread) {
      const sent = publishWs("/app/chat.read", { messageId: msg.id });
      if (!sent) {
        try {
          await api.post(`/chat/read/${msg.id}`);
        } catch {
          // ignore
        }
      }
    }
    if (activeId) {
      setUnreadBySender((prev) => ({ ...prev, [activeId]: 0 }));
    }
  };

  const attachStreams = () => {
    if (localVideoRef.current && localStream) {
      localVideoRef.current.srcObject = localStream;
    }
    if (remoteVideoRef.current && remoteStream) {
      remoteVideoRef.current.srcObject = remoteStream;
    }
  };

  useEffect(() => {
    attachStreams();
  }, [localStream, remoteStream]);

  const cleanupCall = () => {
    peerRef.current?.close();
    peerRef.current = null;
    localStream?.getTracks().forEach((t) => t.stop());
    setLocalStream(null);
    setRemoteStream(null);
    setIncomingOffer(null);
    setCallState("idle");
  };

  const initPeer = async (receiverId: number) => {
    const pc = new RTCPeerConnection({
      iceServers: [{ urls: "stun:stun.l.google.com:19302" }]
    });
    peerRef.current = pc;

    const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
    setLocalStream(stream);
    stream.getTracks().forEach((track) => pc.addTrack(track, stream));

    pc.onicecandidate = (event) => {
      if (!event.candidate) return;
      publishWs("/app/call.ice", {
        receiverId,
        candidate: event.candidate.candidate,
        sdpMid: event.candidate.sdpMid,
        sdpMLineIndex: event.candidate.sdpMLineIndex
      });
    };

    pc.ontrack = (event) => {
      const [remote] = event.streams;
      if (remote) {
        setRemoteStream(remote);
      } else {
        setRemoteStream((prev) => {
          const stream = prev ?? new MediaStream();
          stream.addTrack(event.track);
          return stream;
        });
      }
    };

    pc.onconnectionstatechange = () => {
      if (pc.connectionState === "failed" || pc.connectionState === "disconnected" || pc.connectionState === "closed") {
        cleanupCall();
      }
    };

    return pc;
  };

  const startCall = async () => {
    if (!activeId || callState !== "idle") return;
    try {
      setCallState("calling");
      const pc = await initPeer(activeId);
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      publishWs("/app/call.offer", { receiverId: activeId, sdp: offer.sdp, type: offer.type });
    } catch {
      cleanupCall();
    }
  };

  const acceptCall = async () => {
    if (!incomingOffer) return;
    try {
      setCallState("in-call");
      const senderId = incomingOffer.senderId;
      const pc = await initPeer(senderId);
      await pc.setRemoteDescription(new RTCSessionDescription({ type: "offer", sdp: incomingOffer.sdp }));
      const answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);
      publishWs("/app/call.answer", { receiverId: senderId, sdp: answer.sdp, type: answer.type });
      setIncomingOffer(null);
    } catch {
      cleanupCall();
    }
  };

  const declineCall = () => {
    if (!incomingOffer) return;
    publishWs("/app/call.hangup", { receiverId: incomingOffer.senderId, reason: "declined" });
    cleanupCall();
  };

  const endCall = () => {
    const targetId = incomingOffer?.senderId ?? activeId;
    if (targetId) {
      publishWs("/app/call.hangup", { receiverId: targetId, reason: "hangup" });
    }
    cleanupCall();
  };

  useEffect(() => {
    if (!auth) return;
    connectWs(auth.accessToken);
    if (auth.userRole === "ROLE_PSYCHOLOGIST") {
      api.get<any[]>("/clients").then(setContacts);
    } else if (auth.userRole === "ROLE_CLIENT") {
      api.get<any>("/dashboard/client").then((data) => {
        if (data?.psychologist) {
          setContacts([data.psychologist]);
          setActiveId(data.psychologist.id);
        }
      });
    }
  }, [auth]);

  useEffect(() => {
    if (auth?.userRole === "ROLE_CLIENT" && contacts.length && !activeId) {
      setActiveId(contacts[0].id);
    }
  }, [auth?.userRole, contacts, activeId]);

  useEffect(() => {
    if (!activeId) return;
    api.get<any[]>(`/chat/conversation/${activeId}`).then((data) => {
      const updated = data.map((m) =>
        m.receiverId === auth?.userId && !m.read ? { ...m, read: true } : m
      );
      setMessages(updated);
      markMessagesRead(updated);
      setUnreadBySender((prev) => ({ ...prev, [activeId]: 0 }));
    });
  }, [activeId]);

  useEffect(() => {
    if (!userId) return;
    api.get<any[]>("/chat/unread").then((items) => {
      const next: Record<number, number> = {};
      items.forEach((m) => {
        if (typeof m.senderId === "number") {
          next[m.senderId] = (next[m.senderId] ?? 0) + 1;
        }
      });
      setUnreadBySender(next);
    }).catch(() => {
      setUnreadBySender({});
    });

    let unsubscribeMessages: (() => void) | null = null;
    let unsubscribeTyping: (() => void) | null = null;
    let unsubscribeCall: (() => void) | null = null;

    unsubscribeMessages = subscribeWs(`/user/${userId}/queue/messages`, (payload) => {
      const shouldRead =
        payload?.senderId === activeId &&
        payload?.receiverId === auth?.userId &&
        !payload?.read;
      const nextPayload = shouldRead ? { ...payload, read: true } : payload;

      setMessages((prev) => {
        const exists = prev.some((m) => m.id === payload.id);
        if (exists) return prev;

        // Если это наш же месседж, заменяем оптимистичный tmp
        if (payload?.senderId === auth?.userId) {
          const idx = prev.findIndex(
            (m) =>
              typeof m.id === "string" &&
              m.id.startsWith("tmp-") &&
              m.receiverId === payload.receiverId &&
              m.content === payload.content
          );
          if (idx !== -1) {
            const next = [...prev];
            next[idx] = nextPayload;
            return next;
          }
        }
        return [...prev, nextPayload];
      });

      if (!shouldRead && payload?.receiverId === auth?.userId && payload?.senderId) {
        setUnreadBySender((prev) => ({
          ...prev,
          [payload.senderId]: (prev[payload.senderId] ?? 0) + 1
        }));
      }

      if (shouldRead) {
        markMessagesRead([payload]);
      }
    });

    unsubscribeTyping = subscribeWs(`/user/${userId}/queue/typing`, (payload: { senderId: number; typing: boolean }) => {
      if (payload.senderId === activeId) {
        setTyping(payload.typing);
      }
    });

    unsubscribeCall = subscribeWs(`/user/${userId}/queue/call`, async (payload) => {
      if (!payload?.type) return;
      if (payload.type === "offer") {
        if (callState !== "idle") {
          publishWs("/app/call.hangup", { receiverId: payload.senderId, reason: "busy" });
          return;
        }
        setIncomingOffer(payload);
        setCallState("incoming");
      }
      if (payload.type === "answer" && peerRef.current) {
        await peerRef.current.setRemoteDescription(new RTCSessionDescription({ type: "answer", sdp: payload.sdp }));
        setCallState("in-call");
      }
      if (payload.type === "ice" && peerRef.current && payload.candidate) {
        await peerRef.current.addIceCandidate(new RTCIceCandidate({
          candidate: payload.candidate,
          sdpMid: payload.sdpMid,
          sdpMLineIndex: payload.sdpMLineIndex
        }));
      }
      if (payload.type === "hangup") {
        cleanupCall();
      }
    });

    return () => {
      unsubscribeMessages?.();
      unsubscribeTyping?.();
      unsubscribeCall?.();
    };
  }, [userId, activeId, callState]);

  useEffect(() => {
    if (!activeId) return;
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, activeId]);

  const sendTyping = (isTyping: boolean) => {
    if (!activeId) return;
    publishWs("/app/chat.typing", { receiverId: activeId, typing: isTyping });
  };

  const handleTyping = (value: string) => {
    setText(value);
    const now = Date.now();
    if (now - lastTypingSent.current > 800) {
      sendTyping(true);
      lastTypingSent.current = now;
    }
    if (typingTimer.current) {
      window.clearTimeout(typingTimer.current);
    }
    typingTimer.current = window.setTimeout(() => {
      sendTyping(false);
    }, 1200);
  };

  const sendMessage = async () => {
    if (!activeId || !text) return;

    const payload = {
      receiverId: activeId,
      content: text
    };

    const optimistic = {
      id: `tmp-${Date.now()}`,
      senderId: userId,
      receiverId: activeId,
      content: text,
      sentAt: new Date().toISOString()
    };
    setMessages((prev) => [...prev, optimistic]);

    const sent = await publishWs("/app/chat.send", payload);
    if (!sent) {
      await api.post("/chat/send", payload);
    }

    setText("");
    sendTyping(false);
  };

  return (
    <div className="page chat-page">
      <div className="page-header">
        <h1>Чат</h1>
        <p className="muted">Безопасное общение с вашими клиентами/психологом.</p>
      </div>

      <div className="chat-layout">
        <aside className="chat-contacts">
          {contacts.map((c) => (
            <button
              key={c.id}
              className={
                activeId === c.id
                  ? "contact active"
                  : unreadBySender[c.id]
                    ? "contact unread"
                    : "contact"
              }
              onClick={() => setActiveId(c.id)}
            >
              <div className="contact-inner">
                <div className="contact-name">{c.fullName}</div>
                <div className="muted">{c.specialization ?? c.phone}</div>
              </div>
            </button>
          ))}
        </aside>
        <section className="chat-window">
          {!activeId && <div className="muted">Выберите диалог слева</div>}
          {activeId && (
            <>
              <div className="chat-header">
                <div>
                  <div className="chat-title">{activeContact?.fullName ?? "Диалог"}</div>
                  <div className="muted">
                    {callState === "in-call" ? "Звонок активен" : callState === "calling" ? "Идёт вызов…" : "Онлайн чат"}
                  </div>
                </div>
                <div className="chat-actions">
                  <button className="button ghost" onClick={startCall} disabled={callState !== "idle"}>Позвонить</button>
                  {callState !== "idle" && (
                    <button className="button ghost" onClick={endCall}>Завершить</button>
                  )}
                </div>
              </div>

              {(callState === "calling" || callState === "in-call") && (
                <div className="call-panel">
                  <video ref={remoteVideoRef} className="video-remote" autoPlay playsInline />
                  <video ref={localVideoRef} className="video-local" autoPlay playsInline muted />
                </div>
              )}

              <div className="chat-messages">
                {messages.map((m) => (
                  <div
                    key={m.id}
                    className={
                      m.senderId === auth?.userId
                        ? "message outgoing"
                        : m.receiverId === auth?.userId && !m.read
                          ? "message incoming unread"
                          : "message incoming"
                    }
                  >
                    <div className="message-body">
                      <div className="message-text">{m.content}</div>
                    </div>
                    <div className="message-time">{new Date(m.sentAt).toLocaleTimeString()}</div>
                  </div>
                ))}
                <div ref={endRef} />
                {typing && <div className="typing-indicator">… собеседник печатает</div>}
              </div>
              <div className="chat-input">
                <input value={text} onChange={(e) => handleTyping(e.target.value)} placeholder="Сообщение…" />
                <button className="button" onClick={sendMessage}>Отправить</button>
              </div>
            </>
          )}
        </section>
      </div>

      {callState === "incoming" && incomingOffer && (
        <div className="call-modal">
          <div className="call-card">
            <div className="call-title">Входящий звонок</div>
            <div className="muted">{contacts.find((c) => c.id === incomingOffer.senderId)?.fullName ?? "Контакт"}</div>
            <div className="row">
              <button className="button" onClick={acceptCall}>Принять</button>
              <button className="button ghost" onClick={declineCall}>Отклонить</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
