import { FormEvent, useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../session/AuthProvider";

export function LoginPage() {
  const { state, login, error, clearError } = useAuth();
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin123");
  const [loading, setLoading] = useState(false);
  const location = useLocation() as { state?: { from?: string } };

  useEffect(() => {
    clearError();
  }, [clearError]);

  if (state.user) {
    const target = location.state?.from || "/dashboard";
    return <Navigate to={target} replace />;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    await login(username, password);
    setLoading(false);
  }

  return (
    <div className="page">
      <div className="card">
        <h1>Loyalty Admin</h1>
        <p>Inicia sesión para gestionar configuración y usuarios.</p>
        <form onSubmit={onSubmit} className="form">
          <label>
            Usuario
            <input value={username} onChange={(e) => setUsername(e.target.value)} />
          </label>
          <label>
            Contraseña
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>
          {error ? <p className="error">{error}</p> : null}
          <button type="submit" disabled={loading}>
            {loading ? "Ingresando..." : "Ingresar"}
          </button>
        </form>
      </div>
    </div>
  );
}

