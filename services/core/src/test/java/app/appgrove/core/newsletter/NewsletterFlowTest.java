package app.appgrove.core.newsletter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agroal.api.AgroalDataSource;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Iscrizione dal canale anonimo con double opt-in (UC 0039): pending + email, conferma, campo esca,
 * consenso obbligatorio, idempotenza neutra, token single-use/non valido. L'endpoint è pubblico
 * (senza JWT), come il webhook Paddle.
 */
@QuarkusTest
class NewsletterFlowTest {

    private static final String SUBSCRIBE = "/api/platform/v1/newsletter/subscriptions";
    private static final String CONFIRM = "/api/platform/v1/newsletter/confirm";
    private static final Pattern TOKEN = Pattern.compile("confirm\\?token=([^\\s\"'&<]+)");

    @Inject
    MockMailbox mailbox;

    @Inject
    AgroalDataSource ds;

    @BeforeEach
    void clear() {
        mailbox.clear();
    }

    @Test
    void subscribeCreatesPendingAndSendsConfirmationEmail() {
        String email = "pending-" + System.nanoTime() + "@example.com";
        subscribe(email, "it", "site", "10.0.0.1").statusCode(202);

        assertEquals("pending", statusOf(email));
        assertEquals(1, mailbox.getMessagesSentTo(email).size(), "un'email di conferma");
        // evento grant registrato all'iscrizione
        assertEquals(1, consentEvents(email, "grant"));
    }

    @Test
    void confirmMovesToConfirmedAndLogsProof() {
        String email = "confirm-" + System.nanoTime() + "@example.com";
        subscribe(email, "en", "site", "10.0.0.2").statusCode(202);
        String token = tokenFromEmail(email);

        given().when().get(CONFIRM + "?token=" + token)
                .then().statusCode(200).contentType(ContentType.HTML)
                .body(containsString("confirmed"));

        assertEquals("confirmed", statusOf(email));
        assertEquals(1, consentEvents(email, "confirm"), "prova di conferma nel registro");
    }

    @Test
    void honeypotFilledIsSilentlyAccepted() {
        String email = "bot-" + System.nanoTime() + "@example.com";
        given().contentType(ContentType.JSON)
                .header("X-Forwarded-For", "10.0.0.3")
                .body("{\"email\":\"" + email + "\",\"consent\":true,\"website\":\"http://spam\"}")
                .when().post(SUBSCRIBE)
                .then().statusCode(202);
        // nessuna riga creata, nessuna email
        assertEquals(null, statusOf(email));
        assertEquals(0, mailbox.getMessagesSentTo(email).size());
    }

    @Test
    void missingConsentIsRejected() {
        String email = "noconsent-" + System.nanoTime() + "@example.com";
        given().contentType(ContentType.JSON)
                .header("X-Forwarded-For", "10.0.0.4")
                .body("{\"email\":\"" + email + "\",\"consent\":false}")
                .when().post(SUBSCRIBE)
                .then().statusCode(400);
        assertEquals(null, statusOf(email));
    }

    @Test
    void alreadyConfirmedIsNeutralAndDoesNotResend() {
        String email = "dup-" + System.nanoTime() + "@example.com";
        subscribe(email, "en", "site", "10.0.0.5").statusCode(202);
        String token = tokenFromEmail(email);
        given().when().get(CONFIRM + "?token=" + token).then().statusCode(200);
        mailbox.clear();

        // seconda iscrizione dello stesso indirizzo già confermato: 202 neutro, nessuna email nuova
        subscribe(email, "en", "site", "10.0.0.5").statusCode(202);
        assertEquals("confirmed", statusOf(email));
        assertEquals(0, mailbox.getMessagesSentTo(email).size());
    }

    @Test
    void invalidConfirmTokenReturnsErrorPage() {
        given().when().get(CONFIRM + "?token=does-not-exist")
                .then().statusCode(400).contentType(ContentType.HTML);
    }

    // UC 0039: il sito vetrina è un'origine diversa → la sua POST cross-origin ha bisogno del CORS.
    @Test
    void corsPreflightAllowsTheConfiguredSiteOrigin() {
        given()
                .header("Origin", "https://site.test")
                .header("Access-Control-Request-Method", "POST")
                .when().options(SUBSCRIBE)
                .then()
                .header("Access-Control-Allow-Origin", "https://site.test");
    }

    @Test
    void rateLimitBlocksBurstFromSameIp() {
        String ip = "203.0.113." + (System.nanoTime() % 200); // IP dedicato a questo test
        // max=5 nella finestra: le prime 5 passano, la sesta è respinta (429)
        for (int i = 0; i < 5; i++) {
            subscribe("rl-" + i + "-" + System.nanoTime() + "@example.com", "en", "site", ip).statusCode(202);
        }
        subscribe("rl-over-" + System.nanoTime() + "@example.com", "en", "site", ip).statusCode(429);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private io.restassured.response.ValidatableResponse subscribe(String email, String locale, String channel, String ip) {
        return given().contentType(ContentType.JSON)
                .header("X-Forwarded-For", ip)
                .body("{\"email\":\"" + email + "\",\"locale\":\"" + locale + "\",\"consent\":true,\"channel\":\""
                        + channel + "\"}")
                .when().post(SUBSCRIBE)
                .then();
    }

    private String tokenFromEmail(String email) {
        String body = mailbox.getMessagesSentTo(email).get(0).getText();
        Matcher m = TOKEN.matcher(body);
        assertTrue(m.find(), "token di conferma non trovato nell'email");
        return m.group(1);
    }

    private String statusOf(String email) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select status from platform.newsletter_subscriber where lower(email) = lower(?)")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int consentEvents(String email, String type) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select count(*) from platform.consent_event ce"
                                + " join platform.newsletter_subscriber ns on ns.id = ce.subscriber_id"
                                + " where lower(ns.email) = lower(?) and ce.event_type = ?")) {
            ps.setString(1, email);
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
