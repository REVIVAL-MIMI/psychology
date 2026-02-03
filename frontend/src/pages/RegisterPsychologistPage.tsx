import { ChangeEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { formatPhone, normalizePhone } from "../lib/phone";

const initialForm = {
  phone: "",
  otp: "",
  fullName: "",
  email: "",
  education: "",
  specialization: "",
  description: ""
};

export default function RegisterPsychologistPage() {
  const navigate = useNavigate();
  const { setAuth } = useAuth();
  const [form, setForm] = useState(initialForm);
  const [stage, setStage] = useState<"phone" | "profile">("phone");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const update = (key: keyof typeof form) => (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setForm((prev) => ({ ...prev, [key]: e.target.value }));
  };

  const sendOtp = async () => {
    setLoading(true);
    setError(null);
    try {
      await api.post("/auth/send-otp", { phone: normalizePhone(form.phone) }, { skipAuth: true });
      setStage("profile");
    } catch {
      setError("Не удалось отправить код. Проверьте номер.");
    } finally {
      setLoading(false);
    }
  };

  const register = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.post(
        "/auth/psychologist/register",
        { ...form, phone: normalizePhone(form.phone) },
        { skipAuth: true }
      );
      setAuth(data as any);
      navigate("/app");
    } catch (e) {
      setError("Регистрация не удалась. Проверьте данные и код.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="pill">Психолог</div>
          <h2>Профиль практики</h2>
          <p className="muted">Подтвердите номер и заполните анкету специалиста.</p>
        </div>

        <div className="form">
          <label>
            Номер телефона
            <input
              type="tel"
              value={form.phone}
              onChange={(e) => setForm((prev) => ({ ...prev, phone: formatPhone(e.target.value) }))}
              placeholder="+7 (999) 000-00-00"
              inputMode="tel"
            />
          </label>

          {stage === "profile" && (
            <>
              <label>
                Код из SMS
                <input value={form.otp} onChange={update("otp")} placeholder="123456" />
              </label>
              <label>
                ФИО
                <input value={form.fullName} onChange={update("fullName")} />
              </label>
              <label>
                Email
                <input type="email" value={form.email} onChange={update("email")} />
              </label>
              <label>
                Образование
                <input value={form.education} onChange={update("education")} />
              </label>
              <label>
                Специализация
                <input value={form.specialization} onChange={update("specialization")} />
              </label>
              <label>
                Описание
                <textarea value={form.description} onChange={update("description")} rows={4} />
              </label>
            </>
          )}

          {error && <div className="error">{error}</div>}

          {stage === "phone" ? (
            <button className="button" onClick={sendOtp} disabled={loading || !form.phone}>
              {loading ? "Отправляем…" : "Отправить код"}
            </button>
          ) : (
            <button className="button" onClick={register} disabled={loading || !form.otp || !form.fullName || !form.email}>
              {loading ? "Создаем…" : "Создать профиль"}
            </button>
          )}
        </div>
      </div>

      <div className="auth-aside">
        <div className="aside-card">
          <h3>Верификация</h3>
          <p>После отправки анкеты профиль проходит проверку администратором.</p>
        </div>
        <div className="aside-card">
          <h3>Рабочее пространство</h3>
          <p>Клиенты, расписание, рекомендации и чат — без лишнего.</p>
        </div>
      </div>
    </div>
  );
}
