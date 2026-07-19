package app.appgrove.auth;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agroal.api.AgroalDataSource;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Lingua delle email col provider locale (UC 0018): la lingua scelta alla registrazione viene
 * memorizzata sull'utente e riusata dalle email successive, senza che il client la ripeta.
 */
@QuarkusTest
class EmailLocalizationTest {

    @Inject
    AgroalDataSource ds;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void setup() {
        TestSchema.ensure(ds);
        mailbox.clear();
    }

    @Test
    void signupInItalianStoresTheLanguageAndSendsItalianEmail() {
        String email = "loc-it@signup.test";
        signup(email, "it");

        assertEquals("it", localeOf(email), "lingua memorizzata sull'utente");
        Mail mail = lastMailTo(email);
        assertEquals("Conferma il tuo indirizzo email", mail.getSubject());
        assertTrue(mail.getText().contains("Benvenuto su appgrove"), "corpo testuale in italiano");
        assertTrue(mail.getHtml().contains("appgrove"), "corpo grafico presente");
    }

    @Test
    void signupWithoutLanguageFallsBackToEnglish() {
        String email = "loc-none@signup.test";
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "Password1!"))
                .when().post("/api/auth/signup").then().statusCode(201);

        assertEquals("en", localeOf(email), "senza lingua l'utente è inglese");
        assertEquals("Confirm your email address", lastMailTo(email).getSubject());
    }

    @Test
    void unsupportedLanguageIsStoredAsEnglish() {
        // Il vincolo sulla colonna ammette solo en/it: normalizzare a monte evita che una lingua
        // inattesa dal client faccia fallire la registrazione con un errore del database.
        String email = "loc-de@signup.test";
        signup(email, "de");

        assertEquals("en", localeOf(email));
        assertEquals("Confirm your email address", lastMailTo(email).getSubject());
    }

    /**
     * È il punto della colonna: la reimpostazione password parte da un solo indirizzo, senza
     * contesto di interfaccia. La lingua può arrivare soltanto dall'utente memorizzato.
     */
    @Test
    void forgotPasswordUsesTheStoredLanguage() {
        String email = "loc-reset@signup.test";
        signup(email, "it");
        mailbox.clear();

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email))
                .when().post("/api/auth/password/forgot").then().statusCode(202);

        assertEquals("Reimposta la password", lastMailTo(email).getSubject());
    }

    @Test
    void inviteUsesTheLanguageOfWhoInvites() {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", "loc-invito@esterno.test", "token", "tok-invito-loc",
                        "role", "member", "locale", "it"))
                .when().post("/api/auth/invitations/send").then().statusCode(202);

        Mail mail = lastMailTo("loc-invito@esterno.test");
        assertEquals("Sei stato invitato su appgrove", mail.getSubject());
        assertTrue(mail.getText().contains("tok-invito-loc"), "il collegamento porta il token d'invito");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private static void signup(String email, String locale) {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "Password1!", "locale", locale))
                .when().post("/api/auth/signup").then().statusCode(201);
    }

    private Mail lastMailTo(String to) {
        List<Mail> msgs = mailbox.getMessagesSentTo(to);
        assertTrue(!msgs.isEmpty(), "nessuna email inviata a " + to);
        return msgs.get(msgs.size() - 1);
    }

    private String localeOf(String email) {
        try (var c = ds.getConnection();
                var ps = c.prepareStatement("select locale from platform.users where lower(email) = lower(?)")) {
            ps.setString(1, email);
            try (var rs = ps.executeQuery()) {
                assertTrue(rs.next(), "utente non trovato: " + email);
                return rs.getString(1);
            }
        } catch (java.sql.SQLException e) {
            throw new AssertionError(e);
        }
    }
}
