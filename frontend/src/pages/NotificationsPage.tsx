import { useEffect, useState } from "react";
import { api } from "../lib/api";

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<any[]>([]);

  const loadNotifications = () => {
    api.get<any[]>("/notifications").then(setNotifications);
  };

  useEffect(() => {
    loadNotifications();
  }, []);

  const markRead = async (id: number) => {
    await api.post(`/notifications/${id}/read`);
    loadNotifications();
  };

  const markAll = async () => {
    await api.post("/notifications/read-all");
    loadNotifications();
  };

  const remove = async (id: number) => {
    await api.del(`/notifications/${id}`);
    loadNotifications();
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Уведомления</h1>
        <button className="button ghost" onClick={markAll}>Отметить все</button>
      </div>

      <div className="card">
        <ul className="list">
          {notifications.map((n) => (
            <li key={n.id} className="list-row">
              <div>
                <div className="card-title">{n.title ?? n.type}</div>
                <div className="muted">{n.message ?? n.content}</div>
              </div>
              <div className="row">
                {!n.read && <button className="button ghost" onClick={() => markRead(n.id)}>Прочитано</button>}
                <button className="button ghost" onClick={() => remove(n.id)}>Удалить</button>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
