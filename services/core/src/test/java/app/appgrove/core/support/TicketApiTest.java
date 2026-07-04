package app.appgrove.core.support;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * API ticket lato utente (UC 0034 §9, #13 D21): apertura (privacy → scadenza legale 1 mese),
 * thread, riapertura su risposta a un ticket risolto, 409 su chiuso; <b>security</b>: isolamento
 * tenant (i ticket altrui sono un 404), member ammesso, anonimo respinto; notifica alla casella
 * di supporto best-effort (MockMailbox).
 */
@QuarkusTest
class TicketApiTest {

    private static final String PATH = "/api/platform/v1/tickets";
    private static final String TENANT_A = "66666666-0000-0000-0000-0000000000a1";
    private static final String TENANT_B = "66666666-0000-0000-0000-0000000000b2";
    private static final String SUPPORT_INBOX = "support@appgrove.app";

    @Inject
    TestData data;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void setup() {
        mailbox.clear();
        data.account(TENANT_A, "Ticket Tenant A");
        data.account(TENANT_B, "Ticket Tenant B");
    }

    @Test
    void privacyTicketCarriesLegalDueDateAndNotifiesSupportInbox() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "privacy", "subject", "Richiesta limitazione",
                        "message", "Chiedo la limitazione del trattamento (art. 18)."))
                .when().post(PATH)
                .then().statusCode(201)
                .body("type", equalTo("privacy"))
                .body("status", equalTo("open"))
                .body("dueAt", notNullValue());

        assertFalse(mailbox.getMailsSentTo(SUPPORT_INBOX).isEmpty(),
                "l'apertura deve notificare la casella di supporto");
        // regressione "tenant null": il tenant nella notifica arriva dal JWT, non dall'entità pre-flush
        String text = mailbox.getMailsSentTo(SUPPORT_INBOX).get(0).getText();
        assertTrue(text.contains(TENANT_A), "la notifica deve riportare il tenant reale: " + text);
        assertFalse(text.contains("null"), "la notifica non deve contenere campi null: " + text);
    }

    @Test
    void supportTicketHasNoDueDate() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "member"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "support", "subject", "Domanda", "message", "Come si fa X?"))
                .when().post(PATH)
                .then().statusCode(201)
                .body("dueAt", nullValue());
    }

    @Test
    void threadGrowsWithRepliesAndReopensResolvedTicket() {
        String token = TestTokens.withTenant(TENANT_A, "owner");
        String id = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("type", "support", "subject", "Thread", "message", "Primo messaggio"))
                .when().post(PATH)
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("body", "Secondo messaggio"))
                .when().post(PATH + "/" + id + "/messages")
                .then().statusCode(201)
                .body("author", equalTo("user"));

        given().header("Authorization", "Bearer " + token)
                .when().get(PATH + "/" + id)
                .then().statusCode(200)
                .body("thread.size()", equalTo(2));

        // ticket risolto + risposta dell'utente → riaperto (il thread non è concluso)
        given().header("Authorization",
                        "Bearer " + TestTokens.withTenant("a0000000-0000-4000-8000-000000000003",
                                "owner", "platform-admin"))
                .contentType(ContentType.JSON)
                .body(Map.of("status", "resolved", "priority", "normal"))
                .when().patch("/api/platform/v1/admin/gdpr/tickets/" + id)
                .then().statusCode(200);
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("body", "Non è risolto"))
                .when().post(PATH + "/" + id + "/messages")
                .then().statusCode(201);
        given().header("Authorization", "Bearer " + token)
                .when().get(PATH + "/" + id)
                .then().statusCode(200)
                .body("ticket.status", equalTo("open"));
    }

    @Test
    void closedTicketRejectsReplies() {
        UUID id = data.ticket(TENANT_A, "support", "Chiuso", "closed");
        data.backdateTicketClosure(id, OffsetDateTime.now());
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("body", "Troppo tardi"))
                .when().post(PATH + "/" + id + "/messages")
                .then().statusCode(409);
    }

    @Test
    void ticketsAreTenantIsolated() {
        UUID ticketA = data.ticket(TENANT_A, "support", "Solo di A", "open");
        // B non vede il ticket di A: 404 (filtro discriminator, invariante #2)
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_B, "owner"))
                .when().get(PATH + "/" + ticketA)
                .then().statusCode(404);
        // e non può rispondervi
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_B, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("body", "Intrusione"))
                .when().post(PATH + "/" + ticketA + "/messages")
                .then().statusCode(404);
    }

    @Test
    void anonymousIsRejected() {
        given().when().get(PATH).then().statusCode(401);
    }
}
