export type UserRole = "ROLE_CLIENT" | "ROLE_PSYCHOLOGIST" | "ROLE_ADMIN";

export type AuthState = {
  accessToken: string;
  refreshToken: string;
  userId: number;
  userRole: UserRole;
  fullName: string;
  phone: string;
};

const STORAGE_KEY = "psychology.auth";

export function getStoredAuth(): AuthState | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthState;
  } catch {
    return null;
  }
}

export function setStoredAuth(auth: AuthState) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
}

export function clearStoredAuth() {
  localStorage.removeItem(STORAGE_KEY);
}

export function setRefreshCookie(refreshToken: string) {
  const maxAgeSeconds = 7 * 24 * 60 * 60;
  document.cookie = `refreshToken=${refreshToken}; path=/api/v1/auth/refresh; max-age=${maxAgeSeconds}; SameSite=Lax`;
}

export function clearRefreshCookie() {
  document.cookie = "refreshToken=; path=/api/v1/auth/refresh; max-age=0; SameSite=Lax";
}
