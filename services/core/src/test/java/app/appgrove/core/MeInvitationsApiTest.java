package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * UC 0118 — gli inviti della persona in sessione e le sue due risposte.
 *
 * <p>Il collaudo che conta è il <b>percorso A intero</b>: una persona che ha già un proprio account
 * accetta l'invito di un'altra azienda e si trova con <b>due appartenenze e una sola identità</b>.
 * È la prova che il vincolo sciolto da UC 0116 è ora raggiungibile da un percorso di prodotto, che
 * prima di questa storia non esisteva.
 */
@QuarkusTest
class MeInvitationsApiTest {

    private static final String PATH = "/api/platform/v1/me/invitations";
    /** L'account proprio della persona: è da qui che è autenticata quando accetta. */
    private static final String MIO = "ffffffff-0118-0000-0000-000000000001";
    /** L'account dell'azienda che invita. */
    private static final String AZIENDA = "ffffffff-0118-0000-0000-000000000002";
    /** Un terzo account, per provare che un invito di un altro non è visibile né accettabile. */
    private static final String TERZI = "ffffffff-0118-0000-0000-000000000003";

    @Inject
    TestData data;

    private String sessione(String tenantId, String sub) {
        return "Bearer " + TestTokens.withSubject(sub, tenantId, "owner");
    }

    // ── percorso A: accettazione da parte di chi ha già un'identità ───────────

    @Test
    void accettareUnInvitoCreaLaSecondaAppartenenzaEUnaSolaIdentita() {
        String sub = "sub-0118-accetta";
        String email = "accetta-0118@example.test";
        data.account(MIO, "Il mio account");
        data.account(AZIENDA, "Azienda che invita");
        UUID identity = data.user(MIO, sub, email, "owner");
        UUID invito = data.invitationId(AZIENDA, email, "member");

        // L'invito si vede dal cruscotto, col nome dell'azienda: senza il nome sarebbe un consenso
        // alla cieca.
        given().header("Authorization", sessione(MIO, sub))
                .when().get(PATH)
                .then().statusCode(200)
                .body("invitations.id", org.hamcrest.Matchers.hasItem(invito.toString()))
                .body("invitations.find { it.id == '" + invito + "' }.accountName",
                        org.hamcrest.Matchers.is("Azienda che invita"));

        given().header("Authorization", sessione(MIO, sub))
                .when().post(PATH + "/" + invito + "/accept")
                .then().statusCode(204);

        // Due appartenenze, UNA identità: è il cuore della storia.
        assertEquals(List.of(MIO, AZIENDA), data.tenantsOf(identity));
        assertEquals(1, data.identityCount(email), "nessuna seconda identità");
        assertEquals("accepted", data.invitationStatus(invito));
        assertEquals(identity, data.invitationAcceptedBy(invito));
        assertEquals("member", data.memberRole(AZIENDA, identity));

        // L'account appena accettato diventa quello ATTIVO: si è appena detto di volerci andare, ed è
        // la conseguenza operativa che UC 0117 pretende — senza di essa la persona si troverebbe, al
        // prossimo accesso, con più appartenenze e nessuna scelta.
        assertNotEquals(null, data.activeMembershipOf(identity));
        assertEquals(AZIENDA, data.tenantOfMembership(data.activeMembershipOf(identity)));

        // L'invito non è più in attesa: sparisce dall'elenco e non si riaccetta.
        given().header("Authorization", sessione(MIO, sub))
                .when().get(PATH)
                .then().statusCode(200)
                .body("invitations.id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(invito.toString())));
        given().header("Authorization", sessione(MIO, sub))
                .when().post(PATH + "/" + invito + "/accept")
                .then().statusCode(404);
    }

    @Test
    void rifiutareUnInvitoLoChiudeSenzaAppartenenza() {
        String sub = "sub-0118-rifiuta";
        String email = "rifiuta-0118@example.test";
        data.account(MIO, "Il mio account");
        data.account(AZIENDA, "Azienda che invita");
        UUID identity = data.user(MIO, sub, email, "owner");
        UUID invito = data.invitationId(AZIENDA, email, "member");

        given().header("Authorization", sessione(MIO, sub))
                .when().post(PATH + "/" + invito + "/reject")
                .then().statusCode(204);

        // `rejected` e non `revoked`: revocare è l'atto di chi ha invitato, rifiutare è l'atto della
        // persona invitata. La storia dell'invito deve poter dire chi l'ha chiuso.
        assertEquals("rejected", data.invitationStatus(invito));
        assertNull(data.invitationAcceptedBy(invito), "nessuno ha accettato");
        assertEquals(List.of(MIO), data.tenantsOf(identity), "nessuna appartenenza in più");
    }

    // ── l'invito non è trasferibile ───────────────────────────────────────────

    @Test
    void unInvitoIndirizzatoAUnAltroIndirizzoNonSiVedeENonSiAccetta() {
        String sub = "sub-0118-altrui";
        data.account(MIO, "Il mio account");
        data.account(TERZI, "Azienda di altri");
        data.user(MIO, sub, "altrui-0118@example.test", "owner");
        UUID invitoDiUnAltro = data.invitationId(TERZI, "qualcun-altro-0118@example.test", "member");

        given().header("Authorization", sessione(MIO, sub))
                .when().get(PATH)
                .then().statusCode(200)
                .body("invitations.id",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(invitoDiUnAltro.toString())));

        // 404 e non 403: l'esistenza di un invito di un'altra azienda non è informazione di chi chiede.
        given().header("Authorization", sessione(MIO, sub))
                .when().post(PATH + "/" + invitoDiUnAltro + "/accept")
                .then().statusCode(404);
    }

    @Test
    void unInvitoScadutoNonSiVedeENonSiAccetta() {
        String sub = "sub-0118-scaduto";
        String email = "scaduto-0118@example.test";
        data.account(MIO, "Il mio account");
        data.account(AZIENDA, "Azienda che invita");
        UUID identity = data.user(MIO, sub, email, "owner");
        UUID invito = data.invitationId(AZIENDA, email, "member");
        data.expireInvitation(invito);

        given().header("Authorization", sessione(MIO, sub))
                .when().get(PATH)
                .then().statusCode(200)
                .body("invitations.id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(invito.toString())));
        given().header("Authorization", sessione(MIO, sub))
                .when().post(PATH + "/" + invito + "/accept")
                .then().statusCode(404);
        assertEquals(List.of(MIO), data.tenantsOf(identity));
    }

    @Test
    void accettareDaMembro_giaDentroChiudeLInvitoSenzaAppartenenzaDoppia() {
        // Caso reale: invitata, entrata per un'altra via, poi accetta l'invito rimasto. Il vincolo
        // ux_membership_tenant_identity l'avrebbe fermata con una violazione di indice: qui l'esito è
        // comprensibile e la riga di invito dice la verità.
        String sub = "sub-0118-giadentro";
        String email = "giadentro-0118@example.test";
        data.account(MIO, "Il mio account");
        data.account(AZIENDA, "Azienda che invita");
        UUID identity = data.user(MIO, sub, email, "owner");
        data.membership(AZIENDA, identity, "member");
        UUID invito = data.invitationId(AZIENDA, email, "member");

        given().header("Authorization", sessione(MIO, sub))
                .when().post(PATH + "/" + invito + "/accept")
                .then().statusCode(204);

        assertEquals("accepted", data.invitationStatus(invito));
        assertEquals(List.of(MIO, AZIENDA), data.tenantsOf(identity), "nessuna appartenenza doppia");
    }

    // ── percorso B: chi è membro apre un proprio account ─────────────────────

    @Test
    void chiEMembroApreUnProprioAccountConUnaSolaIdentita() {
        String sub = "sub-0118-percorsoB";
        String email = "percorso-b-0118@example.test";
        data.account(AZIENDA, "Azienda che invita");
        UUID identity = data.user(AZIENDA, sub, email, "member");

        String nuovoAccount = given().header("Authorization", "Bearer " + TestTokens.withSubject(sub, AZIENDA, "member"))
                .contentType(io.restassured.http.ContentType.JSON)
                .body(java.util.Map.of("name", "Il mio nuovo account"))
                .when().post("/api/platform/v1/me/accounts")
                .then().statusCode(201)
                .body("name", org.hamcrest.Matchers.is("Il mio nuovo account"))
                .extract().path("accountId");

        // Una sola identità, due appartenenze, e nel nuovo account è owner: nessuna seconda identità,
        // nessuna parola d'accesso nuova, nessun nome chiesto di nuovo.
        assertEquals(1, data.identityCount(email));
        assertTrue(data.tenantsOf(identity).contains(nuovoAccount));
        assertEquals("owner", data.memberRole(nuovoAccount, identity));
        // Chi apre un account vuole andarci: diventa quello attivo.
        assertEquals(nuovoAccount, data.tenantOfMembership(data.activeMembershipOf(identity)));
    }
}
