package app.appgrove.core.billing.seats;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * La lettura di rete del listino dei posti e la selezione della versione per data (UC 0102 §8).
 */
@QuarkusTest
class SeatPricingApiTest {

    private static final String TENANT = "33333333-3333-3333-3333-333333333331";

    @Inject
    TestData data;

    private void account() {
        data.account(TENANT, "Listino posti SpA");
        data.user(TENANT, "sub-" + TENANT, "owner@listino.test", "owner");
    }

    /**
     * Il listino è leggibile da <b>qualunque</b> autenticato, non solo dall'owner: leggere quanto costa un
     * posto non richiede il diritto di comprarlo.
     */
    @Test
    void ilListinoVigenteELeggibileDaChiunqueSiaAutenticato() {
        account();

        given().auth().oauth2(TestTokens.withTenant(TENANT, "member"))
                .when().get("/api/platform/v1/seat-pricing")
                .then().statusCode(200)
                .body("currency", equalTo("EUR"))
                .body("bands", hasSize(5))
                .body("bands[0].fromSeat", equalTo(1))
                .body("bands[0].toSeat", equalTo(3))
                .body("bands[0].unitPriceCents", equalTo(0))
                .body("bands[1].unitPriceCents", equalTo(299))
                .body("bands[4].fromSeat", equalTo(101))
                .body("bands[4].toSeat", equalTo(null))
                .body("bands[4].unitPriceCents", equalTo(49))
                .body("bands.unitPriceCents", hasItem(199));
    }

    @Test
    void senzaTokenIlListinoNonSiLegge() {
        given().when().get("/api/platform/v1/seat-pricing").then().statusCode(401);
    }

    /**
     * <b>Una versione con decorrenza futura esiste ma non si applica.</b> È il caso che rende sbagliata la
     * lettura «prendi l'ultima creata», e non è un caso di scuola: il governo del listino (UC 0105) crea le
     * versioni proprio così, con decorrenza dal ciclo successivo.
     */
    @Test
    void laVersioneConDecorrenzaFuturaNonEQuellaVigente() {
        account();
        String token = TestTokens.withTenant(TENANT, "owner");
        UUID futura = data.seatPricingVersion(
                OffsetDateTime.now().plusYears(5), "EUR", "listino futuro di collaudo");
        data.seatPricingBand(futura, 1, 3, 0);
        data.seatPricingBand(futura, 4, null, 999);
        try {
            // Adesso vige ancora il listino iniziale, seminato dal file.
            given().auth().oauth2(token)
                    .when().get("/test/seats/vigente?at=" + OffsetDateTime.now().toInstant())
                    .then().statusCode(200)
                    .body("note", org.hamcrest.Matchers.containsString("Listino iniziale"));

            // Fra sei anni vige quello futuro.
            given().auth().oauth2(token)
                    .when().get("/test/seats/vigente?at=" + OffsetDateTime.now().plusYears(6).toInstant())
                    .then().statusCode(200)
                    .body("note", equalTo("listino futuro di collaudo"))
                    .body("bands", equalTo(2));

            // E l'operazione di prodotto, che serve sempre «adesso», non se ne accorge nemmeno.
            given().auth().oauth2(token)
                    .when().get("/api/platform/v1/seat-pricing")
                    .then().statusCode(200)
                    .body("bands[1].unitPriceCents", equalTo(299));
        } finally {
            data.deleteSeatPricingVersion(futura);
        }
    }

    /**
     * Nessuna versione vigente alla data richiesta → il calcolo si <b>nega</b>. Il caso si costruisce
     * chiedendo una data anteriore alla decorrenza del listino iniziale: nessun listino esisteva nel 1960,
     * e inventare una tariffa per rispondere sarebbe peggio dell'errore.
     */
    @Test
    void nessunaVersioneVigenteAllaDataRichiestaEUnRifiutoEsplicito() {
        account();
        String token = TestTokens.withTenant(TENANT, "owner");

        given().auth().oauth2(token)
                .when().get("/test/seats/vigente?at=1960-01-01T00:00:00Z")
                .then().statusCode(404);

        given().auth().oauth2(token)
                .when().get("/test/seats/vigente-obbligatoria?at=1960-01-01T00:00:00Z")
                .then().statusCode(409)
                .body("error", equalTo("NoSeatPricingVersionException"))
                .body("message", org.hamcrest.Matchers.containsString("nessuna versione"));
    }

    /** Il listino seminato è uno solo, e la selezione per data lo trova anche a distanza di decenni. */
    @Test
    void ilListinoInizialeVigeDaSempre() {
        account();

        given().auth().oauth2(TestTokens.withTenant(TENANT, "owner"))
                .when().get("/test/seats/vigente?at=1971-01-01T00:00:00Z")
                .then().statusCode(200)
                .body("bands", equalTo(5));

        assertEquals(1, data.seatPricingVersionCount());
    }
}
