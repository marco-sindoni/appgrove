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
                "select count(*) from platform.users where cognito_sub = '" + sub
                        + "' and email = '" + email + "' and role = 'owner'"),
                "utente owner creato nello schema platform col sub Cognito");
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
