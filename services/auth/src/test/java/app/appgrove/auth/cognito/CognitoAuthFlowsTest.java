package app.appgrove.auth.cognito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.appgrove.auth.TestSchema;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.http.Cookie;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RevokeTokenRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.RevokeTokenResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;

/**
 * Provider Cognito (UC 0015) — contratto HTTP identico al provider locale: login (+challenge 2FA),
 * refresh con rotazione del cookie, logout con revoca, signup→platform, verify/reset con token
 * opaco base64url(email|codice). Client Cognito mockato (#10: fixture deterministiche offline).
 */
@QuarkusTest
@TestProfile(CognitoTestProfile.class)
class CognitoAuthFlowsTest {

    private static final String EMAIL = "cog-user@acme.test";
    private static final String SUB = "cognito-sub-0001";

    @Inject
    AgroalDataSource ds;

    @InjectMock
    CognitoIdentityProviderClient cognito;

    @BeforeEach
    void setup() {
        TestSchema.ensure(ds);
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void loginReturnsTokensAndHardenedRefreshCookie() {
        AtomicReference<InitiateAuthRequest> seen = stubInitiateAuth(authResult(SUB, "rt-1"));

        Response r = login(EMAIL, "Password1!");
        r.then().statusCode(200)
                .body("token_type", is("Bearer"))
                .body("access_token", notNullValue())
                .body("id_token", notNullValue())
                .body("expires_in", is(900))
                // il refresh token NON deve mai comparire nel body (#02 3)
                .body("refresh_token", nullValue())
                .body("refresh_value", nullValue());

        InitiateAuthRequest req = seen.get();
        assertEquals(CognitoTestProfile.CLIENT_ID, req.clientId());
        assertEquals(EMAIL, req.authParameters().get("USERNAME"));
        assertEquals(CognitoStubs.expectedSecretHash(EMAIL), req.authParameters().get("SECRET_HASH"));

        // cookie host-only: HttpOnly + Secure + SameSite=Lax + Path=/api/auth (#02 17)
        Cookie cookie = r.getDetailedCookie("appgrove_refresh");
        assertTrue(cookie.isHttpOnly(), "HttpOnly");
        assertTrue(cookie.isSecured(), "Secure");
        assertEquals("/api/auth", cookie.getPath());
        assertTrue(r.header("Set-Cookie").contains("SameSite=Lax"), "SameSite=Lax");
        assertEquals(CognitoStubs.opaque(SUB, "rt-1"), cookie.getValue(), "cookie = base64url(sub|refresh)");
    }

    @Test
    void wrongCredentialsAreUnauthorized() {
        when(cognito.initiateAuth(anyConsumer(InitiateAuthRequest.Builder.class)))
                .thenThrow(NotAuthorizedException.builder().message("Incorrect username or password.").build());
        login(EMAIL, "sbagliata").then().statusCode(401);
    }

    @Test
    void unconfirmedUserIsForbidden() {
        when(cognito.initiateAuth(anyConsumer(InitiateAuthRequest.Builder.class)))
                .thenThrow(UserNotConfirmedException.builder().message("User is not confirmed.").build());
        login(EMAIL, "Password1!").then().statusCode(403);
    }

    @Test
    void totpChallengeRoundTrip() {
        when(cognito.initiateAuth(anyConsumer(InitiateAuthRequest.Builder.class)))
                .thenAnswer(inv -> InitiateAuthResponse.builder()
                        .challengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA)
                        .session("sessione-cognito-1")
                        .build());
        String challengeToken = login(EMAIL, "Password1!")
                .then().statusCode(200)
                .body("mfa_required", is(true))
                .extract().path("challenge_token");

        AtomicReference<RespondToAuthChallengeRequest> seen = new AtomicReference<>();
        when(cognito.respondToAuthChallenge(anyConsumer(RespondToAuthChallengeRequest.Builder.class)))
                .thenAnswer(inv -> {
                    RespondToAuthChallengeRequest.Builder b = RespondToAuthChallengeRequest.builder();
                    inv.<Consumer<RespondToAuthChallengeRequest.Builder>>getArgument(0).accept(b);
                    seen.set(b.build());
                    return RespondToAuthChallengeResponse.builder()
                            .authenticationResult(authResult(SUB, "rt-mfa"))
                            .build();
                });

        given().contentType(ContentType.JSON)
                .body(Map.of("challenge_token", challengeToken, "code", "123456"))
                .when().post("/api/auth/login/2fa")
                .then().statusCode(200).body("access_token", notNullValue());

        RespondToAuthChallengeRequest req = seen.get();
        assertEquals("sessione-cognito-1", req.session());
        assertEquals(EMAIL, req.challengeResponses().get("USERNAME"));
        assertEquals("123456", req.challengeResponses().get("SOFTWARE_TOKEN_MFA_CODE"));
    }

    /**
     * UC 0118 — sfida di scelta dell'account sul fornitore <b>Cognito</b>, cioè la parità con il
     * fornitore locale.
     *
     * <p>Qui il caso si riconosce dall'<b>assenza del claim {@code tenant_id}</b> nell'access token
     * appena emesso: la funzione che compone il token non solleva eccezioni quando non riesce a
     * scegliere, quindi l'accesso riesce e il token esce senza claim. Il token della sfida è la coppia
     * (sub, refresh token), e la sessione con il claim nuovo si ottiene <b>rinnovando</b> dopo aver
     * conservato la scelta — nessun token da inventare, nessuna password da ricordare.
     */
    @Test
    void piuAppartenenzeSenzaScelta_sfidaDiSceltaPoiSessioneConIlClaim() {
        String sub = "cognito-sub-0118";
        String primo = "a0000000-0118-4000-8000-000000000001";
        String secondo = "a0000000-0118-4000-8000-000000000002";
        creaPersonaConDueAppartenenze(sub, "cog-0118@acme.test", primo, secondo);

        stubInitiateAuth(AuthenticationResultType.builder()
                .accessToken(CognitoStubs.accessTokenWithSub(sub)) // nessun tenant_id: non ha scelto
                .idToken("id-token-0118")
                .expiresIn(900)
                .refreshToken("rt-0118")
                .build());

        Response challenge = login("cog-0118@acme.test", "Password1!");
        challenge.then().statusCode(200)
                .body("account_selection_required", is(true))
                .body("accounts.account_id", org.hamcrest.Matchers.hasItems(primo, secondo))
                .body("access_token", nullValue());
        String choiceToken = challenge.path("choice_token");

        // Il rinnovo dopo la scelta: qui il token nasce CON il claim, perché la funzione del token
        // rilegge la scelta appena conservata.
        when(cognito.initiateAuth(anyConsumer(InitiateAuthRequest.Builder.class)))
                .thenAnswer(inv -> InitiateAuthResponse.builder()
                        .authenticationResult(AuthenticationResultType.builder()
                                .accessToken(CognitoStubs.accessTokenWithTenant(sub, secondo))
                                .idToken("id-token-0118b")
                                .expiresIn(900)
                                .refreshToken("rt-0118-ruotato")
                                .build())
                        .build());

        given().contentType(ContentType.JSON)
                .body(Map.of("choice_token", choiceToken, "account_id", secondo))
                .when().post("/api/auth/login/account")
                .then().statusCode(200).body("access_token", notNullValue());

        // La scelta è conservata lato server: è da lì che il token la rileggerà.
        assertEquals(secondo, TestDb.text(ds,
                "select m.tenant_id from platform.identity i"
                        + " join platform.membership m on m.id = i.active_membership_id"
                        + " where i.cognito_sub = '" + sub + "'"));
    }

    @Test
    void sceltaDiUnAccountCheNonEProprioRifiutataAncheSuCognito() {
        String sub = "cognito-sub-0118b";
        String primo = "a0000000-0118-4000-8000-000000000003";
        String secondo = "a0000000-0118-4000-8000-000000000004";
        creaPersonaConDueAppartenenze(sub, "cog-0118b@acme.test", primo, secondo);
        given().contentType(ContentType.JSON)
                .body(Map.of(
                        "choice_token", CognitoStubs.opaque(sub, "rt-x"),
                        "account_id", "a0000000-0118-4000-8000-00000000ffff"))
                .when().post("/api/auth/login/account")
                .then().statusCode(404);
    }

    private void creaPersonaConDueAppartenenze(String sub, String email, String primo, String secondo) {
        exec("insert into platform.accounts(id,name,status,created_at,updated_at,created_by)"
                + " values ('" + primo + "','Primo 0118','active',now(),now(),'test')"
                + " on conflict (id) do nothing");
        exec("insert into platform.accounts(id,name,status,created_at,updated_at,created_by)"
                + " values ('" + secondo + "','Secondo 0118','active',now(),now(),'test')"
                + " on conflict (id) do nothing");
        exec("insert into platform.identity(id,cognito_sub,email,locale,status,created_at,updated_at)"
                + " values (gen_random_uuid(),'" + sub + "','" + email + "','en','active',now(),now())"
                + " on conflict do nothing");
        for (String tenant : new String[] {primo, secondo}) {
            exec("insert into platform.membership(id,tenant_id,identity_id,role,status,created_at,updated_at)"
                    + " select gen_random_uuid(),'" + tenant + "', i.id,'owner','active',now(),now()"
                    + " from platform.identity i where i.cognito_sub = '" + sub + "'"
                    + " on conflict do nothing");
        }
    }

    private void exec(String sql) {
        try (var c = ds.getConnection(); var ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── refresh / logout ─────────────────────────────────────────────────────

    @Test
    void refreshRotatesCookieAndUsesSubForSecretHash() {
        AtomicReference<InitiateAuthRequest> seen = stubInitiateAuth(
                AuthenticationResultType.builder()
                        .accessToken(CognitoStubs.accessTokenWithSub(SUB))
                        .idToken("id-token-2")
                        .expiresIn(900)
                        .refreshToken("rt-ruotato")
                        .build());

        Response r = given().cookie("appgrove_refresh", CognitoStubs.opaque(SUB, "rt-vecchio"))
                .when().post("/api/auth/refresh").thenReturn();
        r.then().statusCode(200).body("access_token", notNullValue());

        InitiateAuthRequest req = seen.get();
        assertEquals("rt-vecchio", req.authParameters().get("REFRESH_TOKEN"));
        assertEquals(CognitoStubs.expectedSecretHash(SUB), req.authParameters().get("SECRET_HASH"),
                "SECRET_HASH del refresh calcolato col sub dal cookie");
        assertEquals(CognitoStubs.opaque(SUB, "rt-ruotato"), r.getCookie("appgrove_refresh"),
                "cookie ruotato col nuovo refresh token");
    }

    @Test
    void invalidRefreshIsUnauthorizedFailClosed() {
        when(cognito.initiateAuth(anyConsumer(InitiateAuthRequest.Builder.class)))
                .thenThrow(NotAuthorizedException.builder().message("Refresh Token has been revoked").build());
        given().cookie("appgrove_refresh", CognitoStubs.opaque(SUB, "rt-revocato"))
                .when().post("/api/auth/refresh").then().statusCode(401);
    }

    @Test
    void logoutRevokesTokenAndClearsCookie() {
        AtomicReference<RevokeTokenRequest> seen = new AtomicReference<>();
        when(cognito.revokeToken(anyConsumer(RevokeTokenRequest.Builder.class))).thenAnswer(inv -> {
            RevokeTokenRequest.Builder b = RevokeTokenRequest.builder();
            inv.<Consumer<RevokeTokenRequest.Builder>>getArgument(0).accept(b);
            seen.set(b.build());
            return RevokeTokenResponse.builder().build();
        });

        Response r = given().cookie("appgrove_refresh", CognitoStubs.opaque(SUB, "rt-da-revocare"))
                .when().post("/api/auth/logout").thenReturn();
        r.then().statusCode(204);
        assertEquals("rt-da-revocare", seen.get().token());
        assertEquals("", r.getCookie("appgrove_refresh"), "cookie cancellato");
    }

    // ── signup / verify ──────────────────────────────────────────────────────

    @Test
    void signupCreatesCognitoUserAndPlatformAccount() {
        String email = "cog-signup@nuovo.test";
        String sub = "cognito-sub-signup";
        AtomicReference<SignUpRequest> seen = new AtomicReference<>();
        when(cognito.signUp(anyConsumer(SignUpRequest.Builder.class))).thenAnswer(inv -> {
            SignUpRequest.Builder b = SignUpRequest.builder();
            inv.<Consumer<SignUpRequest.Builder>>getArgument(0).accept(b);
            seen.set(b.build());
            return SignUpResponse.builder().userSub(sub).build();
        });

        given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", "Password1!", "displayName", "Nuovo Utente"))
                .when().post("/api/auth/signup")
                .then().statusCode(201).body("status", is("verification_required"));

        assertEquals(email, seen.get().username());
        assertEquals(1, TestDb.count(ds,
                // UC 0116: la registrazione crea identità (la persona) + appartenenza owner.
                "select count(*) from platform.identity i"
                        + " join platform.membership m on m.identity_id = i.id"
                        + " where i.cognito_sub = '" + sub
                        + "' and i.email = '" + email + "' and m.role = 'owner'"),
                "persona e appartenenza owner create nello schema platform col sub Cognito");
    }

    @Test
    void verifyConfirmsWithoutAutoLogin() {
        AtomicReference<ConfirmSignUpRequest> seen = new AtomicReference<>();
        when(cognito.confirmSignUp(anyConsumer(ConfirmSignUpRequest.Builder.class))).thenAnswer(inv -> {
            ConfirmSignUpRequest.Builder b = ConfirmSignUpRequest.builder();
            inv.<Consumer<ConfirmSignUpRequest.Builder>>getArgument(0).accept(b);
            seen.set(b.build());
            return ConfirmSignUpResponse.builder().build();
        });

        Response r = given().contentType(ContentType.JSON)
                .body(Map.of("token", CognitoStubs.opaque(EMAIL, "123456")))
                .when().post("/api/auth/verify").thenReturn();
        r.then().statusCode(200)
                .body("status", is("confirmed"))
                .body("access_token", nullValue()); // niente auto-login: Cognito non emette token qui

        assertEquals(EMAIL, seen.get().username());
        assertEquals("123456", seen.get().confirmationCode());
    }

    @Test
    void verifyWithBadCodeIsBadRequest() {
        when(cognito.confirmSignUp(anyConsumer(ConfirmSignUpRequest.Builder.class)))
                .thenThrow(CodeMismatchException.builder().message("Invalid code").build());
        given().contentType(ContentType.JSON)
                .body(Map.of("token", CognitoStubs.opaque(EMAIL, "000000")))
                .when().post("/api/auth/verify").then().statusCode(400);
    }

    @Test
    void malformedOpaqueTokenIsBadRequest() {
        given().contentType(ContentType.JSON)
                .body(Map.of("token", "non-base64url-senza-separatore"))
                .when().post("/api/auth/verify").then().statusCode(400);
    }

    // ── reset password ───────────────────────────────────────────────────────

    @Test
    void forgotAndResetPasswordFlow() {
        when(cognito.forgotPassword(anyConsumer(ForgotPasswordRequest.Builder.class)))
                .thenAnswer(inv -> ForgotPasswordResponse.builder().build());
        given().contentType(ContentType.JSON)
                .body(Map.of("email", EMAIL))
                .when().post("/api/auth/password/forgot").then().statusCode(202);

        AtomicReference<ConfirmForgotPasswordRequest> seen = new AtomicReference<>();
        when(cognito.confirmForgotPassword(anyConsumer(ConfirmForgotPasswordRequest.Builder.class)))
                .thenAnswer(inv -> {
                    ConfirmForgotPasswordRequest.Builder b = ConfirmForgotPasswordRequest.builder();
                    inv.<Consumer<ConfirmForgotPasswordRequest.Builder>>getArgument(0).accept(b);
                    seen.set(b.build());
                    return ConfirmForgotPasswordResponse.builder().build();
                });
        given().contentType(ContentType.JSON)
                .body(Map.of("token", CognitoStubs.opaque(EMAIL, "654321"), "password", "NuovaPassword1"))
                .when().post("/api/auth/password/reset").then().statusCode(204);

        assertEquals(EMAIL, seen.get().username());
        assertEquals("654321", seen.get().confirmationCode());
        assertEquals("NuovaPassword1", seen.get().password());
    }

    // ── jwks ─────────────────────────────────────────────────────────────────

    @Test
    void jwksIsNotExposedInCloud() {
        given().when().get("/api/auth/jwks").then().statusCode(404);
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private AtomicReference<InitiateAuthRequest> stubInitiateAuth(AuthenticationResultType result) {
        AtomicReference<InitiateAuthRequest> seen = new AtomicReference<>();
        when(cognito.initiateAuth(anyConsumer(InitiateAuthRequest.Builder.class))).thenAnswer(inv -> {
            InitiateAuthRequest.Builder b = InitiateAuthRequest.builder();
            inv.<Consumer<InitiateAuthRequest.Builder>>getArgument(0).accept(b);
            seen.set(b.build());
            return InitiateAuthResponse.builder().authenticationResult(result).build();
        });
        return seen;
    }

    private static AuthenticationResultType authResult(String sub, String refreshToken) {
        return AuthenticationResultType.builder()
                .accessToken(CognitoStubs.accessTokenWithSub(sub))
                .idToken("id-token-1")
                .expiresIn(900)
                .refreshToken(refreshToken)
                .build();
    }

    private static Response login(String email, String password) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when().post("/api/auth/login")
                .thenReturn();
    }

    @SuppressWarnings("unchecked")
    private static <B> Consumer<B> anyConsumer(Class<B> builderType) {
        return (Consumer<B>) any(Consumer.class);
    }
}
