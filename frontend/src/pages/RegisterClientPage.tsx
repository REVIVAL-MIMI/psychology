import { ChangeEvent, useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { formatPhone, normalizePhone } from "../lib/phone";

const initialForm = {
  phone: "",
  otp: "",
  fullName: "",
  age: ""
};

type InviteValidation = {
  valid: boolean;
  psychologistName: string;
  expiresAt: string;
};

export default function RegisterClientPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { setAuth } = useAuth();
  const [form, setForm] = useState(initialForm);
  const [invite, setInvite] = useState<InviteValidation | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [consent, setConsent] = useState(false);
  const inviteToken = useMemo(() => params.get("invite") ?? "", [params]);

  const update = (key: keyof typeof form) => (e: ChangeEvent<HTMLInputElement>) => {
    setForm((prev) => ({ ...prev, [key]: e.target.value }));
  };

  useEffect(() => {
    if (!inviteToken) return;
    api
      .get<InviteValidation>(`/invites/validate/${inviteToken}`, { skipAuth: true })
      .then(setInvite)
      .catch(() => setError("Инвайт недействителен или просрочен."));
  }, [inviteToken]);

  const sendOtp = async () => {
    setLoading(true);
    setError(null);
    try {
      await api.post("/auth/send-otp", { phone: normalizePhone(form.phone) }, { skipAuth: true });
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
      const payload = {
        phone: normalizePhone(form.phone),
        otp: form.otp,
        fullName: form.fullName,
        age: Number(form.age)
      };
      const data = await api.post(
        `/auth/client/register?inviteToken=${encodeURIComponent(inviteToken)}`,
        payload,
        { skipAuth: true }
      );
      setAuth(data as any);
      navigate("/app");
    } catch {
      setError("Регистрация не удалась. Проверьте данные и код.");
    } finally {
      setLoading(false);
    }
  };

  if (!inviteToken) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-header">
            <div className="pill">Клиент</div>
            <h2>Нужен инвайт</h2>
            <p className="muted">Регистрация доступна только по приглашению специалиста.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="pill">Клиент</div>
          <h2>Регистрация по приглашению</h2>
          <p className="muted">Подтвердите номер, заполните базовый профиль и начните практику.</p>
        </div>

        {invite && (
          <div className="info-banner">
            <div>Психолог: <strong>{invite.psychologistName}</strong></div>
            <div>Инвайт действителен до: {new Date(invite.expiresAt).toLocaleString()}</div>
          </div>
        )}

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
          <div className="row">
            <button className="button ghost" onClick={sendOtp} disabled={loading || !form.phone}>
              {loading ? "Отправляем…" : "Отправить код"}
            </button>
          </div>
          <label>
            Код из SMS
            <input value={form.otp} onChange={update("otp")} placeholder="123456" />
          </label>
          <label>
            ФИО
            <input value={form.fullName} onChange={update("fullName")} />
          </label>
          <label>
            Возраст
            <input type="number" value={form.age} onChange={update("age")} />
          </label>
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={consent}
              onChange={(e) => setConsent(e.target.checked)}
            />
            <span>
              Я принимаю условия пользовательского соглашения и даю согласие на обработку персональных данных.
            </span>
          </label>

          {error && <div className="error">{error}</div>}

          <button
            className="button"
            onClick={register}
            disabled={loading || !form.otp || !form.fullName || !form.age || !consent}
          >
            {loading ? "Создаем…" : "Создать аккаунт"}
          </button>
        </div>
      </div>

      <div className="auth-aside">
        <div className="aside-card">
          <h3>Только по приглашению</h3>
          <p>Доступ открывается вашим специалистом. Это часть системы границ.</p>
        </div>
        <div className="aside-card">
          <h3>Ваши данные</h3>
          <p>История сеансов, дневник и рекомендации доступны сразу после входа.</p>
        </div>
      </div>
    </div>
  );
}
