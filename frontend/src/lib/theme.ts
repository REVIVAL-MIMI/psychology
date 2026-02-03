export type Theme = "light" | "dark";

const STORAGE_KEY = "psychology.theme";

export function getStoredTheme(): Theme {
  if (typeof window === "undefined") return "light";
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === "light" || stored === "dark") return stored;
  const prefersDark = window.matchMedia?.("(prefers-color-scheme: dark)")?.matches;
  return prefersDark ? "dark" : "light";
}

export function applyTheme(theme: Theme) {
  if (typeof document === "undefined") return;
  document.documentElement.dataset.theme = theme;
}

export function setStoredTheme(theme: Theme) {
  if (typeof window !== "undefined") {
    localStorage.setItem(STORAGE_KEY, theme);
  }
  applyTheme(theme);
  return theme;
}

export function toggleTheme() {
  const next: Theme = getStoredTheme() === "dark" ? "light" : "dark";
  return setStoredTheme(next);
}
