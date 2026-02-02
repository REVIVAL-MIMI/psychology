import { ChangeEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

const initialForm = {
  phone: "",
  otp: "",
  fullName: "",
  email: "",
  education: "",
  specialization: "",
  description: "",
  photoUrl: ""
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
      await api.post("/auth/send-otp", { phone: form.phone }, { skipAuth: true });
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
        form,
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
    <div className="card">
      <h2>Регистрация психолога</h2>
      <p className="muted">Сначала подтвердите номер, затем заполните профиль.</p>

      <div className="form">
        <label>
          Номер телефона
          <input type="tel" value={form.phone} onChange={update("phone")} placeholder="+79990000000" />
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
            <label>
              Фото (URL)
              <input value={form.photoUrl} onChange={update("photoUrl")} placeholder="https://" />
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
  );
}
