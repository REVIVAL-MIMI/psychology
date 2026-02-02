import { Link } from "react-router-dom";

export default function HomePage() {
  return (
    <div className="hero-card">
      <div className="eyebrow">Онлайн-платформа</div>
      <h1>Психолог и клиент — в одном защищенном пространстве.</h1>
      <p>
        Быстрый доступ к сеансам, рекомендациям, дневнику и чату. Минимум
        лишнего, максимум конфиденциальности.
      </p>
      <div className="hero-actions">
        <Link to="/login" className="button">Войти</Link>
        <Link to="/register/psychologist" className="button ghost">
          Зарегистрироваться как психолог
        </Link>
      </div>
      <div className="hero-note">
        Клиенты регистрируются только по приглашению от психолога.
      </div>
    </div>
  );
}
