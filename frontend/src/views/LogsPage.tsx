import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, AuditLogResponseDto, DiscountApplicationLogResponseDto } from "../../api-client";
import { useAuth } from "../session/AuthProvider";

export function LogsPage() {
  const { manager, state } = useAuth();
  const client = manager.getApiClient();

  const [auditEntityName, setAuditEntityName] = useState("");
  const [auditEcommerceId, setAuditEcommerceId] = useState(state.user?.ecommerceId ?? "");
  const [auditItems, setAuditItems] = useState<AuditLogResponseDto[]>([]);
  const [auditDetailId, setAuditDetailId] = useState("");
  const [auditDetail, setAuditDetail] = useState<AuditLogResponseDto | null>(null);

  const [discountEcommerceId, setDiscountEcommerceId] = useState(state.user?.ecommerceId ?? "");
  const [externalOrderId, setExternalOrderId] = useState("");
  const [discountItems, setDiscountItems] = useState<DiscountApplicationLogResponseDto[]>([]);
  const [discountDetailId, setDiscountDetailId] = useState("");
  const [discountDetail, setDiscountDetail] = useState<DiscountApplicationLogResponseDto | null>(null);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  async function onSearchAudit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const res = await client.logs.listAuditLogs({
        entityName: auditEntityName.trim() || undefined,
        ecommerceId: auditEcommerceId.trim() || undefined,
        page: 0,
        size: 50,
      });
      setAuditItems(res.content);
      setSuccess(`Audit logs encontrados: ${res.totalElements}`);
    } catch (e) {
      setError(resolveApiError(e, "No se pudieron consultar audit logs."));
    } finally {
      setLoading(false);
    }
  }

  async function onGetAuditDetail() {
    if (!auditDetailId.trim()) {
      setError("Ingresa un Audit Log ID.");
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const detail = await client.logs.getAuditLogById(auditDetailId.trim());
      setAuditDetail(detail);
      setSuccess("Detalle de audit log consultado.");
    } catch (e) {
      setError(resolveApiError(e, "No se pudo consultar el detalle de audit log."));
    } finally {
      setLoading(false);
    }
  }

  async function onSearchDiscount(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const res = await client.logs.listDiscountLogs({
        ecommerceId: discountEcommerceId.trim() || undefined,
        externalOrderId: externalOrderId.trim() || undefined,
        page: 0,
        size: 50,
      });
      setDiscountItems(res.content);
      setSuccess(`Discount logs encontrados: ${res.totalElements}`);
    } catch (e) {
      setError(resolveApiError(e, "No se pudieron consultar discount logs."));
    } finally {
      setLoading(false);
    }
  }

  async function onGetDiscountDetail() {
    if (!discountDetailId.trim()) {
      setError("Ingresa un Discount Log ID.");
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const detail = await client.logs.getDiscountLogById(discountDetailId.trim());
      setDiscountDetail(detail);
      setSuccess("Detalle de discount log consultado.");
    } catch (e) {
      setError(resolveApiError(e, "No se pudo consultar el detalle de discount log."));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="card usersPage">
        <div className="row">
          <h1>Logs</h1>
          <div className="row">
            <Link className="linkBtn" to="/dashboard">
              Dashboard
            </Link>
            <Link className="linkBtn" to="/roles-permissions">
              Roles
            </Link>
          </div>
        </div>
        <p>Consulta logs de auditoría y logs de descuentos aplicados.</p>
        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        <div className="usersGrid">
          <section>
            <h2>Audit Logs</h2>
            <form className="form" onSubmit={onSearchAudit}>
              <label>
                Entity Name
                <input
                  value={auditEntityName}
                  onChange={(e) => setAuditEntityName(e.target.value)}
                  placeholder="app_user, ecommerce, rules..."
                />
              </label>
              <label>
                Ecommerce ID
                <input value={auditEcommerceId} onChange={(e) => setAuditEcommerceId(e.target.value)} />
              </label>
              <button type="submit" disabled={loading}>
                {loading ? "Consultando..." : "Buscar audit logs"}
              </button>
            </form>

            <ul className="usersList">
              {auditItems.map((item) => (
                <li key={item.id}>
                  <button
                    className="userItem"
                    onClick={() => {
                      setAuditDetailId(item.id);
                      setAuditDetail(item);
                    }}
                  >
                    <span>{item.action} - {item.entityName}</span>
                    <small>{item.id}</small>
                  </button>
                </li>
              ))}
            </ul>

            <div className="form">
              <label>
                Audit Log ID
                <input value={auditDetailId} onChange={(e) => setAuditDetailId(e.target.value)} />
              </label>
              <button onClick={onGetAuditDetail} disabled={loading}>
                {loading ? "Consultando..." : "Ver detalle audit"}
              </button>
            </div>
          </section>

          <section>
            <h2>Discount Logs</h2>
            <form className="form" onSubmit={onSearchDiscount}>
              <label>
                Ecommerce ID
                <input value={discountEcommerceId} onChange={(e) => setDiscountEcommerceId(e.target.value)} />
              </label>
              <label>
                External Order ID
                <input value={externalOrderId} onChange={(e) => setExternalOrderId(e.target.value)} />
              </label>
              <button type="submit" disabled={loading}>
                {loading ? "Consultando..." : "Buscar discount logs"}
              </button>
            </form>

            <ul className="usersList">
              {discountItems.map((item) => (
                <li key={item.id}>
                  <button
                    className="userItem"
                    onClick={() => {
                      setDiscountDetailId(item.id);
                      setDiscountDetail(item);
                    }}
                  >
                    <span>{item.externalOrderId || "N/A"}</span>
                    <small>{item.id}</small>
                  </button>
                </li>
              ))}
            </ul>

            <div className="form">
              <label>
                Discount Log ID
                <input value={discountDetailId} onChange={(e) => setDiscountDetailId(e.target.value)} />
              </label>
              <button onClick={onGetDiscountDetail} disabled={loading}>
                {loading ? "Consultando..." : "Ver detalle discount"}
              </button>
            </div>
          </section>
        </div>

        <section>
          <h2>Detalle seleccionado</h2>
          <div className="secretBox">
            <code>{JSON.stringify(auditDetail ?? discountDetail ?? "Sin detalle seleccionado", null, 2)}</code>
          </div>
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

