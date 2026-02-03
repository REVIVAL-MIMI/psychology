import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../lib/auth";
import { getStoredTheme, toggleTheme } from "../lib/theme";

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
  const [theme, setTheme] = useState(getStoredTheme());

  if (!auth) return null;
  const nav = navForRole(auth.userRole);

  const handleToggle = () => setTheme(toggleTheme());

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">Ψ</span>
          <span>Psychology</span>
        </div>
        <div className="topbar-actions">
          <div className="user-chip">
            <div className="user-chip-name">{auth.fullName}</div>
            <div className="user-chip-sub">{auth.userRole.replace("ROLE_", "")}</div>
          </div>
          <button className="button ghost theme-toggle" onClick={handleToggle}>
            {theme === "dark" ? "Свет" : "Тьма"}
          </button>
          <button className="button" onClick={logout}>Выйти</button>
        </div>
      </header>

      <div className="app-body">
        <aside className="side-rail">
          <div className="side-rail-header">
            <div className="role-chip">{auth.userRole.replace("ROLE_", "")}</div>
            <div className="user-meta">
              <div className="user-name">{auth.fullName}</div>
              <div className="user-phone">{auth.phone}</div>
            </div>
          </div>
          <nav className="nav-stack">
            {nav.map((item) => (
              <NavLink key={item.to} to={item.to} className={({ isActive }) =>
                isActive ? "nav-pill active" : "nav-pill"
              }>
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
