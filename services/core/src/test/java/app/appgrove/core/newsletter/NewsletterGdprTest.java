package app.appgrove.core.newsletter;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.gdpr.ExportResult;
import app.appgrove.commons.gdpr.GdprScope;
import app.appgrove.core.TestData;
import app.appgrove.core.gdpr.PlatformDataContract;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Copertura GDPR degli iscritti (UC 0039 × UC 0032): l'iscritto la cui email coincide con un utente
 * del tenant entra nell'export e viene eliminato dalla purge per-tenant, anche se si è iscritto dal
 * canale anonimo (sito), collegato per confronto dell'email.
 */
@QuarkusTest
class NewsletterGdprTest {

    private static final String SUBSCRIBE = "/api/platform/v1/newsletter/subscriptions";

    @Inject
    TestData data;

    @Inject
    PlatformDataContract contract;

    @Inject
    AgroalDataSource ds;

    @Test
    void subscriberIsExportedAndPurgedWithTheTenant() {
        String tenant = UUID.randomUUID().toString();
        String email = "gdpr-" + System.nanoTime() + "@example.com";
        data.account(tenant, "Acme");
        data.user(tenant, "sub-" + tenant, email, "owner");

        // iscrizione dal canale anonimo (nessun user_id): il legame col tenant è per email
        given().contentType(ContentType.JSON).header("X-Forwarded-For", "10.2.0.1")
                .body("{\"email\":\"" + email + "\",\"consent\":true,\"channel\":\"site\"}")
                .when().post(SUBSCRIBE).then().statusCode(202);

        ExportResult export = contract.exportData(new GdprScope(tenant));
        List<Map<String, Object>> subs = export.entities().get("newsletter_subscribers");
        assertTrue(subs != null && subs.stream().anyMatch(r -> email.equalsIgnoreCase(String.valueOf(r.get("email")))),
                "l'iscritto deve comparire nell'export del tenant");

        contract.purgeData(new GdprScope(tenant));
        assertEquals(0, countByEmail(email), "la purge del tenant elimina l'iscritto");
    }

    private int countByEmail(String email) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select count(*) from platform.newsletter_subscriber where lower(email) = lower(?)")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
