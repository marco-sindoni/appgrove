package app.appgrove.auth.cognito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import app.appgrove.auth.TestSchema;
import io.agroal.api.AgroalDataSource;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse;

/**
 * Collegamento delle email di Cognito (UC 0018), lato backend.
 *
 * <p>Due cose che, se regredissero, romperebbero le email in cloud <b>senza rompere nulla in
 * locale</b> — e quindi passerebbero inosservate fino all'accensione:
 * <ol>
 *   <li>gli endpoint accettano la forma <b>indirizzo + codice</b> del collegamento, oltre al token
 *       unico: il Custom Message Lambda non può produrre un token unico, perché quando compone il
 *       messaggio il codice non esiste ancora;
 *   <li>la <b>lingua</b> viaggia verso la Lambda come parametro della chiamata Cognito
 *       ({@code ClientMetadata}) — è l'unico canale, dato che la Lambda non legge il database.
 * </ol>
 */
@QuarkusTest
@TestProfile(CognitoTestProfile.class)
class CognitoEmailLinkTest {

    private static final String EMAIL = "cog-user@acme.test";

    @Inject
    AgroalDataSource ds;

    @InjectMock
    CognitoIdentityProviderClient cognito;

    @BeforeEach
    void setup() {
        TestSchema.ensure(ds);
    }

    // ── forma indirizzo + codice ─────────────────────────────────────────────

    @Test
    void verifyAcceptsEmailAndCodeInsteadOfToken() {
        AtomicReference<ConfirmSignUpRequest> seen = new AtomicReference<>();
        when(cognito.confirmSignUp(CognitoAuthFlowsTestHelpers.<ConfirmSignUpRequest.Builder>anyConsumer()))
                .thenAnswer(inv -> {
                    ConfirmSignUpRequest.Builder b = ConfirmSignUpRequest.builder();
                    inv.<Consumer<ConfirmSignUpRequest.Builder>>getArgument(0).accept(b);
                    seen.set(b.build());
                    return ConfirmSignUpResponse.builder().build();
                });

        given().contentType(ContentType.JSON)
                .body(Map.of("email", EMAIL, "code", "123456"))
                .when().post("/api/auth/verify")
                .then().statusCode(200).body("status", is("confirmed"));

        // Il backend ha ricomposto il token: al provider arrivano indirizzo e codice separati.
        assertEquals(EMAIL, seen.get().username());
        assertEquals("123456", seen.get().confirmationCode());
    }

    @Test
    void resetAcceptsEmailAndCodeInsteadOfToken() {
        AtomicReference<ConfirmForgotPasswordRequest> seen = new AtomicReference<>();
        when(cognito.confirmForgotPassword(
                CognitoAuthFlowsTestHelpers.<ConfirmForgotPasswordRequest.Builder>anyConsumer()))
                .thenAnswer(inv -> {
                    ConfirmForgotPasswordRequest.Builder b = ConfirmForgotPasswordRequest.builder();
                    inv.<Consumer<ConfirmForgotPasswordRequest.Builder>>getArgument(0).accept(b);
                    seen.set(b.build());
                    return ConfirmForgotPasswordResponse.builder().build();
                });

        given().contentType(ContentType.JSON)
                .body(Map.of("email", EMAIL, "code", "654321", "password", "NuovaPassword1"))
                .when().post("/api/auth/password/reset").then().statusCode(204);

        assertEquals(EMAIL, seen.get().username());
        assertEquals("654321", seen.get().confirmationCode());
        assertEquals("NuovaPassword1", seen.get().password());
    }

    @Test
    void neitherFormIsBadRequest() {
        // Né token né la coppia: 400, con lo stesso messaggio delle altre condizioni di token non
        // valido (non distinguere "forma sbagliata" da "codice sbagliato" non dà indizi a chi tenta).
        given().contentType(ContentType.JSON)
                .body(Map.of("email", EMAIL))
                .when().post("/api/auth/verify").then().statusCode(400);
    }

    // ── lingua verso la Lambda ───────────────────────────────────────────────

    @Test
    void signupPassesTheRequestedLanguageAsClientMetadata() {
        AtomicReference<SignUpRequest> seen = new AtomicReference<>();
        when(cognito.signUp(CognitoAuthFlowsTestHelpers.<SignUpRequest.Builder>anyConsumer()))
                .thenAnswer(inv -> {
                    SignUpRequest.Builder b = SignUpRequest.builder();
                    inv.<Consumer<SignUpRequest.Builder>>getArgument(0).accept(b);
                    seen.set(b.build());
                    return SignUpResponse.builder().userSub("cognito-sub-loc").build();
                });

        given().contentType(ContentType.JSON)
                .body(Map.of("email", "cog-loc@nuovo.test", "password", "Password1!", "locale", "it"))
                .when().post("/api/auth/signup").then().statusCode(201);

        // Deve viaggiare QUI: Cognito manda l'email durante questa chiamata, quando la riga in
        // platform.users non esiste ancora e nessuno potrebbe leggerne la lingua.
        assertEquals("it", seen.get().clientMetadata().get("locale"));
    }

    @Test
    void forgotPasswordPassesTheStoredLanguageAsClientMetadata() {
        // owner@acme.test viene dal seed: nessuna lingua espressa → inglese.
        AtomicReference<ForgotPasswordRequest> seen = new AtomicReference<>();
        when(cognito.forgotPassword(CognitoAuthFlowsTestHelpers.<ForgotPasswordRequest.Builder>anyConsumer()))
                .thenAnswer(inv -> {
                    ForgotPasswordRequest.Builder b = ForgotPasswordRequest.builder();
                    inv.<Consumer<ForgotPasswordRequest.Builder>>getArgument(0).accept(b);
                    seen.set(b.build());
                    return ForgotPasswordResponse.builder().build();
                });

        given().contentType(ContentType.JSON)
                .body(Map.of("email", "owner@acme.test"))
                .when().post("/api/auth/password/forgot").then().statusCode(202);

        assertEquals("en", seen.get().clientMetadata().get("locale"));
    }

    @Test
    void unknownAddressStillCarriesALanguage() {
        // Indirizzo mai visto: la risposta resta neutra (anti-enumeration) e la lingua ripiega su EN
        // invece di lasciare il parametro assente.
        AtomicReference<ForgotPasswordRequest> seen = new AtomicReference<>();
        when(cognito.forgotPassword(CognitoAuthFlowsTestHelpers.<ForgotPasswordRequest.Builder>anyConsumer()))
                .thenAnswer(inv -> {
                    ForgotPasswordRequest.Builder b = ForgotPasswordRequest.builder();
                    inv.<Consumer<ForgotPasswordRequest.Builder>>getArgument(0).accept(b);
                    seen.set(b.build());
                    return ForgotPasswordResponse.builder().build();
                });

        given().contentType(ContentType.JSON)
                .body(Map.of("email", "mai-visto@nessuno.test"))
                .when().post("/api/auth/password/forgot").then().statusCode(202);

        assertEquals("en", seen.get().clientMetadata().get("locale"));
    }
}
