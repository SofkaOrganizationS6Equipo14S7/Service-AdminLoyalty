import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, CustomerTierResponseDto, RuleResponseDto } from "../../api-client";
import { useAuth } from "../session/AuthProvider";

export function RulesTiersPage() {
  const { manager, state } = useAuth();
  const client = manager.getApiClient();

  const [tiers, setTiers] = useState<CustomerTierResponseDto[]>([]);
  const [rules, setRules] = useState<RuleResponseDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [tierName, setTierName] = useState("");
  const [tierDiscount, setTierDiscount] = useState("10");
  const [tierLevel, setTierLevel] = useState("1");
  const [selectedTierId, setSelectedTierId] = useState("");

  const [ruleName, setRuleName] = useState("");
  const [ruleDescription, setRuleDescription] = useState("");
  const [ruleDiscount, setRuleDiscount] = useState("5");
  const [rulePriorityId, setRulePriorityId] = useState("");
  const [ruleAttributesJson, setRuleAttributesJson] = useState("{\"metricType\":\"TOTAL_SPENT\"}");
  const [selectedRuleId, setSelectedRuleId] = useState("");
  const [assignTierId, setAssignTierId] = useState("");

  const ecommerceId = state.user?.ecommerceId ?? "";

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [tiersPage, rulesPage] = await Promise.all([
        client.customerTiers.list({ page: 0, size: 50 }),
        client.rules.list({ page: 0, size: 50 }),
      ]);
      setTiers(tiersPage.content);
      setRules(rulesPage.content);
      if (!selectedTierId && tiersPage.content.length > 0) {
        setSelectedTierId(tiersPage.content[0].id);
      }
      if (!selectedRuleId && rulesPage.content.length > 0) {
        setSelectedRuleId(rulesPage.content[0].id);
      }
    } catch (e) {
      setError(resolveApiError(e, "No se pudo cargar rules/tiers."));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function onCreateTier(e: FormEvent) {
    e.preventDefault();
    if (!ecommerceId) {
      setError("El usuario no tiene ecommerceId en sesión.");
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await client.customerTiers.create({
        ecommerceId,
        name: tierName.trim(),
        discountPercentage: Number(tierDiscount),
        hierarchyLevel: Number(tierLevel),
      });
      setTierName("");
      setSuccess("Customer tier creado.");
      await loadData();
    } catch (e) {
      setError(resolveApiError(e, "No se pudo crear customer tier."));
    } finally {
      setLoading(false);
    }
  }

  async function onUpdateTier() {
    if (!selectedTierId) return;
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await client.customerTiers.update(selectedTierId, {
        name: tierName.trim(),
        discountPercentage: Number(tierDiscount),
        hierarchyLevel: Number(tierLevel),
      });
      setSuccess("Customer tier actualizado.");
      await loadData();
    } catch (e) {
      setError(resolveApiError(e, "No se pudo actualizar customer tier."));
    } finally {
      setLoading(false);
    }
  }

  async function onDeleteTier() {
    if (!selectedTierId) return;
    const ok = window.confirm("¿Eliminar customer tier?");
    if (!ok) return;
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await client.customerTiers.delete(selectedTierId);
      setSuccess("Customer tier eliminado.");
      setSelectedTierId("");
      await loadData();
    } catch (e) {
      setError(resolveApiError(e, "No se pudo eliminar customer tier."));
    } finally {
      setLoading(false);
    }
  }

  async function onCreateRule(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const parsed = JSON.parse(ruleAttributesJson) as Record<string, string>;
      await client.rules.create({
        name: ruleName.trim(),
        description: ruleDescription.trim(),
        discountPercentage: Number(ruleDiscount),
        discountPriorityId: rulePriorityId.trim(),
        attributes: parsed,
      });
      setRuleName("");
      setRuleDescription("");
      setSuccess("Rule creada.");
      await loadData();
    } catch (e) {
      if (e instanceof SyntaxError) {
        setError("El JSON de attributes es inválido.");
      } else {
        setError(resolveApiError(e, "No se pudo crear rule."));
      }
    } finally {
      setLoading(false);
    }
  }

  async function onDeleteRule() {
    if (!selectedRuleId) return;
    const ok = window.confirm("¿Eliminar rule?");
    if (!ok) return;
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await client.rules.delete(selectedRuleId);
      setSuccess("Rule eliminada.");
      setSelectedRuleId("");
      await loadData();
    } catch (e) {
      setError(resolveApiError(e, "No se pudo eliminar rule."));
    } finally {
      setLoading(false);
    }
  }

  async function onAssignTierToRule() {
    if (!selectedRuleId || !assignTierId) {
      setError("Selecciona rule y tier para asignar.");
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      await client.rules.assignTiers(selectedRuleId, {
        customerTierIds: [assignTierId],
      });
      setSuccess("Tier asignado a la regla.");
    } catch (e) {
      setError(resolveApiError(e, "No se pudo asignar tier a la regla."));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="card usersPage">
        <div className="row">
          <h1>Rules + Customer Tiers</h1>
          <div className="row">
            <Link className="linkBtn" to="/dashboard">
              Dashboard
            </Link>
            <Link className="linkBtn" to="/discount-setup">
              Discount setup
            </Link>
          </div>
        </div>

        <p>Gestión básica de customer tiers y rules con asignación entre ambos.</p>
        {error ? <p className="error">{error}</p> : null}
        {success ? <p className="success">{success}</p> : null}

        <div className="usersGrid">
          <section>
            <h2>Customer Tiers</h2>
            <ul className="usersList">
              {tiers.map((t) => (
                <li key={t.id}>
                  <button
                    className={`userItem ${selectedTierId === t.id ? "selected" : ""}`}
                    onClick={() => {
                      setSelectedTierId(t.id);
                      setTierName(t.name);
                      setTierDiscount(String(t.discountPercentage));
                      setTierLevel(String(t.hierarchyLevel));
                      setAssignTierId(t.id);
                    }}
                  >
                    <span>{t.name}</span>
                    <small>{t.discountPercentage}%</small>
                  </button>
                </li>
              ))}
            </ul>
            <form className="form" onSubmit={onCreateTier}>
              <label>
                Name
                <input value={tierName} onChange={(e) => setTierName(e.target.value)} required />
              </label>
              <label>
                Discount %
                <input value={tierDiscount} onChange={(e) => setTierDiscount(e.target.value)} required />
              </label>
              <label>
                Hierarchy level
                <input value={tierLevel} onChange={(e) => setTierLevel(e.target.value)} required />
              </label>
              <div className="row">
                <button type="submit" disabled={loading || !ecommerceId}>
                  {loading ? "Procesando..." : "Crear tier"}
                </button>
                <button type="button" onClick={() => onUpdateTier()} disabled={loading || !selectedTierId}>
                  {loading ? "Procesando..." : "Actualizar tier"}
                </button>
                <button type="button" className="danger" onClick={onDeleteTier} disabled={loading || !selectedTierId}>
                  Eliminar tier
                </button>
              </div>
            </form>
          </section>

          <section>
            <h2>Rules</h2>
            <ul className="usersList">
              {rules.map((r) => (
                <li key={r.id}>
                  <button
                    className={`userItem ${selectedRuleId === r.id ? "selected" : ""}`}
                    onClick={() => {
                      setSelectedRuleId(r.id);
                      setRuleName(r.name);
                      setRuleDescription(r.description ?? "");
                      setRuleDiscount(String(r.discountPercentage));
                      setRulePriorityId(String(r.discountPriorityId));
                    }}
                  >
                    <span>{r.name}</span>
                    <small>{r.discountPercentage}%</small>
                  </button>
                </li>
              ))}
            </ul>

            <form className="form" onSubmit={onCreateRule}>
              <label>
                Name
                <input value={ruleName} onChange={(e) => setRuleName(e.target.value)} required />
              </label>
              <label>
                Description
                <input value={ruleDescription} onChange={(e) => setRuleDescription(e.target.value)} />
              </label>
              <label>
                Discount %
                <input value={ruleDiscount} onChange={(e) => setRuleDiscount(e.target.value)} required />
              </label>
              <label>
                Discount Priority ID (string)
                <input value={rulePriorityId} onChange={(e) => setRulePriorityId(e.target.value)} required />
              </label>
              <label>
                Attributes JSON
                <textarea
                  className="textArea"
                  value={ruleAttributesJson}
                  onChange={(e) => setRuleAttributesJson(e.target.value)}
                />
              </label>
              <div className="row">
                <button type="submit" disabled={loading}>
                  {loading ? "Procesando..." : "Crear rule"}
                </button>
                <button type="button" className="danger" onClick={onDeleteRule} disabled={loading || !selectedRuleId}>
                  Eliminar rule
                </button>
              </div>
            </form>

            <div className="form">
              <label>
                Tier a asignar (UUID)
                <input value={assignTierId} onChange={(e) => setAssignTierId(e.target.value)} />
              </label>
              <button onClick={onAssignTierToRule} disabled={loading || !selectedRuleId || !assignTierId}>
                Asignar tier a rule
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
