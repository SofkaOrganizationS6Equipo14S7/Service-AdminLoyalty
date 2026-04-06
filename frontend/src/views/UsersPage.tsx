import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, UserCreateRequestDto, UserResponseDto, UserUpdateRequestDto } from "../../api-client";
import { useAuth } from "../session/AuthProvider";

interface CreateUserFormState {
  username: string;
  email: string;
  password: string;
  roleId: string;
  ecommerceId: string;
}

interface UpdateUserFormState {
  username: string;
  email: string;
  password: string;
  ecommerceId: string;
  active: boolean;
}

const EMPTY_CREATE: CreateUserFormState = {
  username: "",
  email: "",
  password: "",
  roleId: "",
  ecommerceId: "",
};

const EMPTY_UPDATE: UpdateUserFormState = {
  username: "",
  email: "",
  password: "",
  ecommerceId: "",
  active: true,
};

export function UsersPage() {
  const { manager, state } = useAuth();
  const client = manager.getApiClient();

  const [users, setUsers] = useState<UserResponseDto[]>([]);
  const [selectedUid, setSelectedUid] = useState<string | null>(null);
  const [selectedUser, setSelectedUser] = useState<UserResponseDto | null>(null);
  const [createForm, setCreateForm] = useState<CreateUserFormState>(EMPTY_CREATE);
  const [updateForm, setUpdateForm] = useState<UpdateUserFormState>(EMPTY_UPDATE);
  const [loadingList, setLoadingList] = useState(false);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [submittingUpdate, setSubmittingUpdate] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const effectiveEcommerceId = useMemo(() => state.user?.ecommerceId ?? undefined, [state.user?.ecommerceId]);

  async function loadUsers() {
    setLoadingList(true);
    setError(null);
    try {
      const list = await client.users.list(effectiveEcommerceId ?? undefined);
      setUsers(list);
      if (!selectedUid && list.length > 0) {
        setSelectedUid(list[0].uid);
      }
      if (list.length === 0) {
        setSelectedUid(null);
        setSelectedUser(null);
      }
    } catch (e) {
      setError(resolveApiError(e, "No se pudo cargar la lista de usuarios."));
    } finally {
      setLoadingList(false);
    }
  }

  async function loadUserDetail(uid: string) {
    setLoadingDetail(true);
    setError(null);
    try {
      const detail = await client.users.getByUid(uid);
      setSelectedUser(detail);
      setUpdateForm({
        username: detail.username ?? "",
        email: detail.email ?? "",
        password: "",
        ecommerceId: detail.ecommerceId ?? "",
        active: detail.isActive,
      });
    } catch (e) {
      setError(resolveApiError(e, "No se pudo cargar el detalle del usuario."));
    } finally {
      setLoadingDetail(false);
    }
  }

  useEffect(() => {
    loadUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [effectiveEcommerceId]);

  useEffect(() => {
    if (selectedUid) {
      loadUserDetail(selectedUid);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedUid]);

  async function onCreateUser(e: FormEvent) {
    e.preventDefault();
    setSubmittingCreate(true);
    setError(null);
    setSuccess(null);
    try {
      const payload: UserCreateRequestDto = {
        username: createForm.username.trim(),
        email: createForm.email.trim(),
        password: createForm.password,
        roleId: createForm.roleId.trim(),
        ecommerceId: createForm.ecommerceId.trim() || effectiveEcommerceId || null,
      };

      const created = await client.users.create(payload);
      setSuccess("Usuario creado correctamente.");
      setCreateForm(EMPTY_CREATE);
      await loadUsers();
      setSelectedUid(created.uid);
    } catch (e) {
      setError(resolveApiError(e, "No se pudo crear el usuario."));
    } finally {
      setSubmittingCreate(false);
    }
  }

  async function onUpdateUser(e: FormEvent) {
    e.preventDefault();
    if (!selectedUid) {
      return;
    }
    setSubmittingUpdate(true);
    setError(null);
    setSuccess(null);
    try {
      const payload: UserUpdateRequestDto = {
        username: updateForm.username.trim() || undefined,
        email: updateForm.email.trim() || undefined,
        password: updateForm.password || undefined,
        ecommerceId: updateForm.ecommerceId.trim() || undefined,
        active: updateForm.active,
      };

      await client.users.update(selectedUid, payload);
      setSuccess("Usuario actualizado correctamente.");
      await loadUsers();
      await loadUserDetail(selectedUid);
    } catch (e) {
      setError(resolveApiError(e, "No se pudo actualizar el usuario."));
    } finally {
      setSubmittingUpdate(false);
    }
  }

  async function onDeleteUser() {
    if (!selectedUid) {
      return;
    }
    const confirmed = globalThis.confirm("¿Eliminar este usuario? Esta acción no se puede deshacer.");
    if (!confirmed) {
      return;
    }

    setDeleting(true);
    setError(null);
    setSuccess(null);
    try {
      await client.users.delete(selectedUid);
      setSuccess("Usuario eliminado correctamente.");
      await loadUsers();
    } catch (e) {
      setError(resolveApiError(e, "No se pudo eliminar el usuario."));
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="page">
      <div className="card usersPage">
        <div className="row">
          <h1>Gestión de Usuarios</h1>
          <Link className="linkBtn" to="/dashboard">
            Volver al dashboard
          </Link>
        </div>
        <p>Administra usuarios del ecommerce actual sin afectar contratos backend existentes.</p>

        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        <div className="usersGrid">
          <section>
            <h2>Listado</h2>
            <button onClick={() => loadUsers()} disabled={loadingList}>
              {loadingList ? "Actualizando..." : "Refrescar"}
            </button>
            <ul className="usersList">
              {users.map((u) => (
                <li key={u.uid}>
                  <button
                    className={`userItem ${selectedUid === u.uid ? "selected" : ""}`}
                    onClick={() => setSelectedUid(u.uid)}
                  >
                    <span>{u.username}</span>
                    <small>{u.roleName}</small>
                  </button>
                </li>
              ))}
              {users.length === 0 && !loadingList ? <li>Sin usuarios disponibles.</li> : null}
            </ul>
          </section>

          <section>
            <h2>Detalle / Edición</h2>
            {!selectedUid ? <p>Selecciona un usuario.</p> : null}
            {loadingDetail ? <p>Cargando detalle...</p> : null}
            {selectedUser ? (
              <form className="form" onSubmit={onUpdateUser}>
                <label>
                  Username
                  <input
                    value={updateForm.username}
                    onChange={(e) => setUpdateForm((p) => ({ ...p, username: e.target.value }))}
                  />
                </label>
                <label>
                  Email
                  <input
                    value={updateForm.email}
                    onChange={(e) => setUpdateForm((p) => ({ ...p, email: e.target.value }))}
                  />
                </label>
                <label>
                  Nueva contraseña (opcional)
                  <input
                    type="password"
                    value={updateForm.password}
                    onChange={(e) => setUpdateForm((p) => ({ ...p, password: e.target.value }))}
                  />
                </label>
                <label>
                  Ecommerce UUID (opcional)
                  <input
                    value={updateForm.ecommerceId}
                    onChange={(e) => setUpdateForm((p) => ({ ...p, ecommerceId: e.target.value }))}
                  />
                </label>
                <label className="inlineField">
                  <input
                    type="checkbox"
                    checked={updateForm.active}
                    onChange={(e) => setUpdateForm((p) => ({ ...p, active: e.target.checked }))}
                  />
                  Activo
                </label>
                <div className="row">
                  <button type="submit" disabled={submittingUpdate}>
                    {submittingUpdate ? "Guardando..." : "Actualizar usuario"}
                  </button>
                  <button type="button" className="danger" onClick={onDeleteUser} disabled={deleting}>
                    {deleting ? "Eliminando..." : "Eliminar usuario"}
                  </button>
                </div>
              </form>
            ) : null}
          </section>
        </div>

        <section>
          <h2>Crear usuario</h2>
          <form className="form" onSubmit={onCreateUser}>
            <label>
              Username
              <input
                value={createForm.username}
                onChange={(e) => setCreateForm((p) => ({ ...p, username: e.target.value }))}
                required
              />
            </label>
            <label>
              Email
              <input
                value={createForm.email}
                onChange={(e) => setCreateForm((p) => ({ ...p, email: e.target.value }))}
                required
              />
            </label>
            <label>
              Password
              <input
                type="password"
                value={createForm.password}
                onChange={(e) => setCreateForm((p) => ({ ...p, password: e.target.value }))}
                required
              />
            </label>
            <label>
              Role UUID
              <input
                value={createForm.roleId}
                onChange={(e) => setCreateForm((p) => ({ ...p, roleId: e.target.value }))}
                required
              />
            </label>
            <label>
              Ecommerce UUID (opcional)
              <input
                value={createForm.ecommerceId}
                onChange={(e) => setCreateForm((p) => ({ ...p, ecommerceId: e.target.value }))}
              />
            </label>
            <button type="submit" disabled={submittingCreate}>
              {submittingCreate ? "Creando..." : "Crear usuario"}
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

