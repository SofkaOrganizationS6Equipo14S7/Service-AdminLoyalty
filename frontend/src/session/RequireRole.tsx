import { Navigate } from "react-router-dom";
import { hasAnyRole } from "../../auth-core";
import { useAuth } from "./AuthProvider";

export function RequireRole({
  roles,
  children,
}: {
  roles: string[];
  children: React.ReactNode;
}) {
  const { state } = useAuth();

  if (!hasAnyRole(state.user, roles)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}

