import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api } from "./api";
import { AUTH_CHANGED_EVENT, AuthState, clearStoredAuth, getStoredAuth, isAccessTokenExpired, setStoredAuth } from "./storage";

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
    } else {
      clearStoredAuth();
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

  useEffect(() => {
    const syncAuth = () => {
      setAuthState(getStoredAuth());
    };

    const checkSession = () => {
      setAuthState((current) => {
        if (!current) return null;
        return isAccessTokenExpired(current.accessToken) ? null : current;
      });
      if (isAccessTokenExpired(getStoredAuth()?.accessToken)) {
        clearStoredAuth();
      }
    };

    window.addEventListener(AUTH_CHANGED_EVENT, syncAuth);
    const timer = window.setInterval(checkSession, 15000);

    return () => {
      window.removeEventListener(AUTH_CHANGED_EVENT, syncAuth);
      window.clearInterval(timer);
    };
  }, []);

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
