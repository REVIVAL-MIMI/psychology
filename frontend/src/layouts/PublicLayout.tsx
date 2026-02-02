import { Link, Outlet } from "react-router-dom";

export default function PublicLayout() {
  return (
    <div className="public-layout">
      <header className="public-header">
        <Link to="/" className="logo">
          Psychology
        </Link>
        <nav className="public-nav">
          <Link to="/login">Войти</Link>
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
