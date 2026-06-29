package app.appgrove.commons.entitlement;

import java.util.List;

/**
 * Read-model degli entitlement del tenant corrente (UC 0027): l'elenco delle app a cui ha accesso,
 * con i tetti di quota per metrica. È l'unica fonte consumata sia dal <b>frontend</b> (popola il
 * registry/sidebar) sia dalle <b>app</b> (gate 402 + risoluzione cap quota, via {@link EntitlementService}).
 *
 * @param entitlements app entitled del tenant (vuoto = nessun accesso)
 */
public record MeEntitlementsView(List<EntitlementView> entitlements) {}
