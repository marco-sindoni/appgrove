package app.appgrove.core.billing;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Storico pagamenti e ricevute (UC 0096): dalla pipeline webhook firmata alla lettura
 * {@code GET /api/platform/v1/me/payments}.
 *
 * <p>Quello che questi test difendono è la promessa della pagina: <b>tutte</b> le transazioni del conto,
 * anche fallite; una sola riga per transazione anche sotto ri-consegna; un evento vecchio che non
 * riscrive un esito più recente; e nessun modo per un conto di vedere i pagamenti di un altro.
 */
@QuarkusTest
class PaymentsApiTest {

    private static final String PAYMENTS = "/api/platform/v1/me/payments";

    @Inject
    PaddleSignature signature;

    @Inject
    WebhookIngestService ingest;

    @Inject
    PaddleWebhookConsumer consumer;

    @Inject
    InMemoryWebhookQueue queue;

    @Inject
    TestData data;

    private final Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(60);

    @BeforeEach
    void reset() {
        queue.clear();
    }

    @Test
    void pagamentoRiuscitoCompareNelloStoricoConTuttoIlNecessario() {
        String tenant = newTenant();
        UUID app = UUID.randomUUID();
        data.app(app, "fatture-storico"); // l'helper condiviso usa lo slug anche come nome
        UUID tier = newTier(app, "Pro");

        deliver(WebhookFixtures.transaction(
                evt(), "transaction.completed", base, tenant, app, tier,
                "txn_ok_1", 1200, "EUR", "monthly", "https://paddle.example/r/1", base));

        payments(tenant, "owner")
                .statusCode(200)
                .body("payments.size()", is(1))
                .body("payments[0].appSlug", is("fatture-storico"))
                .body("payments[0].appName", is("fatture-storico"))
                .body("payments[0].planName", is("Pro"))
                .body("payments[0].billingCycle", is("monthly"))
                .body("payments[0].amount", is(1200))
                .body("payments[0].currency", is("EUR"))
                .body("payments[0].status", is("paid"))
                .body("payments[0].receiptUrl", is("https://paddle.example/r/1"));
    }

    @Test
    void pagamentoFallitoEContestazioneCompaionoConIlLoroEsito() {
        String tenant = newTenant();
        UUID app = newApp("Notes");
        UUID tier = newTier(app, "Notes Pro");

        deliver(WebhookFixtures.transaction(
                evt(), "transaction.payment_failed", base, tenant, app, tier,
                "txn_ko_1", 800, "EUR", "monthly", null, base));
        deliver(WebhookFixtures.transaction(
                evt(), "transaction.disputed", base.plusSeconds(10), tenant, app, tier,
                "txn_ko_2", 800, "EUR", "monthly", null, base.plusSeconds(10)));

        payments(tenant, "owner")
                .statusCode(200)
                .body("payments.size()", is(2))
                // dalla più recente: prima la contestazione, poi il pagamento fallito
                .body("payments[0].status", is("disputed"))
                .body("payments[1].status", is("failed"));
    }

    @Test
    void ricevutaNonDisponibileLasciaLaRigaSenzaCollegamento() {
        String tenant = newTenant();
        UUID app = newApp("Bookings");

        deliver(WebhookFixtures.transaction(
                evt(), "transaction.completed", base, tenant, app, null,
                "txn_noreceipt", 1100, "EUR", "monthly", null, base));

        payments(tenant, "owner")
                .statusCode(200)
                .body("payments.size()", is(1))
                .body("payments[0].receiptUrl", is(nullValue()))
                // nessuna fascia collegata: la riga resta, senza inventare il nome di un piano
                .body("payments[0].planName", is(nullValue()));
    }

    @Test
    void loStessoEventoDueVolteProduceUnaSolaRiga() {
        String tenant = newTenant();
        UUID app = newApp("Expenses");
        String eventId = evt();
        String body = WebhookFixtures.transaction(
                eventId, "transaction.completed", base, tenant, app, null,
                "txn_dup", 700, "EUR", "monthly", null, base);

        deliver(body);
        deliver(body); // stessa consegna: il dedup della pipeline la scarta

        payments(tenant, "owner").statusCode(200).body("payments.size()", is(1));
    }

    @Test
    void dueEventiDiversiSullaStessaTransazioneNonLaDuplicano() {
        String tenant = newTenant();
        UUID app = newApp("Teams");

        // tentativo fallito e poi riuscito sulla stessa transazione: una riga sola, l'esito più recente
        deliver(WebhookFixtures.transaction(
                evt(), "transaction.payment_failed", base, tenant, app, null,
                "txn_retry", 900, "EUR", "monthly", null, base));
        deliver(WebhookFixtures.transaction(
                evt(), "transaction.completed", base.plusSeconds(30), tenant, app, null,
                "txn_retry", 900, "EUR", "monthly", "https://paddle.example/r/2", base.plusSeconds(30)));

        payments(tenant, "owner")
                .statusCode(200)
                .body("payments.size()", is(1))
                .body("payments[0].status", is("paid"))
                .body("payments[0].receiptUrl", is("https://paddle.example/r/2"));
    }

    @Test
    void unEventoFuoriOrdineNonRegredisceLEsito() {
        String tenant = newTenant();
        UUID app = newApp("Legacy");

        deliver(WebhookFixtures.transaction(
                evt(), "transaction.completed", base.plusSeconds(30), tenant, app, null,
                "txn_ooo", 500, "EUR", "monthly", null, base.plusSeconds(30)));
        // arriva DOPO ma è accaduto PRIMA: non deve riportare la riga a "fallito"
        deliver(WebhookFixtures.transaction(
                evt(), "transaction.payment_failed", base, tenant, app, null,
                "txn_ooo", 500, "EUR", "monthly", null, base));

        payments(tenant, "owner").statusCode(200).body("payments[0].status", is("paid"));
    }

    @Test
    void unEventoDiTransazioneSenzaValutaNonSporcaLoStorico() {
        String tenant = newTenant();
        UUID app = newApp("Broken");

        // dati economici incompleti: meglio nessuna riga che una riga con un importo inventato
        deliver(WebhookFixtures.transaction(
                evt(), "transaction.completed", base, tenant, app, null,
                "txn_nocurrency", 0, null, null, null, base));

        payments(tenant, "owner").statusCode(200).body("payments.size()", is(0));
    }

    @Test
    void unEventoDiAbbonamentoNonProduceRigheDiStorico() {
        String tenant = newTenant();
        UUID app = newApp("Plain");

        deliver(WebhookFixtures.subscription(
                evt(), "subscription.activated", "active", base, tenant, app, null,
                base.plus(365, ChronoUnit.DAYS)));

        payments(tenant, "owner").statusCode(200).body("payments.size()", is(0));
    }

    @Test
    void loStoricoEIsolatoPerConto() {
        String tenantA = newTenant();
        String tenantB = newTenant();
        UUID app = newApp("Shared");

        deliver(WebhookFixtures.transaction(
                evt(), "transaction.completed", base, tenantA, app, null,
                "txn_a", 1000, "EUR", "monthly", null, base));

        payments(tenantA, "owner").statusCode(200).body("payments.size()", is(1));
        payments(tenantB, "owner").statusCode(200).body("payments.size()", is(0));
    }

    @Test
    void loStoricoERiservatoAOwnerEAdmin() {
        String tenant = newTenant();
        UUID app = newApp("Roles");
        deliver(WebhookFixtures.transaction(
                evt(), "transaction.completed", base, tenant, app, null,
                "txn_roles", 100, "EUR", "monthly", null, base));

        payments(tenant, "owner").statusCode(200);
        payments(tenant, "admin").statusCode(200);
        // quanto ha pagato il workspace non è informazione che serva a un membro per lavorare
        payments(tenant, "member").statusCode(403);
        given().when().get(PAYMENTS).then().statusCode(401);
    }

    @Test
    void senzaContoNelTokenSiRifiutaInveceDiIndovinare() {
        given().header("Authorization", "Bearer " + TestTokens.withRolesNoTenant("owner"))
                .when().get(PAYMENTS)
                .then().statusCode(403);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private void deliver(String body) {
        ingest.ingest(body, signature.sign(body));
        consumer.drain();
    }

    private static ValidatableResponse payments(String tenant, String role) {
        return given().header("Authorization", "Bearer " + TestTokens.withTenant(tenant, role))
                .when().get(PAYMENTS)
                .then();
    }

    private String newTenant() {
        String tenant = UUID.randomUUID().toString();
        data.account(tenant, "Conto " + tenant.substring(0, 8));
        return tenant;
    }

    private UUID newApp(String name) {
        UUID app = UUID.randomUUID();
        data.app(app, name.toLowerCase() + "-" + app.toString().substring(0, 8));
        return app;
    }

    /** Fascia di catalogo; il nome coincide con la chiave, come nell'helper condiviso dei test. */
    private UUID newTier(UUID appId, String key) {
        UUID tier = UUID.randomUUID();
        data.appTier(tier, appId, key);
        return tier;
    }

    private static String evt() {
        return "evt_pay_" + UUID.randomUUID();
    }
}
