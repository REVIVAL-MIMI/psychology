import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

export default function RecommendationsPage() {
  const { auth } = useAuth();
  const [recommendations, setRecommendations] = useState<any[]>([]);
  const [clients, setClients] = useState<any[]>([]);
  const [clientId, setClientId] = useState("");
  const [stats, setStats] = useState<any>(null);
  const [form, setForm] = useState({ title: "", content: "", deadline: "", priority: "3", categories: "" });

  useEffect(() => {
    if (!auth) return;
    if (auth.userRole === "ROLE_PSYCHOLOGIST") {
      api.get<any[]>("/clients").then(setClients);
      api.get("/recommendations/stats").then(setStats).catch(() => null);
    } else {
      api.get<any[]>("/recommendations").then(setRecommendations);
    }
  }, [auth]);

  const loadForClient = async (id: string) => {
    if (!id) return;
    const data = await api.get<any[]>(`/recommendations/client/${id}`);
    setRecommendations(data);
  };

  const createRecommendation = async () => {
    if (!clientId) return;
    await api.post("/recommendations", {
      clientId: Number(clientId),
      title: form.title,
      content: form.content,
      deadline: form.deadline || null,
      priority: Number(form.priority),
      categories: form.categories ? form.categories.split(",").map((c) => c.trim()).filter(Boolean) : []
    });
    setForm({ title: "", content: "", deadline: "", priority: "3", categories: "" });
    loadForClient(clientId);
  };

  const markCompleted = async (id: number, role: string) => {
    const endpoint = role === "ROLE_PSYCHOLOGIST" ? `/recommendations/${id}/complete` : `/recommendations/${id}/client-complete`;
    await api.post(endpoint);
    if (role === "ROLE_PSYCHOLOGIST") {
      loadForClient(clientId);
    } else {
      const data = await api.get<any[]>("/recommendations");
      setRecommendations(data);
    }
  };

  const removeRecommendation = async (id: number) => {
    await api.del(`/recommendations/${id}`);
    loadForClient(clientId);
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Рекомендации</h1>
      </div>

      {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
        <>
          {stats && (
            <div className="grid-3">
              <div className="card"><div className="label">Всего</div><div className="value">{stats.totalRecommendations}</div></div>
              <div className="card"><div className="label">Выполнены</div><div className="value">{stats.completedRecommendations}</div></div>
              <div className="card"><div className="label">Просрочены</div><div className="value">{stats.overdueRecommendations}</div></div>
            </div>
          )}

          <div className="card">
            <h3>Новая рекомендация</h3>
            <div className="form grid-2">
              <label>
                Клиент
                <select value={clientId} onChange={(e) => { setClientId(e.target.value); loadForClient(e.target.value); }}>
                  <option value="">Выберите клиента</option>
                  {clients.map((c) => (
                    <option key={c.id} value={c.id}>{c.fullName}</option>
                  ))}
                </select>
              </label>
              <label>
                Заголовок
                <input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
              </label>
              <label>
                Описание
                <input value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} />
              </label>
              <label>
                Дедлайн
                <input type="datetime-local" value={form.deadline} onChange={(e) => setForm({ ...form, deadline: e.target.value })} />
              </label>
              <label>
                Приоритет
                <input type="number" min={1} max={5} value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })} />
              </label>
              <label>
                Категории
                <input value={form.categories} onChange={(e) => setForm({ ...form, categories: e.target.value })} placeholder="сон, тревожность" />
              </label>
            </div>
            <button className="button" onClick={createRecommendation} disabled={!clientId || !form.title}>Создать</button>
          </div>
        </>
      )}

      <div className="card">
        <h3>Список рекомендаций</h3>
        <ul className="list">
          {recommendations.map((rec) => (
            <li key={rec.id} className="list-row">
              <div>
                <div className="card-title">{rec.title}</div>
                <div className="muted">{rec.content}</div>
              </div>
              <div className="row">
                {!rec.completed && (
                  <button className="button ghost" onClick={() => markCompleted(rec.id, auth?.userRole ?? "")}>Отметить</button>
                )}
                {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
                  <button className="button ghost" onClick={() => removeRecommendation(rec.id)}>Удалить</button>
                )}
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
