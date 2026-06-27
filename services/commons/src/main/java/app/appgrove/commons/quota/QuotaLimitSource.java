package app.appgrove.commons.quota;

/**
 * Sorgente del <b>tetto</b> (cap) di una metrica per un tenant. È il <b>seam</b> fra il meccanismo
 * di quota (che conta l'uso nella finestra) e la sua <b>provenienza</b>:
 * <ul>
 *   <li>in locale (UC 0051) l'implementazione è <b>config-driven</b> (tetto fisso da configurazione);</li>
 *   <li>l'enforcement reale che risolve il tetto dall'entitlement
 *       (subscription → tier → {@code app_tier.limits}) è di <b>UC 0027</b> e fornirà l'implementazione
 *       "vera" senza toccare il codice dell'app.</li>
 * </ul>
 * Il parametro {@code tenantId} è già nella firma per essere a prova di futuro (l'impl di 0027 ne ha
 * bisogno; quella locale lo ignora).
 */
public interface QuotaLimitSource {

    /** Tetto per la metrica nel tenant indicato. Negativo = nessun limite. */
    long capFor(String tenantId, String metric);
}
