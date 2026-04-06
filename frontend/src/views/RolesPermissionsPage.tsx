import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, PermissionResponseDto, RoleResponseDto } from "../../api-client";
import { useAuth } from "../session/AuthProvider";

export function RolesPermissionsPage() {
  const { manager } = useAuth();
  const client = manager.getApiClient();

  const [roles, setRoles] = useState<RoleResponseDto[]>([]);
  const [permissions, setPermissions] = useState<PermissionResponseDto[]>([]);
  const [selectedRoleId, setSelectedRoleId] = useState("");
  const [selectedPermissionIds, setSelectedPermissionIds] = useState<string[]>([]);
  const [moduleFilter, setModuleFilter] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function loadRoles() {
    const data = await client.roles.listRoles();
    setRoles(data);
    if (!selectedRoleId && data.length > 0) {
      setSelectedRoleId(data[0].id);
    }
  }

  async function loadPermissions(module?: string) {
    const data = await client.roles.listPermissions(module?.trim() || undefined);
    setPermissions(data);
  }

  useEffect(() => {
    setLoading(true);
    setError(null);
    Promise.all([loadRoles(), loadPermissions()])
      .catch((e) => setError(resolveApiError(e, "No se pudieron cargar roles/permisos.")))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function onFilterPermissions(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await loadPermissions(moduleFilter);
    } catch (e) {
      setError(resolveApiError(e, "No se pudieron filtrar permisos."));
    } finally {
      setLoading(false);
    }
  }

  function togglePermission(permissionId: string) {
    setSelectedPermissionIds((prev) =>
      prev.includes(permissionId) ? prev.filter((id) => id !== permissionId) : [...prev, permissionId]
    );
  }

  async function onAssignPermissions() {
    if (!selectedRoleId) {
      setError("Selecciona un rol.");
      return;
    }
    if (selectedPermissionIds.length === 0) {
      setError("Selecciona al menos un permiso.");
      return;
    }

    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await client.roles.assignPermissions(selectedRoleId, { permissionIds: selectedPermissionIds });
      setSuccess("Permisos asignados correctamente.");
    } catch (e) {
      setError(resolveApiError(e, "No se pudieron asignar permisos."));
    } finally {
      setLoading(false);
    }
  }

  async function onLoadRoleDetail() {
    if (!selectedRoleId) return;
    setLoading(true);
    setError(null);
    try {
      const detail = await client.roles.getRole(selectedRoleId);
      setSelectedPermissionIds(detail.permissions.map((p) => p.id));
      setSuccess(`Rol ${detail.name} cargado con ${detail.permissions.length} permisos.`);
    } catch (e) {
      setError(resolveApiError(e, "No se pudo cargar el detalle del rol."));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="card usersPage">
        <div className="row">
          <h1>Roles y Permisos</h1>
          <div className="row">
            <Link className="linkBtn" to="/dashboard">
              Dashboard
            </Link>
            <Link className="linkBtn" to="/ecommerces">
              Ecommerces
            </Link>
          </div>
        </div>
        <p>Módulo de administración de autorizaciones (solo SUPER_ADMIN).</p>
        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        <div className="usersGrid">
          <section>
            <h2>Roles</h2>
            <ul className="usersList">
              {roles.map((role) => (
                <li key={role.id}>
                  <button
                    className={`userItem ${selectedRoleId === role.id ? "selected" : ""}`}
                    onClick={() => setSelectedRoleId(role.id)}
                  >
                    <span>{role.name}</span>
                    <small>{role.isActive ? "ACTIVE" : "INACTIVE"}</small>
                  </button>
                </li>
              ))}
            </ul>
            <div className="row">
              <button onClick={onLoadRoleDetail} disabled={loading || !selectedRoleId}>
                {loading ? "Procesando..." : "Cargar permisos del rol"}
              </button>
            </div>
          </section>

          <section>
            <h2>Permisos</h2>
            <form className="form" onSubmit={onFilterPermissions}>
              <label>
                Filtro por módulo (opcional)
                <input
                  value={moduleFilter}
                  onChange={(e) => setModuleFilter(e.target.value)}
                  placeholder="ecommerce, users, rules..."
                />
              </label>
              <button type="submit" disabled={loading}>
                {loading ? "Procesando..." : "Filtrar permisos"}
              </button>
            </form>

            <ul className="usersList">
              {permissions.map((permission) => {
                const checked = selectedPermissionIds.includes(permission.id);
                return (
                  <li key={permission.id}>
                    <label className="inlineField listCard">
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => togglePermission(permission.id)}
                      />
                      <span>
                        <strong>{permission.code}</strong> ({permission.module}) - {permission.description}
                      </span>
                    </label>
                  </li>
                );
              })}
            </ul>

            <div className="row">
              <button onClick={onAssignPermissions} disabled={loading || !selectedRoleId}>
                {loading ? "Procesando..." : "Asignar permisos seleccionados"}
              </button>
            </div>
          </section>
        </div>
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

