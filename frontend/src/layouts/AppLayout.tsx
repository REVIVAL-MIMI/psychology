import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../lib/auth";

const navForRole = (role: string) => {
  if (role === "ROLE_PSYCHOLOGIST") {
    return [
      { to: "/app", label: "Дашборд" },
      { to: "/app/clients", label: "Клиенты" },
      { to: "/app/sessions", label: "Сеансы" },
      { to: "/app/recommendations", label: "Рекомендации" },
      { to: "/app/chat", label: "Чат" },
      { to: "/app/invites", label: "Инвайты" },
      { to: "/app/notifications", label: "Уведомления" },
      { to: "/app/profile", label: "Профиль" }
    ];
  }
  if (role === "ROLE_ADMIN") {
    return [
      { to: "/app/admin", label: "Админ" },
      { to: "/app/notifications", label: "Уведомления" },
      { to: "/app/profile", label: "Профиль" }
    ];
  }
  return [
    { to: "/app", label: "Дашборд" },
    { to: "/app/sessions", label: "Сеансы" },
    { to: "/app/journal", label: "Дневник" },
    { to: "/app/recommendations", label: "Рекомендации" },
    { to: "/app/chat", label: "Чат" },
    { to: "/app/notifications", label: "Уведомления" },
    { to: "/app/profile", label: "Профиль" }
  ];
};

export default function AppLayout() {
  const { auth, logout } = useAuth();

  if (!auth) return null;
  const nav = navForRole(auth.userRole);

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-top">
          <div className="logo">Psychology</div>
          <div className="role-chip">{auth.userRole.replace("ROLE_", "")}</div>
        </div>
        <nav className="sidebar-nav">
          {nav.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) =>
              isActive ? "nav-link active" : "nav-link"
            }>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="user-block">
            <div className="user-name">{auth.fullName}</div>
            <div className="user-phone">{auth.phone}</div>
          </div>
          <button className="button" onClick={logout}>Выйти</button>
        </div>
      </aside>
      <main className="app-main">
        <Outlet />
      </main>
    </div>
  );
}
