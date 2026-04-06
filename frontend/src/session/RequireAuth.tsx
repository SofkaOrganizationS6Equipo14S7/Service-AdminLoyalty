import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./AuthProvider";

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const { state, bootstrapReady } = useAuth();
  const location = useLocation();

  if (!bootstrapReady) {
    return <div className="page"><p>Cargando sesión...</p></div>;
  }

  if (!state.user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}

