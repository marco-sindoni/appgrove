package app.appgrove.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.agroal.api.AgroalDataSource;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 2FA TOTP (UC 0058): enroll/verify abilita il 2FA; login con 2FA → challenge in due passi. */
@QuarkusTest
class TwoFactorTest {

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
    void enrollThenChallengeLogin() {
        String email = "2fa-user@signup.test";
        String password = "Password1!";
        String access = Flows.register(mailbox, email, password);

        // enroll → segreto
        String secret = given().header("Authorization", "Bearer " + access)
                .when().post("/api/auth/2fa/enroll")
                .then().statusCode(200).body("otpauth_uri", notNullValue())
                .extract().path("secret");

        // verifica enroll con un codice valido → 2FA abilitato
        given().header("Authorization", "Bearer " + access).contentType(ContentType.JSON)
                .body(Map.of("code", Flows.totpCode(secret)))
                .when().post("/api/auth/2fa/verify").then().statusCode(204);

        // login ora risponde con challenge (bypass off nei test), niente token
        String challenge = given().contentType(ContentType.JSON).body(Map.of("email", email, "password", password))
                .when().post("/api/auth/login")
                .then().statusCode(200).body("mfa_required", is(true))
                .extract().path("challenge_token");

        // codice errato → 401
        given().contentType(ContentType.JSON).body(Map.of("challenge_token", challenge, "code", "000000"))
                .when().post("/api/auth/login/2fa").then().statusCode(401);

        // codice corretto → token completi
        given().contentType(ContentType.JSON).body(Map.of("challenge_token", challenge, "code", Flows.totpCode(secret)))
                .when().post("/api/auth/login/2fa")
                .then().statusCode(200).body("access_token", notNullValue());
    }

    @Test
    void enrollRequiresAccessToken() {
        given().when().post("/api/auth/2fa/enroll").then().statusCode(401);
    }
}
