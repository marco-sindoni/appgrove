package app.appgrove.core.billing.seats;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import app.appgrove.core.catalog.PlatformCatalog;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * <b>La voce di catalogo di piattaforma è invisibile in tutte le superfici del cliente</b> (UC 0103 §9).
 *
 * <p>È il prezzo della scelta strutturale dell'epica E22.2 — appendere i posti al catalogo per riusare
 * interi il pagamento e la fatturazione — e va pagato <b>in collaudi</b>: sono sei punti in cui il codice
 * deve <i>non</i> fare qualcosa, e senza una prova per ciascuno uno resterà scoperto e comparirà nel menu
 * laterale di un cliente. Una prova per superficie, e ognuna dice quale schermata sarebbe sbagliata.
 *
 * <p>Il collaudo parte da un account che ha <b>davvero</b> l'abbonamento dei posti: senza quello, ogni
 * esclusione passerebbe per un'altra ragione (una voce senza abbonamento e senza fascia gratuita è già
 * fuori da mezze superfici) e la prova non proverebbe nulla. È esattamente la trappola di questa tabella.
 */
@QuarkusTest
class PlatformSeatsExclusionTest {

    private static final String TENANT = "54444444-4444-4444-8444-444444444401";
    private static final String PLATFORM_TENANT = "54444444-4444-4444-8444-444444444402";
    private static final String SEATS_SLUG = PlatformCatalog.SEATS_SLUG;

    @Inject
    TestData data;

    private UUID ownerIdentity;

    @BeforeEach
    void accountConAbbonamentoDeiPosti() {
        data.account(TENANT, "Con i posti pagati SpA");
        ownerIdentity = data.user(TENANT, "sub-" + TENANT, "owner@esclusioni.test", "owner");
        // L'abbonamento dei posti c'è ed è attivo: è la condizione in cui tutte le esclusioni sono
        // difficili. Senza di esso passerebbero per caso.
        data.subscription(TENANT, PlatformCatalog.seatsAppId(), "active");
    }

    /**
     * <b>Diritti d'accesso.</b> La voce di piattaforma non concede accesso ad alcuna applicazione. È
     * l'esclusione più a monte, e la più importante: da questa lettura discende il menu laterale del
     * cliente, quindi senza di essa i «Posti dell'account» comparirebbero come un'app da aprire.
     */
    @Test
    void nonCompareFraIDirittiDellAccount() {
        given().auth().oauth2(TestTokens.withTenant(TENANT, "owner"))
                .when().get("/api/platform/v1/me/entitlements")
                .then().statusCode(200)
                .body("entitlements.appSlug", not(hasItem(SEATS_SLUG)));
    }

    /**
     * <b>Vetrina del cliente.</b> Il catalogo è l'elenco di ciò che si compra e si apre: i posti non si
     * comprano da una card, si comprano invitando una persona.
     */
    @Test
    void nonCompareNellaVetrina() {
        given().auth().oauth2(TestTokens.withTenant(TENANT, "owner"))
                .when().get("/api/platform/v1/me/catalog")
                .then().statusCode(200)
                .body("apps.appSlug", not(hasItem(SEATS_SLUG)));
    }

    /**
     * <b>«Dove posso entrare».</b> È la lettura che il menu laterale interroga: se la voce comparisse qui,
     * comparirebbe a schermo. L'esclusione è a monte (nei diritti), ma va provata <b>qui</b>, perché è qui
     * che il difetto si vedrebbe.
     */
    @Test
    void nonCompareFraLeApplicazioniInCuiPossoEntrare() {
        given().auth().oauth2(TestTokens.withTenant(TENANT, "owner"))
                .when().get("/api/platform/v1/me/app-access")
                .then().statusCode(200)
                .body("appSlug", not(hasItem(SEATS_SLUG)));
    }

    /**
     * <b>Applicazioni per persona.</b> L'owner ha accesso implicito a tutto ciò a cui l'account ha diritto:
     * senza l'esclusione, la schermata dei membri direbbe «l'owner è abilitato ai Posti dell'account», che
     * non vuol dire niente.
     */
    @Test
    void nonCompareFraLeApplicazioniDiUnaPersona() {
        given().auth().oauth2(TestTokens.withTenant(TENANT, "owner"))
                .when().get("/api/platform/v1/users")
                .then().statusCode(200)
                .body("content.find { it.id == '" + ownerIdentity + "' }.apps.app",
                        not(hasItem(SEATS_SLUG)));
    }

    /**
     * <b>Sezione degli abbonamenti self-service.</b> Qui si cambia fascia, si disdice e si riattiva: nessuna
     * delle tre ha senso sui posti, e offrire quei tre comandi farebbe fare al cliente la cosa sbagliata.
     * La presentazione onesta dei posti — righe, fasce, calcolo — è di UC 0106.
     */
    @Test
    void nonCompareFraGliAbbonamentiSelfService() {
        given().auth().oauth2(TestTokens.withTenant(TENANT, "owner"))
                .when().get("/api/platform/v1/me/subscriptions")
                .then().statusCode(200)
                .body("subscriptions.appSlug", not(hasItem(SEATS_SLUG)));
    }

    /**
     * <b>Console di amministrazione.</b> L'unica superficie in cui la voce <b>si vede</b>, ed è deliberato:
     * chi amministra deve poter constatare che esiste. Ma si vede per quello che è — {@code kind =
     * platform} — e <b>non</b> entra nella matrice dei diritti, che risponde a «che cosa vede questo
     * cliente».
     */
    @Test
    void inConsoleSiVedeMarcataMaNonNellaMatriceDeiDiritti() {
        data.account(PLATFORM_TENANT, "Piattaforma");
        data.user(PLATFORM_TENANT, "sub-" + PLATFORM_TENANT, "admin@esclusioni.test", "owner");
        String admin = TestTokens.withTenant(PLATFORM_TENANT, "owner", "platform-admin");

        given().auth().oauth2(admin)
                .when().get("/api/platform/v1/admin/apps")
                .then().statusCode(200)
                .body("find { it.slug == '" + SEATS_SLUG + "' }.kind", equalTo("platform"))
                // …e ogni altra riga resta un'applicazione: la colonna nuova non ha riclassificato nulla.
                .body("findAll { it.slug != '" + SEATS_SLUG + "' }.kind", everyItem(equalTo("application")));

        given().auth().oauth2(admin)
                .when().get("/api/platform/v1/admin/accounts/" + TENANT)
                .then().statusCode(200)
                .body("entitlements.appSlug", not(hasItem(SEATS_SLUG)));
    }
}
