package app.appgrove.core.newsletter;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agroal.api.AgroalDataSource;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Disiscrizione one-click via token HMAC (UC 0039): idempotente, token errato rifiutato. */
@QuarkusTest
class NewsletterUnsubscribeTest {

    private static final String SUBSCRIBE = "/api/platform/v1/newsletter/subscriptions";
    private static final String CONFIRM = "/api/platform/v1/newsletter/confirm";
    private static final String UNSUB = "/api/platform/v1/newsletter/unsubscribe";
    private static final Pattern TOKEN = Pattern.compile("confirm\\?token=([^\\s\"'&<]+)");

    @Inject
    MockMailbox mailbox;

    @Inject
    AgroalDataSource ds;

    @Inject
    UnsubscribeTokens unsubscribeTokens;

    @Test
    void oneClickUnsubscribeWithValidHmacRevokes() {
        String email = "unsub-" + System.nanoTime() + "@example.com";
        mailbox.clear();
        given().contentType(ContentType.JSON).header("X-Forwarded-For", "10.1.0.1")
                .body("{\"email\":\"" + email + "\",\"consent\":true,\"channel\":\"site\"}")
                .when().post(SUBSCRIBE).then().statusCode(202);
        String body = mailbox.getMessagesSentTo(email).get(0).getText();
        Matcher m = TOKEN.matcher(body);
        m.find();
        given().when().get(CONFIRM + "?token=" + m.group(1)).then().statusCode(200);

        UUID id = idOf(email);
        String token = unsubscribeTokens.tokenFor(id);
        given().when().get(UNSUB + "?sid=" + id + "&t=" + token)
                .then().statusCode(200).contentType(ContentType.HTML);
        assertEquals("unsubscribed", statusOf(email));
    }

    @Test
    void badUnsubscribeTokenIsRejected() {
        String email = "unsub2-" + System.nanoTime() + "@example.com";
        mailbox.clear();
        given().contentType(ContentType.JSON).header("X-Forwarded-For", "10.1.0.2")
                .body("{\"email\":\"" + email + "\",\"consent\":true,\"channel\":\"site\"}")
                .when().post(SUBSCRIBE).then().statusCode(202);
        UUID id = idOf(email);

        given().when().get(UNSUB + "?sid=" + id + "&t=forged-token")
                .then().statusCode(400);
        assertEquals("pending", statusOf(email));
    }

    private UUID idOf(String email) {
        return (UUID) queryOne(email, "id");
    }

    private String statusOf(String email) {
        return (String) queryOne(email, "status");
    }

    private Object queryOne(String email, String column) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select " + column + " from platform.newsletter_subscriber where lower(email) = lower(?)")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
