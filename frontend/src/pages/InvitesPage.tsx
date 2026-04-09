import { useEffect, useState } from "react";
import { api } from "../lib/api";

export default function InvitesPage() {
  const [invites, setInvites] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);

  const loadInvites = () => {
    api.get<any[]>("/invites").then(setInvites);
  };

  useEffect(() => {
    loadInvites();
    const timer = window.setInterval(loadInvites, 15000);
    return () => window.clearInterval(timer);
  }, []);

  const createInvite = async () => {
    setError(null);
    try {
      await api.post("/invites");
      loadInvites();
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
          {invites.length ? "Получить текущую ссылку" : "Создать приглашение"}
        </button>
        {error && <div className="error">{error}</div>}
        {!invites.length && <div className="muted">Сейчас нет активной ссылки приглашения.</div>}
        <ul className="list">
          {invites.map((invite) => {
            const link = `${window.location.origin}/register?invite=${invite.token}`;
            return (
              <li key={invite.token} className="list-row">
                <div>
                  <div className="card-title">Активная ссылка для одного сотрудника</div>
                  <div className="muted">Действует до: {new Date(invite.expiresAt).toLocaleString()}</div>
                  <div className="muted">После использования ссылка автоматически исчезнет из кабинета.</div>
                </div>
                <a className="button ghost" href={link} target="_blank" rel="noreferrer">Открыть форму</a>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
