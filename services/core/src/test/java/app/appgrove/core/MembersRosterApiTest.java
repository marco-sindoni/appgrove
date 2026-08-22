package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * L'elenco delle persone dell'account come lo legge la schermata «Members» dopo UC 0100: <b>un solo
 * elenco</b>, con per ciascuna persona le applicazioni su cui è abilitata e la data di ingresso.
 *
 * <p>Le due cose che vale la pena provare qui, e che una lettura del codice non garantisce:
 *
 * <ol>
 *   <li>l'<b>owner</b> non ha righe di permesso e le sue applicazioni sono quelle a cui l'account ha
 *       diritto (accesso implicito, UC 0098 §5): è il caso che una implementazione ingenua sbaglia,
 *       mostrando «nessuna applicazione» a chi le ha tutte;
 *   <li>il raggruppamento per persona è giusto anche con <b>più persone e più applicazioni</b> —
 *       l'errore tipico è attribuire a una persona le righe di un'altra.
 * </ol>
 */
@QuarkusTest
class MembersRosterApiTest {

    private static final String USERS = "/api/platform/v1/users";
    private static final String TENANT = "aaaaaaaa-0100-0000-0000-000000000001";

    @Inject
    TestData data;

    private static String owner() {
        return "Bearer " + TestTokens.withTenant(TENANT, "owner");
    }

    /** Identità dell'owner dell'account di prova, valorizzata da {@link #conDueApplicazioni()}. */
    private UUID ownerId;

    /** Account con due applicazioni a cui ha diritto, e il suo owner. */
    private List<UUID> conDueApplicazioni() {
        data.account(TENANT, "Acme 0100");
        UUID unoId = UUID.fromString("aaaa0100-0000-4000-8000-000000000001");
        UUID dueId = UUID.fromString("aaaa0100-0000-4000-8000-000000000002");
        data.app(unoId, "app0100-uno");
        data.app(dueId, "app0100-due");
        data.subscription(TENANT, unoId, "active");
        data.subscription(TENANT, dueId, "active");
        ownerId = data.user(TENANT, TestTokens.subjectFor(TENANT), "owner-0100@example.test", "owner");
        return List.of(unoId, dueId);
    }

    private static Map<String, Object> riga(JsonPath body, String email) {
        List<Map<String, Object>> content = body.getList("content");
        return content.stream()
                .filter(r -> email.equals(r.get("email")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("persona assente dall'elenco: " + email));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> apps(Map<String, Object> riga) {
        Object apps = riga.get("apps");
        return apps == null ? List.of() : (List<Map<String, Object>>) apps;
    }

    @Test
    void elencoUnicoConApplicazioniEDataDiIngresso() {
        List<UUID> app = conDueApplicazioni();
        UUID abilitata = data.user(TENANT, "sub-0100-abil", "abilitata-0100@example.test", "member");
        UUID nuda = data.user(TENANT, "sub-0100-nuda", "senza-app-0100@example.test", "member");
        data.appAccess(TENANT, app.get(0), abilitata, "admin");
        data.appAccess(TENANT, app.get(1), abilitata, "editor");

        JsonPath body = given().header("Authorization", owner())
                .when().get(USERS + "?size=100")
                .then().statusCode(200)
                // La data di ingresso c'è per tutti: è la nascita dell'appartenenza, che esiste sempre.
                .body("content.joinedAt", everyItem(notNullValue()))
                .extract().jsonPath();

        // L'owner: le applicazioni dell'ACCOUNT, tutte implicite e senza ruolo sull'applicazione. Il
        // confronto è contro i diritti dell'account letti dalla loro API, non contro un numero fisso:
        // il catalogo di prova porta anche le applicazioni con fascia gratuita, a cui ogni account ha
        // diritto senza abbonamento, e un numero scritto a mano qui diventerebbe falso il giorno in cui
        // il listino cambia.
        List<String> diritti = given().header("Authorization", owner())
                .when().get("/api/platform/v1/me/entitlements")
                .then().statusCode(200)
                .extract().jsonPath().getList("entitlements.appSlug", String.class)
                .stream().sorted().toList();
        List<Map<String, Object>> ownerApps = apps(riga(body, "owner-0100@example.test"));
        assertEquals(diritti, ownerApps.stream().map(a -> (String) a.get("app")).toList(),
                "le applicazioni dell'owner SONO i diritti dell'account, ordinate per nome");
        assertEquals(List.of("app0100-due", "app0100-uno"),
                ownerApps.stream().map(a -> (String) a.get("app"))
                        .filter(n -> n.startsWith("app0100-")).toList(),
                "fra cui le due applicazioni di questo account");
        assertEquals(List.of(), ownerApps.stream().filter(a -> !Boolean.TRUE.equals(a.get("implicit"))).toList(),
                "nessuna riga di permesso: l'accesso dell'owner è implicito");
        assertEquals(List.of(), ownerApps.stream().filter(a -> a.get("role") != null).toList(),
                "l'owner non ha un ruolo SU una applicazione: ce l'ha sull'account");
        assertEquals(0, data.appAccessRowsOf(TENANT, ownerId), "e non ha nemmeno una riga di permesso");

        // La persona abilitata: due applicazioni con il loro ruolo, non implicite.
        List<Map<String, Object>> sue = apps(riga(body, "abilitata-0100@example.test"));
        assertEquals(2, sue.size());
        assertEquals(Map.of("app0100-due", "editor", "app0100-uno", "admin"),
                sue.stream().collect(java.util.stream.Collectors.toMap(a -> (String) a.get("app"),
                        a -> (String) a.get("role"))),
                "ogni applicazione col ruolo che le è stato dato, e non quello di un'altra");
        assertEquals(List.of(false, false), sue.stream().map(a -> a.get("implicit")).toList());

        // La persona senza alcuna applicazione: stato legittimo, elenco vuoto e non assente.
        assertEquals(List.of(), apps(riga(body, "senza-app-0100@example.test")),
                "«nessuna applicazione» è uno stato normale, non un errore");
        assertEquals(0, data.appAccessRowsOf(TENANT, nuda));
    }

    /**
     * Il raggruppamento con <b>più persone</b>: cinque persone e nove righe di permesso, per cogliere
     * l'attribuzione sbagliata che un elenco di due persone non farebbe emergere.
     */
    @Test
    void ilRaggruppamentoReggeConPiuPersone() {
        List<UUID> app = conDueApplicazioni();
        for (int i = 1; i <= 5; i++) {
            UUID persona = data.user(TENANT, "sub-0100-g" + i, "gruppo-" + i + "-0100@example.test", "member");
            data.appAccess(TENANT, app.get(0), persona, i % 2 == 0 ? "editor" : "viewer");
            if (i <= 4) {
                data.appAccess(TENANT, app.get(1), persona, "viewer");
            }
        }

        JsonPath body = given().header("Authorization", owner())
                .when().get(USERS + "?size=100")
                .then().statusCode(200)
                .extract().jsonPath();

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> riga = riga(body, "gruppo-" + i + "-0100@example.test");
            assertEquals(i <= 4 ? 2 : 1, apps(riga).size(), "conteggio della persona " + i);
            String attesoSullaPrima = i % 2 == 0 ? "editor" : "viewer";
            assertEquals(attesoSullaPrima,
                    apps(riga).stream()
                            .filter(a -> "app0100-uno".equals(a.get("app")))
                            .map(a -> a.get("role"))
                            .findFirst()
                            .orElse(null),
                    "ruolo della persona " + i + " sulla prima applicazione");
        }
    }

    /** La lettura di dettaglio ha la stessa forma della riga da cui si arriva, applicazioni comprese. */
    @Test
    void laLetturaDiDettaglioPortaLeStesseApplicazioni() {
        List<UUID> app = conDueApplicazioni();
        UUID persona = data.user(TENANT, "sub-0100-det", "dettaglio-0100@example.test", "member");
        data.appAccess(TENANT, app.get(0), persona, "editor");

        given().header("Authorization", owner())
                .when().get(USERS + "/" + persona)
                .then().statusCode(200)
                .body("apps", hasSize(1))
                .body("apps[0].app", is("app0100-uno"))
                .body("apps[0].role", is("editor"))
                .body("apps[0].implicit", is(false))
                .body("joinedAt", notNullValue());
    }

    /**
     * <b>Il proprio profilo non porta le applicazioni</b>, ed è deliberato: {@code /users/me} parte a
     * ogni caricamento di pagina, per ogni ruolo, e caricarvi i diritti dell'account e gli accessi per
     * applicazione ne farebbe pagare il costo a chi non li usa. Se un giorno servissero là, sarà una
     * decisione, non una svista.
     */
    @Test
    void ilProprioProfiloNonPortaLeApplicazioni() {
        conDueApplicazioni();
        given().header("Authorization", owner())
                .when().get(USERS + "/me")
                .then().statusCode(200)
                .body("email", is("owner-0100@example.test"))
                .body("apps", org.hamcrest.Matchers.nullValue());
    }
}
