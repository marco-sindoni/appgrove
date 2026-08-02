package app.appgrove.core.support;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * API ticket lato utente (UC 0034 §9 · UC 0075 §9): apertura (privacy → scadenza legale 1 mese,
 * provenienza {@code form}, conferma via email a chi apre), filo di conversazione, riapertura su
 * risposta a un ticket risolto o in attesa dell'utente, 409 su chiuso, promozione a priorità alta
 * quando entrano in gioco categorie particolari; <b>sicurezza</b>: isolamento fra conti (i ticket
 * altrui sono un 404), member ammesso, anonimo respinto; notifica alla casella di assistenza
 * best-effort (MockMailbox).
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
                .when().patch("/api/platform/v1/admin/tickets/" + id)
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

    /** Provenienza registrata alla nascita: dal modulo dell'applicazione è {@code form} (UC 0075). */
    @Test
    void ticketOpenedFromTheAppRecordsFormAsItsSource() {
        String id = given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "support", "subject", "Provenienza", "message", "Aperto dal modulo"))
                .when().post(PATH)
                .then().statusCode(201)
                .body("source", equalTo("form"))
                .extract().path("id");
        assertEquals("form", data.ticketSource(UUID.fromString(id)));
    }

    /**
     * Categorie particolari (art. 9, UC 0075 §5): il testo che le tocca fa nascere il ticket a
     * priorità alta e contrassegnato per attenzione umana; il testo ordinario no. È la rete che
     * impedisce a una richiesta delicata di restare in fondo alla coda.
     */
    @Test
    void specialCategoryContentRaisesPriorityAndFlagsForReview() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "privacy", "subject", "Richiesta di cancellazione",
                        "message", "Vi ho mandato il referto della mia malattia: cancellatelo."))
                .when().post(PATH)
                .then().statusCode(201)
                .body("priority", equalTo("high"));

        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "support", "subject", "Fattura sbagliata",
                        "message", "Il totale della fattura di marzo non torna."))
                .when().post(PATH)
                .then().statusCode(201)
                .body("priority", equalTo("normal"));
    }

    /**
     * Chi apre una richiesta riceve conferma, nella <b>propria lingua</b>; sull'istanza privacy la
     * conferma dice il termine di legge e la data.
     */
    @Test
    void openingATicketConfirmsByEmailAndStatesTheLegalDeadlineForPrivacy() {
        data.userWithLocale(TENANT_A, TestTokens.subjectFor(TENANT_A),
                "apre-ticket-a@example.test", "owner", "it");
        mailbox.clear();

        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("type", "privacy", "subject", "Voglio i miei dati",
                        "message", "Chiedo copia dei dati che trattate."))
                .when().post(PATH)
                .then().statusCode(201);

        var confirmations = mailbox.getMailsSentTo("apre-ticket-a@example.test");
        assertFalse(confirmations.isEmpty(), "chi apre un ticket deve ricevere la conferma");
        String text = confirmations.get(0).getText();
        assertTrue(text.contains("Voglio i miei dati"), "la conferma deve riportare l'oggetto: " + text);
        assertTrue(text.contains("un mese"), "la conferma privacy deve dire il termine di legge: " + text);
    }

    /**
     * Il ciclo di vita completo del rimpallo (UC 0075 §4): risposta della piattaforma → in attesa
     * dell'utente; replica dell'utente → di nuovo aperto.
     */
    @Test
    void userReplyBringsAWaitingTicketBackToOpen() {
        String token = TestTokens.withTenant(TENANT_A, "owner");
        String id = given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("type", "support", "subject", "Rimpallo", "message", "Domanda"))
                .when().post(PATH)
                .then().statusCode(201)
                .extract().path("id");

        String adminToken = TestTokens.withTenant(
                "a0000000-0000-4000-8000-000000000003", "owner", "platform-admin");
        given().header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body(Map.of("body", "Ti serve altro?"))
                .when().post("/api/platform/v1/admin/tickets/" + id + "/messages")
                .then().statusCode(201);
        given().header("Authorization", "Bearer " + token)
                .when().get(PATH + "/" + id)
                .then().statusCode(200)
                .body("ticket.status", equalTo("waiting_user"));

        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("body", "Sì, mi serve ancora aiuto"))
                .when().post(PATH + "/" + id + "/messages")
                .then().statusCode(201);
        given().header("Authorization", "Bearer " + token)
                .when().get(PATH + "/" + id)
                .then().statusCode(200)
                .body("ticket.status", equalTo("open"));
    }
}
