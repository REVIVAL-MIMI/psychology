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
  const [file, setFile] = useState<File | null>(null);
  const [typing, setTyping] = useState(false);
  const typingTimer = useRef<number | null>(null);
  const lastTypingSent = useRef<number>(0);

  const userId = useMemo(() => auth?.userId ?? null, [auth]);

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
    if (!activeId) return;
    api.get<any[]>(`/chat/conversation/${activeId}`).then((data) => {
      const updated = data.map((m) =>
        m.receiverId === auth?.userId && !m.read ? { ...m, read: true } : m
      );
      setMessages(updated);
      markMessagesRead(updated);
    });
  }, [activeId]);

  useEffect(() => {
    if (!userId) return;
    let unsubscribeMessages: (() => void) | null = null;
    let unsubscribeTyping: (() => void) | null = null;

    unsubscribeMessages = subscribeWs(`/user/${userId}/queue/messages`, (payload) => {
      const shouldRead =
        payload?.senderId === activeId &&
        payload?.receiverId === auth?.userId &&
        !payload?.read;
      const nextPayload = shouldRead ? { ...payload, read: true } : payload;

      setMessages((prev) => {
        const exists = prev.some((m) => m.id === payload.id);
        if (exists) return prev;
        return [...prev, nextPayload];
      });

      if (shouldRead) {
        markMessagesRead([payload]);
      }
    });

    unsubscribeTyping = subscribeWs(`/user/${userId}/queue/typing`, (payload: { senderId: number; typing: boolean }) => {
      if (payload.senderId === activeId) {
        setTyping(payload.typing);
      }
    });

    return () => {
      unsubscribeMessages?.();
      unsubscribeTyping?.();
    };
  }, [userId, activeId]);

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
    if (!activeId || (!text && !file)) return;

    let attachmentUrl: string | undefined = undefined;
    if (file) {
      const form = new FormData();
      form.append("file", file);
      const upload = await api.upload<{ fileUrl: string }>("/files/upload", form);
      attachmentUrl = upload.fileUrl;
    }

    const payload = {
      receiverId: activeId,
      content: text,
      attachmentUrl
    };

    const optimistic = {
      id: `tmp-${Date.now()}`,
      senderId: userId,
      receiverId: activeId,
      content: text,
      attachmentUrl,
      sentAt: new Date().toISOString()
    };
    setMessages((prev) => [...prev, optimistic]);

    const sent = await publishWs("/app/chat.send", payload);
    if (!sent) {
      await api.post("/chat/send", payload);
    }

    setText("");
    setFile(null);
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
              className={activeId === c.id ? "contact active" : "contact"}
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
              <div className="chat-messages">
                {messages.map((m) => (
                  <div key={m.id} className={m.senderId === auth?.userId ? "message outgoing" : "message incoming"}>
                    <div className="message-body">
                      <div className="message-text">{m.content}</div>
                      {m.attachmentUrl && (
                        <a href={m.attachmentUrl} className="link">Вложение</a>
                      )}
                    </div>
                    <div className="message-time">{new Date(m.sentAt).toLocaleTimeString()}</div>
                  </div>
                ))}
                {typing && <div className="typing-indicator">… собеседник печатает</div>}
              </div>
              <div className="chat-input">
                <input value={text} onChange={(e) => handleTyping(e.target.value)} placeholder="Сообщение…" />
                <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
                <button className="button" onClick={sendMessage}>Отправить</button>
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}
