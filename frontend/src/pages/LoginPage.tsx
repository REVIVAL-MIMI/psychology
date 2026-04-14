import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { normalizeEmail } from "../lib/email";

export default function LoginPage() {
  const navigate = useNavigate();
  const { setAuth, isAuthenticated } = useAuth();
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [stage, setStage] = useState<"email" | "otp">("email");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      navigate("/app", { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const sendOtp = async () => {
    setLoading(true);
    setError(null);
    try {
      await api.post("/auth/send-otp", { phone: normalizeEmail(email) }, { skipAuth: true });
      setStage("otp");
    } catch (e) {
      setError("Не удалось отправить код. Проверьте email.");
    } finally {
      setLoading(false);
    }
  };

  const verifyOtp = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.post<{ accessToken: string; userId: number; userRole: string; fullName: string; phone: string; verified?: boolean }>(
        "/auth/verify-otp",
        { phone: normalizeEmail(email), otp },
        { skipAuth: true }
      );
      setAuth(data as any);
      navigate("/app");
    } catch (e) {
      setError("Неверный код или пользователь не найден.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <div className="pill">Вход</div>
          <h2>Добро пожаловать</h2>
          <p className="muted">
            Используйте email, который вы указывали при регистрации.
          </p>
        </div>

        <div className="form">
          <label>
            Рабочий email
            <input
              type="email"
              placeholder="name@telecombg.ru"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
          {stage === "otp" && (
            <label>
              Код подтверждения
              <input
                type="text"
                placeholder="123456"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
              />
            </label>
          )}

          {error && <div className="error">{error}</div>}

          {stage === "email" ? (
            <button className="button" onClick={sendOtp} disabled={loading || !email}>
              {loading ? "Отправляем…" : "Отправить код"}
            </button>
          ) : (
            <button className="button" onClick={verifyOtp} disabled={loading || !otp}>
              {loading ? "Проверяем…" : "Войти"}
            </button>
          )}
        </div>
      </div>

      <div className="auth-aside">
        <div className="aside-card">
          <h3>Единый доступ</h3>
          <p>Вход занимает меньше минуты и открывает консультации, журнал и рекомендации в одном кабинете.</p>
        </div>
        <div className="aside-card">
          <h3>Конфиденциальность</h3>
          <p>Содержимое консультаций и личных записей доступно только вам и назначенному психологу.</p>
        </div>
      </div>
    </div>
  );
}
