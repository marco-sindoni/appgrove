package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * «Dove posso entrare, e con che ruolo» (UC 0099): {@code GET /api/platform/v1/me/app-access}. La lettura
 * che sostituisce i ruoli per applicazione nel token — la decisione centrale della storia.
 *
 * <p>Le due condizioni che devono valere <b>insieme</b> (diritto dell'account e accesso della persona) sono
 * il cuore di questi collaudi: ognuna da sola non apre nulla, e la separazione fra account non si allenta.
 */
@QuarkusTest
class MeAppAccessApiTest {

    private static final String PATH = "/api/platform/v1/me/app-access";

    private static final String TENANT = "aaaaaaaa-0099-0000-0000-000000000001";
    private static final String OTHER_TENANT = "bbbbbbbb-0099-0000-0000-000000000002";

    @Inject
    TestData data;

    /** Applicazione a cui l'account ha diritto (subscription attiva). */
    private UUID entitledApp(String tenantId, String tag) {
        data.account(tenantId, "Acme 0099 " + tag);
        UUID appId = UUID.randomUUID();
        data.app(appId, "app0099-" + tag + "-" + appId.toString().substring(0, 8));
        data.subscription(tenantId, appId, "active");
        return appId;
    }

    /** Applicazione presente in catalogo ma <b>senza</b> diritto dell'account. */
    private UUID unentitledApp(String tag) {
        UUID appId = UUID.randomUUID();
        data.app(appId, "app0099-" + tag + "-" + appId.toString().substring(0, 8));
        return appId;
    }

    private UUID owner(String tenantId) {
        return data.user(
                tenantId, TestTokens.subjectFor(tenantId), "owner-0099-" + tenantId + "@example.test", "owner");
    }

    private static String token(String tenantId, String role) {
        return TestTokens.withTenant(tenantId, role);
    }

    private static String slugOf(UUID appId, String tag) {
        return "app0099-" + tag + "-" + appId.toString().substring(0, 8);
    }

    // ── il collaboratore vede solo dove è stato abilitato ────────────────────

    @Test
    void aMemberSeesOnlyTheApplicationsTheyWereGivenAccessTo() {
        UUID granted = entitledApp(TENANT, "concessa");
        UUID notGranted = entitledApp(TENANT, "nonconcessa");
        owner(TENANT);
        UUID person = data.user(TENANT, "sub-0099-member", "member-0099@example.test", "member");
        data.appAccess(TENANT, granted, person, "editor");

        given().header("Authorization", "Bearer " + TestTokens.withSubject("sub-0099-member", TENANT, "member"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("appId", hasItem(granted.toString()))
                .body("appId", not(hasItem(notGranted.toString())))
                .body("find { it.appId == '" + granted + "' }.role", is("editor"))
                .body("find { it.appId == '" + granted + "' }.appSlug", is(slugOf(granted, "concessa")));
    }

    /**
     * Il diritto dell'account e l'accesso della persona sono <b>entrambi</b> necessari: un accesso a una
     * applicazione che l'account non ha (o non ha più) non apre nulla. La riga resta — riattivando, gli
     * accessi tornano validi senza ricostruirli — ma non compare qui.
     */
    @Test
    void anAccessToAnApplicationTheAccountIsNotEntitledToOpensNothing() {
        UUID withoutEntitlement = unentitledApp("senzadiritto");
        data.account(TENANT, "Acme 0099 senzadiritto");
        owner(TENANT);
        UUID person = data.user(TENANT, "sub-0099-orfano", "orfano-0099@example.test", "member");
        data.appAccess(TENANT, withoutEntitlement, person, "admin");

        given().header("Authorization", "Bearer " + TestTokens.withSubject("sub-0099-orfano", TENANT, "member"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("appId", not(hasItem(withoutEntitlement.toString())));
    }

    @Test
    void aMemberWithoutAnyAccessSeesAnEmptyList() {
        entitledApp(TENANT, "nulla");
        owner(TENANT);
        data.user(TENANT, "sub-0099-nessuno", "nessuno-0099@example.test", "member");

        given().header("Authorization", "Bearer " + TestTokens.withSubject("sub-0099-nessuno", TENANT, "member"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("$", empty());
    }

    // ── l'owner le vede tutte, col ruolo massimo ─────────────────────────────

    /**
     * L'owner non ha righe di accesso e non deve averne: le ha tutte per costruzione, col ruolo massimo
     * (UC 0098 §5). Deriva dal diritto dell'account, quindi un'applicazione appena acquistata gli compare
     * subito, senza che nessuno gliela abiliti.
     */
    @Test
    void theOwnerSeesEveryEntitledApplicationWithTheHighestRole() {
        UUID first = entitledApp(TENANT, "owner-a");
        UUID second = entitledApp(TENANT, "owner-b");
        UUID withoutEntitlement = unentitledApp("owner-senza");
        owner(TENANT);

        given().header("Authorization", "Bearer " + token(TENANT, "owner"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("appId", hasItem(first.toString()))
                .body("appId", hasItem(second.toString()))
                .body("appId", not(hasItem(withoutEntitlement.toString())))
                .body("find { it.appId == '" + first + "' }.role", is("admin"))
                .body("find { it.appId == '" + second + "' }.role", is("admin"));
    }

    // ── separazione fra account ──────────────────────────────────────────────

    /**
     * La lettura dice dove può entrare <b>chi chiama</b>, dentro l'account del token: nessun parametro
     * identifica la persona o l'account (invariante #1). Le applicazioni di un altro account non compaiono
     * nemmeno se la stessa persona vi ha un accesso.
     */
    @Test
    void anotherAccountsApplicationsNeverAppear() {
        UUID mine = entitledApp(TENANT, "mia");
        UUID theirs = entitledApp(OTHER_TENANT, "loro");
        owner(TENANT);
        owner(OTHER_TENANT);
        UUID person = data.user(TENANT, "sub-0099-doppio", "doppio-0099@example.test", "member");
        data.appAccess(TENANT, mine, person, "viewer");
        // La stessa persona è anche nell'altro account, con un accesso là.
        UUID membershipElsewhere = data.membership(OTHER_TENANT, person, "member");
        data.appAccess(OTHER_TENANT, theirs, person, "admin");

        given().header("Authorization", "Bearer " + TestTokens.withSubject("sub-0099-doppio", TENANT, "member"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("appId", hasItem(mine.toString()))
                .body("appId", not(hasItem(theirs.toString())))
                .body("$", hasSize(1));

        data.closeMembership(OTHER_TENANT, person);
        org.junit.jupiter.api.Assertions.assertNotNull(membershipElsewhere);
    }

    @Test
    void anAnonymousCallerIsRefused() {
        given().when().get(PATH).then().statusCode(401);
    }
}
