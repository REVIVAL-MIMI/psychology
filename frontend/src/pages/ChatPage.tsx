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
  const [screenSharing, setScreenSharing] = useState(false);
  const [ringtoneOn, setRingtoneOn] = useState(false);
  const [ringbackOn, setRingbackOn] = useState(false);
  const [videoEnabled, setVideoEnabled] = useState(true);
  const [audioEnabled, setAudioEnabled] = useState(true);
  const [callExpanded, setCallExpanded] = useState(false);
  const typingTimer = useRef<number | null>(null);
  const lastTypingSent = useRef<number>(0);
  const peerRef = useRef<RTCPeerConnection | null>(null);
  const localVideoRef = useRef<HTMLVideoElement | null>(null);
  const remoteVideoRef = useRef<HTMLVideoElement | null>(null);
  const messagesRef = useRef<HTMLDivElement | null>(null);
  const callPanelRef = useRef<HTMLDivElement | null>(null);
  const ringCtxRef = useRef<AudioContext | null>(null);
  const ringOscRef = useRef<OscillatorNode | null>(null);
  const ringGainRef = useRef<GainNode | null>(null);
  const ringbackTimerRef = useRef<number | null>(null);
  const videoSenderRef = useRef<RTCRtpSender | null>(null);
  const pendingIceRef = useRef<RTCIceCandidateInit[]>([]);

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
    pendingIceRef.current = [];
    localStream?.getTracks().forEach((t) => t.stop());
    setLocalStream(null);
    setRemoteStream(null);
    setIncomingOffer(null);
    setCallState("idle");
    setScreenSharing(false);
    setCallExpanded(false);
    stopRingtone();
    stopRingback();
  };

  const flushPendingIce = async () => {
    const pc = peerRef.current;
    if (!pc || !pc.remoteDescription) return;
    const pending = pendingIceRef.current;
    if (!pending.length) return;
    pendingIceRef.current = [];
    for (const cand of pending) {
      try {
        await pc.addIceCandidate(new RTCIceCandidate(cand));
      } catch {
        // ignore
      }
    }
  };

  const getIceServers = () => {
    const env = (import.meta as any).env ?? {};
    const turnUrl = env.VITE_TURN_URL as string | undefined;
    const turnUser = env.VITE_TURN_USER as string | undefined;
    const turnPass = env.VITE_TURN_PASS as string | undefined;
    const iceServers: RTCIceServer[] = [{ urls: "stun:stun.l.google.com:19302" }];
    if (turnUrl) {
      const urls = turnUrl.split(",").map((u) => u.trim()).filter(Boolean);
      iceServers.push({
        urls: urls.length ? urls : turnUrl,
        username: turnUser,
        credential: turnPass
      });
    }
    return iceServers;
  };

  const initPeer = async (receiverId: number, videoOn: boolean) => {
    const pc = new RTCPeerConnection({
      iceServers: getIceServers()
    });
    peerRef.current = pc;

    const videoTransceiver = pc.addTransceiver("video", { direction: "sendrecv" });
    videoSenderRef.current = videoTransceiver.sender;

    const stream = new MediaStream();
    const audioStream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false });
    const audioTrack = audioStream.getAudioTracks()[0];
    if (audioTrack) {
      audioTrack.enabled = audioEnabled;
      stream.addTrack(audioTrack);
      pc.addTrack(audioTrack, stream);
    }

    if (videoOn) {
      const videoStream = await navigator.mediaDevices.getUserMedia({ audio: false, video: true });
      const videoTrack = videoStream.getVideoTracks()[0];
      if (videoTrack) {
        stream.addTrack(videoTrack);
        await videoTransceiver.sender.replaceTrack(videoTrack);
      }
    } else {
      await videoTransceiver.sender.replaceTrack(null);
    }

    setLocalStream(stream);

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

  const startCall = async (withVideo: boolean) => {
    if (!activeId || callState !== "idle") return;
    try {
      setCallState("calling");
      startRingback();
      setVideoEnabled(withVideo);
      const pc = await initPeer(activeId, withVideo);
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
      stopRingtone();
      const senderId = incomingOffer.senderId;
      const pc = await initPeer(senderId, videoEnabled);
      await pc.setRemoteDescription(new RTCSessionDescription({ type: "offer", sdp: incomingOffer.sdp }));
      await flushPendingIce();
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

  const ensureAudio = () => {
    if (!ringCtxRef.current) {
      ringCtxRef.current = new AudioContext();
    }
  };

  const playTone = (freq: number, gainValue: number) => {
    ensureAudio();
    const ctx = ringCtxRef.current!;
    if (ctx.state === "suspended") {
      ctx.resume().catch(() => null);
    }
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = "sine";
    osc.frequency.value = freq;
    gain.gain.value = gainValue;
    osc.connect(gain).connect(ctx.destination);
    osc.start();
    ringOscRef.current = osc;
    ringGainRef.current = gain;
  };

  const stopTone = () => {
    ringOscRef.current?.stop();
    ringOscRef.current?.disconnect();
    ringGainRef.current?.disconnect();
    ringOscRef.current = null;
    ringGainRef.current = null;
  };

  const startRingtone = () => {
    if (ringtoneOn) return;
    setRingtoneOn(true);
    playTone(680, 0.04);
  };

  const stopRingtone = () => {
    setRingtoneOn(false);
    stopTone();
  };

  const startRingback = () => {
    if (ringbackOn) return;
    setRingbackOn(true);
    const pattern = () => {
      playTone(440, 0.04);
      ringbackTimerRef.current = window.setTimeout(() => {
        stopTone();
        ringbackTimerRef.current = window.setTimeout(pattern, 900);
      }, 900);
    };
    pattern();
  };

  const stopRingback = () => {
    setRingbackOn(false);
    if (ringbackTimerRef.current) {
      window.clearTimeout(ringbackTimerRef.current);
      ringbackTimerRef.current = null;
    }
    stopTone();
  };

  const startScreenShare = async () => {
    if (screenSharing || !peerRef.current) return;
    try {
      const display = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
      const screenTrack = display.getVideoTracks()[0];
      const sender = peerRef.current
        .getSenders()
        .find((s) => s.track && s.track.kind === "video");
      if (sender && screenTrack) {
        await sender.replaceTrack(screenTrack);
        setScreenSharing(true);
        screenTrack.onended = () => {
          stopScreenShare();
        };
      }
    } catch {
      // ignore
    }
  };

  const stopScreenShare = async () => {
    if (!screenSharing || !peerRef.current || !localStream) {
      setScreenSharing(false);
      return;
    }
    const videoTrack = localStream.getVideoTracks()[0];
    const sender = peerRef.current
      .getSenders()
      .find((s) => s.track && s.track.kind === "video");
    if (sender && videoTrack) {
      await sender.replaceTrack(videoTrack);
    }
    setScreenSharing(false);
  };

  const toggleAudio = () => {
    if (!localStream) {
      setAudioEnabled((prev) => !prev);
      return;
    }
    const next = !audioEnabled;
    localStream.getAudioTracks().forEach((track) => {
      track.enabled = next;
    });
    setAudioEnabled(next);
  };

  const enableVideo = async () => {
    if (!peerRef.current) return;
    try {
      const cam = await navigator.mediaDevices.getUserMedia({ video: true, audio: false });
      const camTrack = cam.getVideoTracks()[0];
      const sender = videoSenderRef.current
        ?? peerRef.current.getSenders().find((s) => s.track && s.track.kind === "video")
        ?? null;
      if (sender && camTrack) {
        await sender.replaceTrack(camTrack);
      } else if (camTrack) {
        peerRef.current.addTrack(camTrack, cam);
      }
      setLocalStream((prev) => {
        const next = prev ? new MediaStream(prev.getTracks()) : new MediaStream();
        if (camTrack) next.addTrack(camTrack);
        return next;
      });
      setVideoEnabled(true);
    } catch {
      // ignore
    }
  };

  const disableVideo = async () => {
    if (!localStream) {
      setVideoEnabled(false);
      return;
    }
    localStream.getVideoTracks().forEach((track) => {
      track.stop();
    });
    const sender = videoSenderRef.current
      ?? peerRef.current?.getSenders().find((s) => s.track && s.track.kind === "video")
      ?? null;
    if (sender) {
      await sender.replaceTrack(null);
    }
    setLocalStream((prev) => {
      if (!prev) return prev;
      const next = new MediaStream(prev.getTracks().filter((t) => t.kind !== "video"));
      return next;
    });
    setVideoEnabled(false);
  };

  const toggleVideo = () => {
    if (videoEnabled) {
      disableVideo();
    } else {
      enableVideo();
    }
  };

  const toggleFullscreen = () => {
    const el = callPanelRef.current;
    if (!el) return;
    if (document.fullscreenElement) {
      document.exitFullscreen?.().catch(() => null);
      return;
    }
    el.requestFullscreen?.().catch(() => null);
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
        startRingtone();
        setIncomingOffer(payload);
        setCallState("incoming");
      }
      if (payload.type === "answer" && peerRef.current) {
        await peerRef.current.setRemoteDescription(new RTCSessionDescription({ type: "answer", sdp: payload.sdp }));
        await flushPendingIce();
        setCallState("in-call");
        stopRingback();
      }
      if (payload.type === "ice" && peerRef.current && payload.candidate) {
        const ice: RTCIceCandidateInit = {
          candidate: payload.candidate,
          sdpMid: payload.sdpMid,
          sdpMLineIndex: payload.sdpMLineIndex
        };
        if (!peerRef.current.remoteDescription) {
          pendingIceRef.current.push(ice);
        } else {
          await peerRef.current.addIceCandidate(new RTCIceCandidate(ice));
        }
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
    if (!messagesRef.current) return;
    const el = messagesRef.current;
    window.requestAnimationFrame(() => {
      el.scrollTop = el.scrollHeight;
    });
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
                  <button className="icon-button" onClick={() => startCall(false)} disabled={callState !== "idle"} aria-label="Звонок">
                    <span className="icon-phone" aria-hidden="true" />
                  </button>
                  <button className="icon-button" onClick={() => startCall(true)} disabled={callState !== "idle"} aria-label="Видеозвонок">
                    <span className="icon-video" aria-hidden="true" />
                  </button>
                  {callState !== "idle" && (
                    <button className="button ghost" onClick={endCall}>Завершить</button>
                  )}
                </div>
              </div>

              {(callState === "calling" || callState === "in-call") && (
                <div ref={callPanelRef} className={`call-panel ${callExpanded ? "expanded" : ""}`}>
                  <video ref={remoteVideoRef} className="video-remote" autoPlay playsInline />
                  <video ref={localVideoRef} className="video-local" autoPlay playsInline muted />
                  <div className="call-controls">
                    <button className="icon-button" onClick={toggleAudio} aria-label="Микрофон">
                      <span className={audioEnabled ? "icon-mic" : "icon-mic-off"} aria-hidden="true" />
                    </button>
                    <button className="icon-button" onClick={toggleVideo} aria-label="Камера">
                      <span className={videoEnabled ? "icon-video" : "icon-video-off"} aria-hidden="true" />
                    </button>
                    <button className="icon-button" onClick={screenSharing ? stopScreenShare : startScreenShare} aria-label="Экран">
                      <span className="icon-screen" aria-hidden="true" />
                    </button>
                    <button className="icon-button" onClick={() => setCallExpanded((prev) => !prev)} aria-label="Увеличить">
                      <span className="icon-expand" aria-hidden="true" />
                    </button>
                    <button className="icon-button" onClick={toggleFullscreen} aria-label="На весь экран">
                      <span className="icon-fullscreen" aria-hidden="true" />
                    </button>
                  </div>
                </div>
              )}

              <div className="chat-messages" ref={messagesRef}>
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
