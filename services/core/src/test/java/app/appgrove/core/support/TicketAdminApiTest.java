package app.appgrove.core.support;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Ticket lato admin (UC 0034 §9, #13 D21): lista cross-tenant con nome account, risposta nel
 * thread (open → in_progress + email al richiedente), cambio stato con {@code closed_at};
 * <b>security</b>: solo platform-admin (403 altrimenti).
 */
@QuarkusTest
class TicketAdminApiTest {

    private static final String ADMIN = "/api/platform/v1/admin/gdpr";
    private static final String USER_PATH = "/api/platform/v1/tickets";
    private static final String TENANT_A = "55555555-0000-0000-0000-0000000000a1";
    private static final String TENANT_B = "55555555-0000-0000-0000-0000000000b2";
    private static final String PLATFORM_TENANT = "a0000000-0000-4000-8000-000000000003";

    @Inject
    TestData data;

    @Inject
    MockMailbox mailbox;

    private static String adminToken() {
        return TestTokens.withTenant(PLATFORM_TENANT, "owner", "platform-admin");
    }

    @BeforeEach
    void setup() {
        mailbox.clear();
        data.account(TENANT_A, "Admin Ticket A");
        data.account(TENANT_B, "Admin Ticket B");
        // il richiedente dei ticket di A: cognito_sub = subject del token di test → email nota
        data.user(TENANT_A, TestTokens.subjectFor(TENANT_A), "richiedente-a@example.test", "owner");
    }

    @Test
    void nonPlatformAdminIsForbidden() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().get(ADMIN + "/tickets")
                .then().statusCode(403);
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner", "admin"))
                .when().get(ADMIN + "/requests")
                .then().statusCode(403);
    }

    @Test
    void ticketsAreListedCrossTenantWithAccountName() {
        data.ticket(TENANT_A, "support", "Ticket di A", "open");
        data.ticket(TENANT_B, "privacy", "Ticket di B", "open");
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/tickets")
                .then().statusCode(200)
                .body("subject", hasItem("Ticket di A"))
                .body("subject", hasItem("Ticket di B"))
                .body("findAll { it.subject == 'Ticket di A' }.accountName", hasItem("Admin Ticket A"));
        // filtro per tipo
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/tickets?type=privacy")
                .then().statusCode(200)
                .body("findAll { it.subject == 'Ticket di A' }.size()", equalTo(0));
    }

    @Test
    void adminReplyMovesTicketInProgressAndNotifiesRequester() {
        String id = given()
                .header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "privacy", "subject", "Serve aiuto", "message", "Testo iniziale"))
                .when().post(USER_PATH)
                .then().statusCode(201)
                .extract().path("id");
        mailbox.clear();

        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("body", "Risposta del supporto"))
                .when().post(ADMIN + "/tickets/" + id + "/messages")
                .then().statusCode(201)
                .body("author", equalTo("admin"));

        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/tickets/" + id)
                .then().statusCode(200)
                .body("ticket.status", equalTo("in_progress"))
                .body("thread.size()", equalTo(2));

        assertFalse(mailbox.getMailsSentTo("richiedente-a@example.test").isEmpty(),
                "la risposta admin deve notificare chi ha aperto il ticket");
    }

    @Test
    void statusChangeSetsClosedAtAndNotifies() {
        String id = given()
                .header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "support", "subject", "Da chiudere", "message", "Testo"))
                .when().post(USER_PATH)
                .then().statusCode(201)
                .extract().path("id");
        mailbox.clear();

        given().header("Authorization", "Bearer " + adminToken())
                .contentType(ContentType.JSON)
                .body(Map.of("status", "closed", "priority", "low"))
                .when().patch(ADMIN + "/tickets/" + id)
                .then().statusCode(200)
                .body("status", equalTo("closed"))
                .body("priority", equalTo("low"))
                .body("closedAt", notNullValue());

        assertFalse(mailbox.getMailsSentTo("richiedente-a@example.test").isEmpty(),
                "il cambio stato deve notificare chi ha aperto il ticket");
    }
}
