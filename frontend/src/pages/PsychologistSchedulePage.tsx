import { useEffect, useMemo, useState } from "react";
import { api } from "../lib/api";
import { getSessionStatusLabel } from "../lib/branding";

type Booking = {
  sessionId: number;
  scheduledAt: string;
  durationMinutes: number;
  status: string;
  clientId: number;
  clientName: string;
  problemDescription: string;
};

type ScheduleDay = {
  date: string;
  dayOfWeek: string;
  working: boolean;
  workStartHour: number;
  workEndHour: number;
  bookings: Booking[];
};

type DayEditState = {
  working: boolean;
  workStartHour: number;
  workEndHour: number;
};

function formatDate(date: string) {
  return new Date(`${date}T00:00:00`).toLocaleDateString("ru-RU", {
    day: "2-digit",
    month: "long",
    weekday: "long"
  });
}

function formatDateTime(dateTime: string) {
  return new Date(dateTime).toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function initEdits(days: ScheduleDay[]) {
  const result: Record<string, DayEditState> = {};
  for (const day of days) {
    result[day.date] = {
      working: Boolean(day.working),
      workStartHour: day.workStartHour ?? 9,
      workEndHour: day.workEndHour ?? 20
    };
  }
  return result;
}

export default function PsychologistSchedulePage() {
  const [days, setDays] = useState<ScheduleDay[]>([]);
  const [edits, setEdits] = useState<Record<string, DayEditState>>({});
  const [loading, setLoading] = useState(true);
  const [savingDate, setSavingDate] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const hourOptions = useMemo(() => Array.from({ length: 24 }, (_, i) => i), []);

  const loadSchedule = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.get<ScheduleDay[]>("/sessions/my-schedule?daysAhead=14");
      setDays(data ?? []);
      setEdits(initEdits(data ?? []));
    } catch {
      setError("Не удалось загрузить график психолога.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSchedule();
  }, []);

  const saveDay = async (date: string) => {
    const dayState = edits[date];
    if (!dayState) return;
    setSavingDate(date);
    setError(null);
    setSuccess(null);
    try {
      const updated = await api.put<ScheduleDay[]>("/sessions/availability?daysAhead=14", {
        days: [
          {
            date,
            working: dayState.working,
            workStartHour: dayState.workStartHour,
            workEndHour: dayState.workEndHour
          }
        ]
      });
      setDays(updated ?? []);
      setEdits(initEdits(updated ?? []));
      setSuccess("График обновлён.");
    } catch (e: any) {
      setError(e?.message || "Не удалось сохранить день графика.");
    } finally {
      setSavingDate(null);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <h1>Мой график</h1>
        <p className="muted">Выберите рабочие дни и часы на 2 недели вперёд.</p>
      </div>

      {error && <div className="error">{error}</div>}
      {success && <div className="success">{success}</div>}
      {loading && <div className="muted">Загружаем график...</div>}

      {!loading && (
        <div className="grid cards-grid-2">
          {days.map((day) => {
            const edit = edits[day.date] ?? {
              working: Boolean(day.working),
              workStartHour: day.workStartHour ?? 9,
              workEndHour: day.workEndHour ?? 20
            };
            return (
              <div className="card" key={day.date}>
                <div className="card-title">{formatDate(day.date)}</div>

                <div className="schedule-controls">
                  <label className="schedule-working-toggle">
                    <input
                      type="checkbox"
                      checked={edit.working}
                      onChange={(e) =>
                        setEdits((prev) => ({
                          ...prev,
                          [day.date]: { ...edit, working: e.target.checked }
                        }))
                      }
                    />
                    Рабочий
                  </label>

                  {edit.working && (
                    <>
                      <label className="schedule-hour">
                        С
                        <select
                          value={edit.workStartHour}
                          onChange={(e) =>
                            setEdits((prev) => ({
                              ...prev,
                              [day.date]: { ...edit, workStartHour: Number(e.target.value) }
                            }))
                          }
                        >
                          {hourOptions.map((h) => (
                            <option key={`start-${day.date}-${h}`} value={h}>
                              {String(h).padStart(2, "0")}:00
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="schedule-hour">
                        До
                        <select
                          value={edit.workEndHour}
                          onChange={(e) =>
                            setEdits((prev) => ({
                              ...prev,
                              [day.date]: { ...edit, workEndHour: Number(e.target.value) }
                            }))
                          }
                        >
                          {hourOptions.map((h) => (
                            <option key={`end-${day.date}-${h}`} value={h}>
                              {String(h).padStart(2, "0")}:00
                            </option>
                          ))}
                          <option value={24}>24:00</option>
                        </select>
                      </label>
                    </>
                  )}
                </div>

                <button className="button" type="button" onClick={() => saveDay(day.date)} disabled={savingDate === day.date}>
                  {savingDate === day.date ? "Сохраняем..." : "Сохранить день"}
                </button>

                <div className="section">
                  <div className="section-title">Записи сотрудников</div>
                  {day.bookings.length === 0 && <div className="muted">На этот день записей нет.</div>}
                  {day.bookings.length > 0 && (
                    <ul className="list">
                      {day.bookings.map((booking) => (
                        <li className="list-row" key={booking.sessionId}>
                          <div>
                            <div className="card-title">{formatDateTime(booking.scheduledAt)} — {booking.clientName}</div>
                            <div className="muted">{booking.problemDescription || "Запрос не указан"}</div>
                          </div>
                          <span className={`badge ${booking.status}`}>{getSessionStatusLabel(booking.status)}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
