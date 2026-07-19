package app.appgrove.@@APP_ID@@;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Health endpoint (UC 0006, #08/21): liveness senza DB (un blip del database non deve far
 * uccidere i task ECS), readiness col DB (check agroal di default, nessun timeout aggressivo).
 */
@QuarkusTest
class HealthEndpointsTest {

    @Test
    void livenessRispondeUpSenzaNessunCheckDelDatabase() {
        given().when().get("/q/health/live")
                .then().statusCode(200)
                .body("status", is("UP"))
                .body("checks.name", not(hasItem(containsString("Database"))));
    }

    @Test
    void readinessIncludeIlCheckDelDatabaseERisultaUp() {
        given().when().get("/q/health/ready")
                .then().statusCode(200)
                .body("status", is("UP"))
                .body("checks.name", hasItem(containsString("Database")));
    }
}
