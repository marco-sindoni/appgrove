package app.appgrove.auth;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Account attivo della sessione sul fornitore <b>locale</b> (UC 0117): la tabella dei casi vista
 * dall'esterno, cioè attraverso i token realmente emessi.
 *
 * <p>La tabella dei casi in sé è provata sulla funzione pura ({@code ActiveAccountTest} in
 * {@code commons}) e sulla sua gemella Python ({@code test_handler.py}). Qui si prova che il
 * fornitore locale la applica <b>davvero</b> quando compone i claim — perché una regola giusta usata
 * nel posto sbagliato è indistinguibile da una regola sbagliata.
 *
 * <p>Il collaudo che conta è {@link #accountAttivoManomessoNonProduceMaiUnClaim()}: la colonna scritta
 * a mano su un'appartenenza che non è fra quelle attive non deve produrre nessun token con quel
 * claim. È l'unica prova che il valore conservato <b>non è creduto</b>.
 */
@QuarkusTest
class ActiveAccountTokenTest {

    private static final String LOGIN = "/api/auth/login";
    private static final String PASSWORD = "Password1!";

    // Persona del seme con UNA sola appartenenza: il caso di tutti gli utenti di oggi.
    private static final String BOB = "bob@bob.test";
    private static final String BOB_TENANT = "a0000000-0000-4000-8000-000000000002";
    // Account dove si costruisce la seconda appartenenza di Bob (il seme resta la fotografia del
    // caso normale: il caso «una persona, due account» si costruisce nei collaudi).
    private static final String ALTRO_TENANT = "a0000000-0000-4000-8000-0000000000f1";
    private static final UUID BOB_IDENTITY = UUID.fromString("b0000000-0000-4000-8000-000000000004");
    /** Appartenenza dell'owner di Acme (seme): esiste, ma non è di Bob. */
    private static final UUID APPARTENENZA_ALTRUI = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    @Inject
    AgroalDataSource ds;

    @BeforeEach
    void seed() {
        TestSchema.ensure(ds);
        // Stato di partenza pulito: una sola appartenenza, nessuna scelta conservata.
        exec("delete from platform.membership where tenant_id = ?", ALTRO_TENANT);
        exec("update platform.identity set active_membership_id = null where id = ?", BOB_IDENTITY);
    }

    @Test
    void unaSolaAppartenenzaIlComportamentoNonCambia() {
        // Nessuna scelta conservata e nessun selettore: chi ha un solo account non deve accorgersi
        // che questa storia esiste.
        assertEquals(BOB_TENANT, tenantOfLogin(BOB));
    }

    @Test
    void unaSolaAppartenenzaIgnoraIlValoreConservato() {
        // Colonna puntata all'appartenenza di UN'ALTRA persona (l'owner di Acme, dal seme): con una
        // sola appartenenza il valore conservato è irrilevante e il token esce identico. La chiave
        // esterna impedisce già di puntare a un'appartenenza inesistente; ciò che il vincolo NON può
        // impedire — puntare a un'appartenenza che esiste ma non è tua — lo impedisce la riverifica.
        setActiveMembership(BOB_IDENTITY, APPARTENENZA_ALTRUI);
        assertEquals(BOB_TENANT, tenantOfLogin(BOB));
    }

    @Test
    void piuAppartenenzeUsanoLAccountAttivoConservato() {
        UUID altra = secondaAppartenenza("member");
        setActiveMembership(BOB_IDENTITY, altra);
        assertEquals(ALTRO_TENANT, tenantOfLogin(BOB), "il claim segue l'account attivo, non l'anzianità");
        assertEquals("member", roleOfLogin(BOB), "anche il ruolo è quello dell'appartenenza scelta");
    }

    @Test
    void piuAppartenenzeSenzaSceltaNessunToken() {
        // «Nega e chiede di scegliere»: 409 con un messaggio comprensibile, non «credenziali non
        // valide» — che sarebbe una bugia — e nessun token emesso.
        secondaAppartenenza("member");
        given().contentType(ContentType.JSON)
                .body(Map.of("email", BOB, "password", PASSWORD))
                .when().post(LOGIN)
                .then().statusCode(409);
    }

    @Test
    void accountAttivoManomessoNonProduceMaiUnClaim() {
        // La colonna scritta a mano su un'appartenenza che non è fra quelle attive della persona:
        // nessun token con quell'account, mai. È il varco che questa storia deve chiudere.
        secondaAppartenenza("member");
        setActiveMembership(BOB_IDENTITY, APPARTENENZA_ALTRUI);
        given().contentType(ContentType.JSON)
                .body(Map.of("email", BOB, "password", PASSWORD))
                .when().post(LOGIN)
                .then().statusCode(409);
    }

    @Test
    void appartenenzaRevocataSiRicalcolaSullUnicaRimasta() {
        // L'account attivo punta a un'appartenenza chiusa: si ignora il valore conservato e si
        // ricalcola. Resta una sola appartenenza → si entra lì, senza chiedere nulla.
        UUID altra = secondaAppartenenza("member");
        setActiveMembership(BOB_IDENTITY, altra);
        exec("update platform.membership set deleted_at = now() where id = ?", altra);
        assertEquals(BOB_TENANT, tenantOfLogin(BOB));
    }

    @Test
    void ilCambioSiVedeAlRinnovoEIlTokenPrecedenteRestaValidoPerIlSuoAccount() {
        // Comportamento ATTESO, non un difetto: rinnovare non annulla il token precedente, che per il
        // tempo che gli resta continua a valere per l'account a cui la persona appartiene davvero
        // (UC 0117 §5). Va scritto in un collaudo, così nessuno lo scopre dopo credendolo un varco.
        UUID altra = secondaAppartenenza("member");
        UUID prima = appartenenza(BOB_TENANT);
        setActiveMembership(BOB_IDENTITY, prima);

        Response first = given().contentType(ContentType.JSON)
                .body(Map.of("email", BOB, "password", PASSWORD))
                .when().post(LOGIN).thenReturn();
        first.then().statusCode(200);
        String vecchioAccess = first.path("access_token");
        String cookie = first.getDetailedCookie("appgrove_refresh").getValue();
        assertEquals(BOB_TENANT, tenantOf(vecchioAccess));

        // Il cambio di account attivo (che nel prodotto passa dall'interfaccia del core).
        setActiveMembership(BOB_IDENTITY, altra);

        Response renewed = given().cookie("appgrove_refresh", cookie)
                .when().post("/api/auth/refresh").thenReturn();
        renewed.then().statusCode(200);
        assertEquals(ALTRO_TENANT, tenantOf(renewed.path("access_token")), "il rinnovo porta l'account nuovo");
        assertNotEquals(
                tenantOf(renewed.path("access_token")),
                tenantOf(vecchioAccess),
                "il token precedente NON viene riscritto: continua a portare il suo account");
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private String tenantOfLogin(String email) {
        return tenantOf(accessTokenOfLogin(email));
    }

    @SuppressWarnings("unchecked")
    private String roleOfLogin(String email) {
        Map<String, Object> claims = TestJwt.claims(accessTokenOfLogin(email));
        return ((java.util.List<String>) claims.get("roles")).get(0);
    }

    private String accessTokenOfLogin(String email) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", PASSWORD))
                .when().post(LOGIN)
                .then().statusCode(200)
                .extract().path("access_token");
    }

    private static String tenantOf(String accessToken) {
        return (String) TestJwt.claims(accessToken).get("tenant_id");
    }

    /** Seconda appartenenza di Bob in un account tutto suo; ritorna l'id dell'appartenenza. */
    private UUID secondaAppartenenza(String role) {
        exec("insert into platform.accounts(id,name,status,created_at,updated_at,created_by)"
                        + " values (?,?,'active',now(),now(),'test') on conflict (id) do nothing",
                UUID.fromString(ALTRO_TENANT), "Altro Account");
        UUID id = UUID.randomUUID();
        exec("insert into platform.membership(id,tenant_id,identity_id,role,status,created_at,updated_at,created_by)"
                        + " values (?,?,?,?,'active',now(),now(),'test')",
                id, ALTRO_TENANT, BOB_IDENTITY, role);
        return id;
    }

    private UUID appartenenza(String tenantId) {
        return queryUuid("select id from platform.membership where tenant_id = ? and identity_id = ?"
                + " and deleted_at is null", tenantId, BOB_IDENTITY);
    }

    private void setActiveMembership(UUID identityId, UUID membershipId) {
        exec("update platform.identity set active_membership_id = ? where id = ?", membershipId, identityId);
    }

    private void exec(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private UUID queryUuid(String sql, Object... params) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1, UUID.class) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
