import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

export default function LoginPage() {
  const navigate = useNavigate();
  const { setAuth } = useAuth();
  const [phone, setPhone] = useState("");
  const [otp, setOtp] = useState("");
  const [stage, setStage] = useState<"phone" | "otp">("phone");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const sendOtp = async () => {
    setLoading(true);
    setError(null);
    try {
      await api.post("/auth/send-otp", { phone }, { skipAuth: true });
      setStage("otp");
    } catch (e) {
      setError("Не удалось отправить код. Проверьте номер.");
    } finally {
      setLoading(false);
    }
  };

  const verifyOtp = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.post<{ accessToken: string; userId: number; userRole: string; fullName: string; phone: string }>(
        "/auth/verify-otp",
        { phone, otp },
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
          <h2>Спокойный вход</h2>
          <p className="muted">Введите номер и подтвердите одноразовым кодом.</p>
        </div>

        <div className="form">
          <label>
            Номер телефона
            <input
              type="tel"
              placeholder="+79990000000"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
            />
          </label>

          {stage === "otp" && (
            <label>
              Код из SMS
              <input
                type="text"
                placeholder="123456"
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
              />
            </label>
          )}

          {error && <div className="error">{error}</div>}

          {stage === "phone" ? (
            <button className="button" onClick={sendOtp} disabled={loading || !phone}>
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
          <h3>Без лишнего</h3>
          <p>OTP‑код вместо паролей — меньше шума, больше ясности.</p>
        </div>
        <div className="aside-card">
          <h3>Границы</h3>
          <p>Данные доступны только вам и вашему специалисту.</p>
        </div>
      </div>
    </div>
  );
}
