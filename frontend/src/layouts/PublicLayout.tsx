import { Link, Outlet } from "react-router-dom";
import { useAuth } from "../lib/auth";
import { companyProfile } from "../lib/branding";

export default function PublicLayout() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="public-layout">
      <header className="public-header">
        <Link to="/" className="brand">
          <span className="brand-mark">TBG</span>
          <span className="brand-copy">
            <strong>{companyProfile.platformName}</strong>
            <small>{companyProfile.shortName}</small>
          </span>
        </Link>
        <nav className="public-nav">
          <Link to={isAuthenticated ? "/app" : "/login"} className="nav-link-inline">
            {isAuthenticated ? "Открыть кабинет" : "Войти"}
          </Link>
          {!isAuthenticated && (
            <Link to="/register/psychologist" className="button ghost">
              Анкета психолога
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
