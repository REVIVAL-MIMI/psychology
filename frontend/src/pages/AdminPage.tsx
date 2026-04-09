import { useEffect, useState } from "react";
import { api } from "../lib/api";

export default function AdminPage() {
  const [pending, setPending] = useState<any[]>([]);
  const [all, setAll] = useState<any[]>([]);
  const [stats, setStats] = useState<any>(null);

  const load = () => {
    api.get<any[]>("/admin/psychologists/pending").then(setPending);
    api.get<any[]>("/admin/psychologists").then(setAll);
    api.get("/admin/stats").then(setStats);
  };

  useEffect(() => {
    load();
  }, []);

  const verify = async (id: number) => {
    await api.post(`/admin/psychologists/${id}/verify`);
    load();
  };

  const reject = async (id: number) => {
    const reason = prompt("Причина отклонения") ?? "";
    await api.post(`/admin/psychologists/${id}/reject`, { reason });
    load();
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Координация программы</h1>
        <p className="muted">Подключение специалистов и контроль операционной картины сервиса.</p>
      </div>

      {stats && (
        <div className="grid-3">
          <div className="card"><div className="label">Психологи программы</div><div className="value">{stats.totalPsychologists}</div></div>
          <div className="card"><div className="label">На проверке</div><div className="value">{stats.pendingPsychologists}</div></div>
          <div className="card"><div className="label">Сотрудники</div><div className="value">{stats.totalClients}</div></div>
        </div>
      )}

      <div className="card">
        <h3>Специалисты на подключении</h3>
        <ul className="list">
          {pending.map((p) => (
            <li key={p.id} className="list-row">
              <div>
                <div className="card-title">{p.fullName}</div>
                <div className="muted">{[p.organizationName, p.serviceFormat, p.email].filter(Boolean).join(" • ")}</div>
              </div>
              <div className="row">
                <button className="button ghost" onClick={() => verify(p.id)}>Подтвердить</button>
                <button className="button ghost" onClick={() => reject(p.id)}>Отклонить</button>
              </div>
            </li>
          ))}
        </ul>
      </div>

      <div className="card">
        <h3>Все специалисты</h3>
        <ul className="list">
          {all.map((p) => (
            <li key={p.id} className="list-row">
              <div>
                <div className="card-title">{p.fullName}</div>
                <div className="muted">{[p.organizationName, p.serviceFormat, p.email].filter(Boolean).join(" • ")}</div>
              </div>
              <div className="muted">{p.verified ? "Активен" : "На проверке"}</div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
