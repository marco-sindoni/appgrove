package app.appgrove.authlocal;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import io.quarkus.mailer.MockMailbox;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Helper dei flussi auth per i test (signup→verify, estrazione token email, codice TOTP). */
final class Flows {

    private static final Pattern TOKEN = Pattern.compile("token=([\\w.\\-]+)");

    private Flows() {}

    /** Estrae il token dal link nell'ultima email inviata a {@code to}. */
    static String tokenFromEmail(MockMailbox mailbox, String to) {
        List<io.quarkus.mailer.Mail> msgs = mailbox.getMessagesSentTo(to);
        assertTrue(!msgs.isEmpty(), "nessuna email inviata a " + to);
        Matcher m = TOKEN.matcher(msgs.get(msgs.size() - 1).getText());
        assertTrue(m.find(), "token assente nel corpo email");
        return m.group(1);
    }

    /** Signup + verifica email; ritorna l'access token (auto-login post-verifica). */
    static String register(MockMailbox mailbox, String email, String password) {
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when().post("/api/auth/signup").then().statusCode(201);
        String token = tokenFromEmail(mailbox, email);
        return given().contentType(ContentType.JSON)
                .body(Map.of("token", token))
                .when().post("/api/auth/verify").then().statusCode(200)
                .extract().path("access_token");
    }

    /** Codice TOTP valido per il segreto, nella finestra corrente. */
    static String totpCode(String secret) {
        try {
            TimeProvider tp = new SystemTimeProvider();
            return new DefaultCodeGenerator().generate(secret, Math.floorDiv(tp.getTime(), 30));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
