import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";

export default function PendingApprovalPage() {
  const { auth, setAuth, logout } = useAuth();
  const navigate = useNavigate();
  const [status, setStatus] = useState<"idle" | "checking">("idle");
  const [error, setError] = useState<string | null>(null);

  const checkStatus = async () => {
    setStatus("checking");
    setError(null);
    try {
      const data = await api.get<{ verified: boolean }>("/profile/verification-status");
      if (data.verified) {
        if (auth) {
          setAuth({ ...auth, verified: true });
        }
        navigate("/app");
      }
    } catch {
      setError("Не удалось проверить статус. Попробуйте позже.");
    } finally {
      setStatus("idle");
    }
  };

  useEffect(() => {
    const timer = window.setInterval(() => {
      checkStatus();
    }, 10000);
    return () => window.clearInterval(timer);
  }, []);

  return (
    <div className="page">
      <div className="page-header">
        <h1>Ждём подтверждения</h1>
        <p className="muted">
          Ваш профиль психолога ожидает проверки администратором. Как только проверка завершится,
          доступ к кабинету откроется автоматически.
        </p>
      </div>

      <div className="card">
        <div className="form">
          <div className="info-banner">
            Мы проверяем данные и свяжемся, если потребуется уточнение.
          </div>
          {error && <div className="error">{error}</div>}
          <div className="row">
            <button className="button" onClick={checkStatus} disabled={status === "checking"}>
              {status === "checking" ? "Проверяем…" : "Проверить сейчас"}
            </button>
            <button className="button ghost" onClick={logout}>Выйти</button>
          </div>
        </div>
      </div>
    </div>
  );
}
