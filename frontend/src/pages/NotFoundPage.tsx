import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="card">
      <h2>Страница не найдена</h2>
      <p>Проверьте адрес или вернитесь на главную.</p>
      <Link to="/" className="button">На главную</Link>
    </div>
  );
}
