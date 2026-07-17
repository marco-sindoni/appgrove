package app.appgrove.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** JWKS locale (UC 0010): la chiave pubblica è pubblicata e i token emessi sono verificabili contro di essa. */
@QuarkusTest
class JwksTest {

    @Inject
    AgroalDataSource ds;

    @BeforeEach
    void seed() {
        TestSchema.ensure(ds);
    }

    @Test
    void jwksExposesSigningKey() {
        given().when().get("/api/auth/jwks")
                .then().statusCode(200)
                .body("keys[0].kid", is("auth-local-dev"))
                .body("keys[0].kty", is("RSA"))
                .body("keys[0].use", is("sig"));
    }

    @Test
    void issuedTokenVerifiesAgainstJwks() throws Exception {
        String jwks = given().when().get("/api/auth/jwks").asString();
        String access = given().contentType(ContentType.JSON)
                .body(Map.of("email", "owner@acme.test", "password", "Password1!"))
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("access_token");
        // se la verifica fallisse lancerebbe: prova che un verificatore JWKS (= il core) accetta il token
        TestJwt.verifyAgainstJwks(access, jwks);
    }
}
