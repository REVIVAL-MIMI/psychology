import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

export default function DashboardPage() {
  const { auth } = useAuth();
  const [data, setData] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!auth) return;
    const endpoint = auth.userRole === "ROLE_PSYCHOLOGIST"
      ? "/dashboard/psychologist"
      : auth.userRole === "ROLE_CLIENT"
      ? "/dashboard/client"
      : "/admin/stats";

    api
      .get(endpoint)
      .then(setData)
      .catch(() => setError("Не удалось загрузить дашборд"));
  }, [auth]);

  if (!auth) return null;

  return (
    <div className="page">
      <div className="page-header">
        <h1>Дашборд</h1>
        <p className="muted">Сводка по вашей активности.</p>
      </div>

      {error && <div className="error">{error}</div>}
      {!data && !error && <div className="muted">Загружаем…</div>}

      {auth.userRole === "ROLE_PSYCHOLOGIST" && data && (
        <div className="grid-3">
          <div className="card">
            <div className="label">Всего клиентов</div>
            <div className="value">{data.totalClients}</div>
          </div>
          <div className="card">
            <div className="label">Активные клиенты</div>
            <div className="value">{data.activeClients}</div>
          </div>
          <div className="card">
            <div className="label">Непрочитанные сообщения</div>
            <div className="value">{data.unreadMessages}</div>
          </div>
          <div className="card span-3">
            <div className="label">Ближайшие сеансы</div>
            <ul className="list">
              {data.nextSessions?.map((session: any) => (
                <li key={session.id}>
                  {new Date(session.startTime ?? session.dateTime ?? session.start).toLocaleString()} — {session.client?.fullName ?? "Клиент"}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {auth.userRole === "ROLE_CLIENT" && data && (
        <div className="grid-2">
          <div className="card">
            <div className="label">Ваш психолог</div>
            <div className="value">{data.psychologist?.fullName ?? "—"}</div>
            <div className="muted">{data.psychologist?.specialization ?? ""}</div>
          </div>
          <div className="card">
            <div className="label">Следующий сеанс</div>
            <div className="value">
              {data.nextSession
                ? new Date(data.nextSession.startTime ?? data.nextSession.dateTime ?? data.nextSession.start).toLocaleString()
                : "Не назначен"}
            </div>
          </div>
          <div className="card">
            <div className="label">Непрочитанные сообщения</div>
            <div className="value">{data.unreadMessages}</div>
          </div>
          <div className="card">
            <div className="label">Активные рекомендации</div>
            <div className="value">{data.pendingRecommendations}</div>
          </div>
        </div>
      )}

      {auth.userRole === "ROLE_ADMIN" && data && (
        <div className="grid-3">
          <div className="card">
            <div className="label">Психологи</div>
            <div className="value">{data.totalPsychologists}</div>
          </div>
          <div className="card">
            <div className="label">Ожидают верификацию</div>
            <div className="value">{data.pendingPsychologists}</div>
          </div>
          <div className="card">
            <div className="label">Клиенты</div>
            <div className="value">{data.totalClients}</div>
          </div>
        </div>
      )}
    </div>
  );
}
