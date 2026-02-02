import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../lib/api";

export default function ClientDetailPage() {
  const { id } = useParams();
  const [client, setClient] = useState<any>(null);
  const [stats, setStats] = useState<any>(null);
  const [activity, setActivity] = useState<any>(null);

  useEffect(() => {
    if (!id) return;
    api.get(`/clients/${id}`).then(setClient);
    api.get(`/clients/${id}/stats`).then(setStats);
    api.get(`/clients/${id}/activity`).then(setActivity);
  }, [id]);

  return (
    <div className="page">
      <div className="page-header">
        <h1>Карточка клиента</h1>
        <p className="muted">Полная информация и активность.</p>
      </div>

      {!client && <div className="muted">Загружаем…</div>}

      {client && (
        <div className="grid-2">
          <div className="card">
            <div className="card-title">{client.fullName}</div>
            <div className="muted">Возраст: {client.age ?? "—"}</div>
            <div className="muted">Телефон: {client.phone ?? "—"}</div>
          </div>
          {stats && (
            <div className="card">
              <div className="label">Сеансы</div>
              <div className="value">{stats.totalSessions}</div>
              <div className="muted">Посещаемость: {stats.attendanceRate}%</div>
            </div>
          )}
          {activity && (
            <div className="card span-2">
              <div className="label">Последняя активность</div>
              <ul className="list">
                {activity.recentSessions?.map((s: any, idx: number) => (
                  <li key={`s-${idx}`}>{new Date(s.date).toLocaleString()} — {s.status}</li>
                ))}
                {activity.recentJournalEntries?.map((j: any, idx: number) => (
                  <li key={`j-${idx}`}>Запись в дневнике: {new Date(j.date).toLocaleDateString()} — {j.mood}</li>
                ))}
                {activity.recentRecommendations?.map((r: any, idx: number) => (
                  <li key={`r-${idx}`}>Рекомендация: {r.title} — {r.completed ? "выполнена" : "активна"}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
