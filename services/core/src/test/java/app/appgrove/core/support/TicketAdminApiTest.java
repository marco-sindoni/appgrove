package app.appgrove.core.support;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
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
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Sezione «Ticket» della console di amministrazione (UC 0034 §9 · UC 0075 §9): coda cross-account
 * con nome del conto, filtri per tipo/stato/priorità, <b>ordinamento per scadenza</b>, risposta nel
 * filo (che porta il ticket in attesa dell'utente e avvisa chi l'ha aperto), cambio stato con
 * {@code closed_at}; <b>sicurezza</b>: solo platform-admin (403 altrimenti).
 */
@QuarkusTest
class TicketAdminApiTest {

    private static final String ADMIN = "/api/platform/v1/admin/tickets";
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
                .when().get(ADMIN)
                .then().statusCode(403);
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner", "admin"))
                .when().get("/api/platform/v1/admin/gdpr/requests")
                .then().statusCode(403);
    }

    @Test
    void ticketsAreListedCrossTenantWithAccountName() {
        data.ticket(TENANT_A, "support", "Ticket di A", "open");
        data.ticket(TENANT_B, "privacy", "Ticket di B", "open");
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN)
                .then().statusCode(200)
                .body("subject", hasItem("Ticket di A"))
                .body("subject", hasItem("Ticket di B"))
                .body("findAll { it.subject == 'Ticket di A' }.accountName", hasItem("Admin Ticket A"))
                .body("findAll { it.subject == 'Ticket di A' }.source", hasItem("form"));
        // filtro per tipo
        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "?type=privacy")
                .then().statusCode(200)
                .body("findAll { it.subject == 'Ticket di A' }.size()", equalTo(0));
    }

    /** Filtro per priorità: è la leva con cui l'operatore isola ciò che è stato marcato urgente. */
    @Test
    void queueCanBeFilteredByPriority() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "privacy", "subject", "Istanza delicata",
                        "message", "Riguarda la mia malattia e la relativa terapia."))
                .when().post(USER_PATH)
                .then().statusCode(201);
        data.ticket(TENANT_A, "support", "Domanda ordinaria", "open");

        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "?priority=high")
                .then().statusCode(200)
                .body("subject", hasItem("Istanza delicata"))
                .body("findAll { it.subject == 'Domanda ordinaria' }.size()", equalTo(0))
                .body("findAll { it.subject == 'Istanza delicata' }.flaggedForReview", hasItem(true));
    }

    /**
     * L'ordinamento è la ragione d'essere della coda: prima ciò che aspetta la piattaforma e ha la
     * scadenza più vicina, in fondo ciò che è già chiuso. Sbagliarlo significa mancare il termine
     * di legge di un mese senza accorgersene.
     */
    @Test
    void queueOrdersImminentDeadlinesFirstAndClosedTicketsLast() {
        String tenant = "55555555-0000-0000-0000-0000000000c3";
        data.account(tenant, "Admin Ticket C");
        OffsetDateTime now = OffsetDateTime.now();
        data.ticketDue(tenant, "privacy", "Scade fra un mese", "open", now.plusDays(30));
        data.ticketDue(tenant, "privacy", "Scade domani", "open", now.plusDays(1));
        data.ticket(tenant, "support", "Senza scadenza", "open");
        data.ticketDue(tenant, "privacy", "Già chiusa", "closed", now.plusDays(2));

        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN)
                .then().statusCode(200)
                .body("findAll { it.accountName == 'Admin Ticket C' }.subject",
                        contains("Scade domani", "Scade fra un mese", "Senza scadenza", "Già chiusa"));
    }

    @Test
    void adminReplyMovesTicketToWaitingUserAndNotifiesRequester() {
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
                .when().post(ADMIN + "/" + id + "/messages")
                .then().statusCode(201)
                .body("author", equalTo("admin"));

        given().header("Authorization", "Bearer " + adminToken())
                .when().get(ADMIN + "/" + id)
                .then().statusCode(200)
                .body("ticket.status", equalTo("waiting_user"))
                .body("thread.size()", equalTo(2));

        assertFalse(mailbox.getMailsSentTo("richiedente-a@example.test").isEmpty(),
                "la risposta della piattaforma deve avvisare chi ha aperto il ticket");
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
                .when().patch(ADMIN + "/" + id)
                .then().statusCode(200)
                .body("status", equalTo("closed"))
                .body("priority", equalTo("low"))
                .body("closedAt", notNullValue());

        assertFalse(mailbox.getMailsSentTo("richiedente-a@example.test").isEmpty(),
                "il cambio stato deve avvisare chi ha aperto il ticket");
    }
}
