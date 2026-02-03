import { useState } from "react";
import { Link, Outlet } from "react-router-dom";
import { getStoredTheme, toggleTheme } from "../lib/theme";

export default function PublicLayout() {
  const [theme, setTheme] = useState(getStoredTheme());
  const handleToggle = () => setTheme(toggleTheme());

  return (
    <div className="public-layout">
      <header className="public-header">
        <Link to="/" className="brand">
          <span className="brand-mark">Ψ</span>
          <span>Psychology</span>
        </Link>
        <nav className="public-nav">
          <button className="button ghost theme-toggle" onClick={handleToggle}>
            {theme === "dark" ? "Свет" : "Тьма"}
          </button>
          <Link to="/login" className="nav-link-inline">Войти</Link>
          <Link to="/register/psychologist" className="button ghost">
            Регистрация психолога
          </Link>
        </nav>
      </header>
      <main className="public-main">
        <Outlet />
      </main>
    </div>
  );
}
