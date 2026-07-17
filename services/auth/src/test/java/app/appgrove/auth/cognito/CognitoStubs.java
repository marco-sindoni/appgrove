package app.appgrove.auth.cognito;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Utilità dei test Cognito: JWT finti (payload con sub), SECRET_HASH atteso, token opachi. */
final class CognitoStubs {

    private CognitoStubs() {}

    /** Access token "alla Cognito": firma non verificata dal provider, conta solo il payload. */
    static String accessTokenWithSub(String sub) {
        String header = base64Url("{\"alg\":\"RS256\"}");
        String payload = base64Url("{\"sub\":\"" + sub + "\",\"token_use\":\"access\"}");
        return header + "." + payload + ".firma-finta";
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
