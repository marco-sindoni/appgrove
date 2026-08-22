package app.appgrove.core.billing.seats;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.core.billing.Subscription;
import app.appgrove.core.billing.SubscriptionRepository;
import app.appgrove.core.billing.seats.SeatDowngradeDtos.ReductionPersonView;
import app.appgrove.core.billing.seats.SeatDowngradeDtos.ReductionPreview;
import app.appgrove.core.billing.seats.SeatDowngradeDtos.ReductionView;
import app.appgrove.core.catalog.PlatformCatalog;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Identity;
import app.appgrove.core.platform.IdentityRepository;
import app.appgrove.core.platform.Membership;
import app.appgrove.core.platform.MembershipRepository;
import app.appgrove.core.platform.MembershipRole;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * <b>Ridurre i posti non è immediato</b> (UC 0104): l'owner indica le persone da cessare, l'account entra
 * in riduzione in attesa, e alla scadenza del periodo già pagato la riduzione si esegue.
 *
 * <h2>Perché in attesa e non subito</h2>
 *
 * Il posto è stato pagato per tutto il mese — è la <b>permanenza minima mensile</b> dell'epica E22.2, la
 * stessa proprietà per cui un invito scaduto non produce un rimborso. Eseguire subito cambierebbe l'importo
 * a metà periodo e renderebbe la fattura inspiegabile: si pagherebbe una parte di mese a un prezzo e una
 * parte a un altro, con scaglioni che si spostano nel mezzo. Per questo la riduzione è <b>programmata</b> e
 * la quantità dell'abbonamento non si tocca alla richiesta.
 *
 * <p>Da qui discende la contropartita più importante, e va detta: <b>annullare non costa nulla</b>. Non c'è
 * niente da rimborsare né da riaddebitare, perché niente era stato cambiato.
 *
 * <h2>Che cosa fa questa classe, e che cosa non fa</h2>
 *
 * Questa è la parte <b>dentro una richiesta autenticata</b>: indicare, annullare, togliere una persona,
 * leggere lo stato, e il predicato che l'invito interroga. Lavora con le entità tenant-scoped, quindi il
 * filtro per account lo aggiunge Hibernate (invariante #2 mantenuto per costruzione).
 *
 * <p>L'<b>esecuzione</b> alla scadenza <b>non è qui</b>: gira fuori da una richiesta — lavoro periodico e
 * consumatore degli eventi del fornitore — dove non c'è token e il risolutore del perimetro di Hibernate è
 * chiuso. Vive in {@link SeatDowngradeExecutor}, a SQL nativo con l'account esplicito, come
 * {@code AccountDeletionSweeper} e {@code TenantOffboarding}.
 */
@ApplicationScoped
public class SeatDowngradeService {

    private static final Logger LOG = Logger.getLogger(SeatDowngradeService.class);

    @Inject
    SeatDowngradeRepository downgrades;

    @Inject
    SeatDowngradeItemRepository items;

    @Inject
    MembershipRepository memberships;

    @Inject
    IdentityRepository identities;

    @Inject
    SubscriptionRepository subscriptions;

    @Inject
    SeatPricingRepository pricing;

    @Inject
    SeatCount seatCount;

    @Inject
    CallerContext caller;

    @Inject
    AuditLogger audit;

    /**
     * <b>Il predicato che blocca le aggiunte</b> (UC 0104 §8): con una riduzione in attesa nessuna persona
     * nuova si aggiunge.
     *
     * <p>La ragione del divieto è pratica e vale la pena ricordarla dove il divieto vive: sommare
     * un'aggiunta e una riduzione dentro lo stesso periodo renderebbe il conto del periodo indecidibile —
     * un posto liberato e uno occupato non si compensano, perché quello liberato è pagato fino a scadenza e
     * quello nuovo si paga adesso — e la fattura risultante sarebbe inspiegabile a chi la legge.
     */
    public boolean blocksAdditions() {
        return downgrades.pending().isPresent();
    }

    /** La riduzione in attesa dell'account corrente, in forma di rete; vuoto se non c'è. */
    public Optional<ReductionView> pending() {
        return downgrades.pending().map(this::view);
    }

    /**
     * L'effetto <b>prima della conferma</b> (UC 0104 §4.2). Non crea nulla e non rifiuta nulla che non sia
     * un dato inutilizzabile: è una simulazione, e una simulazione che si nega non aiuta a decidere.
     *
     * <p>Le persone che non appartengono all'account vengono <b>ignorate</b> invece di far fallire la
     * lettura: la selezione a schermo può essere diventata vecchia (qualcuno è stato rimosso da un'altra
     * scheda) e un errore al posto della stima costringerebbe a ricaricare la pagina per capirlo. La
     * richiesta vera, invece, rifiuta: lì il rigore serve, perché lì si crea uno stato.
     */
    public ReductionPreview preview(List<UUID> userIds) {
        Instant now = Instant.now();
        SeatPricingVersion version = pricing.requireVigenteAl(now);
        List<Membership> targets = new ArrayList<>();
        for (UUID userId : distinct(userIds)) {
            memberships.findByIdentity(userId)
                    .filter(m -> m.getRole() != MembershipRole.owner)
                    .ifPresent(targets::add);
        }
        int seatsNow = seatCount.occupiedSeatsAt(now);
        int seatsAfter = Math.max(0, seatsNow - targets.size());
        return new ReductionPreview(
                seatSubscription().map(Subscription::getCurrentPeriodEnd).orElse(null),
                people(targets.stream().map(Membership::getIdentityId).toList()),
                seatsNow,
                seatsAfter,
                SeatPricing.dueCents(seatsNow, version),
                SeatPricing.dueCents(seatsAfter, version),
                version.getCurrency(),
                SeatDowngradeDtos.bands(SeatPricing.breakdown(seatsNow, version)),
                SeatDowngradeDtos.bands(SeatPricing.breakdown(seatsAfter, version)));
    }

    /**
     * <b>Indica le persone da cessare</b>: un atto unico su più persone, con una data di esecuzione comune.
     *
     * <p>L'ordine dei controlli non è casuale — si rifiuta <b>prima</b> di scrivere qualunque cosa:
     *
     * <ol>
     *   <li>esiste un abbonamento dei posti? Senza, non c'è nulla da ridurre (vedi
     *       {@link SeatDowngradeRefusedException#TYPE_NOT_NEEDED});</li>
     *   <li>c'è già una riduzione in attesa? Una sola per account;</li>
     *   <li>ogni persona indicata appartiene a questo account e <b>non è l'owner</b>.</li>
     * </ol>
     *
     * <p>Poi si scrive: la riduzione, con la data di esecuzione presa dalla fine del periodo
     * dell'abbonamento, e una riga per persona. <b>La quantità dell'abbonamento non viene toccata.</b>
     *
     * @throws SeatDowngradeRefusedException rifiuto lecito, con il suo identificativo stabile
     */
    public ReductionView request(List<UUID> userIds) {
        List<UUID> wanted = distinct(userIds);
        if (wanted.isEmpty()) {
            throw new SeatDowngradeRefusedException(
                    SeatDowngradeRefusedException.TYPE_PERSON_UNKNOWN,
                    "Indica almeno una persona da cessare.");
        }

        Subscription subscription = seatSubscription().orElseThrow(() -> new SeatDowngradeRefusedException(
                SeatDowngradeRefusedException.TYPE_NOT_NEEDED,
                "Non stai pagando alcun posto: per far uscire una persona subito rimuovila dall'elenco,"
                        + " è immediato e non costa nulla."));

        if (downgrades.pending().isPresent()) {
            throw new SeatDowngradeRefusedException(
                    SeatDowngradeRefusedException.TYPE_ALREADY_PENDING,
                    "C'è già una riduzione programmata: annullala prima di programmarne un'altra.");
        }

        for (UUID userId : wanted) {
            Membership membership = memberships.findByIdentity(userId).orElseThrow(
                    () -> new SeatDowngradeRefusedException(
                            SeatDowngradeRefusedException.TYPE_PERSON_UNKNOWN,
                            "Una delle persone indicate non fa parte di questo account."));
            if (membership.getRole() == MembershipRole.owner) {
                throw new SeatDowngradeRefusedException(
                        SeatDowngradeRefusedException.TYPE_OWNER,
                        "Chi governa l'account non può essere indicato per la cessazione.");
            }
        }

        // La data di esecuzione è la fine del periodo GIÀ PAGATO. Non «fra un mese» e non «a fine mese
        // solare»: il periodo è quello che l'abbonamento porta scritto, ed è l'unico istante in cui il
        // posto smette di essere stato pagato.
        Instant executeAt = subscription.getCurrentPeriodEnd();
        if (executeAt == null) {
            // Non può accadere: il periodo si scrive alla creazione dell'abbonamento. Se accadesse,
            // fermarsi è meglio che inventare una data — una riduzione con la data sbagliata è una
            // riduzione che si esegue nel mese sbagliato.
            throw new IllegalStateException(
                    "l'abbonamento dei posti non ha una fine di periodo: impossibile programmare la"
                            + " riduzione per l'account " + caller.tenantId());
        }

        SeatDowngrade downgrade = new SeatDowngrade(executeAt, requesterIdentity());
        downgrades.persist(downgrade);
        downgrades.flush(); // forza INSERT: l'identificativo serve alle righe figlie
        for (UUID userId : wanted) {
            items.persist(new SeatDowngradeItem(downgrade.getId(), userId));
        }
        items.flush();

        audit.success("seats.reduction.requested", Map.of(
                "reduction_id", downgrade.getId().toString(),
                "actor", caller.subject(),
                // Quante persone, non chi: il registro di audit non è il posto dei dati personali (#08/5).
                "people", Integer.toString(wanted.size()),
                "execute_at", executeAt.toString()));
        LOG.infof(
                "seats.reduction.requested reduction_id=%s people=%d execute_at=%s",
                downgrade.getId(), wanted.size(), executeAt);
        return view(downgrade);
    }

    /**
     * <b>Annulla</b> l'intera attesa. Ripristina tutto e non ha alcun effetto contabile: la quantità
     * dell'abbonamento non era mai stata cambiata.
     *
     * <p>Le righe delle persone si cancellano (logicamente) insieme all'atto: lasciarle vive renderebbe
     * ambiguo lo stato «questa persona è indicata?», che l'elenco delle persone interroga riga per riga.
     */
    public void cancel() {
        SeatDowngrade downgrade = requirePending();
        close(downgrade, SeatDowngradeStatus.cancelled);
        audit.success("seats.reduction.cancelled", Map.of(
                "reduction_id", downgrade.getId().toString(),
                "actor", caller.subject()));
        LOG.infof("seats.reduction.cancelled reduction_id=%s", downgrade.getId());
    }

    /**
     * Toglie <b>una singola persona</b> dall'elenco degli indicati.
     *
     * <p>Se restano <b>zero</b> persone la riduzione <b>si chiude da sé</b> come annullata: un'attesa senza
     * nessuno da cessare bloccherebbe gli inviti senza ridurre niente, che è il peggiore dei due mondi. È
     * la stessa chiusura automatica prevista per la persona indicata che nel frattempo viene rimossa
     * (UC 0104 §5).
     *
     * @return {@code true} se la riduzione è ancora in attesa, {@code false} se si è chiusa da sé
     */
    public boolean removeItem(UUID userId) {
        SeatDowngrade downgrade = requirePending();
        List<SeatDowngradeItem> current = items.of(downgrade.getId());
        SeatDowngradeItem target = current.stream()
                .filter(i -> i.getIdentityId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new SeatDowngradeRefusedException(
                        SeatDowngradeRefusedException.TYPE_PERSON_UNKNOWN,
                        "Questa persona non è fra quelle indicate per la cessazione."));
        target.markDeleted();
        if (current.size() == 1) {
            downgrade.setStatus(SeatDowngradeStatus.cancelled);
            audit.success("seats.reduction.cancelled", Map.of(
                    "reduction_id", downgrade.getId().toString(),
                    "actor", caller.subject(),
                    "reason", "no-people-left"));
            LOG.infof(
                    "seats.reduction.cancelled reduction_id=%s — nessuna persona indicata è rimasta",
                    downgrade.getId());
            return false;
        }
        audit.success("seats.reduction.person-removed", Map.of(
                "reduction_id", downgrade.getId().toString(),
                "actor", caller.subject(),
                "user_id", userId.toString()));
        return true;
    }

    /**
     * Toglie una persona dall'elenco degli indicati <b>se</b> vi si trova, senza rifiutare nulla se non vi
     * si trova: è la forma che serve alla <b>rimozione immediata</b> di una persona (UC 0104 §5).
     *
     * <p>Rimuovere subito qualcuno resta possibile ed è un'operazione <b>diversa</b> dalla cessazione
     * programmata: la persona esce dall'account adesso, e il suo posto resta pagato fino a scadenza — senza
     * rimborso, come sempre in questa epica. Ma la sua riga fra gli indicati non ha più senso, e se restava
     * l'unica l'attesa bloccherebbe gli inviti senza avere più nulla da ridurre: per questo, a zero
     * indicati, si chiude da sé.
     *
     * @return {@code true} se la persona era indicata e la riga è stata tolta
     */
    public boolean removeIfIndicated(UUID userId) {
        Optional<SeatDowngrade> pending = downgrades.pending();
        if (pending.isEmpty()) {
            return false;
        }
        List<SeatDowngradeItem> current = items.of(pending.get().getId());
        Optional<SeatDowngradeItem> target = current.stream()
                .filter(i -> i.getIdentityId().equals(userId))
                .findFirst();
        if (target.isEmpty()) {
            return false;
        }
        target.get().markDeleted();
        if (current.size() == 1) {
            pending.get().setStatus(SeatDowngradeStatus.cancelled);
            LOG.infof(
                    "seats.reduction.cancelled reduction_id=%s — l'ultima persona indicata è stata rimossa"
                            + " dall'account",
                    pending.get().getId());
        }
        return true;
    }

    /**
     * Chi è in cessazione, e da quando: quanto serve all'<b>elenco unico delle persone</b> per mostrare il
     * quarto stato («in cessazione dal 14 settembre») senza una seconda chiamata.
     *
     * <p>La data è <b>una sola</b> per tutte le persone indicate — è la proprietà per cui la riduzione è un
     * atto e non N contrassegni — quindi si porta una volta e non una per riga.
     */
    public record PendingReduction(Instant executeAt, Set<UUID> identityIds) {}

    /** La riduzione in attesa in forma minima, per le letture che devono solo marcare le righe. */
    public Optional<PendingReduction> pendingSummary() {
        return downgrades.pending().map(d -> new PendingReduction(
                d.getExecuteAt(), new LinkedHashSet<>(items.identitiesOf(d.getId()))));
    }

    // ── interno ──────────────────────────────────────────────────────────────

    private SeatDowngrade requirePending() {
        return downgrades.pending().orElseThrow(() -> new SeatDowngradeRefusedException(
                SeatDowngradeRefusedException.TYPE_NONE_PENDING,
                "Non c'è alcuna riduzione programmata."));
    }

    private void close(SeatDowngrade downgrade, SeatDowngradeStatus status) {
        downgrade.setStatus(status);
        items.of(downgrade.getId()).forEach(SeatDowngradeItem::markDeleted);
    }

    private ReductionView view(SeatDowngrade downgrade) {
        Instant now = Instant.now();
        SeatPricingVersion version = pricing.requireVigenteAl(now);
        List<UUID> identityIds = items.identitiesOf(downgrade.getId());
        int seatsNow = seatCount.occupiedSeatsAt(now);
        int seatsAfter = Math.max(0, seatsNow - identityIds.size());
        return new ReductionView(
                downgrade.getId(),
                downgrade.getExecuteAt(),
                downgrade.getCreatedAt(),
                downgrade.isOverdue(now),
                people(identityIds),
                seatsAfter,
                SeatPricing.dueCents(seatsNow, version),
                SeatPricing.dueCents(seatsAfter, version),
                version.getCurrency(),
                SeatDowngradeDtos.bands(SeatPricing.breakdown(seatsAfter, version)));
    }

    /**
     * Indirizzo e nome delle persone indicate. Si passa dall'identità <b>dopo</b> aver constatato
     * l'appartenenza (la lista arriva da righe di questo account): è la regola che tiene ogni lettura
     * dentro il confine dell'account — mai identità → appartenenze (UC 0116 §8).
     */
    private List<ReductionPersonView> people(List<UUID> identityIds) {
        List<ReductionPersonView> views = new ArrayList<>();
        for (UUID id : identityIds) {
            Identity identity = identities.findById(id);
            views.add(new ReductionPersonView(
                    id,
                    identity == null ? null : identity.getEmail(),
                    identity == null ? null : identity.getDisplayName()));
        }
        return views;
    }

    /** L'identità di chi sta chiedendo: la traccia di chi ha deciso, non un'autorizzazione. */
    private UUID requesterIdentity() {
        return identities.findByCognitoSub(caller.subject()).map(Identity::getId).orElse(null);
    }

    private Optional<Subscription> seatSubscription() {
        return subscriptions.findByApp(PlatformCatalog.seatsAppId());
    }

    /** Le persone indicate, senza duplicati e in ordine di indicazione. */
    private static List<UUID> distinct(List<UUID> userIds) {
        if (userIds == null) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(userIds.stream().filter(java.util.Objects::nonNull).toList()));
    }
}
