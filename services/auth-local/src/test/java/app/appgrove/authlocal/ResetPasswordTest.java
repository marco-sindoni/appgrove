package app.appgrove.authlocal;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agroal.api.AgroalDataSource;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Reset password (UC 0058): email neutra su Mailpit, token, cambio password effettivo. */
@QuarkusTest
class ResetPasswordTest {

    @Inject
    AgroalDataSource ds;

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void setup() {
        TestSchema.ensure(ds);
        mailbox.clear();
    }

    private static void login(String email, String password, int expected) {
        given().contentType(ContentType.JSON).body(Map.of("email", email, "password", password))
                .when().post("/api/auth/login").then().statusCode(expected);
    }

    @Test
    void forgotThenResetChangesPassword() {
        String email = "rp-user@signup.test";
        Flows.register(mailbox, email, "Password1!");
        mailbox.clear();

        given().contentType(ContentType.JSON).body(Map.of("email", email))
                .when().post("/api/auth/password/forgot").then().statusCode(202);
        String token = Flows.tokenFromEmail(mailbox, email);

        given().contentType(ContentType.JSON).body(Map.of("token", token, "password", "NewPass123"))
                .when().post("/api/auth/password/reset").then().statusCode(204);

        login(email, "NewPass123", 200); // nuova password OK
        login(email, "Password1!", 401);  // vecchia password rifiutata
    }

    @Test
    void forgotUnknownEmailIsNeutralAndSendsNothing() {
        given().contentType(ContentType.JSON).body(Map.of("email", "rp-ghost@nowhere.test"))
                .when().post("/api/auth/password/forgot").then().statusCode(202);
        assertEquals(0, mailbox.getTotalMessagesSent(), "nessuna email per indirizzo sconosciuto (anti-enumeration)");
    }
}
