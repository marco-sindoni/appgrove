package app.appgrove.core.catalog;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Vetrina del catalogo per l'account corrente (UC 0095): {@code GET /api/platform/v1/me/catalog}.
 *
 * <p>Usa il seed multi-stato (UC 0011), che offre già una matrice ricca: Acme→teams active,
 * Acme→notes past_due, Acme→legacy active su app <b>spenta</b>, Bob→notes trialing, Bob→teams canceled,
 * più un account senza alcun abbonamento. Verifica gli stati contro dati veri, l'isolamento fra account,
 * la parte descrittiva servita dal listino e il fail-closed sul tenant.
 */
@QuarkusTest
class CatalogApiTest {

    private static final String PATH = "/api/platform/v1/me/catalog";
    private static final String ACME = "a0000000-0000-4000-8000-000000000001";
    private static final String BOB = "a0000000-0000-4000-8000-000000000002";
    private static final String FRESH = "a0000000-0000-4000-8000-0000000000ff";

    @Inject
    AgroalDataSource ds;

    @Inject
    TestData data;

    @BeforeEach
    void seed() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).getParent().getParent();
        String sql = Files.readString(root.resolve("dev/seed/seed.sql"))
                + "\n"
                + Files.readString(root.resolve("dev/seed/seed-subscriptions.sql"));
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        }
    }

    @Test
    void statiDelleCardDerivatiDaiDatiVeri() {
        acme().body("apps.find { it.appSlug == 'teams' }.state", is("active"))
                .body("apps.find { it.appSlug == 'notes' }.state", is("payment_pending"))
                // Coerenza che UC 0076 aveva lasciato aperta: abbonamento formalmente attivo, app spenta
                // dalla piattaforma → la card lo dice, invece di mentire con "Active".
                .body("apps.find { it.appSlug == 'legacy' }.state", is("disabled_by_platform"))
                // Fascia gratuita e nessun abbonamento: l'app è già in uso, non una proposta d'acquisto.
                .body("apps.find { it.appSlug == 'fatture' }.state", is("active"))
                .body("apps.find { it.appSlug == 'fatture' }.planName", is("Fatture Free"));
    }

    @Test
    void provaInCorsoEScadenzaVisibili() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(BOB, "owner"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("apps.find { it.appSlug == 'notes' }.state", is("trial"))
                .body("apps.find { it.appSlug == 'notes' }.trialEndsAt", notNullValue())
                // Abbonamento disdetto e nessuna fascia gratuita: teams torna acquistabile.
                .body("apps.find { it.appSlug == 'teams' }.state", is("available"))
                .body("apps.find { it.appSlug == 'teams' }.trialEndsAt", is(nullValue()));
    }

    @Test
    void appSpentaEMaiSottoscrittaRestaFuoriDallaVetrina() {
        // Bob non ha abbonamenti su legacy (spenta): non deve nemmeno vederla — non si fa vetrina di ciò
        // che la piattaforma ha deciso di non vendere. Acme, che paga, la vede (test sopra).
        given().header("Authorization", "Bearer " + TestTokens.withTenant(BOB, "owner"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("apps.appSlug", not(hasItem("legacy")))
                .body("apps.appSlug", not(hasItem("crm"))); // spenta di default, nessun abbonamento
        acme().body("apps.appSlug", hasItem("legacy"));
    }

    @Test
    void vetrinaIsolataPerAccount() {
        // Stessa app, due account, due stati diversi: notes è in sofferenza di pagamento per Acme e in
        // prova per Bob. Nessuno dei due legge lo stato dell'altro.
        acme().body("apps.find { it.appSlug == 'notes' }.state", is("payment_pending"));
        given().header("Authorization", "Bearer " + TestTokens.withTenant(BOB, "owner"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("apps.find { it.appSlug == 'notes' }.state", is("trial"));
    }

    @Test
    void accountSenzaAbbonamentiVedeProposteEBaselineGratuita() {
        given().header("Authorization", "Bearer " + TestTokens.withTenant(FRESH, "owner"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("apps.find { it.appSlug == 'teams' }.state", is("available"))
                .body("apps.find { it.appSlug == 'notes' }.state", is("active")) // fascia gratuita
                .body("apps.find { it.appSlug == 'fatture' }.state", is("active"));
    }

    @Test
    void prezzoDiPartenzaDalListinoOAssenteSeTuttoGratuito() {
        // teams: solo fascia a pagamento, mensile 1900 e annuale 19000 → si parte dal mensile.
        acme().body("apps.find { it.appSlug == 'teams' }.startingPrice.amount", is(1900))
                .body("apps.find { it.appSlug == 'teams' }.startingPrice.currency", is("EUR"))
                .body("apps.find { it.appSlug == 'teams' }.startingPrice.billingCycle", is("monthly"))
                // fatture: sole fasce gratuite → nessun prezzo di partenza, non "€0".
                .body("apps.find { it.appSlug == 'fatture' }.startingPrice", is(nullValue()));
    }

    @Test
    void parteDescrittivaServitaNelleCinqueLingue() {
        acme().body("apps.find { it.appSlug == 'fatture' }.category", is("green"))
                .body("apps.find { it.appSlug == 'fatture' }.descriptions.it", notNullValue())
                .body("apps.find { it.appSlug == 'fatture' }.descriptions.en", notNullValue())
                .body("apps.find { it.appSlug == 'fatture' }.descriptions.fr", notNullValue())
                .body("apps.find { it.appSlug == 'fatture' }.descriptions.es", notNullValue())
                .body("apps.find { it.appSlug == 'fatture' }.descriptions.de", notNullValue());
    }

    @Test
    void unCicloDiFatturazioneFuoriCatalogoNonSpegneLaVetrina() {
        // Regressione: caricando il listino come entità, una singola riga con un ciclo di fatturazione
        // fuori dall'enum (dato storico o scritto da un'integrazione) faceva fallire l'INTERA pagina con
        // un 500. Qui il ciclo è un'etichetta da mostrare, non un valore su cui decidere: la vetrina
        // deve degradare, non spegnersi.
        UUID appId = UUID.randomUUID();
        UUID tierId = UUID.randomUUID();
        String slug = "uc0095-" + appId.toString().substring(0, 8);
        data.app(appId, slug);
        data.appTier(tierId, appId, "pro");
        data.appPrice(UUID.randomUUID(), tierId, "year", "pri_" + tierId, 9900);

        acme().body("apps.find { it.appSlug == '" + slug + "' }.startingPrice.amount", is(9900))
                .body("apps.find { it.appSlug == '" + slug + "' }.startingPrice.billingCycle", is("year"));
    }

    @Test
    void tenantMancanteEFailClosed() {
        // Token autenticato ma senza tenant_id → fail-closed del resolver → 403 (invariante #1). Non
        // esiste alcun parametro con cui indicare un account: il tenant può venire solo dal token.
        given().header("Authorization", "Bearer " + TestTokens.withRolesNoTenant("owner"))
                .when().get(PATH)
                .then().statusCode(403);
    }

    @Test
    void vetrinaApertaAOgniRuoloMaNonAgliAnonimi() {
        // La vetrina non richiede alcun diritto d'uso: un member vede le stesse app e gli stessi stati
        // dell'owner. Il divieto vero (avviare l'acquisto) vive su CheckoutResource, non qui.
        given().header("Authorization", "Bearer " + TestTokens.withTenant(ACME, "member"))
                .when().get(PATH)
                .then().statusCode(200)
                .body("apps.appSlug", hasItem("teams"))
                .body("apps.find { it.appSlug == 'legacy' }.state", is("disabled_by_platform"));
        given().when().get(PATH).then().statusCode(401);
    }

    private static io.restassured.response.ValidatableResponse acme() {
        return given().header("Authorization", "Bearer " + TestTokens.withTenant(ACME, "owner"))
                .when().get(PATH)
                .then().statusCode(200);
    }
}
