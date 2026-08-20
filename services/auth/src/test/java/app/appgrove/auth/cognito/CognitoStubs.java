package app.appgrove.auth.cognito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Utilità dei test Cognito: JWT finti (payload con sub), SECRET_HASH atteso, token opachi. */
final class CognitoStubs {

    private CognitoStubs() {}

    /**
     * Access token "alla Cognito" <b>senza</b> il claim {@code tenant_id}: firma non verificata dal
     * provider, conta solo il payload. È il token che la funzione del token emette quando non riesce a
     * scegliere l'account — non solleva eccezioni, restituisce l'evento inalterato (UC 0116/0117).
     */
    static String accessTokenWithSub(String sub) {
        return token("{\"sub\":\"" + sub + "\",\"token_use\":\"access\"}");
    }

    /** Access token con il claim {@code tenant_id}: il caso normale, account già stabilito. */
    static String accessTokenWithTenant(String sub, String tenantId) {
        return token("{\"sub\":\"" + sub + "\",\"tenant_id\":\"" + tenantId
                + "\",\"token_use\":\"access\"}");
    }

    private static String token(String payload) {
        return base64Url("{\"alg\":\"RS256\"}") + "." + base64Url(payload) + ".firma-finta";
    }

    static String expectedSecretHash(String username) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    CognitoTestProfile.CLIENT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal((username + CognitoTestProfile.CLIENT_ID).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String opaque(String first, String second) {
        return OpaqueTokens.join(first, second);
    }

    private static String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
}
