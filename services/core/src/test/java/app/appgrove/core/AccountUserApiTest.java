package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * API account/persone (UC 0013 + UC 0116): account corrente, isolamento per account, profilo
 * proprio, patch ruolo, uscita dall'account, e unicità dell'indirizzo spostata sull'<b>identità</b>
 * (non più «1 utente → 1 account», #02 14 superata dalla change 0088).
 */
@QuarkusTest
class AccountUserApiTest {

    private static final String ACCOUNTS = "/api/platform/v1/accounts";
    private static final String USERS = "/api/platform/v1/users";
    private static final String TENANT_A = "dddddddd-0000-0000-0000-000000000004";
    private static final String TENANT_B = "eeeeeeee-0000-0000-0000-000000000005";
    // Account propri dei collaudi «una persona, più appartenenze» (UC 0116): tenerli separati da
    // TENANT_A/TENANT_B evita che la creazione dell'account (idempotente sul nome) faccia dipendere
    // un collaudo dall'ordine in cui girano gli altri.
    private static final String TENANT_UNO = "dddddddd-0000-0000-0000-0000000000a1";
    private static final String TENANT_DUE = "eeeeeeee-0000-0000-0000-0000000000a2";

    @Inject
    TestData data;

    @Test
    void accountMeReturnsTenantAccount() {
        data.account(TENANT_A, "Acme Inc");
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().get(ACCOUNTS + "/me")
                .then().statusCode(200)
                .body("id", is(TENANT_A))
                .body("name", is("Acme Inc"));
    }

    @Test
    void usersAreIsolatedByTenant() {
        data.account(TENANT_A, "Acme");
        data.account(TENANT_B, "Borg");
        data.user(TENANT_A, "sub-iso-a", "iso-a@example.test", "member");
        data.user(TENANT_B, "sub-iso-b", "iso-b@example.test", "member");

        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().get(USERS + "?size=100")
                .then().statusCode(200)
                .body("content.email", hasItem("iso-a@example.test"))
                .body("content.email", not(hasItem("iso-b@example.test")))
                .body("content.tenantId", everyItem(is(TENANT_A)));
    }

    @Test
    void usersMeReturnsOwnProfile() {
        data.account(TENANT_A, "Acme");
        data.user(TENANT_A, TestTokens.subjectFor(TENANT_A), "me-a@example.test", "owner");
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().get(USERS + "/me")
                .then().statusCode(200)
                .body("email", is("me-a@example.test"))
                .body("tenantId", is(TENANT_A));
    }

    @Test
    void patchUserUpdatesRole() {
        data.account(TENANT_A, "Acme");
        UUID id = data.user(TENANT_A, "sub-patch", "patch@example.test", "member");
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .contentType(ContentType.JSON).body(Map.of("role", "admin"))
                .when().patch(USERS + "/" + id)
                .then().statusCode(200)
                .body("role", is("admin"));
    }

    @Test
    void deleteUserSoftDeletes() {
        data.account(TENANT_A, "Acme");
        UUID id = data.user(TENANT_A, "sub-del", "del@example.test", "member");
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().delete(USERS + "/" + id)
                .then().statusCode(204);
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_A, "owner"))
                .when().get(USERS + "?size=100")
                .then().body("content.email", not(hasItem("del@example.test")));
    }

    /**
     * L'indirizzo resta unico globalmente, ma sull'<b>identità</b>: due persone diverse non possono
     * avere lo stesso indirizzo. È l'unicità che serve; quella che è caduta è «la stessa persona non
     * può stare in due account».
     */
    @Test
    void emailIsGloballyUniqueOnTheIdentity() {
        data.userStrict(TENANT_A, "sub-uq-a", "dup@example.test", "member");
        assertThrows(RuntimeException.class,
                () -> data.userStrict(TENANT_B, "sub-uq-b", "dup@example.test", "member"));
    }

    /**
     * Il punto della storia: la stessa persona è owner del proprio account e member di un altro.
     * Ogni account la vede come propria persona, col ruolo che le ha dato lui — e nessuno dei due
     * vede l'altro.
     */
    @Test
    void sameIdentityBelongsToTwoAccounts() {
        data.account(TENANT_UNO, "Conto uno");
        data.account(TENANT_DUE, "Conto due");
        UUID person = data.identity("sub-due-conti", "due-conti@example.test", "Due Conti");
        data.membership(TENANT_UNO, person, "member");
        data.membership(TENANT_DUE, person, "owner");

        assertEquals(List.of(TENANT_UNO, TENANT_DUE), data.tenantsOf(person),
                "due appartenenze vive per la stessa identità");
        assertEquals(1, data.identityCount("due-conti@example.test"), "una sola identità, non due");

        // A la vede come member, e vede SOLO il proprio account nella riga
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_UNO, "owner"))
                .when().get(USERS + "/" + person)
                .then().statusCode(200)
                .body("role", is("member"))
                .body("tenantId", is(TENANT_UNO));

        // B la vede come owner
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_DUE, "owner"))
                .when().get(USERS + "/" + person)
                .then().statusCode(200)
                .body("role", is("owner"))
                .body("tenantId", is(TENANT_DUE));
    }

    /** La seconda appartenenza viva allo stesso account è rifiutata dal VINCOLO, non dall'interfaccia. */
    @Test
    void secondMembershipInTheSameAccountIsRejected() {
        data.account(TENANT_UNO, "Conto uno");
        UUID person = data.identity("sub-doppio", "doppio@example.test", "Doppio");
        data.membershipStrict(TENANT_UNO, person, "member");
        assertThrows(RuntimeException.class, () -> data.membershipStrict(TENANT_UNO, person, "admin"));
    }

    /**
     * Uscire da un account chiude l'appartenenza, <b>non</b> l'identità: l'altra appartenenza resta
     * intatta e la persona continua a esistere. È l'edge che la storia chiede di provare.
     */
    @Test
    void leavingOneAccountLeavesTheOtherMembershipIntact() {
        data.account(TENANT_UNO, "Conto uno");
        data.account(TENANT_DUE, "Conto due");
        UUID person = data.identity("sub-uscita", "uscita@example.test", "Uscita");
        data.membership(TENANT_UNO, person, "member");
        data.membership(TENANT_DUE, person, "owner");

        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_UNO, "owner"))
                .when().delete(USERS + "/" + person)
                .then().statusCode(204);

        assertEquals(List.of(TENANT_DUE), data.tenantsOf(person), "resta solo l'appartenenza al conto due");
        assertEquals(1, data.identityCount("uscita@example.test"), "l'identità sopravvive all'uscita");

        // A non la vede più; B sì, e non si è accorto di nulla
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_UNO, "owner"))
                .when().get(USERS + "/" + person).then().statusCode(404);
        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_DUE, "owner"))
                .when().get(USERS + "/" + person).then().statusCode(200).body("role", is("owner"));
    }

    /**
     * La sospensione decisa da un owner vale nel <b>suo</b> account e non altrove: è la leva
     * dell'appartenenza, non quella della persona (che è la limitazione del trattamento, art. 18).
     */
    @Test
    void suspendingInOneAccountDoesNotTouchTheOther() {
        data.account(TENANT_UNO, "Conto uno");
        data.account(TENANT_DUE, "Conto due");
        UUID person = data.identity("sub-sosp", "sosp@example.test", "Sospesa");
        data.membership(TENANT_UNO, person, "member");
        data.membership(TENANT_DUE, person, "member");

        given().header("Authorization", "Bearer " + TestTokens.withTenant(TENANT_UNO, "owner"))
                .contentType(ContentType.JSON).body(Map.of("status", "suspended"))
                .when().patch(USERS + "/" + person)
                .then().statusCode(200).body("status", is("suspended"));

        assertEquals("suspended", data.membershipStatus(TENANT_UNO, person));
        assertEquals("active", data.membershipStatus(TENANT_DUE, person), "l'altro account non è toccato");
        assertEquals("active", data.userStatus(person), "la persona non è sospesa sulla piattaforma");
    }
}
