import { useEffect, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { ApiError, ApiKeyCreatedResponseDto, ApiKeyListResponseDto } from "../../api-client";
import { useAuth } from "../session/AuthProvider";

export function ApiKeysPage() {
  const { manager, state } = useAuth();
  const client = manager.getApiClient();
  const [params] = useSearchParams();

  const [ecommerceIdInput, setEcommerceIdInput] = useState(params.get("ecommerceId") ?? "");
  const [items, setItems] = useState<ApiKeyListResponseDto[]>([]);
  const [createdKey, setCreatedKey] = useState<ApiKeyCreatedResponseDto | null>(null);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const resolvedEcommerceId = useMemo(() => {
    if (state.user?.roleName === "SUPER_ADMIN") {
      return ecommerceIdInput.trim();
    }
    return state.user?.ecommerceId ?? "";
  }, [ecommerceIdInput, state.user?.ecommerceId, state.user?.roleName]);

  async function loadApiKeys() {
    if (!resolvedEcommerceId) {
      setItems([]);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const list = await client.apiKeys.list(resolvedEcommerceId);
      setItems(list);
    } catch (e) {
      setError(resolveApiError(e, "No se pudo cargar API keys."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadApiKeys();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resolvedEcommerceId]);

  async function onCreate() {
    if (!resolvedEcommerceId) {
      setError("Debes especificar un ecommerceId válido.");
      return;
    }
    setCreating(true);
    setError(null);
    setSuccess(null);
    setCreatedKey(null);
    try {
      const created = await client.apiKeys.create(resolvedEcommerceId);
      setCreatedKey(created);
      setSuccess("API Key creada. Guárdala ahora: se muestra una sola vez.");
      await loadApiKeys();
    } catch (e) {
      setError(resolveApiError(e, "No se pudo crear la API key."));
    } finally {
      setCreating(false);
    }
  }

  async function onDelete(keyId: string) {
    if (!resolvedEcommerceId) return;
    const ok = window.confirm("¿Eliminar esta API key?");
    if (!ok) return;

    setDeletingId(keyId);
    setError(null);
    setSuccess(null);
    try {
      await client.apiKeys.delete(resolvedEcommerceId, keyId);
      setSuccess("API Key eliminada correctamente.");
      await loadApiKeys();
    } catch (e) {
      setError(resolveApiError(e, "No se pudo eliminar la API key."));
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <div className="page">
      <div className="card usersPage">
        <div className="row">
          <h1>Gestión de API Keys</h1>
          <div className="row">
            <Link className="linkBtn" to="/dashboard">
              Dashboard
            </Link>
            <Link className="linkBtn" to="/ecommerces">
              Ecommerces
            </Link>
          </div>
        </div>

        <p>Administra API keys por ecommerce. El engine consume estas credenciales de forma server-to-server.</p>

        {state.user?.roleName === "SUPER_ADMIN" ? (
          <label>
            Ecommerce UUID
            <input
              value={ecommerceIdInput}
              onChange={(e) => setEcommerceIdInput(e.target.value)}
              placeholder="550e8400-e29b-41d4-a716-446655440000"
            />
          </label>
        ) : (
          <p>
            <strong>Ecommerce:</strong> {resolvedEcommerceId}
          </p>
        )}

        <div className="row">
          <button onClick={() => loadApiKeys()} disabled={loading}>
            {loading ? "Actualizando..." : "Refrescar"}
          </button>
          <button onClick={onCreate} disabled={creating || !resolvedEcommerceId}>
            {creating ? "Creando..." : "Crear API Key"}
          </button>
        </div>

        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        {createdKey ? (
          <div className="secretBox">
            <p>
              <strong>API Key nueva (visible una sola vez):</strong>
            </p>
            <code>{createdKey.key}</code>
            <p>
              Expira: <strong>{createdKey.expiresAt}</strong>
            </p>
          </div>
        ) : null}

        <h2>API Keys registradas</h2>
        <ul className="usersList">
          {items.map((k) => (
            <li key={k.uid}>
              <div className="listCard">
                <p>
                  <strong>ID:</strong> {k.uid}
                </p>
                <p>
                  <strong>Key:</strong> {k.maskedKey}
                </p>
                <p>
                  <strong>Activa:</strong> {String(k.isActive)}
                </p>
                <p>
                  <strong>Expira:</strong> {k.expiresAt}
                </p>
                <button className="danger" onClick={() => onDelete(k.uid)} disabled={deletingId === k.uid}>
                  {deletingId === k.uid ? "Eliminando..." : "Eliminar"}
                </button>
              </div>
            </li>
          ))}
          {!loading && items.length === 0 ? <li>Sin API keys para este ecommerce.</li> : null}
        </ul>
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

