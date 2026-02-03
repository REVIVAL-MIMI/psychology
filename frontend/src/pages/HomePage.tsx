import { Link } from "react-router-dom";

export default function HomePage() {
  return (
    <div className="home">
      <section className="hero">
        <div className="hero-content">
          <div className="pill">Практика и дисциплина</div>
          <h1>
            Тихое пространство
            <br />
            для устойчивой терапии
          </h1>
          <p>
            Сеансы, рекомендации, дневник и чат — в ясной структуре.
            Без шума, с опорой на ритм практики и доверие.
          </p>
          <div className="hero-actions">
            <Link to="/login" className="button">Войти</Link>
            <Link to="/register/psychologist" className="button ghost">
              Регистрация психолога
            </Link>
          </div>
          <div className="hero-note">
            Доступ клиентам открывается только по приглашению специалиста.
          </div>
        </div>
        <div className="hero-visual">
          <div className="glass-card">
            <div className="glass-title">Сегодня</div>
            <div className="glass-line">10:30 — Сеанс с клиентом</div>
            <div className="glass-line">12:00 — Запись в дневнике</div>
            <div className="glass-line">15:00 — Рекомендация в работе</div>
          </div>
          <div className="orb orb-1" />
          <div className="orb orb-2" />
        </div>
      </section>

      <section className="feature-grid">
        <div className="feature-card">
          <h3>Ясный вход</h3>
          <p>OTP по номеру телефона — без паролей и лишних действий.</p>
        </div>
        <div className="feature-card">
          <h3>Фокус на практике</h3>
          <p>Сеансы, дневник и рекомендации — в одном спокойном ритме.</p>
        </div>
        <div className="feature-card">
          <h3>Границы и доверие</h3>
          <p>Доступ только к своим данным и строгие роли.</p>
        </div>
      </section>

      <section className="cta">
        <div>
          <h2>Начать практику</h2>
          <p>Создайте профиль и выстройте устойчивый рабочий ритм.</p>
        </div>
        <Link to="/register/psychologist" className="button">
          Создать профиль
        </Link>
      </section>
    </div>
  );
}
