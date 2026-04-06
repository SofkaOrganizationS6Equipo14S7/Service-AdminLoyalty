import { FormEvent, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../api-client";
import { useAuth } from "../session/AuthProvider";

export function DiscountSetupPage() {
  const { manager, state } = useAuth();
  const client = manager.getApiClient();

  const [ecommerceId, setEcommerceId] = useState(state.user?.ecommerceId ?? "");
  const [currency, setCurrency] = useState("USD");
  const [roundingRule, setRoundingRule] = useState<"HALF_UP" | "DOWN" | "UP">("HALF_UP");
  const [capType, setCapType] = useState<"PERCENTAGE">("PERCENTAGE");
  const [capValue, setCapValue] = useState("50");
  const [capAppliesTo, setCapAppliesTo] =
    useState<"SUBTOTAL" | "TOTAL" | "BEFORE_TAX" | "AFTER_TAX">("SUBTOTAL");
  const [priorityType, setPriorityType] = useState("FIDELITY");
  const [priorityOrder, setPriorityOrder] = useState("1");

  const [discountMaxCap, setDiscountMaxCap] = useState("50");
  const [discountCurrency, setDiscountCurrency] = useState("USD");
  const [allowStacking, setAllowStacking] = useState(true);
  const [discountRounding, setDiscountRounding] = useState("HALF_UP");
  const [discountSettingId, setDiscountSettingId] = useState("");
  const [discountTypeId, setDiscountTypeId] = useState("");
  const [discountPriorityLevel, setDiscountPriorityLevel] = useState("1");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [lastResponse, setLastResponse] = useState<string>("");

  const canUseEcommerce = useMemo(() => ecommerceId.trim().length > 0, [ecommerceId]);

  async function onCreateConfiguration(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const resp = await client.configuration.create({
        ecommerceId: ecommerceId.trim(),
        currency: currency.trim().toUpperCase(),
        roundingRule,
        cap: {
          type: capType,
          value: Number(capValue),
          appliesTo: capAppliesTo,
        },
        priority: [
          {
            type: priorityType.trim(),
            order: Number(priorityOrder),
          },
        ],
      });
      setSuccess("Configuration creada correctamente.");
      setLastResponse(JSON.stringify(resp, null, 2));
    } catch (err) {
      setError(resolveApiError(err, "No se pudo crear configuration."));
    } finally {
      setLoading(false);
    }
  }

  async function onPatchConfiguration() {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const resp = await client.configuration.patch(ecommerceId.trim(), {
        currency: currency.trim().toUpperCase(),
        roundingRule,
        cap: {
          type: capType,
          value: Number(capValue),
          appliesTo: capAppliesTo,
        },
        priority: [
          {
            type: priorityType.trim(),
            order: Number(priorityOrder),
          },
        ],
      });
      setSuccess("Configuration actualizada correctamente.");
      setLastResponse(JSON.stringify(resp, null, 2));
    } catch (err) {
      setError(resolveApiError(err, "No se pudo actualizar configuration."));
    } finally {
      setLoading(false);
    }
  }

  async function onSaveDiscountConfig(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const resp = await client.discountConfig.createOrUpdate({
        ecommerceId: ecommerceId.trim(),
        maxDiscountCap: Number(discountMaxCap),
        currencyCode: discountCurrency.trim().toUpperCase(),
        allowStacking,
        roundingRule: discountRounding.trim(),
      });
      setDiscountSettingId(resp.uid);
      setSuccess("Discount config guardada correctamente.");
      setLastResponse(JSON.stringify(resp, null, 2));
    } catch (err) {
      setError(resolveApiError(err, "No se pudo guardar discount-config."));
    } finally {
      setLoading(false);
    }
  }

  async function onGetDiscountConfig() {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const resp = await client.discountConfig.getByEcommerce(ecommerceId.trim());
      setDiscountSettingId(resp.uid);
      setSuccess("Discount config consultada correctamente.");
      setLastResponse(JSON.stringify(resp, null, 2));
    } catch (err) {
      setError(resolveApiError(err, "No se pudo consultar discount-config."));
    } finally {
      setLoading(false);
    }
  }

  async function onSavePriorities(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const resp = await client.discountConfig.savePriorities({
        discountSettingId: discountSettingId.trim(),
        priorities: [
          {
            discountTypeId: discountTypeId.trim(),
            priorityLevel: Number(discountPriorityLevel),
          },
        ],
      });
      setSuccess("Prioridades guardadas correctamente.");
      setLastResponse(JSON.stringify(resp, null, 2));
    } catch (err) {
      setError(resolveApiError(err, "No se pudieron guardar prioridades."));
    } finally {
      setLoading(false);
    }
  }

  async function onGetPriorities() {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const resp = await client.discountConfig.getPriorities(discountSettingId.trim());
      setSuccess("Prioridades consultadas correctamente.");
      setLastResponse(JSON.stringify(resp, null, 2));
    } catch (err) {
      setError(resolveApiError(err, "No se pudieron consultar prioridades."));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="card usersPage">
        <div className="row">
          <h1>Configuración de Descuentos</h1>
          <div className="row">
            <Link className="linkBtn" to="/dashboard">
              Dashboard
            </Link>
            <Link className="linkBtn" to="/api-keys">
              API Keys
            </Link>
          </div>
        </div>

        <label>
          Ecommerce UUID
          <input value={ecommerceId} onChange={(e) => setEcommerceId(e.target.value)} />
        </label>

        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        <div className="usersGrid">
          <section>
            <h2>Configuration (create/patch)</h2>
            <form className="form" onSubmit={onCreateConfiguration}>
              <label>
                Currency
                <input value={currency} onChange={(e) => setCurrency(e.target.value)} required />
              </label>
              <label>
                Rounding rule
                <select
                  value={roundingRule}
                  onChange={(e) => setRoundingRule(e.target.value as "HALF_UP" | "DOWN" | "UP")}
                >
                  <option value="HALF_UP">HALF_UP</option>
                  <option value="DOWN">DOWN</option>
                  <option value="UP">UP</option>
                </select>
              </label>
              <label>
                Cap type
                <select value={capType} onChange={(e) => setCapType(e.target.value as "PERCENTAGE")}>
                  <option value="PERCENTAGE">PERCENTAGE</option>
                </select>
              </label>
              <label>
                Cap value
                <input value={capValue} onChange={(e) => setCapValue(e.target.value)} />
              </label>
              <label>
                Cap appliesTo
                <select
                  value={capAppliesTo}
                  onChange={(e) =>
                    setCapAppliesTo(e.target.value as "SUBTOTAL" | "TOTAL" | "BEFORE_TAX" | "AFTER_TAX")
                  }
                >
                  <option value="SUBTOTAL">SUBTOTAL</option>
                  <option value="TOTAL">TOTAL</option>
                  <option value="BEFORE_TAX">BEFORE_TAX</option>
                  <option value="AFTER_TAX">AFTER_TAX</option>
                </select>
              </label>
              <label>
                Priority type
                <input value={priorityType} onChange={(e) => setPriorityType(e.target.value)} />
              </label>
              <label>
                Priority order
                <input value={priorityOrder} onChange={(e) => setPriorityOrder(e.target.value)} />
              </label>
              <div className="row">
                <button type="submit" disabled={loading || !canUseEcommerce}>
                  {loading ? "Procesando..." : "Create configuration"}
                </button>
                <button type="button" onClick={() => onPatchConfiguration()} disabled={loading || !canUseEcommerce}>
                  {loading ? "Procesando..." : "Patch configuration"}
                </button>
              </div>
            </form>
          </section>

          <section>
            <h2>Discount Config + Priority</h2>
            <form className="form" onSubmit={onSaveDiscountConfig}>
              <label>
                Max discount cap
                <input value={discountMaxCap} onChange={(e) => setDiscountMaxCap(e.target.value)} />
              </label>
              <label>
                Currency code
                <input value={discountCurrency} onChange={(e) => setDiscountCurrency(e.target.value)} />
              </label>
              <label>
                Rounding rule
                <input value={discountRounding} onChange={(e) => setDiscountRounding(e.target.value)} />
              </label>
              <label className="inlineField">
                <input
                  type="checkbox"
                  checked={allowStacking}
                  onChange={(e) => setAllowStacking(e.target.checked)}
                />
                Allow stacking
              </label>
              <div className="row">
                <button type="submit" disabled={loading || !canUseEcommerce}>
                  {loading ? "Procesando..." : "Save discount config"}
                </button>
                <button type="button" onClick={onGetDiscountConfig} disabled={loading || !canUseEcommerce}>
                  {loading ? "Procesando..." : "Get discount config"}
                </button>
              </div>
            </form>

            <form className="form" onSubmit={onSavePriorities}>
              <label>
                Discount setting ID
                <input
                  value={discountSettingId}
                  onChange={(e) => setDiscountSettingId(e.target.value)}
                  placeholder="UUID"
                />
              </label>
              <label>
                Discount type ID
                <input
                  value={discountTypeId}
                  onChange={(e) => setDiscountTypeId(e.target.value)}
                  placeholder="UUID"
                />
              </label>
              <label>
                Priority level
                <input
                  value={discountPriorityLevel}
                  onChange={(e) => setDiscountPriorityLevel(e.target.value)}
                />
              </label>
              <div className="row">
                <button type="submit" disabled={loading || !discountSettingId || !discountTypeId}>
                  {loading ? "Procesando..." : "Save priorities"}
                </button>
                <button type="button" onClick={onGetPriorities} disabled={loading || !discountSettingId}>
                  {loading ? "Procesando..." : "Get priorities"}
                </button>
              </div>
            </form>
          </section>
        </div>

        <section>
          <h2>Última respuesta</h2>
          <div className="secretBox">
            <code>{lastResponse || "Sin respuesta aún."}</code>
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
