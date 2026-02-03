import { useCallback, useEffect, useRef, useState } from "react";
import { NavLink, Outlet, Link, useLocation } from "react-router-dom";
import { useAuth } from "../lib/auth";
import { getStoredTheme, toggleTheme } from "../lib/theme";
import { api } from "../lib/api";

const navForRole = (role: string) => {
  if (role === "ROLE_PSYCHOLOGIST") {
    return [
      { to: "/app", label: "Дашборд", key: "dashboard" },
      { to: "/app/clients", label: "Клиенты", key: "clients" },
      { to: "/app/sessions", label: "Сеансы", key: "sessions" },
      { to: "/app/recommendations", label: "Рекомендации", key: "recommendations" },
      { to: "/app/chat", label: "Чат", key: "chat" },
      { to: "/app/invites", label: "Инвайты", key: "invites" }
    ];
  }
  if (role === "ROLE_ADMIN") {
    return [
      { to: "/app/admin", label: "Админ", key: "admin" }
    ];
  }
  return [
    { to: "/app", label: "Дашборд", key: "dashboard" },
    { to: "/app/sessions", label: "Сеансы", key: "sessions" },
    { to: "/app/journal", label: "Дневник", key: "journal" },
    { to: "/app/recommendations", label: "Рекомендации", key: "recommendations" },
    { to: "/app/chat", label: "Чат", key: "chat" }
  ];
};

export default function AppLayout() {
  const { auth, logout } = useAuth();
  const [theme, setTheme] = useState(getStoredTheme());
  const [unreadCount, setUnreadCount] = useState(0);
  const [navBadges, setNavBadges] = useState<{ chat?: number; recommendations?: number; sessions?: number }>({});
  const [latestCounts, setLatestCounts] = useState<{ chat: number; recommendations: number; sessions: number }>({
    chat: 0,
    recommendations: 0,
    sessions: 0
  });
  const seenCountsRef = useRef<{ chat: number; recommendations: number; sessions: number }>({
    chat: 0,
    recommendations: 0,
    sessions: 0
  });

  useEffect(() => {
    try {
      const raw = localStorage.getItem("psychology.seen");
      if (!raw) return;
      const parsed = JSON.parse(raw);
      seenCountsRef.current = {
        ...seenCountsRef.current,
        ...parsed
      };
    } catch {
      // ignore
    }
  }, []);
  const location = useLocation();

  if (!auth) return null;
  const nav = navForRole(auth.userRole);

  const handleToggle = () => setTheme(toggleTheme());

  useEffect(() => {
    if (auth?.userRole === "ROLE_PSYCHOLOGIST" && auth.verified === false) {
      setUnreadCount(0);
      setNavBadges({});
      return;
    }
    let mounted = true;
    const load = async () => {
      try {
        const data = await api.get<{ unreadCount: number }>("/notifications/unread/count");
        if (mounted) setUnreadCount(data.unreadCount ?? 0);
      } catch {
        if (mounted) setUnreadCount(0);
      }
    };
    load();
    const timer = window.setInterval(load, 30000);
    return () => {
      mounted = false;
      window.clearInterval(timer);
    };
  }, []);

  const loadBadges = useCallback(async () => {
    if (!auth) return;
    if (auth.userRole === "ROLE_PSYCHOLOGIST" && auth.verified === false) {
      setNavBadges({});
      return;
    }
    try {
      if (auth.userRole === "ROLE_CLIENT") {
        const unread = await api.get<{ unreadCount: number }>("/chat/unread/count");
        const recs = await api.get<any[]>("/recommendations?completed=false");
        const sessions = await api.get<any[]>("/sessions/client");
        const now = new Date();
        const upcoming = sessions.filter((s) => {
          if (!(s?.status === "SCHEDULED" || s?.status === "CONFIRMED")) return false;
          if (!s?.scheduledAt) return true;
          return new Date(s.scheduledAt) > now;
        });
        const counts = {
          chat: unread.unreadCount ?? 0,
          recommendations: recs?.length ?? 0,
          sessions: upcoming?.length ?? 0
        };
        setLatestCounts(counts);
        const seen = seenCountsRef.current;
        setNavBadges({
          chat: counts.chat > seen.chat ? counts.chat : 0,
          recommendations: counts.recommendations > seen.recommendations ? counts.recommendations : 0,
          sessions: counts.sessions > seen.sessions ? counts.sessions : 0
        });
      }
      if (auth.userRole === "ROLE_PSYCHOLOGIST") {
        const unread = await api.get<{ unreadCount: number }>("/chat/unread/count");
        const recStats = await api.get<any>("/recommendations/stats");
        const upcoming = await api.get<any[]>("/dashboard/psychologist/upcoming-sessions?daysAhead=7");
        const counts = {
          chat: unread.unreadCount ?? 0,
          recommendations: recStats?.pendingRecommendations ?? 0,
          sessions: upcoming?.length ?? 0
        };
        setLatestCounts(counts);
        const seen = seenCountsRef.current;
        setNavBadges({
          chat: counts.chat > seen.chat ? counts.chat : 0,
          recommendations: counts.recommendations > seen.recommendations ? counts.recommendations : 0,
          sessions: counts.sessions > seen.sessions ? counts.sessions : 0
        });
      }
    } catch {
      setNavBadges({});
    }
  }, [auth]);

  useEffect(() => {
    if (!auth) return;
    let mounted = true;
    const run = async () => {
      if (!mounted) return;
      await loadBadges();
    };
    run();
    const timer = window.setInterval(run, 30000);
    return () => {
      mounted = false;
      window.clearInterval(timer);
    };
  }, [auth?.userRole, loadBadges]);

  useEffect(() => {
    const next = { ...seenCountsRef.current };
    let changed = false;
    if (location.pathname.startsWith("/app/chat")) {
      next.chat = latestCounts.chat;
      changed = true;
    }
    if (location.pathname.startsWith("/app/recommendations")) {
      next.recommendations = latestCounts.recommendations;
      changed = true;
    }
    if (location.pathname.startsWith("/app/sessions")) {
      next.sessions = latestCounts.sessions;
      changed = true;
    }
    if (changed) {
      seenCountsRef.current = next;
      localStorage.setItem("psychology.seen", JSON.stringify(next));
      const seen = next;
      setNavBadges({
        chat: latestCounts.chat > seen.chat ? latestCounts.chat : 0,
        recommendations: latestCounts.recommendations > seen.recommendations ? latestCounts.recommendations : 0,
        sessions: latestCounts.sessions > seen.sessions ? latestCounts.sessions : 0
      });
    }
  }, [location.pathname, latestCounts]);

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [location.pathname]);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark">Ψ</span>
          <span>Psychology</span>
        </div>
        <div className="topbar-actions">
          {!(auth.userRole === "ROLE_PSYCHOLOGIST" && auth.verified === false) && (
            <>
              <Link to="/app/notifications" className="icon-button" aria-label="Уведомления">
                <span className="icon-bell" aria-hidden="true" />
                {unreadCount > 0 && <span className="badge-dot" />}
              </Link>
              <Link to="/app/profile" className="icon-button" aria-label="Профиль">
                <span className="icon-user" aria-hidden="true" />
              </Link>
            </>
          )}
          <div className="user-chip">
            <div className="user-chip-name">{auth.fullName}</div>
            <div className="user-chip-sub">{auth.userRole.replace("ROLE_", "")}</div>
          </div>
          <button className="button ghost theme-toggle" onClick={handleToggle}>
            {theme === "dark" ? "Свет" : "Тьма"}
          </button>
          <button className="button" onClick={logout}>Выйти</button>
        </div>
      </header>

      <div className="app-body">
        <aside className="side-rail">
          <div className="side-rail-header">
            <div className="role-chip">{auth.userRole.replace("ROLE_", "")}</div>
            <div className="user-meta">
              <div className="user-name">{auth.fullName}</div>
              <div className="user-phone">{auth.phone}</div>
            </div>
          </div>
          <nav className="nav-stack">
            {nav.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.key === "dashboard"}
                className={({ isActive }) => (isActive ? "nav-pill active" : "nav-pill")}
              >
                <span>{item.label}</span>
                {item.key === "chat" && (navBadges.chat ?? 0) > 0 && <span className="nav-dot" />}
                {item.key === "recommendations" && (navBadges.recommendations ?? 0) > 0 && <span className="nav-dot" />}
                {item.key === "sessions" && (navBadges.sessions ?? 0) > 0 && <span className="nav-dot" />}
              </NavLink>
            ))}
          </nav>
        </aside>

        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
