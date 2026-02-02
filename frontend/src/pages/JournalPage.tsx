import { useEffect, useState } from "react";
import { api } from "../lib/api";

const MAX_LENGTH = 5000;

export default function JournalPage() {
  const [entries, setEntries] = useState<any[]>([]);
  const [stats, setStats] = useState<any>(null);
  const [form, setForm] = useState({ content: "", mood: "", tags: "" });
  const [searchTag, setSearchTag] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState({ content: "", mood: "", tags: "" });

  const loadEntries = () => {
    api.get<any[]>("/journal").then(setEntries);
    api.get("/journal/stats").then(setStats).catch(() => null);
  };

  useEffect(() => {
    loadEntries();
  }, []);

  const createEntry = async () => {
    setError(null);
    try {
      await api.post("/journal", {
        content: form.content,
        mood: form.mood,
        tags: form.tags ? form.tags.split(",").map((t) => t.trim()).filter(Boolean) : []
      });
      setForm({ content: "", mood: "", tags: "" });
      loadEntries();
    } catch {
      setError("Не удалось сохранить запись.");
    }
  };

  const search = async () => {
    if (!searchTag) return;
    const result = await api.get<any[]>(`/journal/search?tag=${encodeURIComponent(searchTag)}`);
    setEntries(result);
  };

  const deleteEntry = async (id: number) => {
    await api.del(`/journal/${id}`);
    loadEntries();
  };

  const startEdit = (entry: any) => {
    setEditingId(entry.id);
    setEditForm({
      content: entry.content ?? "",
      mood: entry.mood ?? "",
      tags: entry.tags?.join(", ") ?? ""
    });
  };

  const updateEntry = async () => {
    if (!editingId) return;
    await api.put(`/journal/${editingId}`, {
      content: editForm.content,
      mood: editForm.mood,
      tags: editForm.tags ? editForm.tags.split(",").map((t) => t.trim()).filter(Boolean) : []
    });
    setEditingId(null);
    loadEntries();
  };

  const remaining = MAX_LENGTH - form.content.length;

  return (
    <div className="page">
      <div className="page-header">
        <h1>Дневник</h1>
        <p className="muted">Записывайте мысли и наблюдения.</p>
      </div>

      <div className="card">
        <h3>Новая запись</h3>
        <div className="form">
          <label>
            Текст
            <textarea
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value.slice(0, MAX_LENGTH) })}
              rows={4}
            />
            <div className="muted">Осталось символов: {remaining}</div>
          </label>
          <label>
            Настроение
            <input value={form.mood} onChange={(e) => setForm({ ...form, mood: e.target.value })} placeholder="Спокойное, тревожное…" />
          </label>
          <label>
            Теги (через запятую)
            <input value={form.tags} onChange={(e) => setForm({ ...form, tags: e.target.value })} />
          </label>
          {error && <div className="error">{error}</div>}
          <button className="button" onClick={createEntry} disabled={!form.content}>Сохранить</button>
        </div>
      </div>

      {stats && (
        <div className="grid-3">
          <div className="card">
            <div className="label">Всего записей</div>
            <div className="value">{stats.totalEntries}</div>
          </div>
          <div className="card">
            <div className="label">В этом месяце</div>
            <div className="value">{stats.entriesThisMonth}</div>
          </div>
          <div className="card">
            <div className="label">Частое настроение</div>
            <div className="value">{stats.mostCommonMood ?? "—"}</div>
          </div>
        </div>
      )}

      <div className="card">
        <div className="row">
          <input className="search" placeholder="Поиск по тегу" value={searchTag} onChange={(e) => setSearchTag(e.target.value)} />
          <button className="button ghost" onClick={search}>Найти</button>
          <button className="button ghost" onClick={loadEntries}>Сбросить</button>
        </div>
        <ul className="list">
          {entries.map((entry) => (
            <li key={entry.id} className="list-row">
              <div>
                <div className="muted">{new Date(entry.createdAt ?? entry.date).toLocaleString()}</div>
                <div>{entry.content}</div>
                {entry.tags && entry.tags.length > 0 && (
                  <div className="tag-row">
                    {entry.tags.map((tag: string) => (
                      <span key={tag} className="chip">{tag}</span>
                    ))}
                  </div>
                )}
              </div>
              <div className="row">
                <button className="button ghost" onClick={() => startEdit(entry)}>Редактировать</button>
                <button className="button ghost" onClick={() => deleteEntry(entry.id)}>Удалить</button>
              </div>
            </li>
          ))}
        </ul>
      </div>

      {editingId && (
        <div className="card">
          <h3>Редактирование записи</h3>
          <div className="form">
            <label>
              Текст
              <textarea
                value={editForm.content}
                onChange={(e) => setEditForm({ ...editForm, content: e.target.value.slice(0, MAX_LENGTH) })}
                rows={4}
              />
            </label>
            <label>
              Настроение
              <input value={editForm.mood} onChange={(e) => setEditForm({ ...editForm, mood: e.target.value })} />
            </label>
            <label>
              Теги (через запятую)
              <input value={editForm.tags} onChange={(e) => setEditForm({ ...editForm, tags: e.target.value })} />
            </label>
            <div className="row">
              <button className="button" onClick={updateEntry}>Сохранить</button>
              <button className="button ghost" onClick={() => setEditingId(null)}>Отмена</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
