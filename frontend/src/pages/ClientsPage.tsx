import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../lib/api";

export default function ClientsPage() {
  const [clients, setClients] = useState<any[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);

  const loadClients = (q?: string) => {
    setLoading(true);
    const endpoint = q ? `/clients/search?query=${encodeURIComponent(q)}` : "/clients";
    api
      .get<any[]>(endpoint)
      .then(setClients)
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadClients();
  }, []);

  return (
    <div className="page">
      <div className="page-header">
        <h1>Клиенты</h1>
        <div className="row">
          <input
            className="search"
            placeholder="Поиск по имени или телефону"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button className="button ghost" onClick={() => loadClients(query)}>Найти</button>
        </div>
      </div>

      {loading && <div className="muted">Загружаем…</div>}

      <div className="grid-2">
        {clients.map((client) => (
          <div className="card" key={client.id}>
            <div className="card-title">{client.fullName}</div>
            <div className="muted">Возраст: {client.age ?? "—"}</div>
            <div className="muted">Телефон: {client.phone ?? "—"}</div>
            <div className="card-actions">
              <Link to={`/app/clients/${client.id}`} className="button ghost">Открыть</Link>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
