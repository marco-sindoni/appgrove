package app.appgrove.core.gdpr;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import app.appgrove.core.TestData;
import app.appgrove.core.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Rettifica (art. 16) ed export del profilo utente (artt. 15/20) — UC 0033 §9: ogni ruolo agisce
 * sui <b>propri</b> dati ({@code sub} dal JWT, mai da input); l'export contiene profilo, account e
 * inviti indirizzati alla propria email, e scarica come allegato JSON.
 */
@QuarkusTest
class ProfileSelfServiceTest {

    private static final String ME = "/api/platform/v1/users/me";
    private static final String TENANT = "77777777-0000-0000-0000-0000000000c1";

    @Inject
    TestData data;

    private String memberToken;
    private String memberEmail;

    @BeforeEach
    void seed() {
        data.account(TENANT, "Tenant profilo");
        memberToken = TestTokens.withTenant(TENANT, "member");
        memberEmail = "member-" + TENANT + "@esempio.it";
        // l'utente del token: cognito_sub = subjectFor(tenant) (fixture idempotente)
        data.user(TENANT, TestTokens.subjectFor(TENANT), memberEmail, "member");
    }

    @Test
    void memberRectifiesOwnDisplayName() {
        given().header("Authorization", "Bearer " + memberToken)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "Nome Rettificato"))
                .when().patch(ME)
                .then().statusCode(200)
                .body("displayName", equalTo("Nome Rettificato"));

        // persiste: /me riflette la rettifica
        given().header("Authorization", "Bearer " + memberToken)
                .when().get(ME)
                .then().statusCode(200)
                .body("displayName", equalTo("Nome Rettificato"));

        // validazione: nome vuoto → 400
        given().header("Authorization", "Bearer " + memberToken)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "  "))
                .when().patch(ME)
                .then().statusCode(400);
    }

    @Test
    void profileExportDownloadsOwnDataOnly() {
        data.invitation(TENANT, memberEmail, "member");
        data.invitation(TENANT, "altra-persona@esempio.it", "admin");

        given().header("Authorization", "Bearer " + memberToken)
                .when().get(ME + "/export")
                .then().statusCode(200)
                .header("Content-Disposition", containsString("attachment"))
                .body("generatedAt", notNullValue())
                .body("profile.email", equalTo(memberEmail))
                .body("profile.cognitoSub", equalTo(TestTokens.subjectFor(TENANT)))
                .body("account.name", equalTo("Tenant profilo"))
                // solo gli inviti indirizzati alla PROPRIA email, non quelli altrui
                .body("invitations", hasSize(1))
                .body("invitations[0].email", equalTo(memberEmail));
    }

    @Test
    void profileEndpointsRequireExistingProfile() {
        // token valido di un tenant senza riga utente corrispondente → 404 (nessuna creazione implicita)
        String orphan = "77777777-0000-0000-0000-0000000000c2";
        data.account(orphan, "Tenant senza profilo");
        String token = TestTokens.withTenant(orphan, "member");
        given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("displayName", "X"))
                .when().patch(ME)
                .then().statusCode(404);
        given().header("Authorization", "Bearer " + token)
                .when().get(ME + "/export")
                .then().statusCode(404);
    }
}
