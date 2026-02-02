import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

export default function ChatPage() {
  const { auth } = useAuth();
  const [contacts, setContacts] = useState<any[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [messages, setMessages] = useState<any[]>([]);
  const [text, setText] = useState("");
  const [file, setFile] = useState<File | null>(null);

  useEffect(() => {
    if (!auth) return;
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
    api.get<any[]>(`/chat/conversation/${activeId}`).then(setMessages);
  }, [activeId]);

  const sendMessage = async () => {
    if (!activeId || (!text && !file)) return;

    let attachmentUrl: string | undefined = undefined;
    if (file) {
      const form = new FormData();
      form.append("file", file);
      const upload = await api.upload<{ fileUrl: string }>("/files/upload", form);
      attachmentUrl = upload.fileUrl;
    }

    await api.post("/chat/send", {
      receiverId: activeId,
      content: text,
      attachmentUrl
    });

    setText("");
    setFile(null);
    const updated = await api.get<any[]>(`/chat/conversation/${activeId}`);
    setMessages(updated);
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
              <div className="contact-name">{c.fullName}</div>
              <div className="muted">{c.specialization ?? c.phone}</div>
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
              </div>
              <div className="chat-input">
                <input value={text} onChange={(e) => setText(e.target.value)} placeholder="Сообщение…" />
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
