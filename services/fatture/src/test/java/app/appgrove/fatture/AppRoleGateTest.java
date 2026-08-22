package app.appgrove.fatture;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.appgrove.commons.access.AppRole;
import app.appgrove.commons.access.AppRoleRequiredException;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Il contratto dei tre ruoli, visto da dentro l'app <b>fatture</b> (UC 0101 §9): un {@code viewer} legge e
 * non scrive, un {@code editor} scrive, un {@code admin} pure — e le operazioni <b>esenti</b> passano per
 * tutti, anche per chi non ha alcun accesso all'applicazione.
 *
 * <p>Fino a UC 0099 questa applicazione non dichiarava alcun ruolo minimo: era «a utente singolo» e la
 * domanda non si poneva. Questi collaudi sono la prova che la stessa semantica vale nelle due applicazioni
 * esistenti, che è tutto il punto della storia: due applicazioni che interpretano {@code editor} in due modi
 * diversi renderebbero il ruolo una parola vuota.
 *
 * <p>Il percorso attraversato è quello di produzione: copia locale del ruolo → (all'occorrenza) rete di
 * sicurezza finta ({@link MockAppRoleService}). Nessun collaudo scavalca la copia, perché è la copia a
 * decidere in produzione.
 */
@QuarkusTest
class AppRoleGateTest {

    private static final String INVOICES = "/api/fatture/v1/invoices";
    private static final String QUOTA = "/api/fatture/v1/quota";

    /** Un account per test: la quota di `fatture` è a consumo e i residui falserebbero il successivo. */
    private static final AtomicInteger TENANT_SEQ = new AtomicInteger();

    private String token;

    @Inject
    TestProjection projection;

    @BeforeEach
    void prepare() {
        String tenant = String.format("11111111-0101-0000-0000-%012d", TENANT_SEQ.incrementAndGet());
        token = TestTokens.withTenant(tenant, "owner");
        // Le copie locali e il finto servizio del ruolo sono già azzerati da ProjectionResetCallback.
    }

    // ── il ruolo minimo, applicato ───────────────────────────────────────────

    @Test
    void aViewerReadsAndCannotWrite() {
        MockAppRoleService.role = AppRole.viewer;

        list().then().statusCode(200);

        Response refusal = create("Tentativo di un viewer");
        refusal.then().statusCode(403)
                .body("type", is(AppRoleRequiredException.TYPE_INSUFFICIENT))
                .body("requiredRole", is("editor"))
                .body("role", is("viewer"));
        assertTrue(
                refusal.body().asString().contains("editor"),
                "il rifiuto deve NOMINARE il ruolo che serve: «non puoi fare questo» è inutile senza dire cosa serve");
    }

    @Test
    void anEditorReadsAndWrites() {
        MockAppRoleService.role = AppRole.editor;
        list().then().statusCode(200);
        create("Scritta da un editor").then().statusCode(201);
    }

    @Test
    void anAdminReadsAndWritesToo() {
        MockAppRoleService.role = AppRole.admin;
        list().then().statusCode(200);
        create("Scritta da un admin").then().statusCode(201);
    }

    /**
     * Le tre operazioni dispositive chiedono lo <b>stesso</b> ruolo: la cascata di UC 0101 §4 non distingue
     * fra creare, modificare e cancellare. Se una delle tre restasse senza varco, qui si vedrebbe un 201 o
     * un 200 dove ci si aspetta un rifiuto.
     */
    @Test
    void everyDispositiveOperationRefusesAViewer() {
        MockAppRoleService.role = AppRole.editor;
        String id = create("Creata per essere modificata").then().statusCode(201).extract().path("id");

        // Abbassato il ruolo, la copia locale è ancora FRESCA e vale ancora `editor`: è il comportamento
        // voluto (la finestra di pochi secondi dichiarata da UC 0099, provata sul Mini-CRM col vero evento
        // di invalidazione). Qui in prova c'è la CLASSIFICAZIONE, non l'invalidazione: si azzera la copia
        // così che il ruolo nuovo valga subito e le tre operazioni dispositive rispondano per quello che
        // dichiarano.
        MockAppRoleService.role = AppRole.viewer;
        projection.clear();

        given().header("Authorization", "Bearer " + token)
                .when().get(INVOICES + "/" + id)
                .then().statusCode(200);
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("status", "issued"))
                .when().patch(INVOICES + "/" + id)
                .then().statusCode(403)
                .body("requiredRole", is("editor"));
        given().header("Authorization", "Bearer " + token)
                .when().delete(INVOICES + "/" + id)
                .then().statusCode(403)
                .body("requiredRole", is("editor"));
    }

    /**
     * Il collaudo che chiude una regressione trovata col collaudo manuale del 2026-08-22: la lista dei
     * ruoli di <b>piattaforma</b> di {@code @RolesAllowed} escludeva {@code member}, e Quarkus la applica
     * <b>prima</b> dei filtri JAX-RS (sicurezza «eager», a livello di rotta). Ogni collaboratore riceveva
     * quindi un {@code 403} <b>senza corpo</b> e il varco del ruolo non arrivava mai a decidere: la
     * classificazione di questa storia era una dichiarazione senza effetto per tutti tranne l'owner, che ha
     * il ruolo massimo per costruzione. Se qualcuno restringesse di nuovo quella lista, qui si vedrebbe una
     * risposta senza il campo che nomina il ruolo richiesto.
     */
    @Test
    void aPlatformMemberReachesTheApplicationRoleGateAndGetsARefusalThatSpeaks() {
        String collaborator = TestTokens.withTenant(
                String.format("11111111-0101-0000-0000-%012d", TENANT_SEQ.incrementAndGet()), "member");
        MockAppRoleService.role = AppRole.viewer;

        given().header("Authorization", "Bearer " + collaborator).when().get(INVOICES)
                .then().statusCode(200);

        given().header("Authorization", "Bearer " + collaborator)
                .contentType(ContentType.JSON)
                .body(Map.of("customerName", "Tentativo di un collaboratore"))
                .when().post(INVOICES)
                .then().statusCode(403)
                .body("type", is(AppRoleRequiredException.TYPE_INSUFFICIENT))
                .body("requiredRole", is("editor"));
    }

    // ── nessun accesso: un rifiuto DIVERSO ───────────────────────────────────

    @Test
    void withoutAnyAccessEvenReadingIsRefusedWithTheOtherRefusal() {
        MockAppRoleService.role = null; // diniego noto dalla fonte di verità

        list().then().statusCode(403).body("type", is(AppRoleRequiredException.TYPE_NO_ACCESS));
        create("Tentativo senza accesso").then().statusCode(403)
                .body("type", is(AppRoleRequiredException.TYPE_NO_ACCESS));
    }

    // ── le operazioni esenti: passano per tutti ──────────────────────────────

    /**
     * Lo stato di quota è dichiarato <b>esente dai ruoli</b> nel documento delle operazioni. È la prova che
     * l'esenzione non è teorica: se qualcuno mettesse il varco «per coerenza», il banner del consumo
     * diventerebbe un rifiuto per chi non è ancora stato abilitato.
     */
    @Test
    void theExemptQuotaReadIsReachableByEveryone() {
        for (AppRole role : AppRole.values()) {
            MockAppRoleService.role = role;
            quota().then().statusCode(200);
        }
        MockAppRoleService.role = null; // nessun accesso all'applicazione: passa comunque
        quota().then().statusCode(200);

        assertEquals(
                0,
                MockAppRoleService.calls.get(),
                "un'operazione esente non deve nemmeno CHIEDERE il ruolo: se lo chiedesse, con il core giù"
                        + " si spegnerebbe");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private Response list() {
        return given().header("Authorization", "Bearer " + token).when().get(INVOICES);
    }

    private Response quota() {
        return given().header("Authorization", "Bearer " + token).when().get(QUOTA);
    }

    private Response create(String customerName) {
        return given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("customerName", customerName))
                .when().post(INVOICES);
    }
}
