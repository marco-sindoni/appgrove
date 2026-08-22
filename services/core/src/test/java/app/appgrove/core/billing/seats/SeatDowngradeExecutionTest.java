package app.appgrove.core.billing.seats;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.access.AppRole;
import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import app.appgrove.core.catalog.PlatformCatalog;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * <b>L'esecuzione della riduzione alla scadenza</b> (UC 0104 §4.5): le persone escono davvero, i loro
 * accessi si cancellano, e la quantità dell'abbonamento dei posti <b>scende</b> — per la prima volta
 * nell'epica, perché in UC 0103 saliva soltanto.
 *
 * <p>Questo collaudo esercita la parte che gira <b>fuori</b> da una richiesta autenticata: lo spazzino e
 * l'esecutore, che lavorano a SQL nativo con l'account esplicito perché senza token nessuna sessione
 * Hibernate si apre. Si invocano direttamente con un «adesso» scelto: un collaudo che aspettasse la fine di
 * un periodo vero non sarebbe un collaudo.
 *
 * <p>Le due proprietà su cui insiste — perché sono quelle che costano al cliente quando mancano — sono
 * l'<b>idempotenza</b> (lo spazzino può girare due volte) e l'<b>ordine col rinnovo</b> (il periodo nuovo
 * deve nascere con la quantità già ridotta).
 */
@QuarkusTest
class SeatDowngradeExecutionTest {

    private static final String REDUCTION = "/api/platform/v1/me/seats/reduction";
    private static final String INVITATIONS = "/api/platform/v1/invitations";
    private static final String SEATS = "/api/platform/v1/me/seats";
    private static final UUID SEATS_APP = PlatformCatalog.seatsAppId();

    @Inject
    TestData data;

    @Inject
    SeatDowngradeSweeper sweeper;

    @Inject
    SeatDowngradeExecutor executor;

    @Inject
    SeatDowngradeMetrics metrics;

    @Inject
    app.appgrove.core.billing.PaddleSignature signature;

    @Inject
    app.appgrove.core.billing.WebhookIngestService ingest;

    @Inject
    app.appgrove.core.billing.PaddleWebhookConsumer consumer;

    /**
     * <b>Il caso principale dell'esecuzione.</b> Cinque persone, due indicate: alla scadenza le due escono,
     * i loro accessi alle applicazioni sono cancellati, la quantità dell'abbonamento scende da 3 a 0 e il
     * dovuto letto dal riquadro torna a zero.
     *
     * <p>La quantità scende a <b>zero</b> e non a «2 meno 2»: si ricalcola dai posti effettivamente
     * occupati. La differenza si vede su un'esecuzione ripetuta, ed è il collaudo successivo.
     */
    @Test
    void allaScadenzaLePersoneEsconoEIlDovutoScende() {
        Scenario s = scenario("Esegui SpA", "53111111-1111-4111-8111-111111111101");
        // Le due persone da cessare hanno un accesso a una applicazione: deve uscire con loro.
        UUID app = UUID.randomUUID();
        data.app(app, "app-0104-esegui");
        data.appAccess(s.tenant, app, s.membri.get(0), AppRole.editor.name());
        data.appAccess(s.tenant, app, s.membri.get(1), AppRole.viewer.name());

        indica(s, s.membri.get(0), s.membri.get(1));
        data.backdateSeatDowngrade(s.tenant, OffsetDateTime.now().minusMinutes(1));

        List<String> eseguiti = sweeper.sweep(Instant.now());

        assertTrue(eseguiti.contains(s.tenant), "l'account con la riduzione scaduta deve essere eseguito");
        assertEquals("executed", data.seatDowngradeStatus(s.tenant));
        assertNotNull(data.seatDowngradeExecutedAt(s.tenant));
        assertFalse(data.membershipAlive(s.tenant, s.membri.get(0)), "la persona indicata deve essere uscita");
        assertFalse(data.membershipAlive(s.tenant, s.membri.get(1)), "la persona indicata deve essere uscita");
        assertTrue(data.membershipAlive(s.tenant, s.membri.get(2)), "chi non era indicato resta");
        assertEquals(0, data.appAccessCount(s.tenant, app, s.membri.get(0)),
                "gli accessi alle applicazioni escono con la persona");
        assertEquals(0, data.appAccessCount(s.tenant, app, s.membri.get(1)));
        assertEquals(0, data.seatSubscriptionQuantity(s.tenant, SEATS_APP),
                "la quantità dell'abbonamento deve scendere ai posti a pagamento effettivi");
        // Una riduzione che non è più in attesa non ha persone indicate VIVE: è la stessa regola
        // dell'annullamento, e vale anche qui. Difetto trovato eseguendo la guida di collaudo: la
        // chiusura delle righe c'era ma non cancellava nulla. Nessuna informazione si perde —
        // l'esportazione dei dati personali legge anche le righe cancellate logicamente.
        assertEquals(0, data.seatDowngradeItemCount(s.tenant),
                "dopo l'esecuzione nessuna persona deve restare indicata");

        riquadro(s.tenant)
                .body("usedSeats", org.hamcrest.Matchers.equalTo(3))
                .body("dueCents", org.hamcrest.Matchers.equalTo(0))
                .body("pendingReduction", org.hamcrest.Matchers.equalTo(false));

        // Il blocco è caduto con l'esecuzione: gli inviti tornano possibili.
        given().auth().oauth2(owner(s))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "dopo@esegui.test"))
                .when().post(INVITATIONS)
                .then().statusCode(201);
    }

    /**
     * <b>Eseguire due volte non rimuove due volte</b> e non lascia stati incoerenti (trappola nota n. 2 del
     * piano di lavoro): lo spazzino può girare di nuovo sulla stessa riduzione dopo una ripartenza.
     *
     * <p>La prova che conta è la <b>quantità</b>: se scendesse per differenza invece di ricalcolarsi, la
     * seconda esecuzione la porterebbe sotto il dovuto — e l'errore andrebbe nel verso in cui nessuno lo
     * scopre, perché il cliente paga meno.
     */
    @Test
    void eseguireDueVolteNonRimuoveDueVolte() {
        Scenario s = scenario("Ripeti SpA", "53111111-1111-4111-8111-111111111102");
        indica(s, s.membri.get(0));
        data.backdateSeatDowngrade(s.tenant, OffsetDateTime.now().minusMinutes(1));

        sweeper.sweep(Instant.now());
        int quantitaDopoUno = data.seatSubscriptionQuantity(s.tenant, SEATS_APP);
        Instant eseguitaA = data.seatDowngradeExecutedAt(s.tenant);
        int appartenenzeDopoUno = data.membershipCount(s.tenant);

        // Secondo giro: la riduzione non è più in attesa, quindi non c'è nulla da fare.
        List<String> secondo = sweeper.sweep(Instant.now());
        // E anche forzando l'esecutore sullo stesso account, nulla cambia.
        assertFalse(executor.executeFor(s.tenant, Instant.now()),
                "una riduzione già eseguita non deve essere eseguita di nuovo");

        assertFalse(secondo.contains(s.tenant));
        assertEquals(quantitaDopoUno, data.seatSubscriptionQuantity(s.tenant, SEATS_APP),
                "la quantità si ricalcola, non si decrementa: due esecuzioni danno lo stesso valore");
        assertEquals(appartenenzeDopoUno, data.membershipCount(s.tenant));
        assertEquals(eseguitaA, data.seatDowngradeExecutedAt(s.tenant),
                "l'istante di esecuzione è quello della prima volta");
    }

    /**
     * <b>Prima della scadenza non si esegue nulla.</b> Sembra ovvio ed è la promessa centrale della storia:
     * il posto è pagato per tutto il mese, e chi è indicato lavora fino allo scadere.
     */
    @Test
    void primaDellaScadenzaNonSiEseguNulla() {
        Scenario s = scenario("Attesa SpA", "53111111-1111-4111-8111-111111111103");
        indica(s, s.membri.get(0));

        List<String> eseguiti = sweeper.sweep(Instant.now());

        assertFalse(eseguiti.contains(s.tenant));
        assertEquals("pending", data.seatDowngradeStatus(s.tenant));
        assertTrue(data.membershipAlive(s.tenant, s.membri.get(0)));
        assertEquals(3, data.seatSubscriptionQuantity(s.tenant, SEATS_APP));
    }

    /**
     * <b>La misura delle riduzioni scadute e non eseguite.</b> È il presidio contro il guasto che nessuno
     * vede: zero in condizioni normali, positivo quando una data è passata senza esecuzione, e di nuovo zero
     * appena lo spazzino ha fatto il suo giro.
     *
     * <p>Senza questa misura una riduzione non eseguita è invisibile da ogni punto di osservazione, e il
     * primo a scoprirlo è il cliente dalla fattura.
     */
    @Test
    void lAttesaScadutaENonEseguitaSiMisura() {
        Scenario s = scenario("Misura SpA", "53111111-1111-4111-8111-111111111104");
        indica(s, s.membri.get(0));
        Instant adesso = Instant.now();

        long primaDellaScadenza = metrics.publish(adesso);
        data.backdateSeatDowngrade(s.tenant, OffsetDateTime.now().minusHours(2));
        long dopoLaScadenza = metrics.publish(adesso);

        assertEquals(primaDellaScadenza + 1, dopoLaScadenza,
                "una riduzione con la data passata e non eseguita deve comparire nella misura");

        sweeper.sweep(Instant.now());
        assertEquals(0, metrics.publish(adesso),
                "dopo un giro dello spazzino non deve restare alcuna riduzione scaduta: è l'invariante"
                        + " che la misura sorveglia");
    }

    /**
     * <b>L'ordine col rinnovo</b> (UC 0104 §5): un evento del fornitore sull'abbonamento dei posti esegue
     * <b>prima</b> la riduzione dovuta, così il periodo nuovo nasce già con la quantità ridotta.
     *
     * <p>Perché non basta lo spazzino: quello gira ogni ora e l'evento arriva quando arriva — l'ordine fra i
     * due non è garantito, e nell'ordine sbagliato il cliente pagherebbe un mese intero alla quantità
     * vecchia. Qui l'evento si consegna a mano e si constata che, uscendo, l'abbonamento porta il periodo
     * <b>nuovo</b> e la quantità <b>ridotta</b>.
     */
    @Test
    void ilRinnovoDelPeriodoTrovaLaQuantitaGiaRidotta() {
        Scenario s = scenario("Rinnovo SpA", "53111111-1111-4111-8111-111111111105");
        indica(s, s.membri.get(0));
        data.backdateSeatDowngrade(s.tenant, OffsetDateTime.now().minusMinutes(1));
        Instant periodoVecchio = data.seatSubscriptionPeriodEnd(s.tenant, SEATS_APP);
        assertEquals(3, data.seatSubscriptionQuantity(s.tenant, SEATS_APP));

        // L'evento di rinnovo dell'abbonamento dei POSTI, consegnato attraverso la pipeline vera
        // (ingestione firmata → coda → consumatore) e non passando dallo spazzino.
        Instant nuovaFine = Instant.now().plus(30, ChronoUnit.DAYS);
        rinnovoDeiPosti(s.tenant, nuovaFine);

        assertEquals("executed", data.seatDowngradeStatus(s.tenant),
                "la riduzione deve essere stata eseguita dall'arrivo dell'evento, non dallo spazzino");
        assertFalse(data.membershipAlive(s.tenant, s.membri.get(0)));
        assertEquals(1, data.seatSubscriptionQuantity(s.tenant, SEATS_APP),
                "il periodo nuovo deve nascere con la quantità ridotta");
        Instant periodoNuovo = data.seatSubscriptionPeriodEnd(s.tenant, SEATS_APP);
        assertTrue(periodoNuovo.isAfter(periodoVecchio),
                "il periodo dell'abbonamento deve essere avanzato");
    }

    // ── aiuti ────────────────────────────────────────────────────────────────

    private record Scenario(String tenant, UUID owner, List<UUID> membri) {}

    /**
     * Cinque persone (owner compreso) e l'abbonamento dei posti creato dall'<b>acquisto vero</b>: si compra
     * il sesto posto attraverso l'operazione di rete e si revoca l'invito, che serviva solo a far nascere
     * l'abbonamento con la sua fine di periodo autentica. Il posto resta pagato (nessun rimborso, UC 0103),
     * quindi la quantità dell'abbonamento è <b>3</b> mentre i posti a pagamento sono due: lo scarto è
     * voluto, ed è ciò che rende visibile il <b>ricalcolo</b> della quantità invece di una sottrazione.
     */
    private Scenario scenario(String name, String tenantId) {
        data.account(tenantId, name);
        UUID owner = data.user(tenantId, "sub-" + tenantId, "owner@" + tenantId + ".test", "owner");
        List<UUID> membri = new java.util.ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            membri.add(data.user(
                    tenantId, "sub-" + i + "-" + tenantId, "p" + i + "@" + tenantId + ".test", "member"));
        }
        String invito = given().auth().oauth2(TestTokens.withTenant(tenantId, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "sesto@" + tenantId + ".test"))
                .when().post(INVITATIONS)
                .then().statusCode(201)
                .extract().path("id");
        given().auth().oauth2(TestTokens.withTenant(tenantId, "owner"))
                .when().delete(INVITATIONS + "/" + invito)
                .then().statusCode(204);
        return new Scenario(tenantId, owner, List.copyOf(membri));
    }

    private static String owner(Scenario s) {
        return TestTokens.withTenant(s.tenant, "owner");
    }

    private void indica(Scenario s, UUID... userIds) {
        given().auth().oauth2(owner(s))
                .contentType(ContentType.JSON)
                .body(Map.of("userIds", java.util.Arrays.stream(userIds).map(UUID::toString).toList()))
                .when().post(REDUCTION)
                .then().statusCode(201);
    }

    /**
     * Consegna un evento {@code subscription.updated} del fornitore per l'abbonamento dei <b>posti</b>, con
     * un periodo nuovo: la forma che avrà il rinnovo quando il fornitore vero sarà attivo. Passa dalla
     * pipeline vera — ingestione firmata, coda, consumatore — perché è l'unico modo di provare l'ordine
     * <i>dentro</i> la transazione dell'evento.
     */
    private void rinnovoDeiPosti(String tenant, Instant nuovaFine) {
        Instant occorsoA = Instant.now();
        String body = """
                {
                  "event_id": "evt_seat_renewal_%s",
                  "event_type": "subscription.updated",
                  "occurred_at": "%s",
                  "data": {
                    "paddle_subscription_id": "sub_seats_%s",
                    "status": "active",
                    "current_period_start": "%s",
                    "current_period_end": "%s",
                    "custom_data": { "tenant_id": "%s", "app_id": "%s" }
                  }
                }
                """
                .formatted(
                        UUID.randomUUID(), occorsoA, tenant, occorsoA, nuovaFine, tenant, SEATS_APP);
        ingest.ingest(body, signature.sign(body));
        consumer.drain();
    }

    private io.restassured.response.ValidatableResponse riquadro(String tenant) {
        return given().auth().oauth2(TestTokens.withTenant(tenant, "owner"))
                .when().get(SEATS)
                .then().statusCode(200);
    }
}
