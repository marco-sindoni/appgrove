package app.appgrove.authlocal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agroal.api.AgroalDataSource;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Signup + verifica email (UC 0058): email su Mailpit, gate email non verificata, duplicato/policy. */
@QuarkusTest
class SignupVerifyTest {

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
    void signupSendsVerificationThenVerifyLogsIn() {
        String email = "sv-new@signup.test";
        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "Password1!", "displayName", "New User"))
                .when().post("/api/auth/signup")
                .then().statusCode(201).body("status", is("verification_required"));

        assertEquals(1, mailbox.getMessagesSentTo(email).size(), "email di verifica inviata");

        // prima della verifica: login bloccato (email non verificata → 403)
        given().contentType(ContentType.JSON).body(Map.of("email", email, "password", "Password1!"))
                .when().post("/api/auth/login").then().statusCode(403);

        String token = Flows.tokenFromEmail(mailbox, email);
        given().contentType(ContentType.JSON).body(Map.of("token", token))
                .when().post("/api/auth/verify").then().statusCode(200).body("access_token", notNullValue());

        // dopo la verifica il login funziona
        given().contentType(ContentType.JSON).body(Map.of("email", email, "password", "Password1!"))
                .when().post("/api/auth/login").then().statusCode(200);
    }

    @Test
    void duplicateEmailIsConflict() {
        // owner@acme.test esiste già (seed)
        given().contentType(ContentType.JSON).body(Map.of("email", "owner@acme.test", "password", "Password1!"))
                .when().post("/api/auth/signup").then().statusCode(409);
    }

    @Test
    void weakPasswordIsBadRequest() {
        given().contentType(ContentType.JSON).body(Map.of("email", "sv-weak@signup.test", "password", "short"))
                .when().post("/api/auth/signup").then().statusCode(400);
    }
}
