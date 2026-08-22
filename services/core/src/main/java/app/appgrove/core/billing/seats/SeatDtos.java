package app.appgrove.core.billing.seats;

/**
 * Contratto di rete del riquadro dei posti (UC 0103 §6): tutto quello che serve per mostrare «quanti
 * posti usi, quanto paghi, quanto costa il prossimo» — e per mostrare la <b>stima prima della
 * conferma</b> dell'invito.
 *
 * <p><b>Ogni numero è calcolato dal servizio.</b> L'interfaccia non fa aritmetica: non somma scaglioni,
 * non sottrae la franchigia, non decide se la tariffa scende. Non è pignoleria — è la sola forma in cui
 * cinque traduzioni e un servizio dicono lo stesso importo. Il giorno in cui l'interfaccia calcolasse
 * anche un solo numero, quel numero sarebbe l'unico che il cliente vede e nessun collaudo del servizio lo
 * proverebbe.
 */
public final class SeatDtos {

    private SeatDtos() {}

    /**
     * Il riquadro dei posti dell'account corrente.
     *
     * @param usedSeats posti occupati in tutto (persone + inviti in attesa non scaduti)
     * @param composition come quel numero si compone, perché chi legge conta le righe della tabella
     * @param currency valuta del listino vigente
     * @param freeSeats posti compresi nella franchigia (le fasce iniziali a tariffa zero)
     * @param paidSeats posti a pagamento <b>oggi</b>, cioè quanti superano la franchigia
     * @param dueCents dovuto mensile per i posti occupati, in centesimi interi
     * @param paidQuantity posti a pagamento <b>già pagati</b> per il periodo in corso (0 senza abbonamento)
     * @param currentBand la fascia in cui cade l'<b>ultimo</b> posto occupato; assente con zero posti
     * @param next il posto successivo: quanto costa, e che effetto ha sul totale
     * @param pendingReduction c'è una riduzione in attesa? Oggi sempre falso (lo stato è di UC 0104), e il
     *     campo esiste perché il riquadro e il rifiuto dell'invito abbiano <b>un solo</b> posto da cui
     *     leggerlo quando arriverà — non per anticipare quella storia
     * @param hasSubscription esiste già l'abbonamento di piattaforma dei posti
     */
    public record SeatSummaryView(
            int usedSeats,
            SeatCompositionView composition,
            String currency,
            int freeSeats,
            int paidSeats,
            long dueCents,
            int paidQuantity,
            SeatPricingDtos.SeatBandView currentBand,
            NextSeatView next,
            boolean pendingReduction,
            boolean hasSubscription) {}

    /** La composizione dei posti occupati. La somma delle tre voci è {@code usedSeats}. */
    public record SeatCompositionView(int active, int suspended, int pendingInvitations) {}

    /**
     * Il posto successivo — la <b>stima</b> che l'owner vede prima di confermare un invito.
     *
     * @param seatNumber numero d'ordine che quel posto avrà («sarà il posto numero 4»)
     * @param unitPriceCents quanto costa <b>quel</b> posto al mese
     * @param dueCentsAfter dovuto mensile complessivo dopo quel posto: col listino progressivo <b>sale
     *     sempre</b>
     * @param chargeCents quanto si addebita <b>adesso</b>: zero quando il posto è dentro la franchigia
     *     oppure quando era già pagato in questo periodo (invito scaduto o revocato e rimpiazzato)
     * @param cheaperThanPrevious il posto successivo costa <b>meno</b> del precedente, perché entra nello
     *     scaglione seguente. È il caso che va detto per esteso: quello che scende è il costo del posto in
     *     più, il totale sale comunque — e un cliente che legge «costa meno» accanto a un totale più alto
     *     senza spiegazione pensa a un errore di conteggio
     */
    public record NextSeatView(
            int seatNumber,
            int unitPriceCents,
            long dueCentsAfter,
            long chargeCents,
            boolean cheaperThanPrevious) {}
}
