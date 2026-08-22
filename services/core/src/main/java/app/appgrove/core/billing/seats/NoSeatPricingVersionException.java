package app.appgrove.core.billing.seats;

import java.time.Instant;

/**
 * Nessuna versione del listino dei posti è vigente alla data richiesta (UC 0102 §5).
 *
 * <p>Il calcolo si <b>nega</b> invece di inventare una tariffa: un dovuto calcolato su un listino
 * inesistente è peggio di un errore, perché nessuno lo scopre. Nella pratica non accade — il caricamento
 * iniziale crea la prima versione con decorrenza all'inizio dei tempi — e proprio per questo, se accade, è
 * un guasto di piattaforma da far vedere e non da assorbire.
 */
public class NoSeatPricingVersionException extends IllegalStateException {

    public NoSeatPricingVersionException(Instant when) {
        super("nessuna versione del listino dei posti vigente al " + when);
    }
}
