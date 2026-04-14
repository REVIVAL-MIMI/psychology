import { ChangeEvent, useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { getRoleSubtitle } from "../lib/branding";
import { normalizeEmail } from "../lib/email";

export default function ProfilePage() {
  const { auth, setAuth } = useAuth();
  const [profile, setProfile] = useState<any>(null);
  const [form, setForm] = useState<any>({});
  const [status, setStatus] = useState<string | null>(null);
  const [phoneForm, setPhoneForm] = useState({ phone: "", otp: "" });
  const [phoneStatus, setPhoneStatus] = useState<string | null>(null);
  const [phoneLoading, setPhoneLoading] = useState(false);

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

  const sendPhoneOtp = async () => {
    setPhoneLoading(true);
    setPhoneStatus(null);
    try {
      await api.post("/profile/email/send-otp", { phone: normalizeEmail(phoneForm.phone) });
      setPhoneStatus("Код отправлен на email");
    } catch {
      setPhoneStatus("Не удалось отправить код");
    } finally {
      setPhoneLoading(false);
    }
  };

  const confirmPhone = async () => {
    setPhoneLoading(true);
    setPhoneStatus(null);
    try {
      const data = await api.post("/profile/email/confirm", {
        phone: normalizeEmail(phoneForm.phone),
        otp: phoneForm.otp
      });
      // обновляем auth в localStorage через setAuth в контексте
      if (auth) {
        const next = { ...auth, ...data };
        setForm((prev: any) => ({ ...prev, phone: data.phone }));
        setPhoneStatus("Email обновлен");
        setAuth(next);
      }
    } catch {
      setPhoneStatus("Не удалось обновить email");
    } finally {
      setPhoneLoading(false);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Профиль</h1>
        <p className="muted">{getRoleSubtitle(auth?.userRole)}</p>
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
                Организация
                <input value={form.organizationName ?? ""} onChange={update("organizationName")} />
              </label>
            )}
            {auth?.userRole === "ROLE_PSYCHOLOGIST" && (
              <label>
                Формат работы
                <input value={form.serviceFormat ?? ""} onChange={update("serviceFormat")} />
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
                Компания
                <input value={form.companyName ?? ""} onChange={update("companyName")} />
              </label>
            )}
            {auth?.userRole === "ROLE_CLIENT" && (
              <label>
                Рабочий email
                <input type="email" value={form.workEmail ?? ""} onChange={update("workEmail")} />
              </label>
            )}
            {auth?.userRole === "ROLE_CLIENT" && (
              <label>
                Подразделение
                <input value={form.department ?? ""} onChange={update("department")} />
              </label>
            )}
            {auth?.userRole === "ROLE_CLIENT" && (
              <label>
                Должность
                <input value={form.position ?? ""} onChange={update("position")} />
              </label>
            )}
            {auth?.userRole === "ROLE_CLIENT" && (
              <label>
                Табельный номер
                <input value={form.employeeCode ?? ""} onChange={update("employeeCode")} />
              </label>
            )}
            {auth?.userRole === "ROLE_CLIENT" && (
              <label>
                Возраст (опционально)
                <input type="number" value={form.age ?? ""} onChange={update("age")} />
              </label>
            )}
          </div>
        )}
        <button className="button" onClick={save}>Сохранить</button>
        {status && <div className="muted">{status}</div>}
      </div>

      <div className="card">
        <h3>Сменить email для входа</h3>
        <div className="form grid-2">
          <label>
            Новый email
            <input
              type="email"
              value={phoneForm.phone}
              onChange={(e) => setPhoneForm((prev) => ({ ...prev, phone: e.target.value }))}
              placeholder="name@telecombg.ru"
            />
          </label>
          <label>
            Код подтверждения
            <input
              value={phoneForm.otp}
              onChange={(e) => setPhoneForm((prev) => ({ ...prev, otp: e.target.value }))}
              placeholder="123456"
            />
          </label>
        </div>
        <div className="row">
          <button className="button ghost" onClick={sendPhoneOtp} disabled={phoneLoading || !phoneForm.phone}>
            {phoneLoading ? "Отправляем…" : "Отправить код"}
          </button>
          <button className="button" onClick={confirmPhone} disabled={phoneLoading || !phoneForm.phone || !phoneForm.otp}>
            {phoneLoading ? "Сохраняем…" : "Подтвердить"}
          </button>
        </div>
        {phoneStatus && <div className="muted">{phoneStatus}</div>}
      </div>
    </div>
  );
}
