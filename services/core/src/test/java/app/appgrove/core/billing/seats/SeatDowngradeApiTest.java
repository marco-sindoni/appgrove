package app.appgrove.core.billing.seats;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import app.appgrove.core.catalog.PlatformCatalog;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * <b>Ridurre i posti non è immediato</b> (UC 0104): l'owner indica le persone, l'account entra in attesa,
 * nessun posto nuovo si aggiunge, e l'attesa si annulla senza conseguenze.
 *
 * <p>Tutto passa dalle <b>operazioni di rete vere</b> e non dal servizio, perché ciò che questa storia
 * promette non è un calcolo ma un insieme di divieti e di permessi: chi può indicare, chi non è indicabile,
 * che cosa smette di essere possibile durante l'attesa. Un collaudo che chiamasse il servizio direttamente
 * proverebbe le scritture e non le promesse.
 *
 * <p>L'esecuzione alla scadenza ha il suo collaudo a parte ({@code SeatDowngradeExecutionTest}): è l'unica
 * parte che gira <b>fuori</b> da una richiesta autenticata, e va provata come tale.
 */
@QuarkusTest
class SeatDowngradeApiTest {

    private static final String REDUCTION = "/api/platform/v1/me/seats/reduction";
    private static final String INVITATIONS = "/api/platform/v1/invitations";
    private static final String SEATS = "/api/platform/v1/me/seats";

    /** Le tariffe del listino iniziale (UC 0102): franchigia di tre posti, poi 2,99 € l'uno. */
    private static final int TARIFFA = 299;

    private static final UUID SEATS_APP = PlatformCatalog.seatsAppId();

    @Inject
    TestData data;

    /**
     * <b>Il caso principale.</b> Cinque persone, l'ultima entrata a pagamento: si indicano due persone e
     * l'account entra in attesa con la <b>data del periodo già pagato</b>, non con una data inventata.
     *
     * <p>Le tre cose che devono <b>restare come prima</b> sono la parte importante di questo collaudo, non
     * un contorno: le persone indicate sono ancora vive, il conto dei posti non cambia, e la quantità
     * dell'abbonamento è quella di prima. Se una sola delle tre cambiasse, un cliente si vedrebbe
     * addebitare o togliere qualcosa a metà periodo — che è esattamente ciò che l'attesa esiste per evitare.
     */
    @Test
    void indicareDuePersoneApreLAttesaSenzaCambiareNullaDiCiocheSiPaga() {
        Scenario s = scenario("Riduzione SpA", "52111111-1111-4111-8111-111111111101");

        given().auth().oauth2(owner(s))
                .contentType(ContentType.JSON)
                .body(Map.of("userIds", List.of(s.membri.get(0).toString(), s.membri.get(1).toString())))
                .when().post(REDUCTION)
                .then().statusCode(201)
                .body("executeAt", notNullValue())
                .body("people", hasSize(2))
                .body("overdue", equalTo(false))
                // Cinque posti oggi, tre dopo: e il dovuto passa da 2×2,99 a zero (dentro la franchigia).
                .body("seatsAfter", equalTo(3))
                .body("dueCentsNow", equalTo(2 * TARIFFA))
                .body("dueCentsAfter", equalTo(0));

        assertEquals("pending", data.seatDowngradeStatus(s.tenant));
        assertEquals(2, data.seatDowngradeItemCount(s.tenant));
        assertEquals(
                data.seatSubscriptionPeriodEnd(s.tenant, SEATS_APP),
                data.seatDowngradeExecuteAt(s.tenant),
                "la data di esecuzione deve essere la fine del periodo GIÀ PAGATO, non una data inventata");

        // Le persone indicate lavorano fino allo scadere: appartenenza viva e stato invariato.
        for (int i = 0; i < 2; i++) {
            assertTrue(data.membershipAlive(s.tenant, s.membri.get(i)),
                    "la persona indicata deve restare nell'account fino alla scadenza");
            assertEquals("active", data.membershipStatus(s.tenant, s.membri.get(i)));
        }

        // Nulla di ciò che si paga è cambiato: né i posti, né la quantità dell'abbonamento.
        riquadro(s.tenant)
                .body("usedSeats", equalTo(5))
                .body("dueCents", equalTo(2 * TARIFFA))
                .body("paidQuantity", equalTo(3))
                .body("pendingReduction", equalTo(true))
                .body("reduction.people", hasSize(2))
                .body("reduction.seatsAfter", equalTo(3));
        assertEquals(3, data.seatSubscriptionQuantity(s.tenant, SEATS_APP),
                "la riduzione è PROGRAMMATA: la quantità dell'abbonamento non deve cambiare adesso");
    }

    /**
     * <b>L'effetto si vede prima di confermare</b> (UC 0104 §4.2), e con la composizione degli scaglioni:
     * senza il «da 2 × 2,99 a 0» chi legge «pagherai 0,00 €» non ha modo di verificare il conto.
     *
     * <p>La stima non crea nulla: si constata che dopo averla chiesta l'account non è in attesa.
     */
    @Test
    void laStimaMostraLEffettoSenzaProgrammareNulla() {
        Scenario s = scenario("Stima SpA", "52111111-1111-4111-8111-111111111102");

        given().auth().oauth2(owner(s))
                .queryParam("userId", s.membri.get(0).toString())
                .when().get(REDUCTION + "/preview")
                .then().statusCode(200)
                .body("seatsNow", equalTo(5))
                .body("seatsAfter", equalTo(4))
                .body("dueCentsNow", equalTo(2 * TARIFFA))
                .body("dueCentsAfter", equalTo(TARIFFA))
                .body("people", hasSize(1))
                .body("executeAt", notNullValue())
                // La composizione: oggi tre posti compresi più due a 2,99; dopo, tre compresi più uno.
                .body("bandsNow", hasSize(2))
                .body("bandsNow[0].unitPriceCents", equalTo(0))
                .body("bandsNow[0].seats", equalTo(3))
                .body("bandsNow[1].unitPriceCents", equalTo(TARIFFA))
                .body("bandsNow[1].seats", equalTo(2))
                .body("bandsAfter[1].seats", equalTo(1))
                .body("bandsAfter[1].subtotalCents", equalTo(TARIFFA));

        given().auth().oauth2(owner(s)).when().get(REDUCTION).then().statusCode(204);
    }

    /**
     * <b>Nessuna aggiunta durante l'attesa</b>, e il presidio è nel servizio (UC 0104 §8). È la prova che
     * lega questa storia a UC 0103: il rifiuto arriva <b>prima</b> di qualunque addebito, quindi un divieto
     * noto in anticipo non costa denaro.
     *
     * <p>Il rifiuto porta l'identificativo stabile, perché l'interfaccia deve poter offrire le due vie
     * d'uscita nella lingua di chi legge senza interpretare un messaggio italiano.
     */
    @Test
    void durranteLAttesaNonSiInvitaNessunoEIlRifiutoNonCostaNulla() {
        Scenario s = scenario("Blocco SpA", "52111111-1111-4111-8111-111111111103");
        indica(s, s.membri.get(0));
        int quantitaPrima = data.seatSubscriptionQuantity(s.tenant, SEATS_APP);
        int invitiPrima = data.invitationCount(s.tenant);

        given().auth().oauth2(owner(s))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "nuovo@blocco.test"))
                .when().post(INVITATIONS)
                .then().statusCode(409)
                .body("type", equalTo("urn:appgrove:seats:reduction-pending"));

        assertEquals(invitiPrima, data.invitationCount(s.tenant), "nessun invito deve essere nato");
        assertEquals(quantitaPrima, data.seatSubscriptionQuantity(s.tenant, SEATS_APP),
                "un rifiuto noto in anticipo non deve costare denaro: la quantità non si muove");
    }

    /**
     * <b>L'annullamento ripristina tutto</b> e non ha effetti contabili. Subito dopo, invitare è di nuovo
     * possibile — che è la sola prova che il blocco è caduto davvero.
     *
     * <p>La riga della riduzione <b>resta</b>, annullata: la storia di che cosa era stato deciso è
     * un'informazione dell'account, e cancellarla fisicamente la perderebbe.
     */
    @Test
    void annullareRiapreGliInvitiSenzaEffettiContabili() {
        Scenario s = scenario("Annulla SpA", "52111111-1111-4111-8111-111111111104");
        indica(s, s.membri.get(0));
        int quantitaPrima = data.seatSubscriptionQuantity(s.tenant, SEATS_APP);

        given().auth().oauth2(owner(s)).when().delete(REDUCTION).then().statusCode(204);

        assertEquals("cancelled", data.seatDowngradeStatus(s.tenant));
        assertEquals(0, data.seatDowngradeItemCount(s.tenant),
                "annullando, nessuna persona deve restare indicata");
        assertEquals(1, data.seatDowngradeCount(s.tenant),
                "la riga annullata resta: è la storia di quello che era stato deciso");
        assertEquals(quantitaPrima, data.seatSubscriptionQuantity(s.tenant, SEATS_APP),
                "annullare non ha effetti contabili: nulla era stato cambiato");
        riquadro(s.tenant).body("pendingReduction", equalTo(false));

        // La prova vera che il blocco è caduto: l'invito passa.
        given().auth().oauth2(owner(s))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "dopo@annulla.test"))
                .when().post(INVITATIONS)
                .then().statusCode(201);
    }

    /**
     * <b>Togliere l'ultima persona chiude l'attesa da sé.</b> Un'attesa senza nessuno da cessare
     * bloccherebbe gli inviti senza ridurre niente: il peggiore dei due mondi.
     */
    @Test
    void togliendoLUltimaPersonaLAttesaSiChiudeDaSe() {
        Scenario s = scenario("Uno SpA", "52111111-1111-4111-8111-111111111105");
        indica(s, s.membri.get(0), s.membri.get(1));

        // Prima persona tolta: l'attesa resta, con una sola persona.
        given().auth().oauth2(owner(s))
                .when().delete(REDUCTION + "/people/" + s.membri.get(0))
                .then().statusCode(204);
        assertEquals("pending", data.seatDowngradeStatus(s.tenant));
        assertEquals(1, data.seatDowngradeItemCount(s.tenant));

        // Seconda persona tolta: non resta nessuno, quindi l'attesa si chiude.
        given().auth().oauth2(owner(s))
                .when().delete(REDUCTION + "/people/" + s.membri.get(1))
                .then().statusCode(204);
        assertEquals("cancelled", data.seatDowngradeStatus(s.tenant));
        assertEquals(0, data.seatDowngradeItemCount(s.tenant));
        riquadro(s.tenant).body("pendingReduction", equalTo(false));
    }

    /**
     * <b>Rimuovere subito una persona indicata resta possibile</b>, ed è un'operazione diversa: la persona
     * esce adesso, il suo posto resta pagato fino a scadenza (nessun rimborso) e la sua riga fra gli
     * indicati non ha più senso. Era l'unica indicata, quindi l'attesa si chiude — e gli inviti tornano
     * possibili.
     */
    @Test
    void rimuovendoSubitoLUnicaPersonaIndicataLAttesaSiChiude() {
        Scenario s = scenario("Subito SpA", "52111111-1111-4111-8111-111111111106");
        indica(s, s.membri.get(0));
        int quantitaPrima = data.seatSubscriptionQuantity(s.tenant, SEATS_APP);

        given().auth().oauth2(owner(s))
                .when().delete("/api/platform/v1/users/" + s.membri.get(0))
                .then().statusCode(204);

        assertEquals("cancelled", data.seatDowngradeStatus(s.tenant));
        assertEquals(0, data.seatDowngradeItemCount(s.tenant));
        assertEquals(quantitaPrima, data.seatSubscriptionQuantity(s.tenant, SEATS_APP),
                "la rimozione immediata non rimborsa: il posto resta pagato fino a scadenza");
    }

    /** <b>L'owner non è indicabile</b>: chi governa l'account non può cessare se stesso. */
    @Test
    void lOwnerNonEIndicabile() {
        Scenario s = scenario("Owner SpA", "52111111-1111-4111-8111-111111111107");

        given().auth().oauth2(owner(s))
                .contentType(ContentType.JSON)
                .body(Map.of("userIds", List.of(s.owner.toString())))
                .when().post(REDUCTION)
                .then().statusCode(409)
                .body("type", equalTo("urn:appgrove:seats:reduction-owner"));

        assertEquals(0, data.seatDowngradeCount(s.tenant));
    }

    /** <b>Una sola riduzione in attesa per account</b>: la seconda è rifiutata. */
    @Test
    void unaSolaRiduzioneInAttesaPerAccount() {
        Scenario s = scenario("Doppia SpA", "52111111-1111-4111-8111-111111111108");
        indica(s, s.membri.get(0));

        given().auth().oauth2(owner(s))
                .contentType(ContentType.JSON)
                .body(Map.of("userIds", List.of(s.membri.get(1).toString())))
                .when().post(REDUCTION)
                .then().statusCode(409)
                .body("type", equalTo("urn:appgrove:seats:reduction-already-pending"));

        assertEquals(1, data.seatDowngradeCount(s.tenant));
    }

    /**
     * <b>Dentro la franchigia non si programma niente.</b> Un account che non paga alcun posto non
     * otterrebbe alcun risparmio programmando una cessazione, e si vedrebbe negare gli inviti per un mese:
     * la via giusta è la rimozione immediata, che è gratuita — e il testo del rifiuto lo dice.
     */
    @Test
    void senzaPostiAPagamentoLaRiduzioneNonSiProgramma() {
        String tenant = "52111111-1111-4111-8111-111111111109";
        data.account(tenant, "Franchigia SpA");
        UUID proprietario = data.user(tenant, "sub-" + tenant, "owner@franchigia0104.test", "owner");
        UUID membro = data.user(tenant, "sub-m-" + tenant, "uno@franchigia0104.test", "member");
        assertNotNull(proprietario);

        given().auth().oauth2(TestTokens.withTenant(tenant, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("userIds", List.of(membro.toString())))
                .when().post(REDUCTION)
                .then().statusCode(409)
                .body("type", equalTo("urn:appgrove:seats:reduction-not-needed"));

        assertEquals(0, data.seatDowngradeCount(tenant));
    }

    /**
     * <b>Una persona di un altro account non si indica.</b> La separazione fra account non è una cortesia:
     * senza questo rifiuto un owner potrebbe programmare la cessazione di una persona che non è sua.
     */
    @Test
    void unaPersonaDiUnAltroAccountNonSiIndica() {
        Scenario mio = scenario("Mio SpA", "52111111-1111-4111-8111-11111111110a");
        Scenario altro = scenario("Altro SpA", "52111111-1111-4111-8111-11111111110b");

        given().auth().oauth2(owner(mio))
                .contentType(ContentType.JSON)
                .body(Map.of("userIds", List.of(altro.membri.get(0).toString())))
                .when().post(REDUCTION)
                .then().statusCode(409)
                .body("type", equalTo("urn:appgrove:seats:reduction-person-unknown"));

        assertEquals(0, data.seatDowngradeCount(mio.tenant));
        assertTrue(altro.membri.stream().allMatch(id -> data.membershipAlive(altro.tenant, id)));
    }

    /**
     * <b>Lo stato «in cessazione dal …» arriva nell'elenco unico delle persone</b>: è il quarto stato che la
     * storia 0100 aveva elencato e che la change 0096 aveva lasciato fuori perché nessun dato poteva
     * produrlo.
     *
     * <p>E arriva come <b>data</b>, non come etichetta: «in cessazione» senza il quando non dice nulla.
     */
    @Test
    void lElencoDellePersoneMostraLaDataDiCessazione() {
        Scenario s = scenario("Elenco SpA", "52111111-1111-4111-8111-11111111110c");
        indica(s, s.membri.get(0));

        String indicata = given().auth().oauth2(owner(s))
                .queryParam("size", 100)
                .when().get("/api/platform/v1/users")
                .then().statusCode(200)
                .extract()
                .path("content.find { it.id == '" + s.membri.get(0) + "' }.endingAt");
        assertNotNull(indicata, "la persona indicata deve portare la data di cessazione");

        String nonIndicata = given().auth().oauth2(owner(s))
                .queryParam("size", 100)
                .when().get("/api/platform/v1/users")
                .then().statusCode(200)
                .extract()
                .path("content.find { it.id == '" + s.membri.get(1) + "' }.endingAt");
        assertEquals(null, nonIndicata, "chi non è indicato non deve portare alcuna data");
    }

    /**
     * <b>La sezione è dell'owner.</b> Un collaboratore che potesse leggere l'elenco degli indicati saprebbe
     * di un licenziamento prima della persona interessata.
     */
    @Test
    void soloLOwnerGovernaLaRiduzione() {
        Scenario s = scenario("Ruoli SpA", "52111111-1111-4111-8111-11111111110d");

        given().auth().oauth2(TestTokens.withTenant(s.tenant, "member"))
                .when().get(REDUCTION)
                .then().statusCode(403);
        given().auth().oauth2(TestTokens.withTenant(s.tenant, "member"))
                .contentType(ContentType.JSON)
                .body(Map.of("userIds", List.of(s.membri.get(0).toString())))
                .when().post(REDUCTION)
                .then().statusCode(403);
        given().auth().oauth2(TestTokens.withTenant(s.tenant, "member"))
                .when().delete(REDUCTION)
                .then().statusCode(403);
    }

    // ── aiuti ────────────────────────────────────────────────────────────────

    /** Un account con owner e quattro persone, e l'abbonamento dei posti creato dall'acquisto vero. */
    private record Scenario(String tenant, UUID owner, List<UUID> membri) {}

    /**
     * Allestisce un account con <b>cinque persone</b> (owner compreso) e l'abbonamento dei posti creato
     * dall'<b>acquisto vero</b>: si manda un invito attraverso l'operazione di rete, che addebita il posto e
     * scrive l'abbonamento con il suo periodo. È l'unico modo per avere una fine di periodo autentica —
     * scriverla a mano proverebbe la riduzione contro una data che il prodotto non produce.
     *
     * <p>L'invito viene poi <b>revocato</b>: serviva a creare l'abbonamento, non a occupare un posto. Il
     * posto resta pagato (nessun rimborso, UC 0103), quindi la quantità dell'abbonamento resta <b>3</b>
     * mentre i posti occupati sono cinque e quelli a pagamento due — ed è esattamente lo scarto che rende
     * questo allestimento utile: prova che l'esecuzione della riduzione <b>ricalcola</b> la quantità dai
     * posti effettivi invece di sottrarre il numero di persone indicate.
     */
    private Scenario scenario(String name, String tenantId) {
        data.account(tenantId, name);
        UUID owner = data.user(tenantId, "sub-" + tenantId, "owner@" + tenantId + ".test", "owner");
        List<UUID> membri = new java.util.ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            membri.add(data.user(
                    tenantId, "sub-" + i + "-" + tenantId, "p" + i + "@" + tenantId + ".test", "member"));
        }
        // Sesto posto comprato per davvero: crea l'abbonamento con la sua fine di periodo.
        String invito = given().auth().oauth2(TestTokens.withTenant(tenantId, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "sesto@" + tenantId + ".test"))
                .when().post(INVITATIONS)
                .then().statusCode(201)
                .extract().path("id");
        given().auth().oauth2(TestTokens.withTenant(tenantId, "owner"))
                .when().delete(INVITATIONS + "/" + invito)
                .then().statusCode(204);
        assertNotNull(data.seatSubscriptionPeriodEnd(tenantId, SEATS_APP),
                "l'acquisto vero deve avere creato l'abbonamento dei posti con il suo periodo");
        return new Scenario(tenantId, owner, List.copyOf(membri));
    }

    private static String owner(Scenario s) {
        return TestTokens.withTenant(s.tenant, "owner");
    }

    /** Indica una o più persone attraverso l'operazione di rete vera. */
    private void indica(Scenario s, UUID... userIds) {
        given().auth().oauth2(owner(s))
                .contentType(ContentType.JSON)
                .body(Map.of("userIds", java.util.Arrays.stream(userIds).map(UUID::toString).toList()))
                .when().post(REDUCTION)
                .then().statusCode(201);
    }

    private io.restassured.response.ValidatableResponse riquadro(String tenant) {
        return given().auth().oauth2(TestTokens.withTenant(tenant, "owner"))
                .when().get(SEATS)
                .then().statusCode(200);
    }
}
