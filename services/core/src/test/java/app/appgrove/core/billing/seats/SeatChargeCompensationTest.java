package app.appgrove.core.billing.seats;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import app.appgrove.core.billing.PaymentProvider;
import app.appgrove.core.billing.PaymentProvider.SeatChargeCommand;
import app.appgrove.core.billing.PaymentProvider.SeatChargeResult;
import app.appgrove.core.billing.PaymentProvider.SeatChargeReversal;
import app.appgrove.core.catalog.PlatformCatalog;
import app.appgrove.core.platform.InvitationRepository;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <b>Addebito riuscito, invito non nato → l'addebito si annulla</b> (UC 0103 §5).
 *
 * <p>È l'unico ramo della storia in cui un cliente può pagare per qualcosa che non ha, e per definizione
 * non lo raggiunge nessun dato di ingresso: è un guasto. Provarlo richiede di <b>farlo accadere</b>, e
 * l'alternativa — non provarlo — lascerebbe senza rete proprio il caso in cui il danno è denaro.
 *
 * <p>Si fa accadere sostituendo due collaboratori: il fornitore di pagamento, che accetta l'addebito, e il
 * repository degli inviti, la cui scrittura fallisce. Quello che si osserva è duplice, e servono entrambe
 * le metà: lo <b>storno chiesto al fornitore</b> (altrimenti il denaro resta preso) e la <b>banca dati
 * pulita</b> — nessun invito, nessun abbonamento (altrimenti resterebbe un posto pagato che nessuno
 * occupa).
 */
@QuarkusTest
class SeatChargeCompensationTest {

    private static final String TENANT = "53333333-3333-4333-8333-333333333301";
    private static final String INVITATIONS = "/api/platform/v1/invitations";

    @Inject
    TestData data;

    @InjectMock
    PaymentProvider provider;

    @InjectMock
    InvitationRepository invitations;

    @BeforeEach
    void setUp() {
        data.account(TENANT, "Guasto dopo l'addebito SpA");
        data.user(TENANT, "sub-" + TENANT, "owner@guasto.test", "owner");
        // Tre persone in tutto: l'owner più due appartenenze. Il prossimo posto è il quarto, quindi costa.
        data.user(TENANT, "sub-guasto-1", "una@guasto.test", "member");
        data.user(TENANT, "sub-guasto-2", "due@guasto.test", "member");

        // Il conteggio degli inviti passa dal repository sostituito e risponde zero da sé (è il valore
        // predefinito di un sostituto): tre posti sono le tre appartenenze, e il prossimo è il quarto.
        // Il fornitore accetta: siamo esattamente nell'istante dopo l'addebito riuscito.
        when(provider.chargeSeats(any(SeatChargeCommand.class)))
                .thenReturn(SeatChargeResult.accepted("sub_seats_finto", "ctm_finto", "txn_finto"));

        // …e la creazione dell'invito fallisce.
        doThrow(new IllegalStateException("scrittura dell'invito fallita (simulata)"))
                .when(invitations)
                .persist(any(app.appgrove.core.platform.Invitation.class));
    }

    @Test
    void seLInvitoNonNasceLAddebitoVieneAnnullato() {
        given().auth().oauth2(TestTokens.withTenant(TENANT, "owner"))
                .contentType(ContentType.JSON)
                .body(Map.of("email", "quattro@guasto.test"))
                .when().post(INVITATIONS)
                .then().statusCode(500);

        // Lo storno è stato chiesto al fornitore: senza, il denaro resterebbe preso.
        verify(provider, times(1)).releaseSeatCharge(any(SeatChargeReversal.class));

        // E la banca dati è pulita: nessun invito, e nessun abbonamento dei posti.
        assertEquals(0, data.invitationCount(TENANT), "nessuna riga di invito rimasta a metà");
        assertEquals(
                -1,
                data.seatSubscriptionQuantity(TENANT, PlatformCatalog.seatsAppId()),
                "l'abbonamento dei posti non deve restare: il posto che doveva pagare non esiste");
    }
}
