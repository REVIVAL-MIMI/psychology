import type { UserRole } from "./storage";

export const companyProfile = {
  companyName: "ООО «Телеком без границ»",
  shortName: "ТЕЛЕКОМ БЕЗ ГРАНИЦ",
  platformName: "ТЕЛЕКОМ БЕЗ ГРАНИЦ",
  platformSubtitle: "Онлайн-платформа психологической поддержки",
  publicSite: "https://telecombg.ru/"
} as const;

export function getRoleLabel(role?: UserRole | string | null) {
  switch (role) {
    case "ROLE_CLIENT":
      return "Сотрудник";
    case "ROLE_PSYCHOLOGIST":
      return "Психолог";
    case "ROLE_ADMIN":
      return "Координатор";
    default:
      return "Пользователь";
  }
}

export function getRoleSubtitle(role?: UserRole | string | null) {
  switch (role) {
    case "ROLE_CLIENT":
      return "Личный кабинет";
    case "ROLE_PSYCHOLOGIST":
      return "Кабинет психолога";
    case "ROLE_ADMIN":
      return "Панель управления";
    default:
      return "Личный кабинет";
  }
}

const SESSION_STATUS_LABELS: Record<string, string> = {
  SCHEDULED: "Запланирована",
  CONFIRMED: "Подтверждена",
  IN_PROGRESS: "Идет сейчас",
  COMPLETED: "Завершена",
  CANCELLED: "Отменена",
  RESCHEDULED: "Перенесена"
};

export function getSessionStatusLabel(status?: string | null) {
  if (!status) return "Статус уточняется";
  return SESSION_STATUS_LABELS[status] ?? status;
}
