import { useEffect, useMemo, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

const STATUS_OPTIONS = [
  "SCHEDULED",
  "CONFIRMED",
  "IN_PROGRESS",
  "COMPLETED",
  "CANCELLED",
  "RESCHEDULED"
];

export default function SessionsPage() {
  const { auth } = useAuth();
  const [sessions, setSessions] = useState<any[]>([]);
  const [clients, setClients] = useState<any[]>([]);
  const [form, setForm] = useState({ clientId: "", scheduledAt: "", durationMinutes: "50", description: "" });
  const [filters, setFilters] = useState({ from: "", to: "", status: "" });
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({ scheduledAt: "", durationMinutes: "50", description: "", status: "SCHEDULED" });
  const [error, setError] = useState<string | null>(null);

  const query = useMemo(() => {
    const params = new URLSearchParams();
    if (filters.from) params.set("from", filters.from);
    if (filters.to) params.set("to", filters.to);
    return params.toString();
  }, [filters]);

  const loadSessions = () => {
    if (!auth) return;
    const endpoint = auth.userRole === "ROLE_PSYCHOLOGIST" ? "/sessions/psychologist" : "/sessions/client";
    const url = query ? `${endpoint}?${query}` : endpoint;
    api.get<any[]>(url).then((data) => {
      if (filters.status) {
        setSessions(data.filter((s) => s.status === filters.status));
      } else {
        setSessions(data);
      }
    });
  };

  useEffect(() => {
    loadSessions();
    if (auth?.userRole === "ROLE_PSYCHOLOGIST") {
      api.get<any[]>("/clients").then(setClients);
    }
  }, [auth, query, filters.status]);

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

  const openEdit = (session: any) => {
    setEditingId(session.id);
    setEditForm({
      scheduledAt: session.scheduledAt ?? "",
      durationMinutes: String(session.durationMinutes ?? 50),
      description: session.description ?? "",
      status: session.status ?? "SCHEDULED"
    });
  };

  const updateSession = async () => {
    if (!editingId) return;
    await api.put(`/sessions/${editingId}`, {
      scheduledAt: editForm.scheduledAt,
      durationMinutes: Number(editForm.durationMinutes),
      description: editForm.description,
      status: editForm.status
    });
    setEditingId(null);
    loadSessions();
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
        <div className="filter-bar">
          <div className="filter-group">
            <label>
              С даты
              <input type="datetime-local" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} />
            </label>
            <label>
              До даты
              <input type="datetime-local" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} />
            </label>
            <label>
              Статус
              <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
                <option value="">Все</option>
                {STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="filter-actions">
            <button className="button ghost" onClick={() => setFilters({ from: "", to: "", status: "" })}>Сбросить</button>
          </div>
        </div>

        <h3>Список сеансов</h3>
        <ul className="list">
          {sessions.map((session) => (
            <li key={session.id} className="list-row">
              <div>
                <div className="card-title">
                  {new Date(session.scheduledAt).toLocaleString()} — {session.client?.fullName ?? session.psychologist?.fullName ?? ""}
                </div>
                <div className="muted">{session.description ?? ""}</div>
              </div>
              <div className="row">
                <span className={`badge ${session.status}`}>{session.status}</span>
                {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
                  <button className="button ghost" onClick={() => openEdit(session)}>Изменить</button>
                )}
                {(session.status === "SCHEDULED" || session.status === "CONFIRMED") && (
                  <button className="button ghost" onClick={() => cancelSession(session.id)}>Отменить</button>
                )}
              </div>
            </li>
          ))}
        </ul>
      </div>

      {editingId && (
        <div className="card">
          <h3>Перенос / обновление сеанса</h3>
          <div className="form grid-2">
            <label>
              Новая дата и время
              <input type="datetime-local" value={editForm.scheduledAt} onChange={(e) => setEditForm({ ...editForm, scheduledAt: e.target.value })} />
            </label>
            <label>
              Длительность (мин)
              <input type="number" value={editForm.durationMinutes} onChange={(e) => setEditForm({ ...editForm, durationMinutes: e.target.value })} />
            </label>
            <label>
              Описание
              <input value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} />
            </label>
            <label>
              Статус
              <select value={editForm.status} onChange={(e) => setEditForm({ ...editForm, status: e.target.value })}>
                {STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>{status}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="row">
            <button className="button" onClick={updateSession}>Сохранить</button>
            <button className="button ghost" onClick={() => setEditingId(null)}>Отмена</button>
          </div>
        </div>
      )}
    </div>
  );
}
