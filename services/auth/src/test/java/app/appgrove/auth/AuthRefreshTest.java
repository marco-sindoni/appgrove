package app.appgrove.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Refresh/logout del security-core (UC 0010): rotazione cookie e fail-closed. */
@QuarkusTest
class AuthRefreshTest {

    private static final String REFRESH = "/api/auth/refresh";

    @Inject
    AgroalDataSource ds;

    @BeforeEach
    void seed() {
        TestSchema.ensure(ds);
    }

    private static String loginCookie() {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", "owner@acme.test", "password", "Password1!"))
                .when().post("/api/auth/login")
                .then().statusCode(200)
                .extract().detailedCookie("appgrove_refresh").getValue();
    }

    @Test
    void refreshRotatesTokens() {
        String cookie = loginCookie();
        given().cookie("appgrove_refresh", cookie)
                .when().post(REFRESH)
                .then().statusCode(200)
                .body("access_token", notNullValue())
                .cookie("appgrove_refresh", notNullValue());
    }

    @Test
    void refreshWithoutCookieIsUnauthorized() {
        given().when().post(REFRESH).then().statusCode(401);
    }

    @Test
    void refreshWithForgedCookieIsUnauthorized() {
        given().cookie("appgrove_refresh", "not-a-real-token")
                .when().post(REFRESH).then().statusCode(401);
    }

    @Test
    void logoutClearsCookie() {
        Response r = given().when().post("/api/auth/logout").thenReturn();
        r.then().statusCode(204);
        assertEquals(0, r.getDetailedCookie("appgrove_refresh").getMaxAge(), "logout azzera il cookie");
    }
}
