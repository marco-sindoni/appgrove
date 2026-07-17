package app.appgrove.auth.cognito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.appgrove.auth.TestSchema;
import app.appgrove.auth.TokenHashes;
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
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserType;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

/**
 * Inviti col provider Cognito (UC 0015): accept = utente Cognito già confermato (nessuna email
 * Cognito) + utente nel tenant invitante + invito accepted + auto-login; send = email via SES.
 */
@QuarkusTest
@TestProfile(CognitoTestProfile.class)
class CognitoInvitationsTest {

    private static final String ACME = "a0000000-0000-4000-8000-000000000001";
    private static final String SUB = "cognito-sub-invitato";

    @Inject
    AgroalDataSource ds;

    @InjectMock
    CognitoIdentityProviderClient cognito;

    @InjectMock
    SesV2Client ses;

    @BeforeEach
    void setup() {
        TestSchema.ensure(ds);
    }

    @Test
    void acceptInviteCreatesConfirmedUserInInvitingTenant() {
        String email = "invitee-admin@acme.test";
        AtomicReference<AdminCreateUserRequest> createSeen = new AtomicReference<>();
        when(cognito.adminCreateUser(CognitoAuthFlowsTestHelpers.<AdminCreateUserRequest.Builder>anyConsumer()))
                .thenAnswer(inv -> {
                    AdminCreateUserRequest.Builder b = AdminCreateUserRequest.builder();
                    inv.<Consumer<AdminCreateUserRequest.Builder>>getArgument(0).accept(b);
                    createSeen.set(b.build());
                    return AdminCreateUserResponse.builder()
                            .user(UserType.builder()
                                    .username("uuid-cognito-username")
                                    .attributes(AttributeType.builder().name("sub").value(SUB).build())
                                    .build())
                            .build();
                });
        AtomicReference<AdminSetUserPasswordRequest> pwdSeen = new AtomicReference<>();
        when(cognito.adminSetUserPassword(
                CognitoAuthFlowsTestHelpers.<AdminSetUserPasswordRequest.Builder>anyConsumer()))
                .thenAnswer(inv -> {
                    AdminSetUserPasswordRequest.Builder b = AdminSetUserPasswordRequest.builder();
                    inv.<Consumer<AdminSetUserPasswordRequest.Builder>>getArgument(0).accept(b);
                    pwdSeen.set(b.build());
                    return AdminSetUserPasswordResponse.builder().build();
                });
        when(cognito.initiateAuth(
                CognitoAuthFlowsTestHelpers
                        .<software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest.Builder>
                                anyConsumer()))
                .thenAnswer(inv -> InitiateAuthResponse.builder()
                        .authenticationResult(AuthenticationResultType.builder()
                                .accessToken(CognitoStubs.accessTokenWithSub(SUB))
                                .idToken("id-token-inv")
                                .expiresIn(900)
                                .refreshToken("rt-invitato")
                                .build())
                        .build());

        given().contentType(ContentType.JSON)
                .body(Map.of("token", "seed-invite-acme-admin", "password", "Password1!",
                        "displayName", "Invited Admin"))
                .when().post("/api/auth/invitations/accept")
                .then().statusCode(200).body("access_token", notNullValue());

        assertEquals(MessageActionType.SUPPRESS, createSeen.get().messageAction(),
                "nessuna email Cognito: il link d'invito prova già l'email");
        assertTrue(pwdSeen.get().permanent(), "password permanente (utente confermato)");
        assertEquals(1, TestDb.count(ds,
                "select count(*) from platform.users where cognito_sub = '" + SUB
                        + "' and tenant_id = '" + ACME + "' and role = 'admin'"),
                "utente creato nel tenant Acme col sub Cognito e ruolo dell'invito");
        assertEquals("accepted", TestDb.text(ds,
                "select status from platform.invitations where token_hash = '"
                        + TokenHashes.sha256Hex("seed-invite-acme-admin") + "'"));
    }

    @Test
    void sendInviteGoesThroughSes() {
        AtomicReference<SendEmailRequest> seen = new AtomicReference<>();
        when(ses.sendEmail(CognitoAuthFlowsTestHelpers.<SendEmailRequest.Builder>anyConsumer()))
                .thenAnswer(inv -> {
                    SendEmailRequest.Builder b = SendEmailRequest.builder();
                    inv.<Consumer<SendEmailRequest.Builder>>getArgument(0).accept(b);
                    seen.set(b.build());
                    return SendEmailResponse.builder().messageId("msg-1").build();
                });

        given().contentType(ContentType.JSON)
                .body(Map.of("email", "invito@esterno.test", "token", "token-invito-raw", "role", "member"))
                .when().post("/api/auth/invitations/send")
                .then().statusCode(202);

        SendEmailRequest req = seen.get();
        assertEquals("invito@esterno.test", req.destination().toAddresses().get(0));
        assertTrue(req.content().simple().body().text().data().contains("token-invito-raw"),
                "il link nell'email porta il token d'invito");
    }
}

/** Matcher condiviso per gli overload Consumer-based dell'SDK. */
final class CognitoAuthFlowsTestHelpers {

    private CognitoAuthFlowsTestHelpers() {}

    @SuppressWarnings("unchecked")
    static <B> Consumer<B> anyConsumer() {
        return (Consumer<B>) any(Consumer.class);
    }
}
