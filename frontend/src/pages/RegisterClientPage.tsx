import { ChangeEvent, useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { companyProfile } from "../lib/branding";
import { normalizeEmail } from "../lib/email";

const initialForm = {
  phone: "",
  otp: "",
  fullName: "",
  department: "",
  position: ""
};

type InviteValidation = {
  valid: boolean;
  psychologistName: string;
  organizationName?: string;
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
      .catch(() => setError("Приглашение недействительно или уже истекло."));
  }, [inviteToken]);

  const sendOtp = async () => {
    setLoading(true);
    setError(null);
    try {
      await api.post("/auth/send-otp", { phone: normalizeEmail(form.phone) }, { skipAuth: true });
    } catch {
      setError("Не удалось отправить код. Проверьте email.");
    } finally {
      setLoading(false);
    }
  };

  const register = async () => {
    setLoading(true);
    setError(null);
    try {
      const loginEmail = normalizeEmail(form.phone);
      const payload = {
        phone: loginEmail,
        otp: form.otp,
        fullName: form.fullName,
        age: null,
        companyName: invite?.organizationName ?? companyProfile.companyName,
        workEmail: loginEmail,
        department: form.department || null,
        position: form.position || null
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
            <div className="pill">Сотрудник</div>
            <h2>Нужно приглашение</h2>
            <p className="muted">Доступ открывается только после приглашения от психолога.</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="pill">Сотрудник</div>
          <h2>Активация доступа</h2>
          <p className="muted">
            Подтвердите email и заполните профиль участника программы поддержки сотрудников.
          </p>
        </div>

        {invite && (
          <div className="info-banner">
            <div>Организация: <strong>{invite.organizationName ?? companyProfile.companyName}</strong></div>
            <div>Назначенный психолог: <strong>{invite.psychologistName}</strong></div>
            <div>Приглашение действительно до: {new Date(invite.expiresAt).toLocaleString()}</div>
          </div>
        )}

        <div className="form">
          <label>
            Email
            <input
              type="email"
              value={form.phone}
              onChange={(e) => setForm((prev) => ({ ...prev, phone: e.target.value }))}
              placeholder="name@telecombg.ru"
            />
          </label>
          <div className="row">
            <button className="button ghost" onClick={sendOtp} disabled={loading || !form.phone}>
              {loading ? "Отправляем…" : "Отправить код"}
            </button>
          </div>
          <label>
            Код подтверждения
            <input value={form.otp} onChange={update("otp")} placeholder="123456" />
          </label>
          <label>
            ФИО
            <input value={form.fullName} onChange={update("fullName")} />
          </label>
          <label>
            Подразделение
            <input value={form.department} onChange={update("department")} placeholder="Техническая поддержка" />
          </label>
          <label>
            Должность
            <input value={form.position} onChange={update("position")} placeholder="Инженер, менеджер, аналитик" />
          </label>
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={consent}
              onChange={(e) => setConsent(e.target.checked)}
            />
            <span>
              Я принимаю условия
              <a className="legal-link" href="/terms" target="_blank" rel="noreferrer"> пользовательского соглашения</a>
              {" "}и даю согласие на
              <a className="legal-link" href="/privacy" target="_blank" rel="noreferrer"> обработку персональных данных</a>.
            </span>
          </label>

          {error && <div className="error">{error}</div>}

          <button
            className="button"
            onClick={register}
            disabled={loading || !form.otp || !form.fullName || !consent}
          >
            {loading ? "Активируем…" : "Активировать доступ"}
          </button>
        </div>
      </div>

      <div className="auth-aside">
        <div className="aside-card">
          <h3>Только по приглашению</h3>
          <p>Доступ в систему открывается по персональному приглашению.</p>
        </div>
        <div className="aside-card">
          <h3>Ваши данные</h3>
          <p>История консультаций, журнал и рекомендации доступны только вам и назначенному психологу.</p>
        </div>
      </div>
    </div>
  );
}
