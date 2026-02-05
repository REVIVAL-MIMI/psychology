import { Link, Outlet } from "react-router-dom";
import { useAuth } from "../lib/auth";

export default function PublicLayout() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="public-layout">
      <header className="public-header">
        <Link to="/" className="brand">
          <span className="brand-mark">Ψ</span>
          <span>Psychology</span>
        </Link>
        <nav className="public-nav">
          <Link to={isAuthenticated ? "/app" : "/login"} className="nav-link-inline">
            {isAuthenticated ? "Войти в кабинет" : "Войти"}
          </Link>
          {!isAuthenticated && (
            <Link to="/register/psychologist" className="button ghost">
              Регистрация психолога
            </Link>
          )}
        </nav>
      </header>
      <main className="public-main">
        <Outlet />
      </main>
    </div>
  );
}
