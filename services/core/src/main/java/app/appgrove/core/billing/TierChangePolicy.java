package app.appgrove.core.billing;

import app.appgrove.commons.entitlement.MetricLimit;
import java.util.Map;

/**
 * Logica <b>pura</b> di gating del cambio piano (UC 0028 §4.2, #09 E23), senza I/O: classifica
 * upgrade/downgrade e verifica il vincolo <b>stock</b> sul downgrade. Testabile in isolamento.
 *
 * <ul>
 *   <li><b>Direzione</b>: confronto per importo del ciclo richiesto — target &lt; corrente = downgrade
 *       (schedulato a fine periodo), altrimenti immediato (upgrade / stesso prezzo). La semantica dei tier
 *       la conosciamo solo noi (#09 G33): il prezzo è il proxy stabile.</li>
 *   <li><b>Vincolo stock</b> (E23): un downgrade verso un tier con cap <b>stock</b> inferiore all'uso
 *       corrente è <b>bloccato</b> con remediation (non si può scendere sotto lo stato già occupato). Le
 *       metriche <b>flow</b> (finestra che si resetta) non bloccano il cambio: si applicano dal periodo
 *       successivo.</li>
 * </ul>
 *
 * <p>Nota (UC 0028): l'<b>uso corrente</b> per metrica è applicativo e non ancora leggibile da core
 * (vedi 0028 §Punti aperti — sorgente usage per-app differita). Finché non è cablato, il resource passa
 * una mappa vuota → nessun blocco stock a runtime; la logica qui resta reale e coperta da test.
 */
final class TierChangePolicy {

    private TierChangePolicy() {}

    /** Direzione del cambio piano derivata dagli importi (minor units) del ciclo richiesto. */
    enum Direction {
        UPGRADE,
        DOWNGRADE,
        SAME
    }

    /** Esito del gating: {@code blocked=false} = consentito; se bloccato, {@code remediation} spiega perché. */
    record Decision(boolean blocked, String remediation) {
        static Decision allow() {
            return new Decision(false, null);
        }

        static Decision block(String remediation) {
            return new Decision(true, remediation);
        }
    }

    /** Downgrade se l'importo target è strettamente inferiore al corrente; altrimenti immediato. */
    static Direction direction(int currentAmount, int targetAmount) {
        if (targetAmount < currentAmount) {
            return Direction.DOWNGRADE;
        }
        return targetAmount > currentAmount ? Direction.UPGRADE : Direction.SAME;
    }

    /**
     * Verifica il vincolo stock per un downgrade: se una metrica {@code stock} del tier di destinazione ha
     * cap ≥ 0 e l'uso corrente lo supera, blocca con remediation. Le metriche flow non bloccano.
     *
     * @param targetLimits limiti del tier di destinazione ({@code metric → {cap, nature, window}})
     * @param currentUsage uso corrente per metrica (vuoto se non disponibile → nessun blocco)
     */
    static Decision evaluateDowngrade(Map<String, MetricLimit> targetLimits, Map<String, Long> currentUsage) {
        for (Map.Entry<String, MetricLimit> entry : targetLimits.entrySet()) {
            MetricLimit limit = entry.getValue();
            if (limit == null || !"stock".equals(limit.nature()) || limit.cap() < 0) {
                continue; // solo le metriche stock con cap definito vincolano il downgrade
            }
            Long used = currentUsage.get(entry.getKey());
            if (used != null && used > limit.cap()) {
                return Decision.block(
                        "Downgrade bloccato: la metrica '" + entry.getKey() + "' è a " + used
                                + ", sopra il limite " + limit.cap() + " del piano scelto. Riduci l'uso"
                                + " sotto il limite prima di effettuare il downgrade.");
            }
        }
        return Decision.allow();
    }
}
