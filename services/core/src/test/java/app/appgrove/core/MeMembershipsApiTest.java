package app.appgrove.core;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Appartenenze della persona in sessione e cambio di account attivo (UC 0117).
 *
 * <p>Il collaudo che conta è {@link #cambioVersoUnAccountNonProprioNonTrovato()}: il corpo della
 * richiesta porta un account <b>candidato</b>, e un account a cui la persona non appartiene deve dare
 * «non trovato» — non «vietato», che rivelerebbe l'esistenza dell'account.
 */
@QuarkusTest
class MeMembershipsApiTest {

    private static final String MEMBERSHIPS = "/api/platform/v1/me/memberships";
    private static final String ACTIVE_ACCOUNT = "/api/platform/v1/me/active-account";

    // Account propri di questo collaudo (la creazione è idempotente sul nome: account condivisi
    // renderebbero questi test dipendenti dall'ordine in cui girano gli altri).
    private static final String TENANT_UNO = "dddddddd-0000-0000-0000-0000000000b1";
    private static final String TENANT_DUE = "eeeeeeee-0000-0000-0000-0000000000b2";
    private static final String TENANT_ESTRANEO = "cccccccc-0000-0000-0000-0000000000b3";

    @Inject
    TestData data;

    @Test
    void unaSolaAppartenenzaElencaUnAccountAttivo() {
        // Il caso di tutti gli utenti di oggi: un solo account, e nessun valore conservato serve.
        data.account(TENANT_UNO, "Uno Spa");
        String sub = "sub-mem-single";
        data.user(TENANT_UNO, sub, "single@memberships.test", "owner");

        given().header("Authorization", "Bearer " + TestTokens.withSubject(sub, TENANT_UNO, "owner"))
                .when().get(MEMBERSHIPS)
                .then().statusCode(200)
                .body("activeAccountId", is(TENANT_UNO))
                .body("memberships", hasSize(1))
                .body("memberships[0].accountName", is("Uno Spa"));
    }

    @Test
    void piuAppartenenzeElencanoTuttiGliAccountEQualeEAttivo() {
        data.account(TENANT_UNO, "Uno Spa");
        data.account(TENANT_DUE, "Due Srl");
        String sub = "sub-mem-multi";
        UUID identity = data.user(TENANT_UNO, sub, "multi@memberships.test", "owner");
        UUID secondMembership = data.membership(TENANT_DUE, identity, "member");
        data.setActiveMembership(identity, secondMembership);

        given().header("Authorization", "Bearer " + TestTokens.withSubject(sub, TENANT_UNO, "owner"))
                .when().get(MEMBERSHIPS)
                .then().statusCode(200)
                .body("activeAccountId", is(TENANT_DUE))
                .body("memberships.accountName", contains("Uno Spa", "Due Srl"));
    }

    @Test
    void piuAppartenenzeSenzaSceltaNonDichiaranoAlcunAccountAttivo() {
        // Stesso caso in cui il token nasce senza claim: l'interfaccia non deve inventare una scelta.
        data.account(TENANT_UNO, "Uno Spa");
        data.account(TENANT_DUE, "Due Srl");
        String sub = "sub-mem-nochoice";
        UUID identity = data.user(TENANT_UNO, sub, "nochoice@memberships.test", "owner");
        data.membership(TENANT_DUE, identity, "member");
        data.setActiveMembership(identity, null);

        given().header("Authorization", "Bearer " + TestTokens.withSubject(sub, TENANT_UNO, "owner"))
                .when().get(MEMBERSHIPS)
                .then().statusCode(200)
                .body("activeAccountId", is(nullValue()))
                .body("memberships", hasSize(2));
    }

    @Test
    void cambioAccountScriveLaSceltaELaTracciaDiControllo() {
        data.account(TENANT_UNO, "Uno Spa");
        data.account(TENANT_DUE, "Due Srl");
        String sub = "sub-mem-switch";
        UUID identity = data.user(TENANT_UNO, sub, "switch@memberships.test", "owner");
        UUID first = data.membership(TENANT_UNO, identity, "owner");
        UUID second = data.membership(TENANT_DUE, identity, "member");
        data.setActiveMembership(identity, first);
        int auditBefore = data.activeAccountAuditCount(identity);

        given().header("Authorization", "Bearer " + TestTokens.withSubject(sub, TENANT_UNO, "owner"))
                .contentType(ContentType.JSON).body(Map.of("accountId", TENANT_DUE))
                .when().post(ACTIVE_ACCOUNT)
                .then().statusCode(204);

        assertEquals(second, data.activeMembershipOf(identity), "la scelta è conservata sull'identità");
        assertEquals(auditBefore + 1, data.activeAccountAuditCount(identity), "una riga di registro per il cambio");
        assertEquals(TENANT_UNO + " -> " + TENANT_DUE, data.lastActiveAccountAudit(identity));
    }

    @Test
    void primaSceltaRegistrataComeProvenienteDaNessunAccount() {
        // `from_tenant_id` è l'account attivo PRIMA del cambio secondo il valore conservato, non
        // l'account del token: con più appartenenze e nessuna scelta valida non c'era alcun account
        // attivo, e il registro lo dice invece di inventarne uno.
        data.account(TENANT_UNO, "Uno Spa");
        data.account(TENANT_DUE, "Due Srl");
        String sub = "sub-mem-prima-scelta";
        UUID identity = data.user(TENANT_UNO, sub, "prima-scelta@memberships.test", "owner");
        data.membership(TENANT_DUE, identity, "member");
        data.setActiveMembership(identity, null);

        given().header("Authorization", "Bearer " + TestTokens.withSubject(sub, TENANT_UNO, "owner"))
                .contentType(ContentType.JSON).body(Map.of("accountId", TENANT_DUE))
                .when().post(ACTIVE_ACCOUNT)
                .then().statusCode(204);

        assertEquals("nessuno -> " + TENANT_DUE, data.lastActiveAccountAudit(identity));
    }

    @Test
    void cambioVersoLAccountSuCuiSiEGiaNonLasciaTracciaFalsa() {
        // Idempotenza: nessuna transizione, nessuna riga di registro. Una prova di cambio senza
        // cambio sarebbe una prova falsa.
        data.account(TENANT_UNO, "Uno Spa");
        data.account(TENANT_DUE, "Due Srl");
        String sub = "sub-mem-idem";
        UUID identity = data.user(TENANT_UNO, sub, "idem@memberships.test", "owner");
        UUID first = data.membership(TENANT_UNO, identity, "owner");
        data.membership(TENANT_DUE, identity, "member");
        data.setActiveMembership(identity, first);
        int auditBefore = data.activeAccountAuditCount(identity);

        given().header("Authorization", "Bearer " + TestTokens.withSubject(sub, TENANT_UNO, "owner"))
                .contentType(ContentType.JSON).body(Map.of("accountId", TENANT_UNO))
                .when().post(ACTIVE_ACCOUNT)
                .then().statusCode(204);

        assertEquals(auditBefore, data.activeAccountAuditCount(identity), "nessuna transizione, nessuna riga");
    }

    @Test
    void cambioVersoUnAccountNonProprioNonTrovato() {
        // 404 e non 403: un 403 direbbe «quell'account esiste, ma non è tuo», e l'esistenza di un
        // account non è un'informazione che appartiene a chi la chiede.
        data.account(TENANT_UNO, "Uno Spa");
        data.account(TENANT_ESTRANEO, "Estraneo Spa");
        String sub = "sub-mem-estraneo";
        UUID identity = data.user(TENANT_UNO, sub, "estraneo@memberships.test", "owner");

        given().header("Authorization", "Bearer " + TestTokens.withSubject(sub, TENANT_UNO, "owner"))
                .contentType(ContentType.JSON).body(Map.of("accountId", TENANT_ESTRANEO))
                .when().post(ACTIVE_ACCOUNT)
                .then().statusCode(404);

        assertNull(data.activeMembershipOf(identity), "nessuna scelta scritta per un account non proprio");
    }

    @Test
    void cambioVersoUnAppartenenzaRevocataNonTrovato() {
        // L'appartenenza chiusa mentre il menu era aperto: il candidato non è più valido.
        data.account(TENANT_UNO, "Uno Spa");
        data.account(TENANT_DUE, "Due Srl");
        String sub = "sub-mem-revocata";
        UUID identity = data.user(TENANT_UNO, sub, "revocata@memberships.test", "owner");
        data.membership(TENANT_DUE, identity, "member");
        data.closeMembership(TENANT_DUE, identity);

        given().header("Authorization", "Bearer " + TestTokens.withSubject(sub, TENANT_UNO, "owner"))
                .contentType(ContentType.JSON).body(Map.of("accountId", TENANT_DUE))
                .when().post(ACTIVE_ACCOUNT)
                .then().statusCode(404);
    }

    @Test
    void senzaTokenNessunaLettura() {
        given().when().get(MEMBERSHIPS).then().statusCode(401);
    }
}
