package app.appgrove.core.billing.seats;

/**
 * Il listino letto non è utilizzabile: fasce non contigue, prima fascia che non parte dal posto 1, ultima
 * fascia chiusa, tariffa negativa (UC 0102).
 *
 * <p>È un difetto di configurazione, non un caso d'uso: si preferisce un servizio che non parte a un
 * servizio che fattura su un listino con un buco. Il messaggio dice <b>qual è</b> il difetto, perché chi
 * lo legge sta guardando un avvio fallito e non ha altro in mano.
 */
public class IncoherentSeatPricingException extends IllegalStateException {

    public IncoherentSeatPricingException(String message) {
        super(message);
    }
}
