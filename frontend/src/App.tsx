import { useEffect, useState } from "react";
import "./App.css";
import { getHealth } from "./lib/api";

type Health = {
  status: string;
  time?: string;
};

export default function App() {
  const [health, setHealth] = useState<Health | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getHealth()
      .then(setHealth)
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Unknown error");
      });
  }, []);

  return (
    <div className="page">
      <header className="hero">
        <div className="brand">Psychology</div>
        <h1>Быстрый прототип фронтенда</h1>
        <p>
          Подключен пример REST-запроса. Когда API будет готов, мы просто
          заменим базовый URL и расширим эндпоинты.
        </p>
      </header>

      <section className="card">
        <h2>API статус</h2>
        {error && <div className="error">Ошибка: {error}</div>}
        {!error && !health && <div className="muted">Загружаем…</div>}
        {health && (
          <div className="grid">
            <div>
              <div className="label">Status</div>
              <div className="value">{health.status}</div>
            </div>
            <div>
              <div className="label">Time</div>
              <div className="value">{health.time ?? "n/a"}</div>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
