package app.appgrove.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Login del security-core (UC 0010): token firmati, claim dal DB, refresh cookie, fail-closed. */
@QuarkusTest
class AuthLoginTest {

    private static final String LOGIN = "/api/auth/login";

    @Inject
    AgroalDataSource ds;

    @BeforeEach
    void seed() {
        TestSchema.ensure(ds);
    }

    private static Response login(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when().post(LOGIN)
                .thenReturn();
    }

    @Test
    @SuppressWarnings("unchecked")
    void loginReturnsTokensAndClaimsFromDb() {
        Response r = login("owner@acme.test", "Password1!");
        r.then().statusCode(200)
                .body("token_type", is("Bearer"))
                .body("access_token", notNullValue())
                .body("id_token", notNullValue())
                .body("expires_in", is(900));

        Map<String, Object> claims = TestJwt.claims(r.path("access_token"));
        assertEquals("seed-acme-owner", claims.get("sub"), "sub = cognito_sub dal DB");
        assertEquals("a0000000-0000-4000-8000-000000000001", claims.get("tenant_id"), "tenant_id = account id dal DB");
        assertTrue(((List<String>) claims.get("groups")).contains("owner"), "groups dal ruolo DB");
        assertEquals(TestJwt.ISSUER, claims.get("iss"));

        // refresh cookie: HttpOnly, host-only, Path=/api/auth
        Cookie cookie = r.getDetailedCookie("appgrove_refresh");
        assertTrue(cookie != null && cookie.isHttpOnly(), "refresh cookie HttpOnly");
        assertEquals("/api/auth", cookie.getPath());
    }

    @Test
    @SuppressWarnings("unchecked")
    void platformAdminSubjectGetsPlatformAdminGroup() {
        Response r = login("admin@appgrove.test", "Password1!");
        r.then().statusCode(200);
        Map<String, Object> claims = TestJwt.claims(r.path("access_token"));
        assertTrue(((List<String>) claims.get("groups")).contains("platform-admin"),
                "il subject piattaforma del seed riceve il gruppo platform-admin");
    }

    @Test
    void wrongPasswordIsUnauthorized() {
        login("owner@acme.test", "sbagliata").then().statusCode(401);
    }

    @Test
    void unknownUserIsUnauthorized() {
        login("ghost@acme.test", "Password1!").then().statusCode(401);
    }
}
