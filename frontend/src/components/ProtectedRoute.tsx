import { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../lib/auth";
import { UserRole } from "../lib/storage";

export function ProtectedRoute({
  children,
  roles
}: {
  children: ReactNode;
  roles?: UserRole[];
}) {
  const { isAuthenticated, auth } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (
    auth?.userRole === "ROLE_PSYCHOLOGIST" &&
    auth.verified === false &&
    location.pathname !== "/app/pending"
  ) {
    return <Navigate to="/app/pending" replace />;
  }

  if (roles && auth && !roles.includes(auth.userRole)) {
    return <Navigate to="/app" replace />;
  }

  return <>{children}</>;
}
