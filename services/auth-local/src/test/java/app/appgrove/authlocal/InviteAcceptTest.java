package app.appgrove.authlocal;

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

/** Accept invito (UC 0058): l'utente entra nel tenant invitante col ruolo; token scaduto/invalido respinto. */
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
                "select count(*) from platform.users where email = 'invitee-admin@acme.test' "
                        + "and tenant_id = '" + ACME + "' and role = 'admin'"),
                "utente creato nel tenant Acme con ruolo admin");
        assertEquals("accepted", text(
                "select status from platform.invitations where token_hash = '"
                        + TokenHashes.sha256Hex("seed-invite-acme-admin") + "'"),
                "invito segnato accepted");
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

    private void insertExpiredInvite(String token, String email, String role) {
        String sql = "insert into platform.invitations(id, tenant_id, email, role, token_hash, status, expires_at, "
                + "created_at, updated_at, created_by) values (?, ?, ?, ?, ?, 'pending', now() - interval '1 day', "
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
