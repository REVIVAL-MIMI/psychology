import { useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

const TEMPLATES = [
  {
    title: "Дневник эмоций",
    content: "Ежедневно отмечайте эмоции и события, которые на них влияли.",
    categories: "осознанность, эмоции",
    priority: "3"
  },
  {
    title: "Дыхательная практика",
    content: "2 раза в день: 4 секунды вдох, 4 задержка, 6 выдох.",
    categories: "дыхание, тревожность",
    priority: "4"
  },
  {
    title: "Сон и восстановление",
    content: "Ложиться до 23:30, минимум 7 часов сна 5 дней в неделю.",
    categories: "сон, восстановление",
    priority: "2"
  }
];

export default function RecommendationsPage() {
  const { auth } = useAuth();
  const [recommendations, setRecommendations] = useState<any[]>([]);
  const [clients, setClients] = useState<any[]>([]);
  const [clientId, setClientId] = useState("");
  const [stats, setStats] = useState<any>(null);
  const [filters, setFilters] = useState({ completed: "", overdue: "", from: "", to: "" });
  const [form, setForm] = useState({ title: "", content: "", deadline: "", priority: "3", categories: "" });

  useEffect(() => {
    if (!auth) return;
    if (auth.userRole === "ROLE_PSYCHOLOGIST") {
      api.get<any[]>("/clients").then(setClients);
      api.get("/recommendations/stats").then(setStats).catch(() => null);
    } else {
      loadClientRecommendations();
    }
  }, [auth]);

  const buildQuery = () => {
    const params = new URLSearchParams();
    if (filters.completed) params.set("completed", filters.completed);
    if (filters.overdue) params.set("overdue", filters.overdue);
    if (filters.from) params.set("from", filters.from);
    if (filters.to) params.set("to", filters.to);
    return params.toString();
  };

  const loadClientRecommendations = async () => {
    const query = buildQuery();
    const data = await api.get<any[]>(`/recommendations${query ? `?${query}` : ""}`);
    setRecommendations(data);
  };

  const loadForClient = async (id: string) => {
    if (!id) return;
    const query = buildQuery();
    const data = await api.get<any[]>(`/recommendations/client/${id}${query ? `?${query}` : ""}`);
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
      loadClientRecommendations();
    }
  };

  const removeRecommendation = async (id: number) => {
    await api.del(`/recommendations/${id}`);
    loadForClient(clientId);
  };

  const applyTemplate = (index: number) => {
    const template = TEMPLATES[index];
    setForm((prev) => ({
      ...prev,
      title: template.title,
      content: template.content,
      categories: template.categories,
      priority: template.priority
    }));
  };

  const applyFilters = () => {
    if (auth?.userRole === "ROLE_PSYCHOLOGIST") {
      loadForClient(clientId);
    } else {
      loadClientRecommendations();
    }
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
            <div className="template-row">
              {TEMPLATES.map((template, index) => (
                <button key={template.title} className="button ghost" onClick={() => applyTemplate(index)}>
                  Шаблон: {template.title}
                </button>
              ))}
            </div>
            <button className="button" onClick={createRecommendation} disabled={!clientId || !form.title}>Создать</button>
          </div>
        </>
      )}

      <div className="card">
        <div className="filter-bar">
          <div className="filter-group">
            <label>
              Статус
              <select value={filters.completed} onChange={(e) => setFilters({ ...filters, completed: e.target.value })}>
                <option value="">Все</option>
                <option value="true">Выполненные</option>
                <option value="false">Активные</option>
              </select>
            </label>
            <label>
              Просрочка
              <select value={filters.overdue} onChange={(e) => setFilters({ ...filters, overdue: e.target.value })}>
                <option value="">Все</option>
                <option value="true">Просроченные</option>
                <option value="false">Без просрочки</option>
              </select>
            </label>
            <label>
              С даты
              <input type="datetime-local" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} />
            </label>
            <label>
              До даты
              <input type="datetime-local" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} />
            </label>
          </div>
          <div className="filter-actions">
            <button className="button ghost" onClick={applyFilters}>Применить</button>
            <button className="button ghost" onClick={() => setFilters({ completed: "", overdue: "", from: "", to: "" })}>Сбросить</button>
          </div>
        </div>

        <h3>Список рекомендаций</h3>
        <ul className="list">
          {recommendations.map((rec) => (
            <li key={rec.id} className="list-row">
              <div>
                <div className="card-title">{rec.title}</div>
                <div className="muted">{rec.content}</div>
                <div className="tag-row">
                  {rec.categories?.map((cat: string) => (
                    <span key={cat} className="chip">{cat}</span>
                  ))}
                </div>
              </div>
              <div className="row">
                {!rec.completed && (
                  <button className="button ghost" onClick={() => markCompleted(rec.id, auth?.userRole ?? "")}>Отметить</button>
                )}
                {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
                  <button className="button ghost" onClick={() => removeRecommendation(rec.id)}>Удалить</button>
                )}
                <span className={`badge ${rec.completed ? "COMPLETED" : "SCHEDULED"}`}>
                  {rec.completed ? "Выполнена" : "Активна"}
                </span>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
