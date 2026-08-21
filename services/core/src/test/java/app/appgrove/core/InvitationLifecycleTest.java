package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Ciclo di vita inviti (UC 0013 §5): create→list→revoke, duplicato pending (409), ruolo non
 * invitabile (400).
 *
 * <p>Qui vivono anche i <b>tre esiti dell'invio</b> di UC 0118, ed è il posto giusto: sono la stessa
 * operazione. Due sono rifiuti leciti e riconoscibili da un programma; il terzo è la prova che tiene
 * la riservatezza — l'esito è <b>identico</b> per un indirizzo che ha già un'identità e per uno
 * sconosciuto, perché l'esistenza di un rapporto fra quella persona e la piattaforma non è
 * un'informazione dell'account che la invita.
 */
@QuarkusTest
class InvitationLifecycleTest {

    private static final String PATH = "/api/platform/v1/invitations";
    private static final String TENANT = "ffffffff-0000-0000-0000-000000000006";
    /** Un secondo account, dove far esistere un'identità che l'account di sopra non conosce. */
    private static final String ALTROVE = "ffffffff-0118-1111-0000-000000000001";

    @jakarta.inject.Inject
    TestData data;

    private String owner() {
        return "Bearer " + TestTokens.withTenant(TENANT, "owner");
    }

    @Test
    void createReturnsRawToken() {
        given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-token@example.test", "role", "member"))
                .when().post(PATH)
                .then().statusCode(201)
                .body("token", notNullValue())
                .body("status", org.hamcrest.Matchers.is("pending"));
    }

    @Test
    void createListRevoke() {
        String id = given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-flow@example.test", "role", "member"))
                .when().post(PATH)
                .then().statusCode(201)
                .extract().path("id");

        given().header("Authorization", owner())
                .when().get(PATH + "?size=100")
                .then().statusCode(200)
                .body("content.email", hasItem("lc-flow@example.test"));

        given().header("Authorization", owner())
                .when().delete(PATH + "/" + id)
                .then().statusCode(204);

        given().header("Authorization", owner())
                .when().get(PATH + "?size=100")
                .then().body("content.email", not(hasItem("lc-flow@example.test")));
    }

    @Test
    void duplicatePendingIsConflict() {
        given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-dup@example.test", "role", "member"))
                .when().post(PATH).then().statusCode(201);
        given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-dup@example.test", "role", "member"))
                .when().post(PATH).then().statusCode(409);
    }

    @Test
    void ownerRoleIsNotInvitable() {
        given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", "lc-owner@example.test", "role", "owner"))
                .when().post(PATH)
                .then().statusCode(400);
    }

    // ── UC 0118: i tre esiti dell'invio ──────────────────────────────────────

    @Test
    void identitaEsistenteAltrove_esitoIndistinguibileDaIndirizzoSconosciuto() {
        // La prova che tiene la riservatezza (UC 0118 §5). Un indirizzo ha già un'identità in un ALTRO
        // account, l'altro non esiste da nessuna parte: le due risposte devono essere la stessa cosa.
        //
        // NESSUNA asserzione sui tempi, di proposito: una soglia temporale in una suite condivisa è un
        // collaudo instabile, e instabile vuol dire disattivato. La garanzia sui tempi è strutturale —
        // la lettura dell'identità si esegue SEMPRE, in entrambi i rami (vedi InvitationResource).
        data.account(ALTROVE, "Azienda altrove");
        data.user(ALTROVE, "sub-0118-altrove", "esiste-altrove-0118@example.test", "owner");

        var esistente = invita("esiste-altrove-0118@example.test");
        var sconosciuto = invita("mai-visto-0118@example.test");

        assertEquals(esistente.statusCode(), sconosciuto.statusCode(), "stesso codice di stato");
        assertEquals(
                esistente.jsonPath().getMap("$").keySet(),
                sconosciuto.jsonPath().getMap("$").keySet(),
                "stesse chiavi nel corpo: nessun campo in più da una parte");
        assertEquals(esistente.jsonPath().getString("status"), sconosciuto.jsonPath().getString("status"));
        assertEquals(esistente.jsonPath().getString("role"), sconosciuto.jsonPath().getString("role"));

        // La differenza esiste, ma vive SOLO nella banca dati e non esce mai verso chi ha invitato.
        assertNotNull(data.invitationIdentityId(java.util.UUID.fromString(esistente.jsonPath().getString("id"))));
        assertNull(data.invitationIdentityId(java.util.UUID.fromString(sconosciuto.jsonPath().getString("id"))));
    }

    @Test
    void giaMembroDiQuestoAccount_rifiutoRiconoscibile() {
        // Informazione dell'account: lecita, e la più utile delle tre.
        data.account(TENANT, "Acme inviti");
        data.user(TENANT, "sub-0118-giamembro", "gia-membro-0118@example.test", "member");

        invita("gia-membro-0118@example.test")
                .then().statusCode(409)
                .body("type", org.hamcrest.Matchers.is("urn:appgrove:invitation:already-member"));
    }

    @Test
    void invitoGiaInAttesa_rifiutoRiconoscibileEDistinto() {
        invita("attesa-0118@example.test").then().statusCode(201);
        invita("attesa-0118@example.test")
                .then().statusCode(409)
                .body("type", org.hamcrest.Matchers.is("urn:appgrove:invitation:already-invited"));
    }

    private io.restassured.response.Response invita(String email) {
        return given().header("Authorization", owner())
                .contentType(ContentType.JSON).body(Map.of("email", email, "role", "member"))
                .when().post(PATH);
    }
}
