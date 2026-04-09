import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../lib/auth";
import { getSessionStatusLabel } from "../lib/branding";

const STATUS_OPTIONS = [
  "SCHEDULED",
  "CONFIRMED",
  "IN_PROGRESS",
  "COMPLETED",
  "CANCELLED",
  "RESCHEDULED"
] as const;

const ACTIVE_BOOKING_STATUSES = [
  "SCHEDULED",
  "CONFIRMED",
  "IN_PROGRESS",
  "RESCHEDULED"
] as const;
const ACTIVE_BOOKING_STATUS_SET = new Set<string>(ACTIVE_BOOKING_STATUSES);

type SessionStatus = (typeof STATUS_OPTIONS)[number];

type SessionPerson = {
  id: number;
  fullName: string;
};

type SessionItem = {
  id: number;
  scheduledAt: string;
  durationMinutes: number;
  description?: string | null;
  status: string;
  client?: SessionPerson | null;
  psychologist?: SessionPerson | null;
};

type ClientOption = {
  id: number;
  fullName: string;
};

type AvailableDay = {
  date: string;
  dayOfWeek: string;
  working: boolean;
  workStartHour: number | null;
  workEndHour: number | null;
  availableSlots: string[];
};

type ScheduleBooking = {
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
  bookings: ScheduleBooking[];
};

type DayEditState = {
  working: boolean;
  workStartHour: number;
  workEndHour: number;
};

function initScheduleEdits(days: ScheduleDay[]) {
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

function toDateTimeInputValue(value?: string | null) {
  if (!value) return "";
  return value.length >= 16 ? value.slice(0, 16) : value;
}

function formatDate(value: string) {
  return new Date(`${value}T00:00:00`).toLocaleDateString("ru-RU", {
    day: "2-digit",
    month: "long",
    weekday: "long"
  });
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("ru-RU", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function extractErrorMessage(error: unknown, fallback: string) {
  if (error && typeof error === "object" && "payload" in error) {
    const payload = (error as { payload?: unknown }).payload;
    if (payload && typeof payload === "object" && "message" in payload) {
      const message = (payload as { message?: unknown }).message;
      if (typeof message === "string" && message.trim()) {
        return message;
      }
    }
    if (typeof payload === "string" && payload.trim()) {
      return payload;
    }
  }
  return fallback;
}

export default function SessionsPage() {
  const { auth } = useAuth();
  const [sessions, setSessions] = useState<SessionItem[]>([]);
  const [clients, setClients] = useState<ClientOption[]>([]);
  const [form, setForm] = useState({ clientId: "", scheduledAt: "", durationMinutes: "50", description: "" });
  const [filters, setFilters] = useState({ from: "", to: "", status: "" });
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<{ scheduledAt: string; durationMinutes: string; description: string; status: SessionStatus }>({
    scheduledAt: "",
    durationMinutes: "50",
    description: "",
    status: "SCHEDULED"
  });

  const [scheduleDays, setScheduleDays] = useState<ScheduleDay[]>([]);
  const [scheduleEdits, setScheduleEdits] = useState<Record<string, DayEditState>>({});
  const [scheduleLoading, setScheduleLoading] = useState(false);
  const [savingDate, setSavingDate] = useState<string | null>(null);

  const [slotDays, setSlotDays] = useState<AvailableDay[]>([]);
  const [slotLoading, setSlotLoading] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState("");
  const [problem, setProblem] = useState("");
  const [bookingSaving, setBookingSaving] = useState(false);

  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const isPsychologist = auth?.userRole === "ROLE_PSYCHOLOGIST";
  const isClient = auth?.userRole === "ROLE_CLIENT";
  const canWorkWithSessions = isPsychologist || isClient;

  const query = useMemo(() => {
    const params = new URLSearchParams();
    if (filters.from) params.set("from", filters.from);
    if (filters.to) params.set("to", filters.to);
    return params.toString();
  }, [filters]);

  const visibleSlotDays = useMemo(
    () => slotDays.filter((day) => day.working),
    [slotDays]
  );

  const selectedSlotInfo = useMemo(() => {
    if (!selectedSlot) return "";
    return new Date(selectedSlot).toLocaleString("ru-RU", {
      day: "2-digit",
      month: "long",
      hour: "2-digit",
      minute: "2-digit"
    });
  }, [selectedSlot]);

  const nextBookedSession = useMemo(() => {
    if (!isClient) return null;
    const now = new Date();
    return sessions
      .filter((session) => ACTIVE_BOOKING_STATUS_SET.has(session.status))
      .filter((session) => {
        const time = new Date(session.scheduledAt);
        return !Number.isNaN(time.getTime()) && time.getTime() >= now.getTime();
      })
      .sort((a, b) => new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime())[0] ?? null;
  }, [isClient, sessions]);

  useEffect(() => {
    if (!canWorkWithSessions) return;
    const endpoint = isPsychologist ? "/sessions/psychologist" : "/sessions/client";
    const url = query ? `${endpoint}?${query}` : endpoint;

    api.get<SessionItem[]>(url)
      .then((data) => {
        if (filters.status) {
          setSessions(data.filter((s) => s.status === filters.status));
        } else {
          setSessions(data);
        }
      })
      .catch((e) => {
        setError(extractErrorMessage(e, "Не удалось загрузить консультации."));
      });
  }, [canWorkWithSessions, filters.status, isPsychologist, query]);

  const loadPsychologistSchedule = useCallback(async () => {
    if (!isPsychologist) return;
    setScheduleLoading(true);
    try {
      const data = await api.get<ScheduleDay[]>("/sessions/my-schedule?daysAhead=14");
      const next = data ?? [];
      setScheduleDays(next);
      setScheduleEdits(initScheduleEdits(next));
    } catch (e) {
      setError(extractErrorMessage(e, "Не удалось загрузить график."));
    } finally {
      setScheduleLoading(false);
    }
  }, [isPsychologist]);

  const loadClientSlots = useCallback(async () => {
    if (!isClient) return;
    setSlotLoading(true);
    try {
      const data = await api.get<AvailableDay[]>("/sessions/available-slots?daysAhead=14");
      setSlotDays(data ?? []);
    } catch (e) {
      setError(extractErrorMessage(e, "Не удалось загрузить слоты для записи."));
    } finally {
      setSlotLoading(false);
    }
  }, [isClient]);

  useEffect(() => {
    if (isPsychologist) {
      api.get<ClientOption[]>("/clients").then(setClients).catch(() => setClients([]));
      void loadPsychologistSchedule();
    }
  }, [isPsychologist, loadPsychologistSchedule]);

  useEffect(() => {
    if (isClient) {
      void loadClientSlots();
    }
  }, [isClient, loadClientSlots]);

  const refreshAfterSessionChange = async () => {
    if (!canWorkWithSessions) return;
    const endpoint = isPsychologist ? "/sessions/psychologist" : "/sessions/client";
    const url = query ? `${endpoint}?${query}` : endpoint;

    const loaded = await api.get<SessionItem[]>(url);
    setSessions(filters.status ? loaded.filter((s) => s.status === filters.status) : loaded);

    if (isPsychologist) {
      await loadPsychologistSchedule();
    }
    if (isClient) {
      await loadClientSlots();
    }
  };

  const createSession = async () => {
    if (!isPsychologist) return;
    setError(null);
    setSuccess(null);
    try {
      await api.post("/sessions", {
        clientId: Number(form.clientId),
        scheduledAt: form.scheduledAt,
        durationMinutes: Number(form.durationMinutes),
        description: form.description
      });
      setForm({ clientId: "", scheduledAt: "", durationMinutes: "50", description: "" });
      setSuccess("Консультация добавлена в расписание.");
      await refreshAfterSessionChange();
    } catch (e) {
      setError(extractErrorMessage(e, "Не удалось создать консультацию."));
    }
  };

  const cancelSession = async (id: number) => {
    setError(null);
    setSuccess(null);
    try {
      await api.post(`/sessions/${id}/cancel`);
      setSuccess("Консультация отменена.");
      await refreshAfterSessionChange();
    } catch (e) {
      setError(extractErrorMessage(e, "Не удалось отменить консультацию."));
    }
  };

  const openEdit = (session: SessionItem) => {
    setEditingId(session.id);
    setEditForm({
      scheduledAt: toDateTimeInputValue(session.scheduledAt),
      durationMinutes: String(session.durationMinutes ?? 50),
      description: session.description ?? "",
      status: STATUS_OPTIONS.includes(session.status as SessionStatus)
        ? (session.status as SessionStatus)
        : "SCHEDULED"
    });
  };

  const updateSession = async () => {
    if (!editingId || !isPsychologist) return;
    setError(null);
    setSuccess(null);
    try {
      await api.put(`/sessions/${editingId}`, {
        scheduledAt: editForm.scheduledAt,
        durationMinutes: Number(editForm.durationMinutes),
        description: editForm.description,
        status: editForm.status
      });
      setEditingId(null);
      setSuccess("Изменения сохранены.");
      await refreshAfterSessionChange();
    } catch (e) {
      setError(extractErrorMessage(e, "Не удалось обновить консультацию."));
    }
  };

  const saveDay = async (date: string) => {
    if (!isPsychologist) return;
    const dayState = scheduleEdits[date];
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
      setScheduleDays(updated ?? []);
      setScheduleEdits(initScheduleEdits(updated ?? []));
      setSuccess("График обновлен.");
    } catch (e) {
      setError(extractErrorMessage(e, "Не удалось сохранить день графика."));
    } finally {
      setSavingDate(null);
    }
  };

  const bookByClient = async () => {
    if (!isClient) return;
    if (!selectedSlot) return;
    if (!problem.trim()) {
      setError("Опишите, с каким запросом хотите прийти на консультацию.");
      return;
    }

    setBookingSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await api.post("/sessions/book", {
        scheduledAt: selectedSlot,
        description: problem.trim(),
        durationMinutes: 50
      });
      setSelectedSlot("");
      setProblem("");
      setSuccess("Вы записаны на консультацию.");
      await refreshAfterSessionChange();
    } catch (e) {
      setError(extractErrorMessage(e, "Не удалось записаться в выбранный слот."));
    } finally {
      setBookingSaving(false);
    }
  };

  if (!auth) return null;

  if (!canWorkWithSessions) {
    return (
      <div className="page">
        <div className="page-header">
          <h1>Консультации</h1>
          <p className="muted">Раздел доступен сотруднику и психологу.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <h1>{isPsychologist ? "График и консультации" : "Запись и консультации"}</h1>
        <p className="muted">
          {isPsychologist
            ? "Управляйте рабочими днями и записями сотрудников в одном месте."
            : "Выбирайте удобный слот и отслеживайте все консультации на одной странице."}
        </p>
      </div>

      {error && <div className="error">{error}</div>}
      {success && <div className="success">{success}</div>}

      {isPsychologist && (
        <div className="card">
          <h3>Быстрая запись сотрудника</h3>
          <div className="form grid-2">
            <label>
              Сотрудник
              <select value={form.clientId} onChange={(e) => setForm({ ...form, clientId: e.target.value })}>
                <option value="">Выберите сотрудника</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>{c.fullName}</option>
                ))}
              </select>
            </label>
            <label>
              Дата и время
              <input type="datetime-local" value={form.scheduledAt} onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })} />
            </label>
            <label>
              Длительность (мин)
              <input type="number" value={form.durationMinutes} onChange={(e) => setForm({ ...form, durationMinutes: e.target.value })} />
            </label>
            <label>
              Описание
              <input
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="Краткая цель встречи"
              />
            </label>
          </div>
          <button className="button" onClick={createSession} disabled={!form.clientId || !form.scheduledAt}>Запланировать</button>
        </div>
      )}

      {isPsychologist && (
        <div className="section">
          <div className="section-header">
            <div>
              <div className="section-title">Мой график на 2 недели</div>
              <div className="section-note">Отметьте рабочие дни и часы. Здесь же видны записи сотрудников.</div>
            </div>
          </div>

          {scheduleLoading && <div className="muted">Загружаем график...</div>}

          {!scheduleLoading && (
            <div className="grid cards-grid-2">
              {scheduleDays.map((day) => {
                const edit = scheduleEdits[day.date] ?? {
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
                            setScheduleEdits((prev) => ({
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
                                setScheduleEdits((prev) => ({
                                  ...prev,
                                  [day.date]: { ...edit, workStartHour: Number(e.target.value) }
                                }))
                              }
                            >
                              {Array.from({ length: 24 }, (_, i) => i).map((hour) => (
                                <option key={`start-${day.date}-${hour}`} value={hour}>
                                  {String(hour).padStart(2, "0")}:00
                                </option>
                              ))}
                            </select>
                          </label>
                          <label className="schedule-hour">
                            До
                            <select
                              value={edit.workEndHour}
                              onChange={(e) =>
                                setScheduleEdits((prev) => ({
                                  ...prev,
                                  [day.date]: { ...edit, workEndHour: Number(e.target.value) }
                                }))
                              }
                            >
                              {Array.from({ length: 24 }, (_, i) => i).map((hour) => (
                                <option key={`end-${day.date}-${hour}`} value={hour}>
                                  {String(hour).padStart(2, "0")}:00
                                </option>
                              ))}
                              <option value={24}>24:00</option>
                            </select>
                          </label>
                        </>
                      )}
                    </div>

                    <button
                      className="button"
                      type="button"
                      onClick={() => saveDay(day.date)}
                      disabled={savingDate === day.date}
                    >
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
      )}

      {isClient && (
        <div className="card">
          <h3>Запись к психологу</h3>
          <p className="muted">Выберите свободный слот на ближайшие 14 дней и укажите тему запроса.</p>

          {nextBookedSession && (
            <div className="info-banner">
              Ближайшая запись: <strong>{formatDateTime(nextBookedSession.scheduledAt)}</strong>
            </div>
          )}

          <label>
            Ваш запрос
            <textarea
              value={problem}
              onChange={(e) => setProblem(e.target.value)}
              placeholder="Например: тревожность, стресс, выгорание, конфликт в команде..."
              rows={4}
            />
          </label>

          {slotLoading && <div className="muted">Загружаем свободные слоты...</div>}
          {!slotLoading && visibleSlotDays.length === 0 && <div className="muted">Свободные даты пока не опубликованы.</div>}

          {!slotLoading && visibleSlotDays.length > 0 && (
            <div className="grid cards-grid-2">
              {visibleSlotDays.map((day) => (
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
                          {new Date(slot).toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" })}
                        </button>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {selectedSlot && (
            <div className="info-banner">
              Вы выбрали: <strong>{selectedSlotInfo}</strong>
            </div>
          )}

          <button className="button" type="button" onClick={bookByClient} disabled={!selectedSlot || bookingSaving}>
            {bookingSaving ? "Сохраняем..." : "Записаться"}
          </button>
        </div>
      )}

      <div className="section">
        <div className="section-header">
          <div>
            <div className="section-title">{isPsychologist ? "Все консультации" : "Мои консультации"}</div>
            <div className="section-note">Ближайшие и завершенные встречи</div>
          </div>
          <div className="section-meta">{sessions.length} записей</div>
        </div>

        <div className="filter-bar compact">
          <div className="filter-group">
            <label>
              С даты
              <input type="datetime-local" value={filters.from} onChange={(e) => setFilters({ ...filters, from: e.target.value })} />
            </label>
            <label>
              До даты
              <input type="datetime-local" value={filters.to} onChange={(e) => setFilters({ ...filters, to: e.target.value })} />
            </label>
            <label>
              Статус
              <select value={filters.status} onChange={(e) => setFilters({ ...filters, status: e.target.value })}>
                <option value="">Все</option>
                {STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>{getSessionStatusLabel(status)}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="filter-actions">
            <button className="button ghost" onClick={() => setFilters({ from: "", to: "", status: "" })}>Сбросить</button>
          </div>
        </div>

        <div className="panel list-panel">
          <ul className="list">
            {sessions.map((session) => (
              <li key={session.id} className="list-row">
                <div>
                  <div className="card-title">
                    {new Date(session.scheduledAt).toLocaleString()} — {isPsychologist
                      ? (session.client?.fullName ?? "Сотрудник")
                      : (session.psychologist?.fullName ?? "Психолог")}
                  </div>
                  <div className="muted">{session.description ?? ""}</div>
                </div>
                <div className="row">
                  <span className={`badge ${session.status}`}>{getSessionStatusLabel(session.status)}</span>
                  {isPsychologist && (
                    <button className="button ghost" onClick={() => openEdit(session)}>Изменить</button>
                  )}
                  {(session.status === "SCHEDULED" || session.status === "CONFIRMED") && (
                    <button className="button ghost" onClick={() => cancelSession(session.id)}>
                      {isClient ? "Отменить запись" : "Отменить"}
                    </button>
                  )}
                </div>
              </li>
            ))}
            {sessions.length === 0 && (
              <li className="list-row">
                <div className="muted">Пока нет консультаций по выбранным фильтрам.</div>
              </li>
            )}
          </ul>
        </div>
      </div>

      {isPsychologist && editingId && (
        <div className="card">
          <h3>Перенос или обновление консультации</h3>
          <div className="form grid-2">
            <label>
              Новая дата и время
              <input type="datetime-local" value={editForm.scheduledAt} onChange={(e) => setEditForm({ ...editForm, scheduledAt: e.target.value })} />
            </label>
            <label>
              Длительность (мин)
              <input type="number" value={editForm.durationMinutes} onChange={(e) => setEditForm({ ...editForm, durationMinutes: e.target.value })} />
            </label>
            <label>
              Описание
              <input value={editForm.description} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} />
            </label>
            <label>
              Статус
              <select
                value={editForm.status}
                onChange={(e) => {
                  const nextStatus = e.target.value as SessionStatus;
                  setEditForm({
                    ...editForm,
                    status: STATUS_OPTIONS.includes(nextStatus) ? nextStatus : "SCHEDULED"
                  });
                }}
              >
                {STATUS_OPTIONS.map((status) => (
                  <option key={status} value={status}>{getSessionStatusLabel(status)}</option>
                ))}
              </select>
            </label>
          </div>
          <div className="row">
            <button className="button" onClick={updateSession}>Сохранить</button>
            <button className="button ghost" onClick={() => setEditingId(null)}>Отмена</button>
          </div>
        </div>
      )}
    </div>
  );
}
