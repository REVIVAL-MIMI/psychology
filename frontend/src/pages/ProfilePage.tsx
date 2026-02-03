import { ChangeEvent, useEffect, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { formatPhone, normalizePhone } from "../lib/phone";

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
      await api.post("/profile/phone/send-otp", { phone: normalizePhone(phoneForm.phone) });
      setPhoneStatus("Код отправлен");
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
      const data = await api.post("/profile/phone/confirm", {
        phone: normalizePhone(phoneForm.phone),
        otp: phoneForm.otp
      });
      // обновляем auth в localStorage через setAuth в контексте
      if (auth) {
        const next = { ...auth, ...data };
        setForm((prev: any) => ({ ...prev, phone: data.phone }));
        setPhoneStatus("Номер обновлен");
        setAuth(next);
      }
    } catch {
      setPhoneStatus("Не удалось обновить номер");
    } finally {
      setPhoneLoading(false);
    }
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
          </div>
        )}
        <button className="button" onClick={save}>Сохранить</button>
        {status && <div className="muted">{status}</div>}
      </div>

      <div className="card">
        <h3>Сменить номер телефона</h3>
        <div className="form grid-2">
          <label>
            Новый номер
            <input
              type="tel"
              value={phoneForm.phone}
              onChange={(e) => setPhoneForm((prev) => ({ ...prev, phone: formatPhone(e.target.value) }))}
              placeholder="+7 (999) 000-00-00"
              inputMode="tel"
            />
          </label>
          <label>
            Код из SMS
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
