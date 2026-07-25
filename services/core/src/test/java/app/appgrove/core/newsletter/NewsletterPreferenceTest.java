package app.appgrove.core.newsletter;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Preferenza newsletter dell'utente autenticato (UC 0039): il toggle in account NON usa double
 * opt-in (email già provata). {@code tenant_id}/{@code sub} solo dal JWT (invariante #1).
 */
@QuarkusTest
class NewsletterPreferenceTest {

    private static final String PREFERENCE = "/api/platform/v1/newsletter/preference";

    @Inject
    TestData data;

    @Inject
    AgroalDataSource ds;

    @Test
    void toggleOnConfirmsWithoutDoubleOptInThenToggleOffRevokes() {
        String tenant = UUID.randomUUID().toString();
        String email = "member-" + System.nanoTime() + "@example.com";
        data.account(tenant, "Acme");
        data.user(tenant, TestTokens.subjectFor(tenant), email, "owner");
        String token = TestTokens.withTenant(tenant, "owner");

        // stato iniziale: non iscritto
        given().auth().oauth2(token).when().get(PREFERENCE)
                .then().statusCode(200).body("subscribed", org.hamcrest.Matchers.is(false));

        // toggle ON → confermato subito (nessuna conferma via email), evento grant canale account
        given().auth().oauth2(token).contentType(ContentType.JSON).body("{\"subscribed\":true}")
                .when().put(PREFERENCE)
                .then().statusCode(200).body("subscribed", org.hamcrest.Matchers.is(true));
        assertEquals("confirmed", statusOf(email));
        assertEquals(1, accountGrants(email));

        given().auth().oauth2(token).when().get(PREFERENCE)
                .then().statusCode(200).body("subscribed", org.hamcrest.Matchers.is(true));

        // toggle OFF → revoca
        given().auth().oauth2(token).contentType(ContentType.JSON).body("{\"subscribed\":false}")
                .when().put(PREFERENCE)
                .then().statusCode(200).body("subscribed", org.hamcrest.Matchers.is(false));
        assertEquals("unsubscribed", statusOf(email));
    }

    @Test
    void preferenceRequiresAuthentication() {
        given().when().get(PREFERENCE).then().statusCode(401);
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

    private int accountGrants(String email) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "select count(*) from platform.consent_event ce"
                                + " join platform.newsletter_subscriber ns on ns.id = ce.subscriber_id"
                                + " where lower(ns.email) = lower(?) and ce.event_type = 'grant'"
                                + " and ce.channel = 'account'")) {
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
