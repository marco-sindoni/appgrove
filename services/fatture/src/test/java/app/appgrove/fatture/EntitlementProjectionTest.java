package app.appgrove.fatture;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.entitlement.EntitlementEvents;
import app.appgrove.commons.entitlement.projection.EntitlementInvalidationConsumer;
import app.appgrove.commons.gdpr.GdprQueues;
import app.appgrove.commons.gdpr.TenantPurgeConsumer;
import app.appgrove.commons.gdpr.TenantPurgeMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proiezione locale degli entitlement (UC 0046): dimostra che la <b>postura decisa</b> — "cache con
 * rete di sicurezza" — si comporta davvero come stabilito nelle tre situazioni, più il ciclo di
 * invalidazione.
 *
 * <p>Questi test valgono soprattutto per ciò che <b>impediscono</b>: che una modifica futura
 * trasformi silenziosamente la proiezione in fonte di verità (perdendo il rinfresco), o la rete di
 * sicurezza in percorso caldo (perdendo il disaccoppiamento), o che un guasto di core diventi un
 * blocco per clienti paganti.
 */
@QuarkusTest
class EntitlementProjectionTest {

    private static final String INVOICES = "/api/fatture/v1/invoices";
    private static final String TENANT = "77777777-0000-0000-0000-000000000046";

    @Inject
    TestProjection projection;

    @Inject
    TestMessageQueues queues;

    @Inject
    EntitlementInvalidationConsumer consumer;

    @Inject
    TenantPurgeConsumer purgeConsumer;

    @Inject
    ObjectMapper mapper;

    @BeforeEach
    void clean() {
        projection.clear();
        MockEntitlementService.reset();
    }

    @AfterEach
    void restore() {
        MockEntitlementService.reset();
    }

    // ── Situazione 1: proiezione fresca → nessun traffico verso core ──────────

    @Test
    void freshProjectionServesWithoutCallingCore() {
        // Prima richiesta: proiezione assente → rete di sicurezza (e memorizzazione).
        createInvoice("Prima").then().statusCode(201);
        int afterFirst = MockEntitlementService.calls.get();
        assertTrue(afterFirst > 0, "la prima richiesta deve ricorrere alla rete di sicurezza");

        // Seconda richiesta: la proiezione è fresca → nessuna nuova chiamata.
        createInvoice("Seconda").then().statusCode(201);
        assertEquals(
                afterFirst,
                MockEntitlementService.calls.get(),
                "con proiezione fresca non deve esserci alcuna chiamata a core: è il senso del disaccoppiamento");
    }

    // ── Situazione 2: proiezione da rinfrescare + core giù → si serve il vecchio ──

    @Test
    void staleProjectionIsServedWhenCoreIsUnreachable() {
        createInvoice("Popola la proiezione").then().statusCode(201);

        // Un evento invalida la proiezione, ma core non risponde al rinfresco.
        projection.markStale(TENANT);
        MockEntitlementService.unreachable = true;

        // L'ultima verità nota vale più di un blocco: l'accesso resta concesso.
        createInvoice("Con core giù").then().statusCode(201);
    }

    @Test
    void staleProjectionIsRefreshedWhenCoreAnswers() {
        createInvoice("Popola la proiezione").then().statusCode(201);

        // Accesso revocato a monte + invalidazione: al rinfresco l'app deve accorgersene.
        projection.markStale(TENANT);
        MockEntitlementService.accessGranted = false;

        createInvoice("Dopo la revoca").then().statusCode(402);
    }

    // ── Situazione 3: proiezione assente + core giù → si nega ─────────────────

    @Test
    void unknownTenantIsDeniedWhenCoreIsUnreachable() {
        // Nessuna riga e nessuna risposta da core: non c'è alcuna base per decidere.
        MockEntitlementService.unreachable = true;

        createInvoice("Tenant sconosciuto").then().statusCode(402);
    }

    @Test
    void unknownTenantUsesSafetyNetExactlyOncePerRequest() {
        createInvoice("Primo accesso").then().statusCode(201);

        assertTrue(
                projection.rowsFor(TENANT) > 0,
                "dopo il ricorso alla rete di sicurezza la proiezione deve essere popolata,"
                        + " altrimenti ogni richiesta continuerebbe a chiamare core");
    }

    // ── Ciclo di invalidazione ───────────────────────────────────────────────

    @Test
    void invalidationEventMarksProjectionAndRevokesAccess() throws Exception {
        createInvoice("Prima della disdetta").then().statusCode(201);

        // Disdetta a monte + evento di invalidazione sulla coda dell'app.
        MockEntitlementService.accessGranted = false;
        publishInvalidation("subscription.canceled");
        assertEquals(1, consumer.drain(), "l'evento deve essere consumato e confermato");

        createInvoice("Dopo la disdetta").then().statusCode(402);
    }

    @Test
    void repeatedInvalidationIsHarmless() throws Exception {
        createInvoice("Popola la proiezione").then().statusCode(201);

        // Semantica "almeno una volta" delle code: la stessa invalidazione può arrivare due volte.
        publishInvalidation("subscription.updated");
        publishInvalidation("subscription.updated");
        assertEquals(2, consumer.drain(), "entrambe le consegne vanno confermate");

        // Marcare due volte non cambia nulla: l'accesso resta quello che core continua a concedere.
        createInvoice("Dopo doppia invalidazione").then().statusCode(201);
    }

    @Test
    void malformedInvalidationIsNotConfirmed() {
        queues.send(EntitlementEvents.invalidationQueue("fatture"), "{non-json");

        assertEquals(
                0,
                consumer.drain(),
                "un messaggio illeggibile non va confermato: scartarlo in silenzio significherebbe"
                        + " perdere un'invalidazione e servire dati vecchi senza saperlo");
    }

    // ── Erasure: la proiezione non deve sopravvivere alla cancellazione ──────

    @Test
    void purgeRemovesTheProjectionRow() throws Exception {
        createInvoice("Prima della cancellazione").then().statusCode(201);
        assertTrue(projection.rowsFor(TENANT) > 0, "precondizione: la proiezione esiste");

        // Percorso REALE: messaggio sulla coda di purge + consumer, non una scorciatoia di test.
        queues.send(
                GdprQueues.purgeQueue("fatture"),
                mapper.writeValueAsString(new TenantPurgeMessage(TENANT, TenantPurgeMessage.REASON_OFFBOARDED)));
        assertEquals(1, purgeConsumer.drain());

        assertEquals(
                0,
                projection.rowsFor(TENANT),
                "dopo l'erasure non deve restare alcuna riga di proiezione: conterrebbe l'identificativo"
                        + " dell'account e il piano di chi ha chiesto di sparire");
    }

    private void publishInvalidation(String reason) throws Exception {
        String body = mapper.writeValueAsString(
                new EntitlementEvents.InvalidationMessage(TENANT, reason, Instant.now().toString()));
        queues.send(EntitlementEvents.invalidationQueue("fatture"), body);
    }

    private io.restassured.response.Response createInvoice(String customer) {
        return given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("customerName", customer))
                .when()
                .post(INVOICES);
    }
}
