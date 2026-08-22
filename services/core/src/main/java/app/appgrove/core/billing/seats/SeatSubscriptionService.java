package app.appgrove.core.billing.seats;

import app.appgrove.core.billing.PaymentProvider;
import app.appgrove.core.billing.PaymentProvider.SeatChargeCommand;
import app.appgrove.core.billing.PaymentProvider.SeatChargeResult;
import app.appgrove.core.billing.PaymentProvider.SeatChargeReversal;
import app.appgrove.core.billing.Subscription;
import app.appgrove.core.billing.SubscriptionRepository;
import app.appgrove.core.billing.seats.SeatCount.SeatComposition;
import app.appgrove.core.billing.seats.SeatDtos.NextSeatView;
import app.appgrove.core.billing.seats.SeatDtos.SeatCompositionView;
import app.appgrove.core.billing.seats.SeatDtos.SeatSummaryView;
import app.appgrove.core.billing.seats.SeatPricingDtos.SeatBandView;
import app.appgrove.core.catalog.PlatformCatalog;
import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.AccountRepository;
import app.appgrove.core.platform.CallerContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Il cuore economico dell'epica 22: <b>quanto deve questo account per le sue persone, e come si paga il
 * posto in più</b> (UC 0103).
 *
 * <p>Due sole responsabilità, e vale la pena dire quali <b>non</b> sono:
 *
 * <ul>
 *   <li>il <b>riquadro</b> ({@link #summary()}): tutti i numeri che l'interfaccia mostra, calcolati qui.
 *       L'interfaccia non fa aritmetica;</li>
 *   <li>l'<b>acquisto del posto in più</b> ({@link #chargeOneMoreSeat()}): calcola, addebita, aggiorna
 *       l'abbonamento — e restituisce a chi chiama il pezzo di carta con cui creare l'invito.</li>
 * </ul>
 *
 * <p>Non decide <b>chi</b> può invitare (è l'annotazione sull'operazione di rete), non crea l'invito, non
 * manda email. Il calcolo delle tariffe non è qui ma in {@link SeatPricing}, che è una funzione pura: qui
 * si compone il calcolo con lo stato dell'account e con il fornitore di pagamento.
 *
 * <h2>L'ordine degli atti, e perché è quello</h2>
 *
 * Verifica → addebito → invito. Se l'addebito non riesce, l'invito <b>non nasce</b>: è preferibile un
 * invito mancato a un posto attivo non pagato. Il contrario — invito prima, addebito poi — produrrebbe
 * persone che lavorano senza che nessuno le stia pagando, e nessuno se ne accorgerebbe fino alla fattura.
 *
 * <h2>Il blocco sull'account, e il rischio vero</h2>
 *
 * Il conteggio dei posti e l'addebito vanno <b>serializzati per account</b> ({@link #lockAccount()},
 * blocco pessimistico sulla riga dell'account — la stessa via già in uso per le quote a giacenza). Il
 * rischio non è sforare un limite, perché un tetto massimo di posti non esiste: è <b>addebitare due volte
 * lo stesso salto di fascia</b>. Due clic simultanei che leggono entrambi «tre posti» calcolerebbero
 * entrambi «il quarto costa 2,99» e chiederebbero due volte lo stesso denaro per due persone di cui la
 * seconda dovrebbe costarne 2,99 in più, non gli stessi. Con il blocco, la seconda richiesta legge quattro
 * posti e paga il quinto.
 *
 * <p>Il blocco resta preso mentre si chiama il fornitore di pagamento, ed è una <b>deroga consapevole</b>
 * alla regola «mai una transazione aperta su una chiamata di rete»: la sequenza corretta a regime è
 * prenota → addebita fuori → conferma, e richiede uno stato di prenotazione che questa storia non
 * introduce. Oggi in ogni ambiente eseguibile il fornitore è il <b>simulatore</b>, che non fa rete; la
 * deroga e la via d'uscita sono scritte nei punti aperti di UC 0103, da chiudere prima che il fornitore
 * vero sia attivo.
 */
@ApplicationScoped
public class SeatSubscriptionService {

    private static final Logger LOG = Logger.getLogger(SeatSubscriptionService.class);

    @Inject
    EntityManager em;

    @Inject
    SeatCount seatCount;

    @Inject
    SeatPricingRepository pricing;

    @Inject
    SubscriptionRepository subscriptions;

    @Inject
    AccountRepository accounts;

    @Inject
    PaymentProvider provider;

    @Inject
    SeatSubscriptionWriter writer;

    @Inject
    CallerContext caller;

    @Inject
    SeatDowngradeService downgrades;

    /**
     * L'addebito eseguito (o non necessario) per un posto in più: quanto basta a chi crea l'invito per
     * collegarlo al suo addebito e, se qualcosa va storto, per annullarlo.
     *
     * @param chargeRef riferimento della transazione presso il fornitore; {@code null} quando non è stato
     *     necessario alcun addebito (posto dentro la franchigia, o già pagato in questo periodo)
     * @param previousQuantity posti a pagamento pagati <b>prima</b> di questo atto: il valore a cui tornare
     */
    public record SeatCharge(
            String chargeRef, String paddleSubscriptionId, int previousQuantity, int newQuantity) {

        /** Vero se è stato davvero chiesto del denaro al fornitore. */
        public boolean charged() {
            return chargeRef != null;
        }

        /** Nessun addebito necessario: il posto era gratuito o già pagato. */
        static SeatCharge none(int quantity) {
            return new SeatCharge(null, null, quantity, quantity);
        }
    }

    /**
     * Blocca la riga dell'account in modo pessimistico per la durata dell'unità di lavoro corrente.
     *
     * <p>Va chiamato <b>prima</b> di leggere il conteggio dei posti da chi sta per aggiungerne uno: è
     * l'atomicità del §5 dello use case, e senza di essa due inviti simultanei addebitano due volte lo
     * stesso salto di fascia. Non serve a chi solo legge il riquadro (una lettura stantia di un decimo di
     * secondo non fa danno e il blocco costerebbe una serializzazione a ogni apertura di pagina).
     */
    public void lockAccount() {
        Account account = em.find(Account.class, caller.tenantId(), LockModeType.PESSIMISTIC_WRITE);
        if (account == null) {
            // Non può accadere: il token porta un account che esiste. Se accade, è meglio fermarsi che
            // procedere senza serializzazione su un'operazione che muove denaro.
            throw new IllegalStateException("account del token non trovato: " + caller.tenantId());
        }
    }

    /** Il riquadro dei posti dell'account corrente, con tutti i suoi numeri già calcolati. */
    public SeatSummaryView summary() {
        Instant now = Instant.now();
        SeatPricingVersion version = pricing.requireVigenteAl(now);
        SeatComposition composition = seatCount.compositionAt(now);
        int used = composition.total();
        int free = SeatPricing.freeSeats(version);
        long due = SeatPricing.dueCents(used, version);

        Optional<Subscription> seatSubscription = seatSubscription();
        int paidQuantity = seatSubscription.map(Subscription::getQuantity).orElse(0);

        Optional<SeatDowngradeDtos.ReductionView> reduction = downgrades.pending();

        SeatPricingBand currentBand = used >= 1 ? SeatPricing.bandFor(used, version) : null;
        int nextUnitPrice = SeatPricing.nextSeatCents(used, version);
        long dueAfter = SeatPricing.dueCents(used + 1, version);

        return new SeatSummaryView(
                used,
                new SeatCompositionView(
                        composition.active(), composition.suspended(), composition.pendingInvitations()),
                version.getCurrency(),
                free,
                Math.max(0, used - free),
                due,
                paidQuantity,
                currentBand == null
                        ? null
                        : new SeatBandView(
                                currentBand.getFromSeat(),
                                currentBand.getToSeat(),
                                currentBand.getUnitPriceCents()),
                new NextSeatView(
                        used + 1,
                        nextUnitPrice,
                        dueAfter,
                        chargeForTarget(used + 1, paidQuantity, free, version),
                        currentBand != null && nextUnitPrice < currentBand.getUnitPriceCents()),
                // Riduzione in attesa (UC 0104): il campo booleano E il dettaglio, letti da UN SOLO posto —
                // il riquadro, l'avviso e il rifiuto dell'invito non devono poter dissentire fra loro.
                reduction.isPresent(),
                reduction.orElse(null),
                seatSubscription.isPresent());
    }

    /**
     * Addebita il posto in più e porta l'abbonamento dei posti alla quantità nuova.
     *
     * <p>Da chiamare <b>dopo</b> {@link #lockAccount()} e <b>prima</b> di creare l'invito. Restituisce
     * l'addebito eseguito, o un addebito «nessuno» quando il posto non costa niente:
     *
     * <ul>
     *   <li>il posto è dentro la <b>franchigia</b>: nessuna chiamata al fornitore, nessun abbonamento. È
     *       il caso dei primi tre posti, e non c'è alcuna condizione «se i posti sono al massimo tre»: il
     *       dovuto semplicemente non cambia, perché la prima fascia è a tariffa zero;</li>
     *   <li>il posto era <b>già pagato</b> in questo periodo, perché un invito è scaduto o è stato
     *       revocato e questo lo rimpiazza. Non si paga due volte lo stesso posto nello stesso mese, e non
     *       si rimborsa: è la permanenza minima mensile dell'epica E22.2.</li>
     * </ul>
     *
     * @throws SeatChargeDeclinedException il fornitore ha rifiutato: l'invito non deve nascere
     * @throws NoSeatPricingVersionException nessun listino vigente: il calcolo si nega invece di inventare
     */
    public SeatCharge chargeOneMoreSeat() {
        Instant now = Instant.now();
        SeatPricingVersion version = pricing.requireVigenteAl(now);
        int used = seatCount.occupiedSeatsAt(now);
        int target = used + 1;
        int free = SeatPricing.freeSeats(version);

        Optional<Subscription> existing = seatSubscription();
        int paidQuantity = existing.map(Subscription::getQuantity).orElse(0);
        int targetQuantity = Math.max(0, target - free);

        long delta = chargeForTarget(target, paidQuantity, free, version);
        if (delta <= 0) {
            LOG.infof(
                    "seats.charge.skipped used=%d target=%d free=%d paid_quantity=%d — nessun denaro dovuto",
                    used, target, free, paidQuantity);
            return SeatCharge.none(paidQuantity);
        }

        Account account = accounts.findById(caller.tenantId());
        String paddleSubscriptionId = existing.map(Subscription::getPaddleSubscriptionId).orElse(null);
        SeatChargeResult result = provider.chargeSeats(new SeatChargeCommand(
                caller.tenantId().toString(),
                paddleSubscriptionId,
                account == null ? null : account.getPaddleCustomerId(),
                caller.email(),
                targetQuantity,
                SeatPricing.dueCents(target, version),
                delta,
                version.getCurrency()));

        if (!result.accepted()) {
            LOG.warnf(
                    "seats.charge.declined target=%d quantity=%d delta_cents=%d reason=%s",
                    target, targetQuantity, delta, result.declineReason());
            throw new SeatChargeDeclinedException(result.declineReason());
        }

        // Cliente pigro (#09 C15): al primo acquisto in assoluto l'identificativo presso il fornitore
        // nasce qui, esattamente come nel checkout di una applicazione.
        if (account != null && account.getPaddleCustomerId() == null && result.paddleCustomerId() != null) {
            account.setPaddleCustomerId(result.paddleCustomerId());
        }

        writer.upsert(caller.tenantId(), targetQuantity, result.paddleSubscriptionId());
        LOG.infof(
                "seats.charge.accepted target=%d quantity=%d→%d delta_cents=%d due_cents=%d txn=%s",
                target, paidQuantity, targetQuantity, delta,
                SeatPricing.dueCents(target, version), result.paddleTransactionId());
        return new SeatCharge(
                result.paddleTransactionId(),
                result.paddleSubscriptionId(),
                paidQuantity,
                targetQuantity);
    }

    /**
     * Annulla un addebito appena eseguito, perché quello che doveva autorizzare non è mai nato (UC 0103
     * §5): storno presso il fornitore e ritorno alla quantità precedente.
     *
     * <p>Se anche l'annullamento fallisce si registra un <b>avviso di severità alta</b> e non si solleva
     * nulla: chi chiama sta già gestendo un guasto, e sostituirgli l'errore originale con un secondo errore
     * gli toglierebbe l'unica informazione utile. Questo è uno dei pochi casi in cui una persona deve
     * intervenire — un posto pagato che nessuno occupa — e va reso visibile, non sepolto.
     */
    public void release(SeatCharge charge) {
        if (!charge.charged()) {
            return;
        }
        try {
            provider.releaseSeatCharge(new SeatChargeReversal(
                    caller.tenantId().toString(),
                    charge.paddleSubscriptionId(),
                    charge.chargeRef(),
                    charge.previousQuantity()));
            writer.restoreQuantity(caller.tenantId(), charge.previousQuantity());
            LOG.warnf(
                    "seats.charge.released txn=%s quantity=%d→%d — l'invito non è nato, l'addebito è stato"
                            + " annullato",
                    charge.chargeRef(), charge.newQuantity(), charge.previousQuantity());
        } catch (RuntimeException e) {
            LOG.errorf(
                    e,
                    "seats.charge.release-failed txn=%s quantity=%d — ADDEBITO NON ANNULLATO: l'account ha"
                            + " pagato un posto che nessuno occupa. Intervento manuale necessario.",
                    charge.chargeRef(), charge.newQuantity());
        }
    }

    /** L'abbonamento di piattaforma dei posti dell'account corrente, se esiste (lettura per account, #2). */
    private Optional<Subscription> seatSubscription() {
        return subscriptions.findByApp(PlatformCatalog.seatsAppId());
    }

    /**
     * Quanto si addebita <b>adesso</b> per arrivare a {@code target} posti: la differenza fra il dovuto dei
     * posti bersaglio e quello dei posti <b>già pagati</b> nel periodo. Mai negativo: una riduzione non è
     * un rimborso (UC 0104 la governa a fine periodo, e senza restituire denaro).
     */
    private static long chargeForTarget(
            int target, int paidQuantity, int freeSeats, SeatPricingVersion version) {
        long alreadyPaid = SeatPricing.dueCents(safeSeats(paidQuantity, freeSeats), version);
        return Math.max(0, SeatPricing.dueCents(target, version) - alreadyPaid);
    }

    /**
     * I posti corrispondenti a una quantità pagata: la franchigia più i posti a pagamento. Con la
     * franchigia illimitata (fascia gratuita aperta) non ci sono posti a pagamento e il conto è zero.
     */
    private static int safeSeats(int paidQuantity, int freeSeats) {
        if (freeSeats == Integer.MAX_VALUE) {
            return 0;
        }
        return freeSeats + paidQuantity;
    }
}
