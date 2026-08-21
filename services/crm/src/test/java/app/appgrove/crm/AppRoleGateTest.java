package app.appgrove.crm;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.access.AppRole;
import app.appgrove.commons.access.AppRoleRequiredException;
import app.appgrove.commons.entitlement.EntitlementEvents;
import app.appgrove.commons.entitlement.projection.EntitlementInvalidationConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Il <b>varco condiviso</b> del ruolo per applicazione, visto da dentro un'applicazione reale (UC 0099 §9):
 * il Mini-CRM dichiara {@code viewer} sulle letture e {@code editor} sulle scritture e non contiene un solo
 * confronto fra ruoli. Questi collaudi provano i tre esiti distinti, il ciclo di invalidazione e il
 * fallimento chiuso.
 *
 * <p>Il percorso attraversato è quello di produzione: copia locale del ruolo → (all'occorrenza) rete di
 * sicurezza finta ({@link MockAppRoleService}). Nessun collaudo scavalca la copia, perché è la copia a
 * decidere in produzione.
 */
@QuarkusTest
class AppRoleGateTest {

    /**
     * Un account diverso per ogni test: i posti hanno una metrica a giacenza e i residui di un test
     * consumerebbero il tetto del successivo (stessa ragione già scritta in EntitlementProjectionTest).
     */
    private static final AtomicInteger TENANT_SEQ = new AtomicInteger();

    private String tenant;
    private String subject;
    private String token;

    @Inject
    TestProjection projection;

    @Inject
    TestMessageQueues queues;

    @Inject
    EntitlementInvalidationConsumer consumer;

    @Inject
    ObjectMapper mapper;

    @BeforeEach
    void prepare() {
        tenant = String.format("55555555-0099-0000-0000-%012d", TENANT_SEQ.incrementAndGet());
        subject = TestTokens.subOf(tenant);
        token = TestTokens.withTenant(tenant, "member");
        // Il varco dei posti è ancora in piedi (si ritira in UC 0111): serve un posto per arrivare al
        // varco del ruolo, che è quello in prova qui.
        MockAppRoleService.role = AppRole.admin;
        CrmApi.assignSeat(TestTokens.withTenant(tenant, "owner"), subject);
        projection.clear();
        MockAppRoleService.reset();
    }

    // ── ruolo sufficiente ────────────────────────────────────────────────────

    @Test
    void aViewerReadsAndCannotWrite() {
        MockAppRoleService.role = AppRole.viewer;

        read().then().statusCode(200);

        Response refusal = write("Tentativo di un viewer");
        refusal.then().statusCode(403)
                .body("type", is(AppRoleRequiredException.TYPE_INSUFFICIENT))
                .body("requiredRole", is("editor"))
                .body("role", is("viewer"));
        assertTrue(
                refusal.body().asString().contains("editor"),
                "il rifiuto deve NOMINARE il ruolo che serve: «non puoi fare questo» è inutile senza dire cosa serve");
    }

    @Test
    void anEditorReadsAndWrites() {
        MockAppRoleService.role = AppRole.editor;
        read().then().statusCode(200);
        write("Scritto da un editor").then().statusCode(201);
    }

    @Test
    void anAdminReadsAndWritesToo() {
        MockAppRoleService.role = AppRole.admin;
        read().then().statusCode(200);
        write("Scritto da un admin").then().statusCode(201);
    }

    // ── nessun accesso: un rifiuto DIVERSO ───────────────────────────────────

    /**
     * «Non entri» e «non puoi fare <i>questo</i>» sono due cose diverse e devono restare distinguibili
     * senza interpretare un messaggio: chi non ha accesso deve sapere a chi chiederlo.
     */
    @Test
    void withoutAnyAccessBothReadAndWriteAreRefusedWithTheOtherRefusal() {
        MockAppRoleService.role = null; // diniego noto dalla fonte di verità

        read().then().statusCode(403)
                .body("type", is(AppRoleRequiredException.TYPE_NO_ACCESS));
        write("Tentativo senza accesso").then().statusCode(403)
                .body("type", is(AppRoleRequiredException.TYPE_NO_ACCESS));
    }

    @Test
    void aKnownDenialIsCopiedLocallyToAvoidCallingCoreForever() {
        MockAppRoleService.role = null;
        read().then().statusCode(403);
        int afterFirst = MockAppRoleService.calls.get();

        read().then().statusCode(403);
        assertEquals(
                afterFirst,
                MockAppRoleService.calls.get(),
                "sapere che una persona NON ha accesso vale quanto sapere che ce l'ha: si copia e non si"
                        + " richiede al core a ogni richiesta");
        assertNull(projection.roleOf(tenant, subject), "il diniego è copiato come riga con ruolo assente");
        assertTrue(projection.roleRowsFor(tenant) > 0, "e la riga esiste, altrimenti non risparmierebbe nulla");
    }

    // ── la copia locale è davvero il percorso normale ────────────────────────

    @Test
    void aFreshLocalCopyServesWithoutCallingTheSourceOfTruth() {
        MockAppRoleService.role = AppRole.editor;
        read().then().statusCode(200);
        int afterFirst = MockAppRoleService.calls.get();
        assertTrue(afterFirst > 0, "la prima richiesta popola la copia interpellando il core");
        assertEquals("editor", projection.roleOf(tenant, subject));

        read().then().statusCode(200);
        write("Con copia fresca").then().statusCode(201);
        assertEquals(
                afterFirst,
                MockAppRoleService.calls.get(),
                "con copia fresca non deve esserci alcuna chiamata al core: è il senso del disaccoppiamento");
    }

    // ── invalidazione: il nuovo ruolo vale senza che la persona rientri ───────

    /**
     * La prova che nessuno scrive spontaneamente e che serve più di tutte (UC 0099 §9): dopo il cambio di
     * ruolo nel core e il consumo dell'evento, l'applicazione applica il ruolo <b>nuovo</b>. Prima
     * dell'evento la copia vecchia vale ancora — ed è il comportamento voluto, non un difetto: è la
     * finestra di pochi secondi che la storia dichiara di accettare.
     */
    @Test
    void afterAnInvalidationEventTheNewRoleAppliesAndTheOldCopyDoesNotSurvive() throws Exception {
        MockAppRoleService.role = AppRole.editor;
        write("Prima della retrocessione").then().statusCode(201);

        // Retrocessione decisa nel core: senza evento la copia fresca vale ancora.
        MockAppRoleService.role = AppRole.viewer;
        write("Prima che l'evento arrivi").then().statusCode(201);

        publishInvalidation("app_access.role_changed");
        assertEquals(1, consumer.drain(), "l'evento deve essere consumato e confermato");

        write("Dopo l'evento").then().statusCode(403)
                .body("type", is(AppRoleRequiredException.TYPE_INSUFFICIENT));
        assertEquals("viewer", projection.roleOf(tenant, subject), "la copia vecchia non sopravvive all'evento");
        read().then().statusCode(200);
    }

    @Test
    void aRevocationEventClosesTheDoorEntirely() throws Exception {
        MockAppRoleService.role = AppRole.editor;
        read().then().statusCode(200);

        MockAppRoleService.role = null; // accesso revocato nel core
        publishInvalidation("app_access.revoked");
        assertEquals(1, consumer.drain());

        read().then().statusCode(403)
                .body("type", is(AppRoleRequiredException.TYPE_NO_ACCESS));
    }

    /**
     * Una coda sola per servizio: lo stesso evento marca da rinfrescare <b>entrambe</b> le copie locali —
     * i diritti d'accesso e il ruolo. Se un giorno una delle due venisse dimenticata, questo collaudo
     * diventa rosso invece di lasciare una copia vecchia in giro senza che nulla lo dica.
     */
    @Test
    void oneEventInvalidatesEveryLocalCopyOfTheService() throws Exception {
        MockAppRoleService.role = AppRole.editor;
        read().then().statusCode(200);
        assertTrue(projection.roleRowsFor(tenant) > 0);
        assertTrue(projection.rowsFor(tenant) > 0, "anche la copia dei diritti è stata popolata");

        MockAppRoleService.role = AppRole.viewer;
        MockEntitlementService.accessGranted = false;
        publishInvalidation("subscription.canceled");
        assertEquals(1, consumer.drain());

        // Il diritto dell'account è il varco più esterno: risponde prima di quello del ruolo, quindi il
        // varco del ruolo non è nemmeno arrivato a decidere.
        read().then().statusCode(402);

        // Ripristinato il diritto (e invalidata la sua copia, che intanto ha registrato il diniego), la
        // richiesta arriva al varco del ruolo: la copia del RUOLO era stata marcata dallo stesso evento e
        // viene rinfrescata adesso — se il consumatore l'avesse dimenticata, qui si leggerebbe ancora
        // `editor`.
        MockEntitlementService.reset();
        projection.markStale(tenant);
        read().then().statusCode(200);
        assertEquals("viewer", projection.roleOf(tenant, subject), "anche la copia del ruolo era stata marcata");
    }

    // ── fallimento chiuso: si nega, ma dicendo che è un guasto nostro ────────

    @Test
    void withNoCopyAndAnUnreachableCoreTheRefusalIsAFaultAndNotAPermissionProblem() {
        MockAppRoleService.unreachable = true;

        read().then().statusCode(503)
                .body("type", is(AppRoleRequiredException.TYPE_UNAVAILABLE));
        assertEquals(0, projection.roleRowsFor(tenant), "un guasto non deve lasciare una copia inventata");
    }

    /**
     * Con una copia vecchia e il core giù si continua con l'ultima verità nota: un guasto del core non
     * deve bloccare tutte le persone di tutti gli account. Il rischio accettato — una revoca decisa
     * <i>durante</i> il guasto arriva in ritardo — dura quanto il guasto.
     */
    @Test
    void withAnOldCopyAndAnUnreachableCoreTheLastKnownTruthIsUsed() throws Exception {
        MockAppRoleService.role = AppRole.editor;
        write("Popola la copia").then().statusCode(201);

        publishInvalidation("app_access.role_changed");
        assertEquals(1, consumer.drain());
        MockAppRoleService.unreachable = true;

        write("Con core giù").then().statusCode(201);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private Response read() {
        return given().header("Authorization", "Bearer " + token).when().get(CrmApi.CONTACTS);
    }

    private Response write(String displayName) {
        return given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", displayName))
                .when().post(CrmApi.CONTACTS);
    }

    private void publishInvalidation(String reason) throws Exception {
        String body = mapper.writeValueAsString(
                new EntitlementEvents.InvalidationMessage(tenant, reason, Instant.now().toString()));
        queues.send(EntitlementEvents.invalidationQueue("crm"), body);
    }
}
