import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { companyProfile, getSessionStatusLabel } from "../lib/branding";

export default function DashboardPage() {
  const { auth } = useAuth();
  const [data, setData] = useState<any>(null);
  const [stats, setStats] = useState<any>(null);
  const [upcoming, setUpcoming] = useState<any[]>([]);
  const [activeClients, setActiveClients] = useState<any[]>([]);
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

    if (auth.userRole === "ROLE_PSYCHOLOGIST") {
      api.get("/dashboard/psychologist/stats").then(setStats).catch(() => null);
      api.get("/dashboard/psychologist/upcoming-sessions?daysAhead=14").then(setUpcoming).catch(() => null);
      api.get("/dashboard/psychologist/active-clients").then(setActiveClients).catch(() => null);
    }
  }, [auth]);

  if (!auth) return null;

  const formatEmployeeMeta = (employee: any) =>
    [employee.department, employee.position, employee.workEmail]
      .filter(Boolean)
      .join(" • ") || `Телефон: ${employee.phone ?? "—"}`;

  return (
    <div className="page">
      <div className="page-header">
        <h1>Дашборд</h1>
        <p className="muted">Ключевые события и ближайшие консультации.</p>
      </div>

      {error && <div className="error">{error}</div>}
      {!data && !error && <div className="muted">Загружаем…</div>}

      {auth.userRole === "ROLE_PSYCHOLOGIST" && data && (
        <>
          <div className="grid-4">
            <div className="card stat-card">
              <div className="label">Всего сотрудников</div>
              <div className="value">{data.totalClients}</div>
            </div>
            <div className="card stat-card">
              <div className="label">Активные сотрудники</div>
              <div className="value">{data.activeClients}</div>
            </div>
            <div className="card stat-card">
              <div className="label">Консультации сегодня</div>
              <div className="value">{data.upcomingSessionsToday}</div>
            </div>
            <div className="card stat-card">
              <div className="label">Непрочитанные</div>
              <div className="value">{data.unreadMessages}</div>
            </div>
          </div>

          {stats && (
            <div className="card">
              <h3>Статистика за период</h3>
              <div className="grid-4">
                <div>
                  <div className="label">Всего консультаций</div>
                  <div className="value">{stats.totalSessions}</div>
                </div>
                <div>
                  <div className="label">Завершено</div>
                  <div className="value">{stats.completedSessions}</div>
                </div>
                <div>
                  <div className="label">Отменено</div>
                  <div className="value">{stats.cancelledSessions}</div>
                </div>
                <div>
                  <div className="label">Новые подключения</div>
                  <div className="value">{stats.newClients}</div>
                </div>
              </div>
            </div>
          )}

          <div className="grid-2">
            <div className="card">
              <h3>Ближайшие консультации</h3>
              {upcoming.length === 0 && <div className="muted">Нет ближайших консультаций</div>}
              <ul className="list">
                {upcoming.map((session) => (
                  <li key={session.id} className="list-row">
                    <div>
                      <div className="card-title">{session.client?.fullName ?? "Сотрудник"}</div>
                      <div className="muted">{new Date(session.scheduledAt).toLocaleString()}</div>
                    </div>
                    <span className={`badge ${session.status}`}>{getSessionStatusLabel(session.status)}</span>
                  </li>
                ))}
              </ul>
            </div>
            <div className="card">
              <h3>Активные сотрудники</h3>
              {activeClients.length === 0 && <div className="muted">Нет активных сотрудников</div>}
              <ul className="list">
                {activeClients.map((client) => (
                  <li key={client.id} className="list-row">
                    <div>
                      <div className="card-title">{client.fullName}</div>
                      <div className="muted">{formatEmployeeMeta(client)}</div>
                    </div>
                    <span className="badge">В сопровождении</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </>
      )}

      {auth.userRole === "ROLE_CLIENT" && data && (
        <div className="grid-2">
          <div className="card stat-card">
            <div className="label">Ваш психолог программы</div>
            <div className="value">{data.psychologist?.fullName ?? "—"}</div>
            <div className="muted">{data.psychologist?.specialization ?? "Психологическая поддержка"}</div>
          </div>
          <div className="card stat-card">
            <div className="label">Следующая консультация</div>
            <div className="value">
              {data.nextSession
                ? new Date(data.nextSession.scheduledAt).toLocaleString()
                : "Не назначен"}
            </div>
          </div>
          <div className="card stat-card">
            <div className="label">Непрочитанные сообщения</div>
            <div className="value">{data.unreadMessages}</div>
          </div>
          <div className="card stat-card">
            <div className="label">Программа</div>
            <div className="value">{companyProfile.shortName}</div>
            <div className="muted">Психологическая поддержка</div>
          </div>
          <div className="card stat-card">
            <div className="label">Активные рекомендации</div>
            <div className="value">{data.pendingRecommendations}</div>
          </div>
          <div className="card span-2">
            <h3>Ближайшие консультации</h3>
            {!data.upcomingSessions?.length && <div className="muted">Пока нет запланированных консультаций</div>}
            <ul className="list">
              {data.upcomingSessions?.map((session: any) => (
                <li key={session.id} className="list-row">
                  <div>
                    <div className="card-title">{new Date(session.scheduledAt).toLocaleString()}</div>
                    <div className="muted">{getSessionStatusLabel(session.status)}</div>
                  </div>
                  <span className={`badge ${session.status}`}>{getSessionStatusLabel(session.status)}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {auth.userRole === "ROLE_ADMIN" && data && (
        <div className="grid-3">
          <div className="card">
            <div className="label">Психологи программы</div>
            <div className="value">{data.totalPsychologists}</div>
          </div>
          <div className="card">
            <div className="label">На проверке</div>
            <div className="value">{data.pendingPsychologists}</div>
          </div>
          <div className="card">
            <div className="label">Сотрудники в программе</div>
            <div className="value">{data.totalClients}</div>
          </div>
        </div>
      )}
    </div>
  );
}
