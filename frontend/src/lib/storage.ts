export type UserRole = "ROLE_CLIENT" | "ROLE_PSYCHOLOGIST" | "ROLE_ADMIN";

export type AuthState = {
  accessToken: string;
  userId: number;
  userRole: UserRole;
  fullName: string;
  phone: string;
  verified?: boolean;
};

const STORAGE_KEY = "psychology.auth";
export const AUTH_CHANGED_EVENT = "psychology-auth-changed";

export function isAccessTokenExpired(token?: string | null) {
  if (!token) return true;
  try {
    const [, payload] = token.split(".");
    if (!payload) return true;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = JSON.parse(atob(normalized));
    if (!decoded?.exp) return true;
    return Date.now() >= Number(decoded.exp) * 1000;
  } catch {
    return true;
  }
}

export function getStoredAuth(): AuthState | null {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as AuthState;
    if (isAccessTokenExpired(parsed.accessToken)) {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

export function setStoredAuth(auth: AuthState) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}

export function clearStoredAuth() {
  localStorage.removeItem(STORAGE_KEY);
  window.dispatchEvent(new Event(AUTH_CHANGED_EVENT));
}
