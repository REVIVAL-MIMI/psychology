import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

export default function SessionsPage() {
  const { auth } = useAuth();
  const [sessions, setSessions] = useState<any[]>([]);
  const [clients, setClients] = useState<any[]>([]);
  const [form, setForm] = useState({ clientId: "", scheduledAt: "", durationMinutes: "50", description: "" });
  const [error, setError] = useState<string | null>(null);

  const loadSessions = () => {
    if (!auth) return;
    const endpoint = auth.userRole === "ROLE_PSYCHOLOGIST" ? "/sessions/psychologist" : "/sessions/client";
    api.get<any[]>(endpoint).then(setSessions);
  };

  useEffect(() => {
    loadSessions();
    if (auth?.userRole === "ROLE_PSYCHOLOGIST") {
      api.get<any[]>("/clients").then(setClients);
    }
  }, [auth]);

  const createSession = async () => {
    setError(null);
    try {
      await api.post("/sessions", {
        clientId: Number(form.clientId),
        scheduledAt: form.scheduledAt,
        durationMinutes: Number(form.durationMinutes),
        description: form.description
      });
      setForm({ clientId: "", scheduledAt: "", durationMinutes: "50", description: "" });
      loadSessions();
    } catch {
      setError("Не удалось создать сеанс.");
    }
  };

  const cancelSession = async (id: number) => {
    try {
      await api.post(`/sessions/${id}/cancel`);
      loadSessions();
    } catch {
      setError("Не удалось отменить сеанс.");
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Сеансы</h1>
        <p className="muted">Управление расписанием и история.</p>
      </div>

      {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
        <div className="card">
          <h3>Новый сеанс</h3>
          <div className="form grid-2">
            <label>
              Клиент
              <select value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
                <option value="">Выберите клиента</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>{c.fullName}</option>
                ))}
              </select>
            </label>
            <label>
              Дата и время
              <input type="datetime-local" value={form.scheduledAt} onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })} />
            </label>
            <label>
              Длительность (мин)
              <input type="number" value={form.durationMinutes} onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })} />
            </label>
            <label>
              Описание
              <input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
            </label>
          </div>
          {error && <div className="error">{error}</div>}
          <button className="button" onClick={createSession} disabled={!form.clientId || !form.scheduledAt}>Создать</button>
        </div>
      )}

      <div className="card">
        <h3>Список сеансов</h3>
        <ul className="list">
          {sessions.map((session) => (
            <li key={session.id} className="list-row">
              <div>
                {new Date(session.scheduledAt).toLocaleString()} — {session.client?.fullName ?? session.psychologist?.fullName ?? ""}
              </div>
              <div className="muted">{session.status}</div>
              {session.status === "SCHEDULED" && (
                <button className="button ghost" onClick={() => cancelSession(session.id)}>Отменить</button>
              )}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
