package app.appgrove.crm;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Quota dei <b>posti</b>, metrica {@code seats} di natura <b>stock</b> (tetto free = 2 posti
 * contemporanei, UC 0054): due posti convivono, il terzo è bloccato con 429 problem+json, e — la
 * differenza sostanziale con la natura a consumo — <b>revocare un posto lo libera SUBITO</b>, senza
 * attendere alcuna finestra temporale.
 *
 * <p>La seconda parte è quella che conta: un tetto a giacenza contato a consumo passerebbe il primo
 * controllo e non si libererebbe mai. Tenant dedicato per non ereditare posti da altri test.
 */
@QuarkusTest
class QuotaTest {

    private static final String TENANT = "22222222-0000-0000-0000-000000000002";
    private static final int CAP = 2;

    @Test
    void seatHardLimitReturns429AndFreesOnRevoke() {
        String owner = TestTokens.withTenant(TENANT, "owner");

        // fino al tetto: 2 posti passano
        for (int i = 0; i < CAP; i++) {
            Assertions.assertEquals(201, CrmApi.assignSeat(owner, "membro-" + i), "il posto entro il tetto deve entrare");
        }

        // oltre il tetto → 429 problem+json
        given().header("Authorization", "Bearer " + owner)
                .contentType(ContentType.JSON)
                .body(Map.of("userId", "membro-oltre-il-tetto"))
                .when().post(CrmApi.SEATS)
                .then().statusCode(429)
                .contentType("application/problem+json")
                .body("status", is(429));

        // giacenza: revocare un posto lo rende IMMEDIATAMENTE disponibile (nessuna finestra da attendere)
        given().header("Authorization", "Bearer " + owner)
                .when().delete(CrmApi.SEATS + "/membro-0")
                .then().statusCode(204);

        Assertions.assertEquals(
                201, CrmApi.assignSeat(owner, "membro-al-posto-liberato"),
                "dopo la revoca il posto liberato deve poter essere riassegnato subito");
    }

    @Test
    void reassigningSameUserIsIdempotentAndDoesNotConsumeQuota() {
        String tenant = "22222222-0000-0000-0000-0000000000ff";
        String owner = TestTokens.withTenant(tenant, "owner");

        Assertions.assertEquals(201, CrmApi.assignSeat(owner, "stessa-persona"));
        // riassegnare lo stesso utente è idempotente (200) e NON occupa un secondo posto
        Assertions.assertEquals(200, CrmApi.assignSeat(owner, "stessa-persona"));
        // quindi resta un solo posto libero e se ne può assegnare un altro
        Assertions.assertEquals(201, CrmApi.assignSeat(owner, "altra-persona"));
        // ora i due posti sono occupati: il terzo è bloccato
        Assertions.assertEquals(429, CrmApi.assignSeat(owner, "terza-persona"));
    }
}
