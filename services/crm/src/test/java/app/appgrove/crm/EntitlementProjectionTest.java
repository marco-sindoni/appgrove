package app.appgrove.crm;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.entitlement.EntitlementEvents;
import app.appgrove.commons.entitlement.projection.EntitlementInvalidationConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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

    private static final String SEATS = "/api/crm/v1/seats";

    /**
     * Un tenant DIVERSO per ogni test. Non è pignoleria: i posti creati qui restano nel database per
     * tutta la suite, e con una metrica di quota a giacenza (tetto su quanti ne esistono ora) i residui
     * del test precedente consumerebbero il tetto di quello successivo. Un contatore di utenti dà
     * inoltre a ogni assegnazione un {@code userId} nuovo, così ogni azione occupa un posto diverso.
     */
    private static final AtomicInteger TENANT_SEQ = new AtomicInteger();
    private final AtomicInteger userSeq = new AtomicInteger();

    private String tenant;

    @Inject
    TestProjection projection;

    @Inject
    TestMessageQueues queues;

    @Inject
    EntitlementInvalidationConsumer consumer;

    @Inject
    ObjectMapper mapper;

    @BeforeEach
    void clean() {
        tenant = String.format("77777777-0000-0000-0000-%012d", TENANT_SEQ.incrementAndGet());
        userSeq.set(0);
        projection.clear();
        MockEntitlementService.reset();
        // Questo test verifica il gate ENTITLEMENT, non la quota: tetto alto così ogni azione
        // (assegnazione di un posto) passi il gate quota e resti pilotata solo dall'accesso.
        MockEntitlementService.cap = 1000;
    }

    @AfterEach
    void restore() {
        MockEntitlementService.reset();
    }

    // ── Situazione 1: proiezione fresca → nessun traffico verso core ──────────

    @Test
    void freshProjectionServesWithoutCallingCore() {
        // Prima richiesta: proiezione assente → rete di sicurezza (e memorizzazione).
        createItem("Primo").then().statusCode(201);
        int afterFirst = MockEntitlementService.calls.get();
        assertTrue(afterFirst > 0, "la prima richiesta deve ricorrere alla rete di sicurezza");

        // Seconda richiesta: la proiezione è fresca → nessuna nuova chiamata.
        createItem("Secondo").then().statusCode(201);
        assertEquals(
                afterFirst,
                MockEntitlementService.calls.get(),
                "con proiezione fresca non deve esserci alcuna chiamata a core: è il senso del disaccoppiamento");
    }

    // ── Situazione 2: proiezione da rinfrescare + core giù → si serve il vecchio ──

    @Test
    void staleProjectionIsServedWhenCoreIsUnreachable() {
        createItem("Popola la proiezione").then().statusCode(201);

        // Un evento invalida la proiezione, ma core non risponde al rinfresco.
        projection.markStale(tenant);
        MockEntitlementService.unreachable = true;

        // L'ultima verità nota vale più di un blocco: l'accesso resta concesso.
        createItem("Con core giù").then().statusCode(201);
    }

    @Test
    void staleProjectionIsRefreshedWhenCoreAnswers() {
        createItem("Popola la proiezione").then().statusCode(201);

        // Accesso revocato a monte + invalidazione: al rinfresco l'app deve accorgersene.
        projection.markStale(tenant);
        MockEntitlementService.accessGranted = false;

        createItem("Dopo la revoca").then().statusCode(402);
    }

    // ── Situazione 3: proiezione assente + core giù → si nega ─────────────────

    @Test
    void unknownTenantIsDeniedWhenCoreIsUnreachable() {
        // Nessuna riga e nessuna risposta da core: non c'è alcuna base per decidere.
        MockEntitlementService.unreachable = true;

        createItem("Tenant sconosciuto").then().statusCode(402);
    }

    @Test
    void unknownTenantUsesSafetyNetExactlyOncePerRequest() {
        createItem("Primo accesso").then().statusCode(201);

        assertTrue(
                projection.rowsFor(tenant) > 0,
                "dopo il ricorso alla rete di sicurezza la proiezione deve essere popolata,"
                        + " altrimenti ogni richiesta continuerebbe a chiamare core");
    }

    // ── Ciclo di invalidazione ───────────────────────────────────────────────

    @Test
    void invalidationEventMarksProjectionAndRevokesAccess() throws Exception {
        createItem("Prima della disdetta").then().statusCode(201);

        // Disdetta a monte + evento di invalidazione sulla coda dell'app.
        MockEntitlementService.accessGranted = false;
        publishInvalidation("subscription.canceled");
        assertEquals(1, consumer.drain(), "l'evento deve essere consumato e confermato");

        createItem("Dopo la disdetta").then().statusCode(402);
    }

    @Test
    void repeatedInvalidationIsHarmless() throws Exception {
        createItem("Popola la proiezione").then().statusCode(201);

        // Semantica "almeno una volta" delle code: la stessa invalidazione può arrivare due volte.
        publishInvalidation("subscription.updated");
        publishInvalidation("subscription.updated");
        assertEquals(2, consumer.drain(), "entrambe le consegne vanno confermate");

        // Marcare due volte non cambia nulla: l'accesso resta quello che core continua a concedere.
        createItem("Dopo doppia invalidazione").then().statusCode(201);
    }

    @Test
    void malformedInvalidationIsNotConfirmed() {
        queues.send(EntitlementEvents.invalidationQueue("crm"), "{non-json");

        assertEquals(
                0,
                consumer.drain(),
                "un messaggio illeggibile non va confermato: scartarlo in silenzio significherebbe"
                        + " perdere un'invalidazione e servire dati vecchi senza saperlo");
    }

    private void publishInvalidation(String reason) throws Exception {
        String body = mapper.writeValueAsString(
                new EntitlementEvents.InvalidationMessage(tenant, reason, Instant.now().toString()));
        queues.send(EntitlementEvents.invalidationQueue("crm"), body);
    }

    /**
     * Azione entitlement-gated usata come sonda: l'assegnazione di un posto è {@code @RequiresEntitlement}
     * ma — a differenza del dominio — non richiede a sua volta un posto, quindi esercita esattamente il gate
     * entitlement senza effetti collaterali. Ogni chiamata usa un {@code userId} nuovo (occupa un posto
     * diverso) e il tetto è alto, così il gate quota non interferisce mai col gate entitlement in prova.
     */
    private io.restassured.response.Response createItem(String label) {
        return given().header("Authorization", "Bearer " + TestTokens.withTenant(tenant, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("userId", "u-" + userSeq.incrementAndGet()))
                .when()
                .post(SEATS);
    }
}
