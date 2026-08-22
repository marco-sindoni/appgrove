package app.appgrove.core.billing.seats;

/**
 * Il fornitore di pagamento ha <b>rifiutato</b> l'addebito del posto (UC 0103 §5).
 *
 * <p>È la regola d'oro della storia, resa un tipo: <b>meglio un invito mancato che un posto attivo non
 * pagato</b>. Chi la solleva sta dicendo «non creare nulla», e l'unità di lavoro che la riceve non deve
 * lasciare dietro di sé né invito né abbonamento.
 *
 * <p>Porta con sé il <b>motivo del fornitore</b> perché quel motivo deve arrivare a chi ha invitato: una
 * carta scaduta si rimedia in due minuti, «operazione non riuscita» no.
 */
public class SeatChargeDeclinedException extends RuntimeException {

    private final String reason;

    public SeatChargeDeclinedException(String reason) {
        super("addebito del posto rifiutato dal fornitore di pagamento: " + reason);
        this.reason = reason;
    }

    /** Il motivo restituito dal fornitore, da mostrare a chi ha invitato. */
    public String getReason() {
        return reason;
    }
}
