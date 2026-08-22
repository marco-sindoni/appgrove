package app.appgrove.core.billing.seats;

import app.appgrove.core.billing.EntitlementInvalidationPublisher;
import app.appgrove.core.catalog.PlatformCatalog;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * <b>L'esecuzione</b> di una riduzione dei posti alla scadenza (UC 0104 §4.5): le persone indicate escono
 * davvero, i loro accessi alle applicazioni si cancellano, e la quantità dell'abbonamento dei posti
 * <b>scende</b> — per la prima volta nella storia dell'epica, perché in UC 0103 saliva soltanto.
 *
 * <h2>Perché SQL nativo con l'account esplicito</h2>
 *
 * Questo codice gira <b>fuori da una richiesta autenticata</b>: lo chiama il lavoro periodico e lo chiama
 * il consumatore degli eventi del fornitore di pagamento. Senza token il risolutore del perimetro di
 * Hibernate è chiuso per costruzione (fail-closed) e nessuna sessione si apre: la via è la stessa già usata
 * da {@code AccountDeletionSweeper}, {@code TenantOffboarding} e {@code SubscriptionWriter} — interfaccia di
 * programmazione verso la banca dati diretta, con il {@code tenant_id} passato a mano. Il dovere di chi
 * chiama è averlo ricavato dallo <b>stato del sistema</b> (una riga di riduzione, il perimetro di un evento
 * firmato) e mai da un valore arrivato dalla rete.
 *
 * <h2>Idempotenza: la proprietà che regge tutto</h2>
 *
 * Lo spazzino può girare due volte sulla stessa riduzione — una ripartenza, una sovrapposizione, un
 * ritentativo dopo un guasto a metà. Ogni passo è scritto per essere <b>ripetibile senza danni</b>:
 *
 * <ul>
 *   <li>la rimozione delle persone ha {@code deleted_at is null} nella condizione: una riga già cancellata
 *       non si ricancella, e la data della prima cancellazione resta quella vera;</li>
 *   <li>la quantità si <b>ricalcola</b> dal numero di posti effettivamente occupati e non si decrementa di
 *       un delta: un decremento applicato due volte lascerebbe l'account a pagare meno del dovuto, che è il
 *       verso in cui gli errori non si scoprono;</li>
 *   <li>la chiusura della riduzione è una scrittura <b>condizionata</b> allo stato {@code pending}: la
 *       seconda esecuzione non trova nulla da chiudere e non fa nulla.</li>
 * </ul>
 *
 * <p>E soprattutto: l'ordine è <b>persone → accessi → quantità → chiusura</b>, tutto nella stessa
 * transazione. Se si interrompe nel mezzo, nulla è stato applicato e la riduzione resta in attesa con la
 * data passata — cioè nello stato che la {@link SeatDowngradeMetrics misura} rende visibile.
 */
@ApplicationScoped
public class SeatDowngradeExecutor {

    private static final Logger LOG = Logger.getLogger(SeatDowngradeExecutor.class);

    private static final String DUE_TENANTS =
            "select tenant_id from platform.seat_downgrade"
                    + " where status = 'pending' and deleted_at is null and execute_at <= ?"
                    + " order by execute_at, tenant_id";

    private static final String OVERDUE_COUNT =
            "select count(*) from platform.seat_downgrade"
                    + " where status = 'pending' and deleted_at is null and execute_at <= ?";

    private static final String PENDING_OF_TENANT =
            "select id from platform.seat_downgrade"
                    + " where tenant_id = ? and status = 'pending' and deleted_at is null"
                    + " and execute_at <= ?";

    private static final String ITEMS_OF =
            "select identity_id from platform.seat_downgrade_item"
                    + " where downgrade_id = ? and tenant_id = ? and deleted_at is null";

    /**
     * Rimozione <b>logica</b> dell'appartenenza, come la fa la rimozione manuale ({@code UserResource}): la
     * storia dell'account resta leggibile. La condizione sul {@code deleted_at} è ciò che rende il passo
     * ripetibile.
     */
    private static final String CLOSE_MEMBERSHIP =
            "update platform.membership set deleted_at = ?, updated_at = ?, updated_by = 'seats'"
                    + " where tenant_id = ? and identity_id = ? and deleted_at is null";

    /**
     * Gli accessi alle applicazioni escono con la persona (UC 0098 §5): un permesso che sopravvive a chi non
     * fa più parte dell'account tornerebbe valido — silenziosamente, e con i poteri di prima — il giorno in
     * cui quella persona rientra.
     */
    private static final String REVOKE_ACCESSES =
            "update platform.app_access set deleted_at = ?, updated_at = ?, updated_by = 'seats'"
                    + " where tenant_id = ? and identity_id = ? and deleted_at is null";

    /**
     * La quantità <b>ricalcolata</b>: i posti a pagamento che restano occupati, cioè i posti occupati meno
     * la franchigia. I posti occupati si contano con la stessa regola di {@code SeatCount} — appartenenze
     * vive più inviti in attesa non scaduti — riscritta qui in SQL perché quella classe ha bisogno del
     * token. <b>Le due definizioni devono restare d'accordo</b>: un collaudo le confronta sullo stesso
     * stato, ed è l'unico presidio possibile contro la divergenza.
     */
    private static final String OCCUPIED_SEATS =
            "select (select count(*) from platform.membership"
                    + "         where tenant_id = ? and deleted_at is null)"
                    + "     + (select count(*) from platform.invitations"
                    + "         where tenant_id = ? and status = 'pending' and expires_at > ?"
                    + "           and deleted_at is null)";

    private static final String LOWER_QUANTITY =
            "update platform.subscription set quantity = ?, updated_at = now(), updated_by = 'seats'"
                    + " where tenant_id = ? and app_id = ? and deleted_at is null";

    private static final String CLOSE_DOWNGRADE =
            "update platform.seat_downgrade"
                    + " set status = 'executed', executed_at = ?, updated_at = ?, updated_by = 'seats'"
                    + " where id = ? and status = 'pending'";

    /** La versione del listino <b>vigente</b> a una data: la più recente fra quelle già decorse (UC 0102). */
    private static final String PRICING_VERSION =
            "select id from platform.seat_pricing_version"
                    + " where effective_from <= ? and deleted_at is null"
                    + " order by effective_from desc, id desc limit 1";

    /** Il primo posto <b>a pagamento</b> del listino: la franchigia finisce lì. */
    private static final String FIRST_PAID_SEAT =
            "select min(from_seat) from platform.seat_pricing_band"
                    + " where version_id = ? and unit_price_cents > 0 and deleted_at is null";

    /**
     * Chiude anche le righe delle persone indicate, e la regola è una sola per tutti i modi in cui una
     * riduzione finisce: <b>una riduzione che non è più in attesa non ha persone indicate vive</b>. Vale
     * per l'annullamento (che passa dal servizio) e per l'esecuzione (che passa da qui).
     *
     * <p>Nessuna informazione si perde: l'esportazione dei dati personali legge <b>anche</b> le righe
     * cancellate logicamente (art. 15: tutto ciò che è conservato), quindi «chi era stato indicato» resta
     * leggibile dove conta. Ciò che si guadagna è che la domanda «questa persona è indicata?» ha una sola
     * risposta possibile, invece di dipendere dallo stato della riga padre.
     *
     * <p>Questo passo, come gli altri, ha {@code deleted_at is null} nella condizione: una seconda
     * esecuzione non riscrive la data della prima.
     */
    private static final String CLOSE_ITEMS =
            "update platform.seat_downgrade_item set deleted_at = ?, updated_at = ?, updated_by = 'seats'"
                    + " where downgrade_id = ? and deleted_at is null";

    @Inject
    AgroalDataSource ds;

    @Inject
    EntitlementInvalidationPublisher invalidation;

    /**
     * Esegue tutte le riduzioni la cui data è passata, <b>una transazione per account</b>: un guasto su un
     * account non blocca gli altri — è la ragione per cui il ciclo è qui e non dentro una sola transazione
     * che abbraccia tutto.
     *
     * @return gli account su cui la riduzione è stata eseguita
     */
    public List<String> executeDue(Instant now) {
        List<String> done = new ArrayList<>();
        for (String tenantId : dueTenants(now)) {
            try {
                if (executeFor(tenantId, now)) {
                    done.add(tenantId);
                }
            } catch (RuntimeException e) {
                // Un account che fallisce resta in attesa con la data passata: la misura lo rende visibile
                // e il giro successivo ritenta. Non si rilancia, o il primo guasto fermerebbe tutti.
                LOG.errorf(
                        e,
                        "seats.reduction.execute-failed tenant_id=%s — la riduzione resta in attesa e sarà"
                                + " ritentata; se persiste, l'account paga posti che credeva chiusi",
                        tenantId);
            }
        }
        return done;
    }

    /**
     * Esegue la riduzione dovuta di <b>un</b> account, in una transazione propria.
     *
     * @return {@code true} se c'era qualcosa da eseguire ed è stato eseguito
     */
    public boolean executeFor(String tenantId, Instant now) {
        boolean executed;
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                executed = execute(c, tenantId, now);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e instanceof RuntimeException re
                        ? re
                        : new RuntimeException("esecuzione della riduzione fallita", e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "esecuzione della riduzione dei posti fallita per l'account " + tenantId, e);
        }
        if (executed) {
            // Fuori dalla transazione, e non per eleganza: i servizi delle applicazioni tengono una copia
            // locale del ruolo (UC 0099) e la rinfrescano leggendo lo stato. Pubblicare prima del commit
            // significherebbe farli leggere lo stato vecchio. La pubblicazione non è mai bloccante.
            invalidation.invalidateAllApps(tenantId, "seats.reduction.executed");
        }
        return executed;
    }

    /**
     * Esegue la riduzione dovuta di un account <b>sulla connessione data</b>, senza confermare la
     * transazione: è la forma che serve a chi ha già una transazione aperta e vuole che la riduzione
     * avvenga <b>prima</b> del proprio effetto — il caso dell'evento di rinnovo dell'abbonamento dei posti
     * (UC 0104 §5, «l'ordine conta»).
     *
     * @return {@code true} se c'era qualcosa da eseguire ed è stato eseguito
     */
    public boolean execute(Connection c, String tenantId, Instant now) throws SQLException {
        UUID downgradeId = pendingDue(c, tenantId, now);
        if (downgradeId == null) {
            return false;
        }
        List<UUID> people = itemsOf(c, downgradeId, tenantId);
        Timestamp at = Timestamp.from(now);
        int removed = 0;
        int revoked = 0;
        for (UUID identityId : people) {
            removed += update(c, CLOSE_MEMBERSHIP, at, at, tenantId, identityId);
            revoked += update(c, REVOKE_ACCESSES, at, at, tenantId, identityId);
        }

        int quantity = paidSeatsAfter(c, tenantId, now);
        update(c, LOWER_QUANTITY, quantity, tenantId, PlatformCatalog.seatsAppId());

        update(c, CLOSE_ITEMS, at, at, downgradeId);
        int closed = update(c, CLOSE_DOWNGRADE, at, at, downgradeId);
        if (closed == 0) {
            // Qualcun altro ha chiuso la riduzione mentre la eseguivamo: la transazione ha comunque
            // applicato le rimozioni, che sono idempotenti. Non è un guasto, è la concorrenza.
            LOG.infof(
                    "seats.reduction.execute tenant_id=%s reduction_id=%s già chiusa da un'altra esecuzione",
                    tenantId, downgradeId);
        }
        LOG.infof(
                "seats.reduction.executed tenant_id=%s reduction_id=%s people=%d memberships_closed=%d"
                        + " app_accesses_revoked=%d seat_quantity=%d",
                tenantId, downgradeId, people.size(), removed, revoked, quantity);
        return true;
    }

    /** Quante riduzioni sono <b>scadute e non eseguite</b>: la misura del guasto che costa al cliente. */
    public long overdueCount(Instant now) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(OVERDUE_COUNT)) {
            ps.setTimestamp(1, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("lettura delle riduzioni scadute fallita", e);
        }
    }

    // ── interno ──────────────────────────────────────────────────────────────

    private List<String> dueTenants(Instant now) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(DUE_TENANTS)) {
            ps.setTimestamp(1, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                List<String> tenants = new ArrayList<>();
                while (rs.next()) {
                    tenants.add(rs.getString(1));
                }
                return tenants;
            }
        } catch (SQLException e) {
            throw new RuntimeException("lettura delle riduzioni da eseguire fallita", e);
        }
    }

    private static UUID pendingDue(Connection c, String tenantId, Instant now) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(PENDING_OF_TENANT)) {
            ps.setString(1, tenantId);
            ps.setTimestamp(2, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, UUID.class) : null;
            }
        }
    }

    private static List<UUID> itemsOf(Connection c, UUID downgradeId, String tenantId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(ITEMS_OF)) {
            ps.setObject(1, downgradeId);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<UUID> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getObject(1, UUID.class));
                }
                return ids;
            }
        }
    }

    /**
     * I posti <b>a pagamento</b> che restano dopo le rimozioni: posti occupati meno la franchigia, mai
     * negativo. La franchigia si legge dal <b>listino vigente</b>, non è una costante (UC 0102).
     */
    private static int paidSeatsAfter(Connection c, String tenantId, Instant now) throws SQLException {
        int occupied;
        try (PreparedStatement ps = c.prepareStatement(OCCUPIED_SEATS)) {
            ps.setString(1, tenantId);
            ps.setString(2, tenantId);
            ps.setTimestamp(3, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                occupied = rs.next() ? rs.getInt(1) : 0;
            }
        }
        return Math.max(0, occupied - freeSeats(c, now));
    }

    /**
     * I posti compresi nella franchigia secondo il listino vigente, letti in SQL.
     *
     * <p><b>Perché non {@code SeatPricing.freeSeats}</b>, che è la definizione ufficiale: quella ha bisogno
     * dell'entità del listino, cioè di una sessione Hibernate, cioè di un token — che qui non c'è. La
     * lettura equivalente in SQL sfrutta una proprietà che il listino ha per vincolo di coerenza: le fasce
     * sono <b>contigue dal posto 1</b>, quindi la franchigia finisce dove comincia la prima fascia a
     * pagamento. Nessuna fascia a pagamento significa posti illimitati compresi.
     *
     * <p><b>Listino assente = errore</b>, non franchigia zero: senza listino non si sa quanti posti sono
     * compresi, e indovinare vorrebbe dire scrivere una quantità sbagliata sull'abbonamento di un cliente.
     * È la stessa postura di {@code SeatQuoteResource}.
     */
    private static int freeSeats(Connection c, Instant now) throws SQLException {
        UUID versionId;
        try (PreparedStatement ps = c.prepareStatement(PRICING_VERSION)) {
            ps.setTimestamp(1, Timestamp.from(now));
            try (ResultSet rs = ps.executeQuery()) {
                versionId = rs.next() ? rs.getObject(1, UUID.class) : null;
            }
        }
        if (versionId == null) {
            throw new NoSeatPricingVersionException(now);
        }
        try (PreparedStatement ps = c.prepareStatement(FIRST_PAID_SEAT)) {
            ps.setObject(1, versionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int firstPaid = rs.getInt(1);
                    // NULL = nessuna fascia a pagamento: tutti i posti sono compresi, quindi nulla da pagare.
                    return rs.wasNull() ? Integer.MAX_VALUE : firstPaid - 1;
                }
            }
        }
        return Integer.MAX_VALUE;
    }

    private static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }
}
