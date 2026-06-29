package app.appgrove.commons.entitlement;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Tetto (cap) di una metrica di quota, con la sua <b>natura</b> e la finestra. È la faccia "letta"
 * del JSON {@code app_tier.limits} esposta da {@code /me/entitlements} (UC 0027): l'app la usa per
 * risolvere il cap reale dall'entitlement (vedi {@link EntitlementService#capFor}) invece che da
 * configurazione locale.
 *
 * @param cap tetto della metrica; {@code < 0} = nessun limite
 * @param nature {@code "flow"} (finestra che si resetta) o {@code "stock"} (livello istantaneo) — vedi
 *     {@code QuotaNature}
 * @param window finestra per le metriche {@code flow} (es. {@code "month"}); {@code null} per {@code stock}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MetricLimit(long cap, String nature, String window) {}
