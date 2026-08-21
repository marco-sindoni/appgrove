package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * Accessi per applicazione (UC 0098): concessione, cambio di ruolo, revoca, e i confini che li tengono
 * — il potere circoscritto dell'{@code admin} di una applicazione, la persona di un altro account che
 * non esiste, la persona non attiva, l'applicazione senza diritto, l'owner implicito e intoccabile.
 */
@QuarkusTest
class AppAccessApiTest {

    private static final String TENANT = "aaaaaaaa-0098-0000-0000-000000000001";
    private static final String OTHER_TENANT = "bbbbbbbb-0098-0000-0000-000000000002";

    @Inject
    TestData data;

    private static String access(UUID appId) {
        return "/api/platform/v1/apps/" + appId + "/access";
    }

    /** Account con un'applicazione a cui ha diritto (subscription attiva) e il suo owner. */
    private UUID entitledApp(String tenantId, String tag) {
        data.account(tenantId, "Acme 0098 " + tag);
        UUID appId = UUID.randomUUID();
        data.app(appId, "app0098-" + tag + "-" + appId.toString().substring(0, 8));
        data.subscription(tenantId, appId, "active");
        return appId;
    }

    private UUID owner(String tenantId) {
        return data.user(tenantId, TestTokens.subjectFor(tenantId), "owner-0098-" + tenantId + "@example.test", "owner");
    }

    private static String ownerToken(String tenantId) {
        return TestTokens.withTenant(tenantId, "owner");
    }

    // ── ciclo completo: concessione, cambio, revoca ──────────────────────────

    @Test
    void grantChangeRoleAndRevoke() {
        UUID appId = entitledApp(TENANT, "ciclo");
        owner(TENANT);
        UUID target = data.user(TENANT, "sub-0098-ciclo", "ciclo-0098@example.test", "member");

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", target.toString(), "role", "viewer"))
                .when().post(access(appId))
                .then().statusCode(201)
                .body("identityId", is(target.toString()))
                .body("role", is("viewer"))
                .body("implicit", is(false));

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .contentType(ContentType.JSON)
                .body(Map.of("role", "editor"))
                .when().put(access(appId) + "/" + target)
                .then().statusCode(200)
                .body("role", is("editor"));

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .when().delete(access(appId) + "/" + target)
                .then().statusCode(204);

        // Revocato = riga cancellata logicamente: non c'è più, e riconcedere è possibile.
        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .when().get(access(appId))
                .then().statusCode(200)
                .body("identityId", not(hasItem(target.toString())));
    }

    /**
     * Un accesso già esistente non è un errore ma un <b>cambio di ruolo</b> (UC 0098 §5): l'interfaccia
     * non deve chiedere due operazioni per una intenzione sola, e non nasce una seconda riga.
     */
    @Test
    void grantingAnExistingAccessChangesTheRoleAndDoesNotDuplicate() {
        UUID appId = entitledApp(TENANT, "upsert");
        owner(TENANT);
        UUID target = data.user(TENANT, "sub-0098-upsert", "upsert-0098@example.test", "member");
        data.appAccess(TENANT, appId, target, "viewer");

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", target.toString(), "role", "admin"))
                .when().post(access(appId))
                .then().statusCode(200)
                .body("role", is("admin"));

        assertEquals(1, data.appAccessCount(TENANT, appId));
    }

    // ── il potere circoscritto dell'admin di una applicazione ────────────────

    @Test
    void appAdminManagesItsOwnApplicationOnly() {
        UUID appOne = entitledApp(TENANT, "admin-uno");
        UUID appTwo = entitledApp(TENANT, "admin-due");
        owner(TENANT);
        UUID adminOfOne = data.user(TENANT, "sub-0098-admin", "admin-0098@example.test", "member");
        data.appAccess(TENANT, appOne, adminOfOne, "admin");
        UUID target = data.user(TENANT, "sub-0098-target", "target-0098@example.test", "member");
        String token = TestTokens.withSubject("sub-0098-admin", TENANT, "member");

        // sulla PROPRIA applicazione: sì — e può anche nominare un altro admin (UC 0098 §5)
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", target.toString(), "role", "admin"))
                .when().post(access(appOne))
                .then().statusCode(201);

        // su un'ALTRA applicazione: no. Il suo ruolo là è assente, e il token non lo aiuta.
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", target.toString(), "role", "viewer"))
                .when().post(access(appTwo))
                .then().statusCode(403);
    }

    @Test
    void editorReadsButDoesNotWriteAndWithoutAccessSeesNothing() {
        UUID appId = entitledApp(TENANT, "editor");
        owner(TENANT);
        UUID editor = data.user(TENANT, "sub-0098-editor", "editor-0098@example.test", "member");
        data.appAccess(TENANT, appId, editor, "editor");
        UUID outsider = data.user(TENANT, "sub-0098-fuori", "fuori-0098@example.test", "member");

        given().header("Authorization", "Bearer " + TestTokens.withSubject("sub-0098-editor", TENANT, "member"))
                .when().get(access(appId))
                .then().statusCode(200);
        given().header("Authorization", "Bearer " + TestTokens.withSubject("sub-0098-editor", TENANT, "member"))
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", outsider.toString(), "role", "viewer"))
                .when().post(access(appId))
                .then().statusCode(403);

        // Chi non ha accesso all'applicazione non ne conosce nemmeno le persone.
        given().header("Authorization", "Bearer " + TestTokens.withSubject("sub-0098-fuori", TENANT, "member"))
                .when().get(access(appId))
                .then().statusCode(403);
    }

    // ── i rifiuti ────────────────────────────────────────────────────────────

    /**
     * La persona di un altro account risponde «non trovato» e <b>non</b> «vietato»: l'esistenza degli
     * utenti di altri account non è un'informazione di chi chiede (UC 0098 §5).
     */
    @Test
    void aPersonOfAnotherAccountIsNotFound() {
        UUID appId = entitledApp(TENANT, "altro");
        owner(TENANT);
        data.account(OTHER_TENANT, "Borg 0098");
        UUID stranger = data.user(OTHER_TENANT, "sub-0098-borg", "borg-0098@example.test", "member");

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", stranger.toString(), "role", "viewer"))
                .when().post(access(appId))
                .then().statusCode(404);
    }

    @Test
    void aSuspendedPersonDoesNotReceiveAccess() {
        UUID appId = entitledApp(TENANT, "sospesa");
        owner(TENANT);
        UUID target = data.user(TENANT, "sub-0098-sosp", "sosp-0098@example.test", "member");
        data.setMembershipStatus(TENANT, target, "suspended");

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", target.toString(), "role", "viewer"))
                .when().post(access(appId))
                .then().statusCode(409)
                .body("type", is("urn:appgrove:app-access:person-not-active"));
    }

    @Test
    void anApplicationTheAccountHasNoRightToIsRefused() {
        data.account(TENANT, "Acme 0098 senza-diritto");
        owner(TENANT);
        UUID appId = UUID.randomUUID();
        data.app(appId, "app0098-nodiritto-" + appId.toString().substring(0, 8));
        // nessuna subscription e nessun tier gratuito: l'account non ha diritto a questa applicazione
        UUID target = data.user(TENANT, "sub-0098-nodir", "nodir-0098@example.test", "member");

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", target.toString(), "role", "viewer"))
                .when().post(access(appId))
                .then().statusCode(409)
                .body("type", is("urn:appgrove:app-access:not-entitled"));
    }

    // ── l'owner: implicito e intoccabile ────────────────────────────────────

    @Test
    void theOwnerIsListedFirstWithoutARowAndIsNotTouchable() {
        UUID appId = entitledApp(TENANT, "owner");
        UUID ownerId = owner(TENANT);

        // Applicazione appena installata: nessuna riga, e l'elenco contiene comunque l'owner.
        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .when().get(access(appId))
                .then().statusCode(200)
                .body("[0].identityId", is(ownerId.toString()))
                .body("[0].implicit", is(true))
                .body("[0].role", is("admin"));
        assertEquals(0, data.appAccessRowsOf(TENANT, ownerId));

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", ownerId.toString(), "role", "viewer"))
                .when().post(access(appId))
                .then().statusCode(409)
                .body("type", is("urn:appgrove:app-access:owner-implicit"));

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .when().delete(access(appId) + "/" + ownerId)
                .then().statusCode(409)
                .body("type", is("urn:appgrove:app-access:owner-implicit"));

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .contentType(ContentType.JSON)
                .body(Map.of("role", "viewer"))
                .when().put(access(appId) + "/" + ownerId)
                .then().statusCode(409)
                .body("type", is("urn:appgrove:app-access:owner-implicit"));
    }

    /**
     * Chi esce dall'account <b>porta via i suoi permessi</b> (UC 0098 §5). Senza questa regola un
     * accesso sopravvivrebbe alla persona e tornerebbe valido il giorno in cui quella persona rientra —
     * in silenzio, e con i poteri di prima.
     */
    @Test
    void removingAPersonFromTheAccountRevokesTheirAccesses() {
        UUID appId = entitledApp(TENANT, "uscita");
        owner(TENANT);
        UUID target = data.user(TENANT, "sub-0098-uscita", "uscita-0098@example.test", "member");
        data.appAccess(TENANT, appId, target, "editor");

        given().header("Authorization", "Bearer " + ownerToken(TENANT))
                .when().delete("/api/platform/v1/users/" + target)
                .then().statusCode(204);

        assertEquals(0, data.appAccessCount(TENANT, appId, target),
                "l'accesso dev'essere cancellato logicamente insieme all'appartenenza");
        assertEquals(1, data.appAccessRowsOf(TENANT, target),
                "cancellazione LOGICA: la riga resta, la storia è leggibile");
    }

    // ── concorrenza ─────────────────────────────────────────────────────────

    /**
     * Due concessioni <b>simultanee</b> sulla stessa terna producono <b>una</b> riga: l'arbitro è
     * l'indice unico della banca dati, non la lettura che precede la scrittura. Il collaudo serve
     * proprio a impedire che qualcuno "ottimizzi" togliendo il flush esplicito.
     */
    @Test
    void twoSimultaneousGrantsProduceOneRow() throws Exception {
        UUID appId = entitledApp(TENANT, "gara");
        owner(TENANT);
        UUID target = data.user(TENANT, "sub-0098-gara", "gara-0098@example.test", "member");
        Callable<Integer> grant = () -> given()
                .header("Authorization", "Bearer " + ownerToken(TENANT))
                .contentType(ContentType.JSON)
                .body(Map.of("identityId", target.toString(), "role", "viewer"))
                .when().post(access(appId))
                .then().extract().statusCode();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> outcomes = pool.invokeAll(List.of(grant, grant));
            for (Future<Integer> outcome : outcomes) {
                int status = outcome.get();
                // 201 vince, 200 = trovato già presente e trattato come cambio di ruolo,
                // 409 = perdente della gara sull'indice. Nessun 500: il conflitto è previsto.
                assertEquals(true, status == 201 || status == 200 || status == 409,
                        "esito inatteso della concessione simultanea: " + status);
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, data.appAccessCount(TENANT, appId, target));
    }
}
