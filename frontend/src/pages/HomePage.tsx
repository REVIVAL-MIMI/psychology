import { Link } from "react-router-dom";
import { companyProfile } from "../lib/branding";

export default function HomePage() {
  return (
    <div className="home">
      <section className="hero">
        <div className="hero-content">
          <div className="pill">Платформа поддержки</div>
          <h1>
            Психологическая поддержка сотрудников
            <br />
            {companyProfile.shortName} в онлайн-формате
          </h1>
          <p>
            Платформа объединяет дистанционные консультации, журнал самонаблюдения,
            рекомендации и чат в одном понятном интерфейсе.
          </p>
          <div className="hero-actions">
            <Link to="/login" className="button">Войти</Link>
            <Link to="/register/psychologist" className="button ghost">
              Подключить психолога
            </Link>
          </div>
          <div className="hero-note">
            Компания получает только агрегированную статистику программы. Содержимое консультаций,
            чата и личных записей остается доступным только сотруднику и психологу.
          </div>
          <div className="hero-badges">
            <span className="chip strong">Дистанционные консультации</span>
            <span className="chip">Персональные маршруты поддержки</span>
            <span className="chip">Координатор программы</span>
          </div>
        </div>
        <div className="hero-visual">
          <div className="glass-card hero-panel">
            <div className="glass-title">Программа поддержки</div>
            <div className="metric-grid">
              <div className="metric-card">
                <div className="metric-value">24/7</div>
                <div className="metric-label">доступ к кабинету сотрудника</div>
              </div>
              <div className="metric-card">
                <div className="metric-value">1</div>
                <div className="metric-label">единый контур для консультаций и рекомендаций</div>
              </div>
              <div className="metric-card">
                <div className="metric-value">0</div>
                <div className="metric-label">доступа работодателя к содержимому консультаций</div>
              </div>
              <div className="metric-card">
                <div className="metric-value">HR</div>
                <div className="metric-label">координация программы без нарушения конфиденциальности</div>
              </div>
            </div>
            <div className="glass-note">
              Платформа переводит сопровождение сотрудников из разрозненных переписок и таблиц
              в единый рабочий сервис с понятными ролями доступа.
            </div>
          </div>
        </div>
      </section>

      <section className="feature-grid">
        <div className="feature-card">
          <h3>Консультации онлайн</h3>
          <p>Сотрудники получают доступ к дистанционным консультациям и истории встреч в одном кабинете.</p>
        </div>
        <div className="feature-card">
          <h3>Координация программы</h3>
          <p>Психологи, сотрудники и координатор работают в одной системе с разделением прав доступа.</p>
        </div>
        <div className="feature-card">
          <h3>Обезличенная аналитика</h3>
          <p>Компания видит загрузку программы и активность сервиса без доступа к личному содержимому консультаций.</p>
        </div>
        <div className="feature-card">
          <h3>Поддержка в ритме бизнеса</h3>
          <p>Журнал, рекомендации и чат помогают сопровождать сотрудника между консультациями без перегрузки интерфейса.</p>
        </div>
      </section>

      <section className="home-flow">
        <h2>Как это работает</h2>
        <div className="flow-grid">
          <div className="flow-card">
            <div className="flow-step">01</div>
            <h3>Подключение специалистов</h3>
            <p>Психолог заполняет анкету и сразу получает доступ к рабочему кабинету.</p>
          </div>
          <div className="flow-card">
            <div className="flow-step">02</div>
            <h3>Онбординг сотрудников</h3>
            <p>Сотрудник активирует доступ по приглашению и сразу попадает в личный кабинет.</p>
          </div>
          <div className="flow-card">
            <div className="flow-step">03</div>
            <h3>Сопровождение и аналитика</h3>
            <p>Консультации, журнал и рекомендации доступны сотруднику, а координатор видит только операционные показатели программы.</p>
          </div>
        </div>
      </section>

      <section className="home-values">
        <div className="value-card">
          <div className="value-title">Конфиденциальность</div>
          <div className="value-text">Личные записи и содержание консультаций не раскрываются работодателю.</div>
        </div>
        <div className="value-card">
          <div className="value-title">Прозрачный контур</div>
          <div className="value-text">Каждая роль получает только те данные, которые нужны для ее задач.</div>
        </div>
        <div className="value-card">
          <div className="value-title">Стабильность</div>
          <div className="value-text">Консультации, напоминания и коммуникации собраны в одном сервисе без ручной координации.</div>
        </div>
        <div className="value-card">
          <div className="value-title">Фокус на людях</div>
          <div className="value-text">Интерфейс и процессы выстроены вокруг поддержки сотрудника в реальных рабочих ситуациях.</div>
        </div>
      </section>

      <section className="cta">
        <div>
          <h2>Запустить программу поддержки</h2>
          <p>
            Подключите психологов, откройте доступ сотрудникам и переведите дистанционные консультации
            в единый цифровой стандарт {companyProfile.shortName}.
          </p>
        </div>
        <Link to="/register/psychologist" className="button">
          Открыть платформу
        </Link>
      </section>
    </div>
  );
}
