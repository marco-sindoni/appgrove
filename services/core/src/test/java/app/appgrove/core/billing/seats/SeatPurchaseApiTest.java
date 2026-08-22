package app.appgrove.core.billing.seats;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import app.appgrove.core.catalog.PlatformCatalog;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * <b>L'invito passa dalla cassa</b> (UC 0103): la franchigia non costa nulla, il quarto posto crea
 * l'abbonamento di piattaforma, il quinto ne alza la quantità, e un posto già pagato non si paga due volte.
 *
 * <p>Passa dall'operazione di rete vera ({@code POST /api/platform/v1/invitations}) e non dal servizio,
 * perché quello che questa storia promette è l'<b>ordine degli atti</b>: verifica, addebito, invito. Un
 * collaudo che chiamasse il servizio direttamente proverebbe il calcolo — che è già provato altrove — e non
 * la promessa.
 *
 * <p>Il fornitore di pagamento è il <b>simulatore</b>, che accetta: è l'unica configurazione eseguibile
 * (il fornitore reale è bloccato dal prerequisito #14) e permette di provare tutto senza toccare nulla
 * fuori da questa macchina. Il rifiuto ha il suo collaudo a parte, con il simulatore configurato per
 * rifiutare.
 */
@QuarkusTest
class SeatPurchaseApiTest {

    private static final String INVITATIONS = "/api/platform/v1/invitations";
    private static final String SEATS = "/api/platform/v1/me/seats";

    /** Le tariffe del listino iniziale (UC 0102): franchigia di tre posti, poi 2,99 € l'uno. */
    private static final int TARIFFA_QUARTO_POSTO = 299;

    private static final UUID SEATS_APP = PlatformCatalog.seatsAppId();

    @Inject
    TestData data;

    /**
     * L'identificativo della voce di catalogo dei posti è scritto in <b>due</b> posti — nella migrazione,
     * in chiaro, e nel codice, derivato dallo slug — perché una migrazione non può chiamare Java. Due copie
     * dello stesso valore divergono, prima o poi: qui si prova che non è ancora accaduto, e se accadesse
     * questo collaudo diventerebbe rosso invece di lasciare in tabella una riga che nessuno raggiunge.
     */
    @Test
    void laVoceDeiPostiHaLIdentificativoDerivatoDalSuoSlug() {
        assertEquals(
                UUID.fromString("22c25c07-0247-3196-8d05-a2d26587295a"),
                SEATS_APP,
                "l'identificativo della voce dei posti nella migrazione non corrisponde a quello derivato"
                        + " dallo slug: la riga inserita dalla migrazione è orfana");
    }

    /**
     * <b>La franchigia non costa nulla e non crea nulla.</b> Non c'è alcuna condizione «se i posti sono al
     * massimo tre» nel codice: il dovuto semplicemente non cambia, perché la prima fascia del listino è a
     * tariffa zero. Il collaudo prova la conseguenza osservabile — nessun abbonamento in tabella — che è
     * quella su cui un cliente potrebbe accorgersi di un addebito che non doveva esserci.
     */
    @Test
    void dentroLaFranchigiaNonNasceAlcunAbbonamento() {
        String tenant = account("Franchigia SpA", "51111111-1111-4111-8111-111111111101");

        // L'owner è il primo posto. Due inviti portano a tre posti: tutti dentro la franchigia.
        UUID primo = invita(tenant, "uno@franchigia.test");
        UUID secondo = invita(tenant, "due@franchigia.test");

        assertEquals(-1, data.seatSubscriptionQuantity(tenant, SEATS_APP),
                "dentro la franchigia l'abbonamento dei posti non deve esistere affatto");
        assertNull(data.invitationSeatChargeRef(primo), "un posto gratuito non ha un addebito");
        assertNull(data.invitationSeatChargeRef(secondo), "un posto gratuito non ha un addebito");

        given().auth().oauth2(TestTokens.withTenant(tenant, "owner"))
                .when().get(SEATS)
                .then().statusCode(200)
                .body("usedSeats", equalTo(3))
                .body("freeSeats", equalTo(3))
                .body("paidSeats", equalTo(0))
                .body("dueCents", equalTo(0))
                .body("paidQuantity", equalTo(0))
                .body("hasSubscription", equalTo(false))
                // La stima del posto successivo: è il numero che l'owner vede PRIMA di confermare.
                .body("next.seatNumber", equalTo(4))
                .body("next.unitPriceCents", equalTo(TARIFFA_QUARTO_POSTO))
                .body("next.dueCentsAfter", equalTo(TARIFFA_QUARTO_POSTO))
                .body("next.chargeCents", equalTo(TARIFFA_QUARTO_POSTO))
                .body("next.cheaperThanPrevious", equalTo(false))
                .body("pendingReduction", equalTo(false));
    }

    /**
     * <b>Il quarto posto crea l'abbonamento; il quinto ne alza la quantità.</b> Sono i due numeri scritti
     * nei requisiti di test dello use case, e vanno letti insieme: quantità 1 → dovuto 2,99 €, quantità 2 →
     * dovuto 5,98 €. Il dovuto è il <b>totale mensile</b>, non l'addebito del momento: col listino
     * progressivo il totale sale sempre, ed è la cifra che il cliente confronta con la fattura.
     */
    @Test
    void ilQuartoPostoCreaLAbbonamentoEIlQuintoNeAlzaLaQuantita() {
        String tenant = account("Oltre la franchigia SpA", "51111111-1111-4111-8111-111111111102");
        invita(tenant, "uno@oltre.test");
        invita(tenant, "due@oltre.test");

        UUID quarto = invita(tenant, "quattro@oltre.test");
        assertEquals(1, data.seatSubscriptionQuantity(tenant, SEATS_APP));
        assertEquals(1, data.subscriptionCount(tenant, SEATS_APP), "un solo abbonamento dei posti");
        assertNotNull(data.invitationSeatChargeRef(quarto), "il quarto posto è pagato: porta il suo addebito");
        riquadro(tenant)
                .body("usedSeats", equalTo(4))
                .body("paidSeats", equalTo(1))
                .body("dueCents", equalTo(299))
                .body("paidQuantity", equalTo(1))
                .body("hasSubscription", equalTo(true))
                .body("currentBand.fromSeat", equalTo(4))
                .body("currentBand.unitPriceCents", equalTo(299));

        UUID quinto = invita(tenant, "cinque@oltre.test");
        assertEquals(2, data.seatSubscriptionQuantity(tenant, SEATS_APP));
        assertEquals(1, data.subscriptionCount(tenant, SEATS_APP), "sempre lo stesso abbonamento");
        assertNotNull(data.invitationSeatChargeRef(quinto));
        riquadro(tenant)
                .body("usedSeats", equalTo(5))
                .body("paidSeats", equalTo(2))
                .body("dueCents", equalTo(598))
                .body("paidQuantity", equalTo(2));
    }

    /**
     * <b>Un posto liberato non si rimborsa e non si ripaga.</b> Un invito che scade — o che l'owner revoca —
     * libera il posto, ma il periodo è già pagato: il posto resta a disposizione dell'account, e un invito
     * nuovo entro lo stesso periodo <b>non</b> produce un secondo addebito.
     *
     * <p>È la lettura coerente con la permanenza minima mensile, e la prova osservabile è la quantità
     * dell'abbonamento: resta a 1, invece di salire a 2 come farebbe se il posto fosse stato ricomprato.
     */
    @Test
    void unPostoLiberatoNonSiRipagaNelloStessoPeriodo() {
        String tenant = account("Riuso del posto SpA", "51111111-1111-4111-8111-111111111103");
        invita(tenant, "uno@riuso.test");
        invita(tenant, "due@riuso.test");

        UUID pagato = invita(tenant, "quattro@riuso.test");
        assertEquals(1, data.seatSubscriptionQuantity(tenant, SEATS_APP));

        // L'invito scade: il posto si libera, il denaro resta pagato.
        data.expireInvitation(pagato);
        riquadro(tenant)
                .body("usedSeats", equalTo(3))
                // La quantità pagata NON scende con il posto liberato: il periodo è pagato.
                .body("paidQuantity", equalTo(1))
                // Il prossimo posto è già coperto: la stima dice zero, ed è la cosa che l'owner deve leggere
                // prima di reinvitare qualcuno al posto di chi non ha risposto.
                .body("next.chargeCents", equalTo(0))
                .body("next.dueCentsAfter", equalTo(299));

        UUID rimpiazzo = invita(tenant, "quattro-bis@riuso.test");
        assertEquals(1, data.seatSubscriptionQuantity(tenant, SEATS_APP),
                "il posto era già pagato in questo periodo: nessun secondo addebito");
        assertNull(data.invitationSeatChargeRef(rimpiazzo),
                "nessun addebito eseguito → nessun riferimento da scrivere");

        // Stessa storia con la revoca: è un atto diverso dalla scadenza, con lo stesso effetto sul denaro.
        UUID quinto = invita(tenant, "cinque@riuso.test");
        assertEquals(2, data.seatSubscriptionQuantity(tenant, SEATS_APP));
        given().auth().oauth2(TestTokens.withTenant(tenant, "owner"))
                .when().delete(INVITATIONS + "/" + quinto)
                .then().statusCode(204);
        UUID quintoBis = invita(tenant, "cinque-bis@riuso.test");
        assertEquals(2, data.seatSubscriptionQuantity(tenant, SEATS_APP));
        assertNull(data.invitationSeatChargeRef(quintoBis));
    }

    /**
     * <b>Due inviti simultanei non addebitano due volte lo stesso salto di fascia.</b> È il rischio che il
     * blocco pessimistico sull'account esiste per chiudere: senza, entrambe le richieste leggerebbero «tre
     * posti», calcolerebbero entrambe «il quarto costa 2,99» e chiederebbero due volte lo stesso denaro,
     * lasciando l'abbonamento a quantità 1 con cinque posti occupati.
     *
     * <p>La prova non è «non è andato in errore»: è che alla fine ci sono <b>due</b> inviti e la quantità è
     * <b>due</b>. Un solo invito riuscito, o una quantità di uno, sarebbero entrambi il difetto.
     */
    @Test
    void dueInvitiSimultaneiNonAddebitanoDueVolteLoStessoSalto() throws Exception {
        String tenant = account("Concorrenza SpA", "51111111-1111-4111-8111-111111111104");
        invita(tenant, "uno@concorrenza.test");
        invita(tenant, "due@concorrenza.test");
        String token = TestTokens.withTenant(tenant, "owner");

        Callable<Integer> invio = () -> given().auth().oauth2(token)
                .contentType(ContentType.JSON)
                .body(Map.of("email", "p" + UUID.randomUUID() + "@concorrenza.test"))
                .when().post(INVITATIONS)
                .then().extract().statusCode();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> esiti = pool.invokeAll(List.of(invio, invio));
            for (Future<Integer> esito : esiti) {
                assertEquals(201, esito.get(), "entrambi gli inviti devono riuscire: non c'è un tetto di posti");
            }
        } finally {
            pool.shutdownNow();
        }

        assertEquals(5, data.invitationCount(tenant) + 1, "owner + 4 inviti = 5 posti");
        assertEquals(2, data.seatSubscriptionQuantity(tenant, SEATS_APP),
                "cinque posti occupati = due posti a pagamento: il salto di fascia non si paga due volte");
        riquadro(tenant).body("usedSeats", equalTo(5)).body("dueCents", equalTo(598));
    }

    /**
     * <b>Un account in attesa di eliminazione non invita nessuno</b> (UC 0103 §5). Aggiungere una persona a
     * un conto che sta chiudendo le farebbe occupare — e pagare — un posto che sparisce fra pochi giorni.
     * Il rifiuto è un conflitto con lo stato del conto, non una mancanza di permessi.
     */
    @Test
    void unAccountInAttesaDiEliminazioneNonInvita() {
        String tenant = account("In chiusura SpA", "51111111-1111-4111-8111-111111111105");
        data.setAccountPendingDeletion(tenant);

        given().auth().oauth2(TestTokens.withTenant(tenant, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "tardi@chiusura.test"))
                .when().post(INVITATIONS)
                .then().statusCode(409)
                .body("type", equalTo("urn:appgrove:account:pending-deletion"));

        assertEquals(0, data.invitationCount(tenant));
    }

    /**
     * <b>Il costo del posto successivo scende ai confini di fascia, e il totale sale comunque.</b> È il caso
     * che l'interfaccia deve saper raccontare per esteso: quello che scende è il costo del posto in più.
     * Qui si prova che il servizio dà i due numeri giusti — {@code cheaperThanPrevious} vero e un totale
     * maggiore — perché è il servizio a doverlo dire, non l'interfaccia a doverlo dedurre.
     */
    @Test
    void alConfineDiFasciaIlPostoSuccessivoCostaMenoMaIlTotaleSale() {
        String tenant = account("Confine SpA", "51111111-1111-4111-8111-111111111106");
        // Dieci posti: l'ultimo cade nella fascia 4–10 (2,99 €), l'undicesimo nella 11–50 (1,99 €).
        for (int i = 1; i <= 9; i++) {
            data.invitationId(tenant, "p" + i + "@confine.test", "member");
        }

        riquadro(tenant)
                .body("usedSeats", equalTo(10))
                .body("dueCents", equalTo(7 * 299))
                .body("currentBand.unitPriceCents", equalTo(299))
                .body("next.seatNumber", equalTo(11))
                .body("next.unitPriceCents", equalTo(199))
                .body("next.cheaperThanPrevious", equalTo(true))
                .body("next.dueCentsAfter", equalTo(7 * 299 + 199));
    }

    // ── aiuti ────────────────────────────────────────────────────────────────

    /** Un account con il suo owner: il primo posto è sempre il suo (la franchigia lo comprende). */
    private String account(String name, String tenantId) {
        data.account(tenantId, name);
        data.user(tenantId, "sub-" + tenantId, "owner@" + tenantId + ".test", "owner");
        return tenantId;
    }

    /** Manda un invito attraverso l'operazione di rete vera e restituisce l'identificativo creato. */
    private UUID invita(String tenant, String email) {
        String id = given().auth().oauth2(TestTokens.withTenant(tenant, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", email))
                .when().post(INVITATIONS)
                .then().statusCode(201)
                .body("token", notNullValue())
                // Il collegamento all'identità non esce mai verso chi invita (UC 0118 §5), e nemmeno
                // l'addebito: chi invita non ha bisogno del riferimento della transazione.
                .body("identityId", nullValue())
                .extract().path("id");
        assertTrue(id != null && !id.isBlank());
        return UUID.fromString(id);
    }

    private io.restassured.response.ValidatableResponse riquadro(String tenant) {
        return given().auth().oauth2(TestTokens.withTenant(tenant, "owner"))
                .when().get(SEATS)
                .then().statusCode(200);
    }
}
