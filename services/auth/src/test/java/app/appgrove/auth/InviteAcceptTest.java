package app.appgrove.auth;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Accept invito (UC 0058): la persona entra nel tenant invitante col ruolo dell'invito; token
 * scaduto/invalido respinto. Dopo UC 0116 «entrare» significa una <b>appartenenza</b> in più, e
 * l'identità si crea solo se quella persona non esiste ancora sulla piattaforma.
 */
@QuarkusTest
class InviteAcceptTest {

    private static final String ACME = "a0000000-0000-4000-8000-000000000001";

    @Inject
    AgroalDataSource ds;

    @BeforeEach
    void setup() {
        TestSchema.ensure(ds);
    }

    @Test
    void acceptSeedInviteCreatesUserInInvitingTenant() {
        // seed: invito admin per invitee-admin@acme.test (token grezzo documentato nel README del seed)
        given().contentType(ContentType.JSON)
                .body(Map.of("token", "seed-invite-acme-admin", "password", "Password1!", "displayName", "Invited Admin"))
                .when().post("/api/auth/invitations/accept")
                .then().statusCode(200).body("access_token", org.hamcrest.Matchers.notNullValue());

        assertEquals(1, scalar(
                // UC 0116: la persona (identità) + la sua appartenenza al tenant invitante.
                "select count(*) from platform.identity i"
                        + " join platform.membership m on m.identity_id = i.id"
                        + " where i.email = 'invitee-admin@acme.test'"
                        + " and m.tenant_id = '" + ACME + "' and m.role = 'admin'"),
                "appartenenza creata nel tenant Acme con ruolo admin");
        assertEquals("accepted", text(
                "select status from platform.invitations where token_hash = '"
                        + TokenHashes.sha256Hex("seed-invite-acme-admin") + "'"),
                "invito segnato accepted");
    }

    /**
     * UC 0116 — chi esiste già sulla piattaforma non entra da questo percorso, e il rifiuto è
     * <b>comprensibile</b> (409 con un messaggio) invece di essere una violazione di indice. Il modello
     * ammetterebbe l'appartenenza in più; è la password nuova che non si può applicare a un'identità
     * altrui. Il percorso completo — accettare dalla propria sessione — è di UC 0118.
     */
    @Test
    void acceptForAnAlreadyRegisteredAddressIsRefusedWithAMessage() {
        // 'owner@acme.test' è una persona del seme: esiste già come identità
        insertPendingInvite("ia-esistente-token", "owner@acme.test", "member");
        given().contentType(ContentType.JSON)
                .body(Map.of("token", "ia-esistente-token", "password", "Password1!"))
                .when().post("/api/auth/invitations/accept")
                .then().statusCode(409);
        // nessuna appartenenza fantasma creata nel tenant che invitava
        assertEquals(1, scalar(
                "select count(*) from platform.membership m"
                        + " join platform.identity i on i.id = m.identity_id"
                        + " where lower(i.email) = 'owner@acme.test' and m.deleted_at is null"),
                "l'appartenenza resta una: il rifiuto non lascia nulla dietro di sé");
    }

    // ── UC 0118: che cosa chiedere a chi apre il collegamento ────────────────

    @Test
    void lookupDiceSeServeUnaParolaDAccessoOSoloDiAutenticarsi() {
        // Esiste per non far compilare un modulo destinato al rifiuto: scoprire dopo aver premuto
        // «accetta» che la parola d'accesso non serviva è il modo peggiore di dirlo.
        insertPendingInvite("ia-lookup-nuovo", "ia-lookup-nuovo@acme.test", "member");
        given().contentType(ContentType.JSON).body(Map.of("token", "ia-lookup-nuovo"))
                .when().post("/api/auth/invitations/lookup")
                .then().statusCode(200)
                .body("mode", org.hamcrest.Matchers.is("register"))
                .body("email", org.hamcrest.Matchers.is("ia-lookup-nuovo@acme.test"));

        insertPendingInvite("ia-lookup-esistente", "bob@bob.test", "member");
        given().contentType(ContentType.JSON).body(Map.of("token", "ia-lookup-esistente"))
                .when().post("/api/auth/invitations/lookup")
                .then().statusCode(200)
                .body("mode", org.hamcrest.Matchers.is("signin"))
                .body("email", org.hamcrest.Matchers.is("bob@bob.test"));
    }

    @Test
    void lookupConTokenNonValidoOScadutoRispondeComeLAccettazione() {
        // Stesso codice e stesso messaggio dell'accettazione: non si distingue «invito inesistente» da
        // «indirizzo sconosciuto».
        given().contentType(ContentType.JSON).body(Map.of("token", "ia-lookup-inesistente"))
                .when().post("/api/auth/invitations/lookup").then().statusCode(400);
        insertExpiredInvite("ia-lookup-scaduto", "ia-lookup-scaduto@acme.test", "member");
        given().contentType(ContentType.JSON).body(Map.of("token", "ia-lookup-scaduto"))
                .when().post("/api/auth/invitations/lookup").then().statusCode(410);
    }

    /**
     * UC 0118 — l'indirizzo di un'identità <b>cancellata</b> non si riusa, e il rifiuto è un messaggio
     * e non un errore del servizio. L'unicità di {@code platform.identity} sull'indirizzo è
     * incondizionata (vale anche sulle righe cancellate): prima di questa storia il controllo di
     * esistenza le ignorava, quindi il caso passava il controllo e sbatteva contro l'indice unico.
     */
    @Test
    void indirizzoDiUnIdentitaCancellataNonSiRiusa() {
        exec("insert into platform.identity(id,cognito_sub,email,locale,status,created_at,updated_at,deleted_at)"
                + " values (gen_random_uuid(),'sub-0118-cancellata','riuso-0118@acme.test','en','active',"
                + "now(),now(),now())");

        // Iscrizione: messaggio comprensibile, IDENTICO a quello di un indirizzo vivo — non rivela
        // nulla in più di quanto già rivelasse.
        given().contentType(ContentType.JSON)
                .body(Map.of("email", "riuso-0118@acme.test", "password", "Password1!"))
                .when().post("/api/auth/signup").then().statusCode(409);

        // Accettazione di un invito allo stesso indirizzo: idem, e nessuna appartenenza creata.
        insertPendingInvite("ia-riuso-cancellata", "riuso-0118@acme.test", "member");
        given().contentType(ContentType.JSON)
                .body(Map.of("token", "ia-riuso-cancellata", "password", "Password1!"))
                .when().post("/api/auth/invitations/accept").then().statusCode(409);
        assertEquals(0, scalar(
                "select count(*) from platform.membership m join platform.identity i on i.id = m.identity_id"
                        + " where lower(i.email) = 'riuso-0118@acme.test'"),
                "nessuna appartenenza per un'identità cancellata");
    }

    @Test
    void invalidTokenIsRejected() {
        given().contentType(ContentType.JSON)
                .body(Map.of("token", "non-existent-token", "password", "Password1!"))
                .when().post("/api/auth/invitations/accept").then().statusCode(400);
    }

    @Test
    void expiredInviteIsGone() {
        insertExpiredInvite("ia-expired-token", "ia-expired@acme.test", "member");
        given().contentType(ContentType.JSON)
                .body(Map.of("token", "ia-expired-token", "password", "Password1!"))
                .when().post("/api/auth/invitations/accept").then().statusCode(410);
    }

    private void insertPendingInvite(String token, String email, String role) {
        insertInvite(token, email, role, "now() + interval '7 days'");
    }

    private void insertExpiredInvite(String token, String email, String role) {
        insertInvite(token, email, role, "now() - interval '1 day'");
    }

    private void insertInvite(String token, String email, String role, String expiresAt) {
        String sql = "insert into platform.invitations(id, tenant_id, email, role, token_hash, status, expires_at, "
                + "created_at, updated_at, created_by) values (?, ?, ?, ?, ?, 'pending', " + expiresAt + ", "
                + "now(), now(), 'test')";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, ACME);
            ps.setString(3, email);
            ps.setString(4, role);
            ps.setString(5, TokenHashes.sha256Hex(token));
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void exec(String sql) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long scalar(String sql) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String text(String sql) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
