package app.appgrove.auth.cognito;

import app.appgrove.auth.InvalidTokenException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Codifica/decodifica dei valori opachi del provider Cognito: il client vede sempre UNA stringa
 * (parità di contratto col provider locale), dentro ci stanno le coppie che Cognito richiede.
 *
 * <ul>
 *   <li>token email (verify/reset) = {@code base64url(email "|" codice)} — il link nelle email lo
 *       costruirà il Custom Message Lambda (UC 0018) con lo stesso schema;</li>
 *   <li>cookie refresh = {@code base64url(sub "|" refreshToken)} — il sub serve al SECRET_HASH del
 *       flusso {@code REFRESH_TOKEN_AUTH};</li>
 *   <li>challenge 2FA = {@code base64url(email "|" session)} — la Session Cognito da sola non basta
 *       a {@code RespondToAuthChallenge} (serve USERNAME).</li>
 * </ul>
 */
final class OpaqueTokens {

    private static final String SEPARATOR = "|";

    private OpaqueTokens() {}

    static String join(String first, String second) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((first + SEPARATOR + second).getBytes(StandardCharsets.UTF_8));
    }

    /** Decodifica in due parti; token malformato → {@link InvalidTokenException} (fail-closed). */
    static String[] split(String opaque) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(opaque), StandardCharsets.UTF_8);
            int idx = decoded.indexOf(SEPARATOR);
            if (idx <= 0 || idx == decoded.length() - 1) {
                throw new InvalidTokenException();
            }
            return new String[] {decoded.substring(0, idx), decoded.substring(idx + 1)};
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException();
        }
    }
}
