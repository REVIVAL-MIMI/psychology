import { ReactNode } from "react";
import { Navigate } from "react-router-dom";
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

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (roles && auth && !roles.includes(auth.userRole)) {
    return <Navigate to="/app" replace />;
  }

  return <>{children}</>;
}
