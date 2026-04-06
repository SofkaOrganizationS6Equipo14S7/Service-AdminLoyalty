import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, EcommerceResponseDto } from "../../api-client";
import { useAuth } from "../session/AuthProvider";

type StatusFilter = "ALL" | "ACTIVE" | "INACTIVE";

export function EcommercesPage() {
  const { manager } = useAuth();
  const client = manager.getApiClient();

  const [items, setItems] = useState<EcommerceResponseDto[]>([]);
  const [selectedUid, setSelectedUid] = useState<string | null>(null);
  const [selected, setSelected] = useState<EcommerceResponseDto | null>(null);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [createName, setCreateName] = useState("");
  const [createSlug, setCreateSlug] = useState("");
  const [updateStatus, setUpdateStatus] = useState<"ACTIVE" | "INACTIVE">("ACTIVE");

  async function loadEcommerces() {
    setLoading(true);
    setError(null);
    try {
      const resp = await client.ecommerce.list({
        status: statusFilter === "ALL" ? undefined : statusFilter,
        page,
        size,
      });
      setItems(resp.content);
      setTotalPages(resp.totalPages);
      if (resp.content.length > 0 && !selectedUid) {
        setSelectedUid(resp.content[0].uid);
      }
      if (resp.content.length === 0) {
        setSelectedUid(null);
        setSelected(null);
      }
    } catch (e) {
      setError(resolveApiError(e, "No se pudo cargar ecommerces."));
    } finally {
      setLoading(false);
    }
  }

  async function loadDetail(uid: string) {
    try {
      const detail = await client.ecommerce.getByUid(uid);
      setSelected(detail);
      setUpdateStatus(detail.status === "INACTIVE" ? "INACTIVE" : "ACTIVE");
    } catch (e) {
      setError(resolveApiError(e, "No se pudo cargar el detalle del ecommerce."));
    }
  }

  useEffect(() => {
    loadEcommerces();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, page, size]);

  useEffect(() => {
    if (selectedUid) {
      loadDetail(selectedUid);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedUid]);

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setCreating(true);
    setError(null);
    setSuccess(null);
    try {
      const created = await client.ecommerce.create({
        name: createName.trim(),
        slug: createSlug.trim(),
      });
      setCreateName("");
      setCreateSlug("");
      setSuccess("Ecommerce creado correctamente.");
      await loadEcommerces();
      setSelectedUid(created.uid);
    } catch (e) {
      setError(resolveApiError(e, "No se pudo crear el ecommerce."));
    } finally {
      setCreating(false);
    }
  }

  async function onUpdateStatus(e: FormEvent) {
    e.preventDefault();
    if (!selectedUid) return;

    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      await client.ecommerce.updateStatus(selectedUid, { status: updateStatus });
      setSuccess("Estado actualizado correctamente.");
      await loadEcommerces();
      await loadDetail(selectedUid);
    } catch (e) {
      setError(resolveApiError(e, "No se pudo actualizar el estado."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="page">
      <div className="card usersPage">
        <div className="row">
          <h1>Gestión de Ecommerces</h1>
          <div className="row">
            <Link className="linkBtn" to="/dashboard">
              Dashboard
            </Link>
            <Link className="linkBtn" to="/users">
              Usuarios
            </Link>
          </div>
        </div>

        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        <div className="usersGrid">
          <section>
            <h2>Listado</h2>
            <div className="form">
              <label>
                Filtro estado
                <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}>
                  <option value="ALL">Todos</option>
                  <option value="ACTIVE">ACTIVE</option>
                  <option value="INACTIVE">INACTIVE</option>
                </select>
              </label>
              <div className="row">
                <label>
                  Page
                  <input
                    type="number"
                    value={page}
                    onChange={(e) => setPage(Number(e.target.value))}
                    min={0}
                  />
                </label>
                <label>
                  Size
                  <input
                    type="number"
                    value={size}
                    onChange={(e) => setSize(Number(e.target.value))}
                    min={1}
                  />
                </label>
              </div>
              <p>Total páginas: {totalPages}</p>
              <button onClick={() => loadEcommerces()} disabled={loading}>
                {loading ? "Actualizando..." : "Refrescar"}
              </button>
            </div>

            <ul className="usersList">
              {items.map((it) => (
                <li key={it.uid}>
                  <button
                    className={`userItem ${selectedUid === it.uid ? "selected" : ""}`}
                    onClick={() => setSelectedUid(it.uid)}
                  >
                    <span>{it.name}</span>
                    <small>{it.status}</small>
                  </button>
                </li>
              ))}
              {!loading && items.length === 0 ? <li>Sin ecommerces.</li> : null}
            </ul>
          </section>

          <section>
            <h2>Detalle / Estado</h2>
            {!selected ? <p>Selecciona un ecommerce.</p> : null}
            {selected ? (
              <>
                <ul className="meta">
                  <li>
                    <strong>UID:</strong> {selected.uid}
                  </li>
                  <li>
                    <strong>Name:</strong> {selected.name}
                  </li>
                  <li>
                    <strong>Slug:</strong> {selected.slug}
                  </li>
                  <li>
                    <strong>Status:</strong> {selected.status}
                  </li>
                </ul>

                <form className="form" onSubmit={onUpdateStatus}>
                  <label>
                    Nuevo estado
                    <select
                      value={updateStatus}
                      onChange={(e) => setUpdateStatus(e.target.value as "ACTIVE" | "INACTIVE")}
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="INACTIVE">INACTIVE</option>
                    </select>
                  </label>
                  <button type="submit" disabled={saving}>
                    {saving ? "Guardando..." : "Actualizar estado"}
                  </button>
                </form>
              </>
            ) : null}
          </section>
        </div>

        <section>
          <h2>Crear ecommerce</h2>
          <form className="form" onSubmit={onCreate}>
            <label>
              Nombre
              <input value={createName} onChange={(e) => setCreateName(e.target.value)} required />
            </label>
            <label>
              Slug
              <input value={createSlug} onChange={(e) => setCreateSlug(e.target.value)} required />
            </label>
            <button type="submit" disabled={creating}>
              {creating ? "Creando..." : "Crear ecommerce"}
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

