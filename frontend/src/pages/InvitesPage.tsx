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
  }, []);

  const createInvite = async () => {
    setError(null);
    try {
      await api.post("/invites");
      loadInvites();
    } catch {
      setError("Не удалось создать инвайт.");
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Инвайты</h1>
        <p className="muted">Создавайте приглашения для новых клиентов.</p>
      </div>

      <div className="card">
        <button className="button" onClick={createInvite}>Создать инвайт</button>
        {error && <div className="error">{error}</div>}
        <ul className="list">
          {invites.map((invite) => {
            const link = `${window.location.origin}/register?invite=${invite.token}`;
            return (
              <li key={invite.token} className="list-row">
                <div>
                  <div className="card-title">{invite.token}</div>
                  <div className="muted">До: {invite.expiresAt}</div>
                  <div className="muted">{invite.used ? "Использован" : "Активен"}</div>
                </div>
                <a className="button ghost" href={link} target="_blank" rel="noreferrer">Открыть</a>
              </li>
            );
          })}
        </ul>
      </div>
    </div>
  );
}
