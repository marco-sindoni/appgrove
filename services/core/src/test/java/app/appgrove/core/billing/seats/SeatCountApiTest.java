package app.appgrove.core.billing.seats;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Che cosa occupa un posto e che cosa no (UC 0102 §4), stato per stato.
 *
 * <p>Passa da una richiesta autenticata perché è l'unico modo <b>corretto</b> di esercitare il conteggio: il
 * perimetro dell'account viene dal claim {@code tenant_id} del token verificato, e fuori da una richiesta il
 * risolutore del tenant è fail-closed. Un collaudo che aggirasse quel percorso proverebbe un conteggio che in
 * produzione non esiste.
 */
@QuarkusTest
class SeatCountApiTest {

    private static final String TENANT = "33333333-3333-3333-3333-333333333341";
    private static final String SOLO = "33333333-3333-3333-3333-333333333342";
    private static final String ALTRO_TENANT = "33333333-3333-3333-3333-333333333343";

    @Inject
    TestData data;

    /**
     * Il percorso completo, in un solo racconto: l'owner occupa il primo posto, ogni stato aggiunge o non
     * aggiunge, e il numero mostrato coincide sempre con il numero di persone che l'account vede.
     */
    @Test
    void cosaOccupaUnPostoECosaNo() {
        data.account(TENANT, "Conteggio posti SpA");

        // L'OWNER occupa un posto: la franchigia è di tre persone in tutto, lui compreso.
        data.user(TENANT, "sub-" + TENANT, "owner@conteggio.test", "owner");
        assertPosti(1, TENANT);

        // Una persona ATTIVA occupa un posto.
        UUID attiva = data.user(TENANT, "sub-attiva-" + TENANT, "attiva@conteggio.test", "member");
        assertPosti(2, TENANT);

        // Una persona SOSPESA occupa un posto: la sospensione è un provvedimento reversibile, non una
        // riduzione. Chi vuole liberare il posto indica la persona per la cessazione (UC 0104).
        UUID sospesa = data.user(TENANT, "sub-sospesa-" + TENANT, "sospesa@conteggio.test", "member");
        data.setMembershipStatus(TENANT, sospesa, "suspended");
        assertPosti(3, TENANT);

        // Un INVITO IN ATTESA non scaduto occupa un posto: il posto si paga prima che l'invito parta.
        UUID invito = data.invitationId(TENANT, "invitato@conteggio.test", "member");
        assertPosti(4, TENANT);

        // Un invito SCADUTO non occupa posto — e conta la data, non solo lo stato: fra la scadenza dell'ora
        // e il passaggio che scrive «expired» esistono righe ancora in attesa con la data già passata.
        data.expireInvitation(invito);
        assertPosti(3, TENANT);

        // Un invito REVOCATO non occupa posto.
        UUID revocato = data.invitationId(TENANT, "revocato@conteggio.test", "member");
        assertPosti(4, TENANT);
        data.setInvitationStatus(revocato, "revoked");
        assertPosti(3, TENANT);

        // Un invito RIFIUTATO dalla persona invitata non occupa posto (atto diverso dalla revoca, UC 0118).
        UUID rifiutato = data.invitationId(TENANT, "rifiutato@conteggio.test", "member");
        data.setInvitationStatus(rifiutato, "rejected");
        assertPosti(3, TENANT);

        // Un invito ACCETTATO non si conta come invito: l'accettazione ha già prodotto l'appartenenza, che è
        // contata. Contarli entrambi raddoppierebbe la persona.
        UUID accettato = data.invitationId(TENANT, "accettato@conteggio.test", "member");
        assertPosti(4, TENANT);
        data.setInvitationStatus(accettato, "accepted");
        UUID entrata = data.user(TENANT, "sub-entrata-" + TENANT, "accettato@conteggio.test", "member");
        assertPosti(4, TENANT);

        // Una persona RIMOSSA libera il posto.
        data.closeMembership(TENANT, entrata);
        assertPosti(3, TENANT);
        data.closeMembership(TENANT, sospesa);
        assertPosti(2, TENANT);
        data.closeMembership(TENANT, attiva);
        assertPosti(1, TENANT);
    }

    /**
     * I posti di un account non contano in un altro. È l'invariante di separazione applicato al conto che
     * decide quanto si paga: farlo sbagliare vorrebbe dire fatturare a un cliente le persone di un altro.
     *
     * <p>I due account di questo collaudo sono suoi e non condivisi con gli altri metodi: un conteggio va
     * confrontato con un numero atteso, non con sé stesso.
     */
    @Test
    void iPostiDiUnAccountNonContanoNellAltro() {
        data.account(SOLO, "Un conto con una persona");
        data.user(SOLO, "sub-" + SOLO, "owner@solo.test", "owner");

        data.account(ALTRO_TENANT, "Un conto con quattro posti");
        data.user(ALTRO_TENANT, "sub-" + ALTRO_TENANT, "owner@altro.test", "owner");
        data.user(ALTRO_TENANT, "sub-altro-1", "uno@altro.test", "member");
        data.user(ALTRO_TENANT, "sub-altro-2", "due@altro.test", "member");
        data.invitationId(ALTRO_TENANT, "tre@altro.test", "member");

        assertPosti(4, ALTRO_TENANT);
        assertPosti(1, SOLO);
    }

    @Test
    void ilConteggioRichiedeUnTokenConAccount() {
        given().when().get("/test/seats/count").then().statusCode(401);
        given().auth().oauth2(TestTokens.withRolesNoTenant("owner"))
                .when().get("/test/seats/count")
                .then().statusCode(403);
    }

    private static void assertPosti(int attesi, String tenantId) {
        given().auth().oauth2(TestTokens.withTenant(tenantId, "owner"))
                .when().get("/test/seats/count")
                .then().statusCode(200)
                .body("seats", equalTo(attesi));
    }
}
