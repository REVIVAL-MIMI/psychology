import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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
  const [loadingContacts, setLoadingContacts] = useState(false);
  const [chatError, setChatError] = useState<string | null>(null);
  const [unreadBySender, setUnreadBySender] = useState<Record<number, number>>({});
  const [callState, setCallState] = useState<"idle" | "calling" | "incoming" | "in-call">("idle");
  const [incomingOffer, setIncomingOffer] = useState<any | null>(null);
  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null);
  const [screenSharing, setScreenSharing] = useState(false);
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
  const ringNodesRef = useRef<Array<{ osc: OscillatorNode; gain: GainNode }>>([]);
  const noteTimersRef = useRef<number[]>([]);
  const ringtoneLoopTimerRef = useRef<number | null>(null);
  const ringbackLoopTimerRef = useRef<number | null>(null);
  const ringtoneActiveRef = useRef(false);
  const ringbackActiveRef = useRef(false);
  const videoSenderRef = useRef<RTCRtpSender | null>(null);
  const pendingIceRef = useRef<RTCIceCandidateInit[]>([]);
  const callStateRef = useRef(callState);

  const userId = useMemo(() => auth?.userId ?? null, [auth]);
  const activeContact = useMemo(
    () => contacts.find((c) => activeId !== null && String(c.id) === String(activeId)),
    [contacts, activeId]
  );
  const hasLocalVideo = useMemo(
    () => Boolean(localStream && localStream.getVideoTracks().length > 0),
    [localStream]
  );
  const counterpartLabel = auth?.userRole === "ROLE_PSYCHOLOGIST" ? "сотрудника" : "психолога";

  const applyContacts = useCallback((nextContacts: any[]) => {
    setContacts(nextContacts);
    setActiveId((prev) => {
      if (!nextContacts.length) return null;
      if (prev !== null && nextContacts.some((c) => String(c.id) === String(prev))) {
        return prev;
      }
      return nextContacts[0].id;
    });
  }, []);

  const loadContacts = useCallback(async () => {
    if (!auth) return;
    setLoadingContacts(true);
    setChatError(null);

    const fetchContacts = async () => {
      if (auth.userRole === "ROLE_PSYCHOLOGIST") {
        return (await api.get<any[]>("/clients")) ?? [];
      }
      if (auth.userRole === "ROLE_CLIENT") {
        const data = await api.get<any>("/dashboard/client");
        return data?.psychologist ? [data.psychologist] : [];
      }
      return [];
    };

    try {
      const data = await fetchContacts();
      applyContacts(data);
    } catch {
      try {
        await new Promise((resolve) => window.setTimeout(resolve, 700));
        const retryData = await fetchContacts();
        applyContacts(retryData);
      } catch {
        setChatError("Не удалось загрузить диалоги.");
      }
    } finally {
      setLoadingContacts(false);
    }
  }, [auth, applyContacts]);

  const publishSignalWithRetry = useCallback(
    async (destination: string, payload: unknown, attempts = 6, delayMs = 180) => {
      for (let attempt = 0; attempt < attempts; attempt += 1) {
        if (publishWs(destination, payload)) {
          return true;
        }
        await new Promise((resolve) => window.setTimeout(resolve, delayMs));
      }
      return false;
    },
    []
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
      localVideoRef.current.play().catch(() => null);
    } else if (localVideoRef.current) {
      localVideoRef.current.srcObject = null;
    }
    if (remoteVideoRef.current && remoteStream) {
      remoteVideoRef.current.srcObject = remoteStream;
      remoteVideoRef.current.play().catch(() => null);
    } else if (remoteVideoRef.current) {
      remoteVideoRef.current.srcObject = null;
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
    videoSenderRef.current = null;

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
        videoSenderRef.current = pc.addTrack(videoTrack, stream);
      }
    }

    setLocalStream(stream);

    pc.onicecandidate = (event) => {
      if (!event.candidate) return;
      void publishSignalWithRetry("/app/call.ice", {
        receiverId,
        candidate: event.candidate.candidate,
        sdpMid: event.candidate.sdpMid,
        sdpMLineIndex: event.candidate.sdpMLineIndex
      }, 3, 120);
    };

    pc.ontrack = (event) => {
      const [remote] = event.streams;
      if (remote) {
        setRemoteStream(remote);
        if (remoteVideoRef.current) {
          remoteVideoRef.current.srcObject = remote;
          remoteVideoRef.current.play().catch(() => null);
        }
      } else {
        setRemoteStream((prev) => {
          const stream = prev ?? new MediaStream();
          stream.addTrack(event.track);
          if (remoteVideoRef.current) {
            remoteVideoRef.current.srcObject = stream;
            remoteVideoRef.current.play().catch(() => null);
          }
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
      setChatError(null);
      setCallState("calling");
      startRingback();
      setVideoEnabled(withVideo);
      const pc = await initPeer(activeId, withVideo);
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      const sent = await publishSignalWithRetry("/app/call.offer", {
        receiverId: activeId,
        videoEnabled: withVideo,
        sdp: offer.sdp,
        type: offer.type
      });
      if (!sent) {
        setChatError("Не удалось начать звонок. Проверьте подключение и попробуйте еще раз.");
        cleanupCall();
      }
    } catch {
      setChatError("Не удалось запустить звонок. Проверьте доступ к микрофону и камере.");
      cleanupCall();
    }
  };

  const acceptCall = async () => {
    if (!incomingOffer) return;
    try {
      setChatError(null);
      setCallState("in-call");
      stopRingtone();
      const senderId = incomingOffer.senderId;
      const remoteRequestedVideo = incomingOffer.videoEnabled !== false;
      setVideoEnabled(remoteRequestedVideo);
      const pc = await initPeer(senderId, remoteRequestedVideo);
      await pc.setRemoteDescription(new RTCSessionDescription({ type: "offer", sdp: incomingOffer.sdp }));
      await flushPendingIce();
      const answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);
      const sent = await publishSignalWithRetry("/app/call.answer", {
        receiverId: senderId,
        videoEnabled: remoteRequestedVideo,
        sdp: answer.sdp,
        type: answer.type
      });
      if (!sent) {
        setChatError("Не удалось принять звонок. Попробуйте еще раз.");
        cleanupCall();
        return;
      }
      setIncomingOffer(null);
    } catch {
      setChatError("Не удалось подключиться к звонку.");
      cleanupCall();
    }
  };

  const declineCall = () => {
    if (!incomingOffer) return;
    void publishSignalWithRetry("/app/call.hangup", {
      receiverId: incomingOffer.senderId,
      reason: "declined"
    }, 4, 120);
    cleanupCall();
  };

  const endCall = () => {
    const targetId = incomingOffer?.senderId ?? activeId;
    if (targetId) {
      void publishSignalWithRetry("/app/call.hangup", { receiverId: targetId, reason: "hangup" }, 4, 120);
    }
    cleanupCall();
  };

  const ensureAudio = () => {
    if (!ringCtxRef.current) {
      ringCtxRef.current = new AudioContext();
    }
  };

  const queueNote = (delayMs: number, callback: () => void) => {
    const timer = window.setTimeout(callback, delayMs);
    noteTimersRef.current.push(timer);
  };

  const clearQueuedNotes = () => {
    noteTimersRef.current.forEach((timer) => window.clearTimeout(timer));
    noteTimersRef.current = [];
  };

  const playTone = (freq: number, gainValue: number, durationMs: number, wave: OscillatorType) => {
    ensureAudio();
    const ctx = ringCtxRef.current!;
    if (ctx.state === "suspended") {
      ctx.resume().catch(() => null);
    }
    const now = ctx.currentTime;
    const durationSec = durationMs / 1000;

    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = wave;
    osc.frequency.setValueAtTime(freq, now);
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.exponentialRampToValueAtTime(gainValue, now + 0.016);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + durationSec);
    osc.connect(gain).connect(ctx.destination);
    osc.start(now);
    osc.stop(now + durationSec + 0.04);

    const harmonic = ctx.createOscillator();
    const harmonicGain = ctx.createGain();
    harmonic.type = "sine";
    harmonic.frequency.setValueAtTime(freq * 2, now);
    harmonicGain.gain.setValueAtTime(gainValue * 0.2, now);
    harmonicGain.gain.exponentialRampToValueAtTime(0.0001, now + durationSec);
    harmonic.connect(harmonicGain).connect(ctx.destination);
    harmonic.start(now);
    harmonic.stop(now + durationSec + 0.04);

    ringNodesRef.current.push({ osc, gain });
    ringNodesRef.current.push({ osc: harmonic, gain: harmonicGain });
  };

  const stopTone = () => {
    ringNodesRef.current.forEach(({ osc, gain }) => {
      try {
        osc.stop();
      } catch {
        // already stopped
      }
      osc.disconnect();
      gain.disconnect();
    });
    ringNodesRef.current = [];
  };

  const stopAllRingerAudio = () => {
    if (ringtoneLoopTimerRef.current !== null) {
      window.clearTimeout(ringtoneLoopTimerRef.current);
      ringtoneLoopTimerRef.current = null;
    }
    if (ringbackLoopTimerRef.current !== null) {
      window.clearTimeout(ringbackLoopTimerRef.current);
      ringbackLoopTimerRef.current = null;
    }
    clearQueuedNotes();
    stopTone();
  };

  const startRingtone = () => {
    if (ringtoneActiveRef.current) return;
    ringtoneActiveRef.current = true;
    const pattern = () => {
      clearQueuedNotes();
      playTone(1046.5, 0.028, 240, "triangle");
      queueNote(220, () => playTone(1318.5, 0.026, 250, "triangle"));
      queueNote(460, () => playTone(1567.98, 0.024, 300, "triangle"));
      queueNote(740, () => playTone(1318.5, 0.02, 320, "sine"));
      ringtoneLoopTimerRef.current = window.setTimeout(pattern, 2200);
    };
    pattern();
  };

  const stopRingtone = () => {
    ringtoneActiveRef.current = false;
    if (ringtoneLoopTimerRef.current !== null) {
      window.clearTimeout(ringtoneLoopTimerRef.current);
      ringtoneLoopTimerRef.current = null;
    }
    clearQueuedNotes();
    stopTone();
  };

  const startRingback = () => {
    if (ringbackActiveRef.current) return;
    ringbackActiveRef.current = true;
    const pattern = () => {
      clearQueuedNotes();
      playTone(440, 0.02, 430, "sine");
      queueNote(560, () => playTone(480, 0.02, 430, "sine"));
      ringbackLoopTimerRef.current = window.setTimeout(pattern, 2000);
    };
    pattern();
  };

  const stopRingback = () => {
    ringbackActiveRef.current = false;
    if (ringbackLoopTimerRef.current !== null) {
      window.clearTimeout(ringbackLoopTimerRef.current);
      ringbackLoopTimerRef.current = null;
    }
    clearQueuedNotes();
    stopTone();
  };

  const startScreenShare = async () => {
    if (screenSharing || !peerRef.current) return;
    try {
      const display = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
      const screenTrack = display.getVideoTracks()[0];
      const sender = peerRef.current
        .getSenders()
        .find((s) => s.track && s.track.kind === "video")
        ?? videoSenderRef.current
        ?? null;
      if (sender && screenTrack) {
        await sender.replaceTrack(screenTrack);
        videoSenderRef.current = sender;
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
      .find((s) => s.track && s.track.kind === "video")
      ?? videoSenderRef.current
      ?? null;
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
      const sender = peerRef.current.getSenders().find((s) => s.track && s.track.kind === "video")
        ?? videoSenderRef.current
        ?? null;
      if (sender && camTrack) {
        await sender.replaceTrack(camTrack);
        videoSenderRef.current = sender;
      } else if (camTrack) {
        const baseStream = localStream ?? new MediaStream();
        videoSenderRef.current = peerRef.current.addTrack(camTrack, baseStream);
      }
      setLocalStream((prev) => {
        const next = new MediaStream((prev?.getTracks() ?? []).filter((t) => t.kind !== "video"));
        if (camTrack) {
          next.addTrack(camTrack);
        }
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
      videoSenderRef.current = sender;
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
    return () => {
      stopAllRingerAudio();
      ringCtxRef.current?.close().catch(() => null);
      ringCtxRef.current = null;
    };
  }, []);

  useEffect(() => {
    callStateRef.current = callState;
  }, [callState]);

  useEffect(() => {
    if (!auth) return;
    connectWs(auth.accessToken);
    loadContacts();
    const timer = window.setInterval(loadContacts, 30000);
    return () => {
      window.clearInterval(timer);
    };
  }, [auth, loadContacts]);

  useEffect(() => {
    if (!contacts.length) {
      setActiveId(null);
      return;
    }
    setActiveId((prev) => {
      if (prev !== null && contacts.some((c) => String(c.id) === String(prev))) {
        return prev;
      }
      return contacts[0].id;
    });
  }, [contacts]);

  useEffect(() => {
    const handleFocus = () => {
      loadContacts();
    };
    const handleVisibility = () => {
      if (document.visibilityState === "visible") {
        loadContacts();
      }
    };
    window.addEventListener("focus", handleFocus);
    document.addEventListener("visibilitychange", handleVisibility);
    return () => {
      window.removeEventListener("focus", handleFocus);
      document.removeEventListener("visibilitychange", handleVisibility);
    };
  }, [loadContacts]);

  useEffect(() => {
    if (!activeId) return;
    api.get<any[]>(`/chat/conversation/${activeId}`).then((data) => {
      const updated = data.map((m) =>
        m.receiverId === auth?.userId && !m.read ? { ...m, read: true } : m
      );
      setMessages(updated);
      markMessagesRead(updated);
      setUnreadBySender((prev) => ({ ...prev, [activeId]: 0 }));
    }).catch(() => {
      setMessages([]);
      setChatError("Не удалось загрузить сообщения.");
    });
  }, [activeId, auth?.userId]);

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
        if (callStateRef.current !== "idle") {
          void publishSignalWithRetry("/app/call.hangup", { receiverId: payload.senderId, reason: "busy" }, 4, 120);
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
          try {
            await peerRef.current.addIceCandidate(new RTCIceCandidate(ice));
          } catch {
            pendingIceRef.current.push(ice);
          }
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
  }, [userId, activeId, auth?.userId]);

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
        <p className="muted">Личный диалог между вами и специалистом.</p>
      </div>
      {chatError && (
        <div className="row">
          <div className="error">{chatError}</div>
          <button className="button ghost" type="button" onClick={() => loadContacts()}>
            Повторить
          </button>
        </div>
      )}
      {loadingContacts && <div className="muted">Загружаем диалоги...</div>}

      <div className="chat-layout">
        <aside className="chat-contacts">
          {!loadingContacts && contacts.length === 0 && (
            <div className="muted">Пока нет доступных диалогов.</div>
          )}
          {contacts.map((c) => (
            <button
              key={c.id}
              className={
                activeId !== null && String(activeId) === String(c.id)
                  ? "contact active"
                  : unreadBySender[c.id]
                    ? "contact unread"
                    : "contact"
              }
              onClick={() => setActiveId(c.id)}
            >
              <div className="contact-inner">
                <div className="contact-name">{c.fullName}</div>
                <div className="muted">{c.specialization ?? c.department ?? c.position ?? c.phone}</div>
              </div>
            </button>
          ))}
        </aside>
        <section className="chat-window">
          {!activeId && <div className="muted">Выберите {counterpartLabel} слева</div>}
          {activeId && (
            <>
              <div className="chat-header">
                <div>
                  <div className="chat-title">{activeContact?.fullName ?? "Диалог"}</div>
                  <div className="muted">
                    {callState === "in-call" ? "Консультация в эфире" : callState === "calling" ? "Идет вызов…" : "Онлайн-чат"}
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
                  {hasLocalVideo && <video ref={localVideoRef} className="video-local" autoPlay playsInline muted />}
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
                <input value={text} onChange={(e) => handleTyping(e.target.value)} placeholder={`Сообщение для ${counterpartLabel}…`} />
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
