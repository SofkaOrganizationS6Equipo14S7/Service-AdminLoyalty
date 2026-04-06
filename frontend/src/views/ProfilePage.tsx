import { FormEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../api-client";
import { useAuth } from "../session/AuthProvider";

export function ProfilePage() {
  const { state, manager } = useAuth();
  const client = manager.getApiClient();

  const [email, setEmail] = useState(state.user?.email ?? "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [loadingPassword, setLoadingPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const canSaveProfile = useMemo(() => email.trim().length > 0, [email]);

  async function onSaveProfile(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);
    setLoadingProfile(true);
    try {
      await client.users.updateProfile({ email: email.trim() });
      await manager.refreshCurrentUser();
      setSuccess("Perfil actualizado correctamente.");
    } catch (err) {
      setError(resolveApiError(err, "No se pudo actualizar el perfil."));
    } finally {
      setLoadingProfile(false);
    }
  }

  async function onChangePassword(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (newPassword !== confirmPassword) {
      setError("La nueva contraseña y su confirmación no coinciden.");
      return;
    }

    setLoadingPassword(true);
    try {
      const resp = await client.users.changePassword({
        currentPassword,
        newPassword,
        confirmPassword,
      });

      if (resp.token) {
        manager.setToken(resp.token);
        await manager.refreshCurrentUser();
      }

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setSuccess("Contraseña actualizada correctamente.");
    } catch (err) {
      setError(resolveApiError(err, "No se pudo cambiar la contraseña."));
    } finally {
      setLoadingPassword(false);
    }
  }

  return (
    <div className="page">
      <div className="card profilePage">
        <div className="row">
          <h1>Mi Perfil</h1>
          <div className="row">
            <Link className="linkBtn" to="/dashboard">
              Dashboard
            </Link>
            <Link className="linkBtn" to="/users">
              Usuarios
            </Link>
          </div>
        </div>

        <p>Gestiona tus datos y credenciales de acceso.</p>
        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        <div className="usersGrid">
          <section>
            <h2>Datos de sesión</h2>
            <ul className="meta">
              <li>
                <strong>Username:</strong> {state.user?.username}
              </li>
              <li>
                <strong>Rol:</strong> {state.user?.roleName}
              </li>
              <li>
                <strong>UID:</strong> {state.user?.uid}
              </li>
              <li>
                <strong>Ecommerce:</strong> {state.user?.ecommerceId ?? "null"}
              </li>
            </ul>
          </section>

          <section>
            <h2>Actualizar perfil</h2>
            <form className="form" onSubmit={onSaveProfile}>
              <label>
                Email
                <input value={email} onChange={(e) => setEmail(e.target.value)} required />
              </label>
              <button type="submit" disabled={!canSaveProfile || loadingProfile}>
                {loadingProfile ? "Guardando..." : "Guardar perfil"}
              </button>
            </form>
          </section>
        </div>

        <section>
          <h2>Cambiar contraseña</h2>
          <form className="form" onSubmit={onChangePassword}>
            <label>
              Contraseña actual
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
              />
            </label>
            <label>
              Nueva contraseña
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />
            </label>
            <label>
              Confirmar nueva contraseña
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />
            </label>
            <button type="submit" disabled={loadingPassword}>
              {loadingPassword ? "Actualizando..." : "Cambiar contraseña"}
            </button>
          </form>
        </section>
      </div>
    </div>
  );
}

function resolveApiError(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    return error.message || fallback;
  }
  return fallback;
}
