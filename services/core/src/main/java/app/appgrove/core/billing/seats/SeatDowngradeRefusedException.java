package app.appgrove.core.billing.seats;

/**
 * La riduzione dei posti è stata <b>rifiutata</b>, e il rifiuto è <b>lecito</b> (UC 0104 §5): non un
 * guasto, ma uno stato dell'account che non ammette quell'atto.
 *
 * <p>Porta con sé l'<b>identificativo stabile</b> del motivo, non solo un messaggio: il messaggio del
 * servizio è in italiano, l'interfaccia parla cinque lingue, e il testo che offre la via d'uscita va
 * scritto una volta per lingua e non tradotto a orecchio da un errore. Gli identificativi sono contratto:
 * cambiarli è un cambio di contratto.
 */
public class SeatDowngradeRefusedException extends RuntimeException {

    /** L'owner non è indicabile per la cessazione: chi governa l'account non può cessare se stesso. */
    public static final String TYPE_OWNER = "urn:appgrove:seats:reduction-owner";

    /** Esiste già una riduzione in attesa: una sola per account (indice unico parziale in banca dati). */
    public static final String TYPE_ALREADY_PENDING = "urn:appgrove:seats:reduction-already-pending";

    /**
     * Nessun posto a pagamento da ridurre: l'account è interamente dentro la franchigia e non ha un
     * abbonamento dei posti. Programmare una cessazione per fine periodo non gli darebbe alcun risparmio e
     * gli negherebbe gli inviti per un mese: la via giusta è la rimozione immediata, che è gratuita.
     */
    public static final String TYPE_NOT_NEEDED = "urn:appgrove:seats:reduction-not-needed";

    /** La persona indicata non appartiene a questo account (o non esiste): niente da cessare. */
    public static final String TYPE_PERSON_UNKNOWN = "urn:appgrove:seats:reduction-person-unknown";

    /** Non c'è alcuna riduzione in attesa: non c'è nulla da annullare né da cui togliere una persona. */
    public static final String TYPE_NONE_PENDING = "urn:appgrove:seats:reduction-none-pending";

    private final String type;

    public SeatDowngradeRefusedException(String type, String message) {
        super(message);
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
