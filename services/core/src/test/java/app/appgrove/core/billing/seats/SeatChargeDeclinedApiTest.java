package app.appgrove.core.billing.seats;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import app.appgrove.core.catalog.PlatformCatalog;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * <b>Addebito rifiutato → l'invito non nasce</b> (UC 0103 §5).
 *
 * <p>È il collaudo più importante della storia, perché è quello che protegge la regola d'oro: meglio un
 * invito mancato che un posto attivo non pagato. Il difetto che chiude non è un errore visibile — è una
 * persona che entra e lavora senza che nessuno la stia pagando, e nessuno se ne accorge fino alla fattura.
 *
 * <p>Il rifiuto si ottiene configurando il <b>simulatore</b> del fornitore di pagamento perché rifiuti,
 * con un profilo di collaudo tutto suo. Costa un riavvio dell'applicazione per questa classe, e vale il
 * prezzo: l'alternativa sarebbe cablare nel simulatore una regola «magica» sui dati (un indirizzo, un
 * importo) che un altro collaudo inciamperebbe per caso senza capire perché.
 */
@QuarkusTest
@TestProfile(SeatChargeDeclinedApiTest.DeclineProfile.class)
class SeatChargeDeclinedApiTest {

    static final String MOTIVO = "carta scaduta";

    public static class DeclineProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("appgrove.seats.stub-decline-reason", MOTIVO);
        }
    }

    private static final String TENANT = "52222222-2222-4222-8222-222222222201";
    private static final String INVITATIONS = "/api/platform/v1/invitations";

    @Inject
    TestData data;

    @Test
    void senzaAddebitoRiuscitoLInvitoNonNasce() {
        data.account(TENANT, "Carta scaduta SpA");
        data.user(TENANT, "sub-" + TENANT, "owner@declino.test", "owner");
        String token = TestTokens.withTenant(TENANT, "owner");

        // Dentro la franchigia il fornitore non viene nemmeno interpellato: i primi tre posti passano anche
        // con il simulatore configurato per rifiutare. È la prova che la franchigia non è un addebito da
        // zero euro ma l'assenza di un addebito.
        invita(token, "uno@declino.test", 201);
        invita(token, "due@declino.test", 201);
        assertEquals(2, data.invitationCount(TENANT));

        // Il quarto posto costa: il fornitore rifiuta, e l'invito non nasce.
        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(Map.of("email", "quattro@declino.test"))
                .when().post(INVITATIONS)
                .then().statusCode(402)
                .body("type", equalTo("urn:appgrove:seats:charge-declined"))
                // Il motivo del fornitore arriva a chi ha invitato: «non è andata» senza il perché non
                // permette di rimediare, e una carta scaduta si rimedia in due minuti.
                .body("detail", containsString(MOTIVO));

        assertEquals(2, data.invitationCount(TENANT), "nessuna riga di invito rimasta a metà");
        assertEquals(
                -1,
                data.seatSubscriptionQuantity(TENANT, PlatformCatalog.seatsAppId()),
                "nessun abbonamento dei posti: non si è pagato nulla, quindi non c'è nulla da abbonare");
    }

    private static void invita(String token, String email, int atteso) {
        given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(Map.of("email", email))
                .when().post(INVITATIONS)
                .then().statusCode(atteso);
    }
}
