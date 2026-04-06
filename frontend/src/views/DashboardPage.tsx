import { Link } from "react-router-dom";
import { useMemo } from "react";
import { useAuth } from "../session/AuthProvider";

export function DashboardPage() {
  const { state, logout } = useAuth();
  const user = state.user;

  const roleLabel = useMemo(() => user?.roleName ?? "N/A", [user]);

  return (
    <div className="page">
      <div className="card">
        <div className="row">
          <h1>Dashboard</h1>
          <button onClick={() => logout()}>Cerrar sesión</button>
        </div>
        <p>Sesión activa sobre service-admin.</p>
        <ul className="meta">
          <li>
            <strong>Usuario:</strong> {user?.username}
          </li>
          <li>
            <strong>Rol:</strong> {roleLabel}
          </li>
          <li>
            <strong>UID:</strong> {user?.uid}
          </li>
          <li>
            <strong>Ecommerce ID:</strong> {user?.ecommerceId ?? "null"}
          </li>
          <li>
            <strong>Email:</strong> {user?.email}
          </li>
        </ul>
        <div className="row">
          <Link className="linkBtn" to="/users">
            Ir a gestión de usuarios
          </Link>
        </div>
      </div>
    </div>
  );
}
