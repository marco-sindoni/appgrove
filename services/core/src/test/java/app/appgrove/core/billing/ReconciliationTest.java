package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import app.appgrove.core.billing.ReconciliationDtos.PayoutView;
import app.appgrove.core.billing.ReconciliationDtos.ReconciliationPeriod;
import app.appgrove.core.billing.ReconciliationDtos.ReconciliationTotals;
import app.appgrove.core.billing.ReconciliationDtos.ReconciliationView;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Riconciliazione fra ricavo lordo e denaro davvero accreditato (UC 0071): dalla pipeline webhook firmata
 * alla vista {@code lordo → commissioni → netto → accredito}.
 *
 * <p>Quello che questi test difendono è la promessa della vista: il netto è <b>derivato</b> e non
 * inventato, e si sa sempre se viene dal fornitore o da una nostra stima; un accredito quadra con la somma
 * dei netti che dichiara di contenere, e quando non quadra lo dice; uno storno esce dall'incassato senza
 * far apparire sbagliato un accredito già registrato; e valute diverse nello stesso accredito producono un
 * "non quadrabile" invece di una differenza priva di senso.
 *
 * <p>Le asserzioni sugli aggregati sono <b>differenziali</b> (misura prima, misura dopo): la vista è
 * cross-tenant e il database è condiviso con tutta la suite, quindi un totale assoluto sarebbe un test che
 * si rompe quando qualcun altro aggiunge una transazione.
 */
@QuarkusTest
class ReconciliationTest {

    @Inject
    PaddleSignature signature;

    @Inject
    WebhookIngestService ingest;

    @Inject
    PaddleWebhookConsumer consumer;

    @Inject
    InMemoryWebhookQueue queue;

    @Inject
    ReconciliationService reconciliation;

    @Inject
    ReconciliationMetrics metrics;

    @Inject
    PaymentFees fees;

    @Inject
    StubScenarioEmitter emitter;

    @Inject
    TestData data;

    @Inject
    AgroalDataSource ds;

    private final Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(120);

    /** Conto da cui si consulta la vista amministrativa: serve solo perché il token porti un tenant. */
    private String platformTenant;

    @BeforeEach
    void reset() {
        queue.clear();
        platformTenant = newTenant();
    }

    // ── commissione e netto per transazione ──────────────────────────────────

    @Test
    void laCommissioneDichiaratraDalFornitoreVinceSullaStima() {
        String tenant = newTenant();
        UUID app = newApp("declared");
        String tx = "txn_declared_" + UUID.randomUUID();

        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, tx, 5000, "EUR", base, 310));

        assertEquals(310, feeOf(tx), "la commissione è quella comunicata dal fornitore");
        assertEquals(4690, netOf(tx), "il netto è l'importo meno la commissione dichiarata");
        assertEquals("provider", sourceOf(tx), "la provenienza dice che il numero non è una nostra stima");
    }

    @Test
    void senzaCommissioneNelPayloadIlNettoVieneStimatoEMarcatoComeTale() {
        String tenant = newTenant();
        UUID app = newApp("estimated");
        String tx = "txn_estimated_" + UUID.randomUUID();

        deliver(WebhookFixtures.transaction(
                evt(), "transaction.completed", base, tenant, app, null, tx, 5000, "EUR", "monthly", null, base));

        int expectedFee = fees.estimate(5000); // 5% + quota fissa, la formula del listino
        assertEquals(expectedFee, feeOf(tx));
        assertEquals(5000 - expectedFee, netOf(tx));
        assertEquals("estimated", sourceOf(tx), "una stima non deve poter essere scambiata per un dato vero");
    }

    @Test
    void unTentativoFallitoNonHaCommissioneNeNetto() {
        String tenant = newTenant();
        UUID app = newApp("failed");
        String tx = "txn_failed_" + UUID.randomUUID();

        deliver(WebhookFixtures.transaction(
                evt(), "transaction.payment_failed", base, tenant, app, null, tx, 900, "EUR", "monthly", null, base));

        assertNull(feeOf(tx), "su un incasso mai avvenuto non c'è nulla da trattenere");
        assertEquals(0, netOf(tx));
    }

    // ── quadratura degli accrediti ───────────────────────────────────────────

    @Test
    void unAccreditoQuadraConLaSommaDeiNettiCheDichiara() {
        String tenant = newTenant();
        UUID app = newApp("settled");
        String tx1 = "txn_s1_" + UUID.randomUUID();
        String tx2 = "txn_s2_" + UUID.randomUUID();
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, tx1, 2000, "EUR", base, 150));
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base.plusSeconds(1), tenant, app, null, tx2, 3000, "EUR", base, 200));

        String payoutId = "pay_ok_" + UUID.randomUUID();
        deliver(WebhookFixtures.payout(
                evt(), base.plusSeconds(10), payoutId, 1850 + 2800, "EUR", base.plusSeconds(10),
                new WebhookFixtures.PayoutLine(tx1, 1850, "EUR"),
                new WebhookFixtures.PayoutLine(tx2, 2800, "EUR")));

        PayoutView payout = payout(payoutId);
        assertEquals(ReconciliationService.MATCHED, payout.status());
        assertEquals(4650, payout.amount());
        assertEquals(4650, payout.linesNet());
        assertEquals(0L, payout.difference());
        assertEquals(2, payout.lines());
        assertNotNull(payout.coveredFrom(), "l'intervallo coperto si deriva dagli addebiti collegati");
    }

    @Test
    void unAccreditoDiImportoDiversoDalDettaglioRisultaInScostamento() {
        String tenant = newTenant();
        UUID app = newApp("mismatch");
        String tx = "txn_m_" + UUID.randomUUID();
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, tx, 2000, "EUR", base, 150));

        String payoutId = "pay_ko_" + UUID.randomUUID();
        deliver(WebhookFixtures.payout(
                evt(), base.plusSeconds(10), payoutId, 1800, "EUR", base.plusSeconds(10),
                new WebhookFixtures.PayoutLine(tx, 1850, "EUR")));

        PayoutView payout = payout(payoutId);
        assertEquals(ReconciliationService.MISMATCH, payout.status());
        assertEquals(-50L, payout.difference(), "lo scostamento è quello che manca all'appello");
    }

    @Test
    void valuteDiverseNelloStessoAccreditoNonProduconoUnoScostamentoInventato() {
        String tenant = newTenant();
        UUID app = newApp("mixed");
        String txEur = "txn_eur_" + UUID.randomUUID();
        String txUsd = "txn_usd_" + UUID.randomUUID();
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, txEur, 2000, "EUR", base, 150));
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base.plusSeconds(1), tenant, app, null, txUsd, 2000, "USD", base, 150));

        String payoutId = "pay_mixed_" + UUID.randomUUID();
        deliver(WebhookFixtures.payout(
                evt(), base.plusSeconds(10), payoutId, 3700, "EUR", base.plusSeconds(10),
                new WebhookFixtures.PayoutLine(txEur, 1850, "EUR"),
                new WebhookFixtures.PayoutLine(txUsd, 1850, "USD")));

        PayoutView payout = payout(payoutId);
        assertEquals(ReconciliationService.MIXED_CURRENCY, payout.status());
        assertNull(payout.difference(), "sommare valute diverse darebbe un numero che sembra una differenza");
    }

    @Test
    void loStessoEventoDiAccreditoDueVolteNonRaddoppiaNulla() {
        String tenant = newTenant();
        UUID app = newApp("dupe");
        String tx = "txn_dup_" + UUID.randomUUID();
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, tx, 1000, "EUR", base, 100));

        String payoutId = "pay_dup_" + UUID.randomUUID();
        String body = WebhookFixtures.payout(
                evt(), base.plusSeconds(10), payoutId, 900, "EUR", base.plusSeconds(10),
                new WebhookFixtures.PayoutLine(tx, 900, "EUR"));
        deliver(body);
        deliver(body);

        PayoutView payout = payout(payoutId);
        assertEquals(1, payout.lines());
        assertEquals(900, payout.linesNet());
        assertEquals(ReconciliationService.MATCHED, payout.status());
    }

    @Test
    void unEventoDiAccreditoFuoriOrdineNonSovrascriveQuelloPiuRecente() {
        String tenant = newTenant();
        UUID app = newApp("ooo");
        String tx = "txn_ooo_" + UUID.randomUUID();
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, tx, 1000, "EUR", base, 100));

        String payoutId = "pay_ooo_" + UUID.randomUUID();
        deliver(WebhookFixtures.payout(
                evt(), base.plusSeconds(30), payoutId, 900, "EUR", base.plusSeconds(30),
                new WebhookFixtures.PayoutLine(tx, 900, "EUR")));
        // arriva dopo ma è accaduto prima: non deve riportare l'accredito a un importo sorpassato
        deliver(WebhookFixtures.payout(
                evt(), base, payoutId, 500, "EUR", base, new WebhookFixtures.PayoutLine(tx, 500, "EUR")));

        assertEquals(900, payout(payoutId).amount());
        assertEquals(900, payout(payoutId).linesNet(), "nemmeno il dettaglio deve tornare indietro");
    }

    // ── storni ───────────────────────────────────────────────────────────────

    @Test
    void unRimborsoEsceDallIncassatoEToraIndietroNellAccreditoSuccessivo() {
        String tenant = newTenant();
        UUID app = newApp("refund");
        String tx = "txn_ref_" + UUID.randomUUID();
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, tx, 2000, "EUR", base, 150));

        String firstPayout = "pay_ref_1_" + UUID.randomUUID();
        deliver(WebhookFixtures.payout(
                evt(), base.plusSeconds(10), firstPayout, 1850, "EUR", base.plusSeconds(10),
                new WebhookFixtures.PayoutLine(tx, 1850, "EUR")));

        ReconciliationTotals before = totals();
        // il rimborso cambia lo stato della riga esistente: il riferimento presso il fornitore è la chiave
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.refunded", base.plusSeconds(20), tenant, app, null, tx, 2000, "EUR", base, 150));
        ReconciliationTotals after = totals();

        assertEquals("refunded", statusOf(tx));
        assertEquals(0, netOf(tx), "un incasso restituito non è più netto per noi");
        assertEquals(-2000, after.gross() - before.gross(), "l'importo esce dall'incassato");
        assertEquals(2000, after.reversed() - before.reversed(), "e ricompare fra gli storni");

        // l'accredito già registrato NON deve diventare sbagliato per un fatto successivo
        assertEquals(ReconciliationService.MATCHED, payout(firstPayout).status());

        String secondPayout = "pay_ref_2_" + UUID.randomUUID();
        deliver(WebhookFixtures.payout(
                evt(), base.plusSeconds(30), secondPayout, -1850, "EUR", base.plusSeconds(30),
                new WebhookFixtures.PayoutLine(tx, -1850, "EUR")));

        PayoutView reversal = payout(secondPayout);
        assertEquals(ReconciliationService.MATCHED, reversal.status());
        assertEquals(-1850, reversal.amount(), "la restituzione è un accredito negativo, non un accredito mancante");
    }

    @Test
    void unaContestazioneRiduceLIncassatoComeUnRimborso() {
        String tenant = newTenant();
        UUID app = newApp("chargeback");
        String tx = "txn_cb_" + UUID.randomUUID();
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, tx, 1500, "EUR", base, 125));

        ReconciliationTotals before = totals();
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.disputed", base.plusSeconds(20), tenant, app, null, tx, 1500, "EUR", base, 125));
        ReconciliationTotals after = totals();

        assertEquals("disputed", statusOf(tx));
        assertEquals(0, netOf(tx));
        assertEquals(1500, after.reversed() - before.reversed());
    }

    // ── attribuzione per periodo ─────────────────────────────────────────────

    @Test
    void unAccreditoACavalloDiDueMesiLasciaOgniTransazioneNelMeseDelSuoAddebito() {
        String tenant = newTenant();
        UUID app = newApp("multiperiod");
        Instant lastMonth = base.atOffset(ZoneOffset.UTC).minusMonths(1).toInstant();
        String txOld = "txn_p1_" + UUID.randomUUID();
        String txNew = "txn_p2_" + UUID.randomUUID();

        String oldPeriod = period(lastMonth);
        String newPeriod = period(base);
        long oldGrossBefore = grossOf(oldPeriod);
        long newGrossBefore = grossOf(newPeriod);

        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, txOld, 1000, "EUR", lastMonth, 100));
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base.plusSeconds(1), tenant, app, null, txNew, 2000, "EUR", base, 150));

        String payoutId = "pay_span_" + UUID.randomUUID();
        deliver(WebhookFixtures.payout(
                evt(), base.plusSeconds(10), payoutId, 900 + 1850, "EUR", base.plusSeconds(10),
                new WebhookFixtures.PayoutLine(txOld, 900, "EUR"),
                new WebhookFixtures.PayoutLine(txNew, 1850, "EUR")));

        assertEquals(1000, grossOf(oldPeriod) - oldGrossBefore, "l'addebito del mese scorso resta nel mese scorso");
        assertEquals(2000, grossOf(newPeriod) - newGrossBefore, "quello di questo mese resta in questo mese");

        PayoutView payout = payout(payoutId);
        assertEquals(ReconciliationService.MATCHED, payout.status());
        assertTrue(
                payout.coveredFrom().isBefore(payout.coveredTo()),
                "l'accredito resta uno solo, con l'intervallo di addebiti che copre");
    }

    @Test
    void ilNettoNonAncoraAccreditatoSiRiduceQuandoLAccreditoArriva() {
        String tenant = newTenant();
        UUID app = newApp("unsettled");
        String tx = "txn_uns_" + UUID.randomUUID();

        ReconciliationTotals before = totals();
        deliver(WebhookFixtures.transactionWithFee(
                evt(), "transaction.completed", base, tenant, app, null, tx, 4000, "EUR", base, 250));
        ReconciliationTotals pending = totals();
        assertEquals(3750, pending.unsettled() - before.unsettled(), "finché non arriva l'accredito, il netto è fermo");

        deliver(WebhookFixtures.payout(
                evt(), base.plusSeconds(10), "pay_uns_" + UUID.randomUUID(), 3750, "EUR", base.plusSeconds(10),
                new WebhookFixtures.PayoutLine(tx, 3750, "EUR")));

        ReconciliationTotals settled = totals();
        assertEquals(0, settled.unsettled() - before.unsettled(), "una volta accreditato non è più in attesa");
        assertEquals(3750, settled.settled() - before.settled());
    }

    // ── scenari dello stub locale ────────────────────────────────────────────

    @Test
    void loScenarioLocaleDiAccreditoAccreditaIPagamentiCheTrova() {
        String tenant = newTenant();
        UUID app = newApp("stubpayout");
        UUID tier = pricedTier(app, 1900);

        emitter.emit(LifecycleScenario.happy_path, tenant, app, tier, null);
        consumer.drain();
        assertEquals(1, pendingCount(tenant), "il pagamento del percorso felice attende di essere accreditato");

        emitter.emit(LifecycleScenario.payout, tenant, app, tier, null);
        consumer.drain();
        assertEquals(0, pendingCount(tenant), "dopo l'accredito non resta nulla in attesa");
    }

    @Test
    void loScenarioLocaleDiAccreditoSenzaPagamentiLoDiceInveceDiFingere() {
        String tenant = newTenant();
        UUID app = newApp("stubempty");
        UUID tier = pricedTier(app, 1900);

        IllegalArgumentException error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> emitter.emit(LifecycleScenario.payout, tenant, app, tier, null));
        assertTrue(error.getMessage().contains("nessuna transazione da accreditare"));
    }

    @Test
    void loScenarioLocaleDiRimborsoRestituisceIlDenaroNellAccreditoSuccessivo() {
        String tenant = newTenant();
        UUID app = newApp("stubrefund");
        UUID tier = pricedTier(app, 2500);

        emitter.emit(LifecycleScenario.happy_path, tenant, app, tier, null);
        consumer.drain();
        emitter.emit(LifecycleScenario.refund, tenant, app, tier, null);
        consumer.drain();

        assertEquals(1, countWhere("status = 'refunded' and tenant_id = ?", tenant));
        assertEquals(0, countWhere("status = 'paid' and tenant_id = ?", tenant));
    }

    // ── osservabilità ────────────────────────────────────────────────────────

    @Test
    void unAccreditoAttesoOltreLaSogliaVieneSegnalato() {
        Instant old = Instant.now().minus(60, ChronoUnit.DAYS);
        ReconciliationTotals overdue =
                new ReconciliationTotals(10_000, 800, 9_200, 0, 0, 9_200, 5, 0, old);
        assertTrue(metrics.publish(overdue), "netto fermo da due mesi: l'accredito atteso non è arrivato");
    }

    @Test
    void unNettoAppenaMaturatoNonEUnAccreditoInRitardo() {
        ReconciliationTotals fresh =
                new ReconciliationTotals(10_000, 800, 9_200, 0, 0, 9_200, 5, 0, Instant.now());
        assertFalse(metrics.publish(fresh), "sotto soglia non c'è nulla da segnalare");
    }

    @Test
    void ilPesoDelleCommissioniSiMisuraSulLordoENonSiRompeSenzaIncassi() {
        assertEquals(10.0, ReconciliationService.feePercent(1000, 100));
        assertEquals(0d, ReconciliationService.feePercent(0, 0), "nessun incasso: nessuna percentuale, non un errore");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private void deliver(String body) {
        ingest.ingest(body, signature.sign(body));
        consumer.drain();
    }

    /**
     * La vista si legge <b>dall'endpoint amministrativo</b>, non invocando il servizio: le query native
     * passano dall'{@code EntityManager}, che apre la sessione con il risolutore di tenant — fuori da una
     * richiesta autenticata non c'è tenant e la lettura viene negata (fail-closed, invariante #1). Passare
     * di qui prova anche che la superficie sia davvero servita.
     */
    private ReconciliationView view() {
        return given().header("Authorization", "Bearer " + TestTokens.withTenant(platformTenant, "owner", "platform-admin"))
                .when().get("/api/platform/v1/admin/reconciliation")
                .then().statusCode(200)
                .extract().as(ReconciliationView.class);
    }

    private ReconciliationTotals totals() {
        return view().totals();
    }

    private PayoutView payout(String paddlePayoutId) {
        Optional<PayoutView> found = view().payouts().stream()
                .filter(p -> paddlePayoutId.equals(p.paddlePayoutId()))
                .findFirst();
        assertTrue(found.isPresent(), "accredito " + paddlePayoutId + " non trovato nella vista");
        return found.get();
    }

    private long grossOf(String period) {
        return view().periods().stream()
                .filter(p -> period.equals(p.period()))
                .findFirst()
                .map(ReconciliationPeriod::gross)
                .orElse(0L);
    }

    private static String period(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC).toString().substring(0, 7);
    }

    private Integer feeOf(String tx) {
        return (Integer) column(tx, "fee_amount");
    }

    private int netOf(String tx) {
        Object value = column(tx, "net_amount");
        return value == null ? 0 : ((Number) value).intValue();
    }

    private String sourceOf(String tx) {
        Object value = column(tx, "fee_source");
        return value == null ? null : value.toString();
    }

    private String statusOf(String tx) {
        return String.valueOf(column(tx, "status"));
    }

    /**
     * Lettura diretta della riga di transazione: la commissione e il netto non sono esposti al cliente
     * (la sua pagina di fatturazione mostra quanto ha pagato, non quanto abbiamo incassato), quindi
     * l'unico modo onesto di verificarli è guardare la riga.
     */
    private Object column(String paddleTransactionId, String column) {
        String sql = "select " + column + " from platform.billing_transaction where paddle_transaction_id = ?";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, paddleTransactionId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "transazione " + paddleTransactionId + " non registrata");
                Object value = rs.getObject(1);
                return rs.wasNull() ? null : value;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Fascia con listino: senza un prezzo lo stub non saprebbe quale importo mettere nella transazione. */
    private UUID pricedTier(UUID appId, int amount) {
        UUID tier = UUID.randomUUID();
        data.appTier(tier, appId, "Pro");
        data.appPrice(UUID.randomUUID(), tier, "monthly", "pri_" + tier.toString().substring(0, 8), amount);
        return tier;
    }

    /** Quante transazioni riuscite del conto nessun accredito ha ancora citato. */
    private int pendingCount(String tenant) {
        return countWhere(
                "status = 'paid' and tenant_id = ? and not exists ("
                        + " select 1 from platform.payout_line l"
                        + " where l.paddle_transaction_id = t.paddle_transaction_id)",
                tenant);
    }

    private int countWhere(String predicate, String tenant) {
        String sql = "select count(*) from platform.billing_transaction t"
                + " where t.deleted_at is null and " + predicate;
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenant);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private String newTenant() {
        String tenant = UUID.randomUUID().toString();
        data.account(tenant, "Conto " + tenant.substring(0, 8));
        return tenant;
    }

    private UUID newApp(String name) {
        UUID app = UUID.randomUUID();
        data.app(app, name + "-" + app.toString().substring(0, 8));
        return app;
    }

    private static String evt() {
        return "evt_rec_" + UUID.randomUUID();
    }
}
