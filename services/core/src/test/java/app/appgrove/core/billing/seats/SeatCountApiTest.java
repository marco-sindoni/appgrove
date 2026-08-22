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
 *
 * <p><b>Da UC 0103 interroga l'operazione vera</b> — {@code GET /api/platform/v1/me/seats}, il riquadro dei
 * posti — e non più l'endpoint di collaudo con cui UC 0102 aveva anticipato la superficie che non c'era.
 * Cambia anche la portata del collaudo, in meglio: quello che era un numero letto da un probe è ora il numero
 * che il cliente <b>vede a schermo</b>.
 */
@QuarkusTest
class SeatCountApiTest {

    private static final String TENANT = "33333333-3333-3333-3333-333333333341";
    private static final String SOLO = "33333333-3333-3333-3333-333333333342";
    private static final String ALTRO_TENANT = "33333333-3333-3333-3333-333333333343";
    private static final String COMPOSIZIONE = "33333333-3333-3333-3333-333333333344";

    /** L'operazione vera del riquadro dei posti (UC 0103), che ha sostituito l'endpoint di collaudo. */
    private static final String SEATS = "/api/platform/v1/me/seats";

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

    /**
     * Il riquadro dei posti prende l'account dal <b>token verificato</b> e da nessun altro posto: senza
     * token non risponde, con un token senza account non risponde. Non esiste alcun parametro con cui
     * chiedere i posti di un altro conto — è l'invariante #1 applicato a una lettura che dice quanto paga
     * un'azienda.
     */
    @Test
    void ilConteggioRichiedeUnTokenConAccount() {
        given().when().get(SEATS).then().statusCode(401);
        given().auth().oauth2(TestTokens.withRolesNoTenant("owner"))
                .when().get(SEATS)
                .then().statusCode(403);
    }

    /**
     * <b>Il riquadro è dell'owner.</b> Qui non si leggono tariffe — quelle sono pubbliche dentro il
     * prodotto — ma quanto paga l'account e quante persone ha: un collaboratore che lo leggesse conoscerebbe
     * il costo della propria azienda senza averne titolo.
     */
    @Test
    void ilRiquadroDeiPostiELetturaDellOwner() {
        data.account(TENANT, "Conteggio posti SpA");

        given().auth().oauth2(TestTokens.withTenant(TENANT, "member"))
                .when().get(SEATS)
                .then().statusCode(403);
    }

    /**
     * La composizione dei posti: quanti attivi, quanti sospesi, quanti inviti in attesa. La somma è il
     * numero grande, e serve perché chi legge conta le righe della tabella e vuole che gli torni.
     */
    @Test
    void laComposizioneDeiPostiSpiegaIlNumeroGrande() {
        data.account(COMPOSIZIONE, "Composizione SpA");
        data.user(COMPOSIZIONE, "sub-" + COMPOSIZIONE, "owner@composizione.test", "owner");
        UUID sospesa = data.user(COMPOSIZIONE, "sub-comp-sosp", "sospesa@composizione.test", "member");
        data.setMembershipStatus(COMPOSIZIONE, sospesa, "suspended");
        data.invitationId(COMPOSIZIONE, "invitata@composizione.test", "member");

        given().auth().oauth2(TestTokens.withTenant(COMPOSIZIONE, "owner"))
                .when().get(SEATS)
                .then().statusCode(200)
                .body("usedSeats", equalTo(3))
                .body("composition.active", equalTo(1))
                .body("composition.suspended", equalTo(1))
                .body("composition.pendingInvitations", equalTo(1));
    }

    private static void assertPosti(int attesi, String tenantId) {
        given().auth().oauth2(TestTokens.withTenant(tenantId, "owner"))
                .when().get(SEATS)
                .then().statusCode(200)
                .body("usedSeats", equalTo(attesi));
    }
}
