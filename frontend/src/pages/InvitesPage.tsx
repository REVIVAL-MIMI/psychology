import { useEffect, useState } from "react";
import { api } from "../lib/api";

type Invite = {
  token: string;
  expiresAt: string;
  used?: boolean;
};

type InviteCreateResponse = {
  token: string;
  inviteLink?: string;
};

export default function InvitesPage() {
  const [invites, setInvites] = useState<Invite[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);

  const buildInviteLink = (token: string) => `${window.location.origin}/register?invite=${token}`;

  const loadInvites = async () => {
    const items = await api.get<Invite[]>("/invites");
    setInvites(items);
  };

  const copyAndShareInvite = async (link: string) => {
    let copied = false;
    try {
      await navigator.clipboard.writeText(link);
      copied = true;
    } catch {
      copied = false;
    }

    if (navigator.share) {
      try {
        await navigator.share({
          title: "Приглашение в личный кабинет",
          text: "Ссылка для регистрации в кабинете сотрудника:",
          url: link
        });
        setStatus(copied ? "Ссылка скопирована и отправлена." : "Ссылка отправлена.");
        return;
      } catch {
        // ignore cancel/share errors
      }
    }

    if (copied) {
      setStatus("Ссылка приглашения скопирована. Отправьте ее сотруднику.");
    } else {
      setStatus(`Скопируйте ссылку и отправьте сотруднику: ${link}`);
    }
  };

  useEffect(() => {
    void loadInvites();
    const timer = window.setInterval(() => {
      void loadInvites();
    }, 15000);
    return () => window.clearInterval(timer);
  }, []);

  const createInvite = async () => {
    setError(null);
    setStatus(null);
    try {
      const created = await api.post<InviteCreateResponse>("/invites");
      const token = created?.token ?? invites[0]?.token;
      if (!token) {
        throw new Error("Invite token not found");
      }
      await copyAndShareInvite(buildInviteLink(token));
      await loadInvites();
    } catch {
      setError("Не удалось создать приглашение.");
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Приглашения</h1>
        <p className="muted">У психолога доступна только одна активная ссылка. После регистрации сотрудника она исчезает автоматически.</p>
      </div>

      <div className="card">
        <button className="button" onClick={createInvite}>
          {invites.length ? "Скопировать текущую ссылку" : "Создать и скопировать ссылку"}
        </button>
        {error && <div className="error">{error}</div>}
        {status && <div className="muted">{status}</div>}
        {!invites.length && <div className="muted">Сейчас нет активной ссылки приглашения.</div>}
        <ul className="list">
          {invites.map((invite) => {
            const link = buildInviteLink(invite.token);
            return (
              <li key={invite.token} className="list-row">
                <div>
                  <div className="card-title">Активная ссылка для одного сотрудника</div>
                  <div className="muted">Действует до: {new Date(invite.expiresAt).toLocaleString()}</div>
                  <div className="muted">После использования ссылка автоматически исчезнет из кабинета.</div>
                  <div className="muted">{link}</div>
                </div>
                <button className="button ghost" onClick={() => void copyAndShareInvite(link)}>
                  Скопировать ссылку
                </button>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
