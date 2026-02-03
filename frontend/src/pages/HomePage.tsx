import { Link } from "react-router-dom";

export default function HomePage() {
  return (
    <div className="home">
      <section className="hero">
        <div className="hero-content">
          <div className="pill">Психологическая практика</div>
          <h1>
            Спокойная работа
            <br />
            с вниманием к человеку
          </h1>
          <p>
            Сеансы, дневник и рекомендации собраны в одном месте.
            Без лишнего — только то, что помогает вести практику и поддерживать клиента.
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
            <div className="glass-line">15:00 — Рекомендация</div>
          </div>
        </div>
      </section>

      <section className="feature-grid">
        <div className="feature-card">
          <h3>Простой вход</h3>
          <p>Доступ без лишних шагов и перегрузки.</p>
        </div>
        <div className="feature-card">
          <h3>Структура</h3>
          <p>Сеансы, дневник и рекомендации — в единой логике.</p>
        </div>
        <div className="feature-card">
          <h3>Конфиденциальность</h3>
          <p>Доступ к данным только у клиента и специалиста.</p>
        </div>
      </section>

      <section className="home-flow">
        <h2>Как устроен процесс</h2>
        <div className="flow-grid">
          <div className="flow-card">
            <div className="flow-step">01</div>
            <h3>Профиль специалиста</h3>
            <p>Психолог заполняет данные и ожидает подтверждения.</p>
          </div>
          <div className="flow-card">
            <div className="flow-step">02</div>
            <h3>Приглашение клиента</h3>
            <p>Клиент получает доступ по приглашению и подтверждает номер.</p>
          </div>
          <div className="flow-card">
            <div className="flow-step">03</div>
            <h3>Совместная работа</h3>
            <p>Сеансы, заметки и рекомендации — в одном спокойном ритме.</p>
          </div>
        </div>
      </section>

      <section className="home-values">
        <div className="value-card">
          <div className="value-title">Спокойствие</div>
          <div className="value-text">Ровный тон и ясная структура.</div>
        </div>
        <div className="value-card">
          <div className="value-title">Границы</div>
          <div className="value-text">Только то, что нужно для работы.</div>
        </div>
        <div className="value-card">
          <div className="value-title">Доверие</div>
          <div className="value-text">Данные доступны только участникам процесса.</div>
        </div>
      </section>

      <section className="cta">
        <div>
          <h2>Начать работу</h2>
          <p>Создайте профиль и выстройте спокойный рабочий процесс.</p>
        </div>
        <Link to="/register/psychologist" className="button">
          Создать профиль
        </Link>
      </section>
    </div>
  );
}
