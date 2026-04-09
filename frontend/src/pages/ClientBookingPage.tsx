import { useEffect, useMemo, useState } from "react";
import { api } from "../lib/api";

type AvailableDay = {
  date: string;
  dayOfWeek: string;
  working: boolean;
  workStartHour: number | null;
  workEndHour: number | null;
  availableSlots: string[];
};

function formatDate(date: string) {
  return new Date(`${date}T00:00:00`).toLocaleDateString("ru-RU", {
    day: "2-digit",
    month: "long",
    weekday: "long"
  });
}

function formatTime(dateTime: string) {
  return new Date(dateTime).toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" });
}

export default function ClientBookingPage() {
  const [days, setDays] = useState<AvailableDay[]>([]);
  const [selectedSlot, setSelectedSlot] = useState("");
  const [problem, setProblem] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const selectedInfo = useMemo(() => {
    if (!selectedSlot) return "";
    const value = new Date(selectedSlot);
    return value.toLocaleString("ru-RU", {
      day: "2-digit",
      month: "long",
      hour: "2-digit",
      minute: "2-digit"
    });
  }, [selectedSlot]);

  const visibleDays = useMemo(
    () => days.filter((day) => day.working),
    [days]
  );

  const loadSlots = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<AvailableDay[]>("/sessions/available-slots?daysAhead=14");
      setDays(data ?? []);
    } catch {
      setError("Не удалось загрузить свободные даты. Попробуйте обновить страницу.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSlots();
  }, []);

  const book = async () => {
    if (!selectedSlot) return;
    if (!problem.trim()) {
      setError("Опишите, с каким запросом хотите прийти на консультацию.");
      return;
    }
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await api.post("/sessions/book", {
        scheduledAt: selectedSlot,
        description: problem.trim(),
        durationMinutes: 50
      });
      setSuccess("Вы записаны на консультацию. Слот сохранён в разделе «Консультации».");
      setSelectedSlot("");
      setProblem("");
      await loadSlots();
    } catch (e: any) {
      setError(e?.message || "Не удалось записаться в выбранный слот.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Запись на консультацию</h1>
        <p className="muted">Выберите удобный слот на ближайшие 14 дней. Базовое окно консультаций: 09:00-20:00.</p>
      </div>

      <div className="card">
        <h3>Ваш запрос</h3>
        <div className="muted">Кратко опишите тему обращения. Психолог увидит её в своём графике.</div>
        <textarea
          value={problem}
          onChange={(e) => setProblem(e.target.value)}
          placeholder="Например: высокий стресс, выгорание, сложности с концентрацией, конфликт в команде..."
          rows={4}
        />
      </div>

      {loading && <div className="muted">Загружаем свободные даты...</div>}
      {!loading && visibleDays.length === 0 && <div className="muted">Свободные даты пока не опубликованы.</div>}

      {!loading && visibleDays.length > 0 && (
        <div className="grid cards-grid-2">
          {visibleDays.map((day) => (
            <div className="card" key={day.date}>
              <div className="card-title">{formatDate(day.date)}</div>
              <div className="muted">
                {`Время приема: ${String(day.workStartHour ?? 9).padStart(2, "0")}:00 - ${String(day.workEndHour ?? 20).padStart(2, "0")}:00`}
              </div>

              {day.availableSlots.length === 0 && <div className="muted">Свободных слотов нет</div>}

              {day.availableSlots.length > 0 && (
                <div className="row">
                  {day.availableSlots.map((slot) => (
                    <button
                      key={slot}
                      className={selectedSlot === slot ? "button" : "button ghost"}
                      onClick={() => setSelectedSlot(slot)}
                      type="button"
                    >
                      {formatTime(slot)}
                    </button>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {selectedSlot && (
        <div className="card">
          <div className="card-title">Выбранный слот: {selectedInfo}</div>
        </div>
      )}

      {error && <div className="error">{error}</div>}
      {success && <div className="success">{success}</div>}

      <button className="button" disabled={!selectedSlot || saving} onClick={book}>
        {saving ? "Сохраняем..." : "Записаться"}
      </button>
    </div>
  );
}
