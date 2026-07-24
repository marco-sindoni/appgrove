package app.appgrove.crm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Percorsi multi-utente di UC 0054: il <b>varco posto</b> (403 per chi non ha un posto), la <b>matrice
 * ruoli</b> sulla gestione dei posti (solo owner/admin) e la garanzia che owner/admin possano gestire i
 * posti anche a posti esauriti. È il presidio dei requisiti «un membro senza posto non accede all'app» e
 * «un account pieno deve poter liberare un posto».
 */
@QuarkusTest
class SeatAndRolesTest {

    private static final String TENANT = "99999999-0000-0000-0000-000000000054";

    // ── Varco posto: membro senza posto → 403 sul dominio ─────────────────────

    @Test
    void memberWithoutSeatIsForbiddenOnDomain() {
        // Un membro dell'account, autenticato ed entitled, ma SENZA posto: 403 sui contatti.
        String member = TestTokens.withTenantAndUser(TENANT, "membro-senza-posto", "member");
        given().header("Authorization", "Bearer " + member)
                .when().get(CrmApi.CONTACTS)
                .then().statusCode(403);

        given().header("Authorization", "Bearer " + member)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "Tentativo"))
                .when().post(CrmApi.CONTACTS)
                .then().statusCode(403);
    }

    @Test
    void memberWithSeatCanUseDomain() {
        String owner = TestTokens.withTenant(TENANT, "owner");
        String memberId = "membro-con-posto";
        String member = TestTokens.withTenantAndUser(TENANT, memberId, "member");

        // l'owner assegna un posto al membro → ora il membro accede
        given().header("Authorization", "Bearer " + owner)
                .contentType(ContentType.JSON)
                .body(Map.of("userId", memberId))
                .when().post(CrmApi.SEATS)
                .then().statusCode(201);

        CrmApi.createContact(member, "Creato dal membro");
        given().header("Authorization", "Bearer " + member)
                .when().get(CrmApi.CONTACTS)
                .then().statusCode(200);
    }

    // ── Matrice ruoli: solo owner/admin gestiscono i posti ────────────────────

    @Test
    void onlyOwnerAndAdminManageSeats() {
        String tenant = "99999999-0000-0000-0000-0000000000a1";
        String member = TestTokens.withTenantAndUser(tenant, "membro", "member");

        // un member non può assegnare posti → 403 (ruolo)
        given().header("Authorization", "Bearer " + member)
                .contentType(ContentType.JSON)
                .body(Map.of("userId", "chiunque"))
                .when().post(CrmApi.SEATS)
                .then().statusCode(403);

        // admin sì
        String admin = TestTokens.withTenantAndUser(tenant, "capo", "admin");
        given().header("Authorization", "Bearer " + admin)
                .contentType(ContentType.JSON)
                .body(Map.of("userId", "assunto"))
                .when().post(CrmApi.SEATS)
                .then().statusCode(201);
    }

    // ── A posti esauriti, owner/admin gestiscono comunque i posti ─────────────

    @Test
    void ownerCanManageSeatsEvenAtCapacity() {
        String tenant = "99999999-0000-0000-0000-0000000000b2";
        String owner = TestTokens.withTenant(tenant, "owner");

        // riempie i 2 posti free con due membri (l'owner non si è dato un posto)
        given().header("Authorization", "Bearer " + owner).contentType(ContentType.JSON)
                .body(Map.of("userId", "u1")).when().post(CrmApi.SEATS).then().statusCode(201);
        given().header("Authorization", "Bearer " + owner).contentType(ContentType.JSON)
                .body(Map.of("userId", "u2")).when().post(CrmApi.SEATS).then().statusCode(201);

        // a posti esauriti l'owner NON è bloccato dal varco posto: legge il riepilogo e può revocare
        given().header("Authorization", "Bearer " + owner)
                .when().get(CrmApi.SEATS)
                .then().statusCode(200)
                .body("used", is(2))
                .body("limit", is(2))
                .body("remaining", is(0));

        given().header("Authorization", "Bearer " + owner)
                .when().delete(CrmApi.SEATS + "/u1")
                .then().statusCode(204);

        // liberato un posto, se ne può assegnare un altro
        given().header("Authorization", "Bearer " + owner).contentType(ContentType.JSON)
                .body(Map.of("userId", "u3")).when().post(CrmApi.SEATS).then().statusCode(201);
    }
}
