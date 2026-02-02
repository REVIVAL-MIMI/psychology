import { Link } from "react-router-dom";

export default function HomePage() {
  return (
    <div className="home">
      <section className="hero">
        <div className="hero-content">
          <div className="pill">Платформа для психологов и клиентов</div>
          <h1>
            Спокойное пространство
            <br />
            для терапии онлайн
          </h1>
          <p>
            Сеансы, рекомендации, дневник и чат — в одной защищенной среде.
            Без лишнего, с фокусом на доверии и ритуале терапии.
          </p>
          <div className="hero-actions">
            <Link to="/login" className="button">Войти</Link>
            <Link to="/register/psychologist" className="button ghost">
              Регистрация психолога
            </Link>
          </div>
          <div className="hero-note">
            Клиенты регистрируются только по приглашению.
          </div>
        </div>
        <div className="hero-visual">
          <div className="glass-card">
            <div className="glass-title">Сегодня</div>
            <div className="glass-line">10:30 — Сеанс с клиентом</div>
            <div className="glass-line">12:00 — Запись в дневнике</div>
            <div className="glass-line">15:00 — Рекомендация активна</div>
          </div>
          <div className="orb orb-1" />
          <div className="orb orb-2" />
        </div>
      </section>

      <section className="feature-grid">
        <div className="feature-card">
          <h3>Простой вход</h3>
          <p>OTP по номеру телефона, без паролей и лишних шагов.</p>
        </div>
        <div className="feature-card">
          <h3>Терапия в фокусе</h3>
          <p>Планирование сеансов, заметки, история и рекомендации.</p>
        </div>
        <div className="feature-card">
          <h3>Конфиденциальность</h3>
          <p>Разделение ролей и доступ только к своим данным.</p>
        </div>
      </section>

      <section className="cta">
        <div>
          <h2>Готовы начать?</h2>
          <p>Создайте профиль психолога и начните работать с клиентами.</p>
        </div>
        <Link to="/register/psychologist" className="button">
          Создать профиль
        </Link>
      </section>
    </div>
  );
}
