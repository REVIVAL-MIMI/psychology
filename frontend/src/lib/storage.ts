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
