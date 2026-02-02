import React, { createContext, useCallback, useContext, useMemo, useState } from "react";
import { api } from "./api";
import {
  AuthState,
  clearRefreshCookie,
  clearStoredAuth,
  getStoredAuth,
  setRefreshCookie,
  setStoredAuth
} from "./storage";

export type AuthContextValue = {
  auth: AuthState | null;
  isAuthenticated: boolean;
  setAuth: (auth: AuthState | null) => void;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuthState] = useState<AuthState | null>(() => getStoredAuth());

  const setAuth = useCallback((next: AuthState | null) => {
    setAuthState(next);
    if (next) {
      setStoredAuth(next);
      setRefreshCookie(next.refreshToken);
    } else {
      clearStoredAuth();
      clearRefreshCookie();
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post("/auth/logout");
    } catch {
      // ignore
    } finally {
      setAuth(null);
    }
  }, [setAuth]);

  const value = useMemo<AuthContextValue>(
    () => ({ auth, isAuthenticated: Boolean(auth?.accessToken), setAuth, logout }),
    [auth, logout, setAuth]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
