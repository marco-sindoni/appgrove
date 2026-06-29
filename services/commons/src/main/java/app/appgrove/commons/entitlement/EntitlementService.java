package app.appgrove.commons.entitlement;

import app.appgrove.commons.quota.QuotaNature;

/**
 * Faccia di piattaforma per l'enforcement entitlement/quota lato servizio (UC 0027), consumata dal
 * gate 402 ({@code @RequiresEntitlement}) e dalla {@code QuotaLimitSource} entitlement-driven. Astrae
 * <b>come</b> si ottiene l'entitlement (oggi: chiamata REST a core via {@link EntitlementClient}); il
 * codice di dominio dell'app dipende solo da questa interfaccia.
 */
public interface EntitlementService {

    /** {@code true} se il tenant del JWT ha accesso all'app (gate 2+3, #09 dec.30). */
    boolean hasAccess(String appSlug);

    /** Tetto della metrica per l'app dall'entitlement; {@code < 0} = nessun limite / sconosciuto. */
    long capFor(String appSlug, String metric);

    /** Natura della metrica (flow/stock) dall'entitlement; {@code null} se sconosciuta. */
    QuotaNature natureOf(String appSlug, String metric);
}
