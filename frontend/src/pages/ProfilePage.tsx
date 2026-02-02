import { ChangeEvent, useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

export default function ProfilePage() {
  const { auth } = useAuth();
  const [profile, setProfile] = useState<any>(null);
  const [form, setForm] = useState<any>({});
  const [status, setStatus] = useState<string | null>(null);

  useEffect(() => {
    api.get("/profile").then((data) => {
      setProfile(data);
      setForm(data);
    });
  }, []);

  const update = (key: string) => (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setForm({ ...form, [key]: e.target.value });
  };

  const save = async () => {
    setStatus(null);
    const endpoint = auth?.userRole === "ROLE_PSYCHOLOGIST" ? "/profile/psychologist" : "/profile/client";
    await api.put(endpoint, form);
    setStatus("Сохранено");
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Профиль</h1>
      </div>

      <div className="card">
        {!profile && <div className="muted">Загружаем…</div>}
        {profile && (
          <div className="form grid-2">
            <label>
              ФИО
              <input value={form.fullName ?? ""} onChange={update("fullName")} />
            </label>
            {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
              <label>
                Email
                <input value={form.email ?? ""} onChange={update("email")} />
              </label>
            )}
            {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
              <label>
                Образование
                <input value={form.education ?? ""} onChange={update("education")} />
              </label>
            )}
            {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
              <label>
                Специализация
                <input value={form.specialization ?? ""} onChange={update("specialization")} />
              </label>
            )}
            {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
              <label>
                Описание
                <textarea value={form.description ?? ""} onChange={update("description")} rows={4} />
              </label>
            )}
            {auth?.userRole === "ROLE_CLIENT" && (
              <label>
                Возраст
                <input type="number" value={form.age ?? ""} onChange={update("age")} />
              </label>
            )}
            <label>
              Фото (URL)
              <input value={form.photoUrl ?? ""} onChange={update("photoUrl")} />
            </label>
          </div>
        )}
        <button className="button" onClick={save}>Сохранить</button>
        {status && <div className="muted">{status}</div>}
      </div>
    </div>
  );
}
