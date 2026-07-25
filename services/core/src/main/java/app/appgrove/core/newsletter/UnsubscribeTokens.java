package app.appgrove.core.newsletter;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Token del collegamento di disiscrizione one-click. A differenza del token di conferma (single-use,
 * hash su DB), qui il token è <b>calcolato</b> — HMAC-SHA256 del segreto di configurazione sull'id
 * dell'iscritto — così è ricalcolabile e verificabile a ogni futura email marketing senza persistere
 * nulla e senza scadenza. Capacità di sola disiscrizione: chi la possiede può solo revocare, mai
 * iscrivere o leggere.
 */
@ApplicationScoped
public class UnsubscribeTokens {

    private static final String ALGO = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    @ConfigProperty(name = "appgrove.newsletter.unsubscribe-secret")
    String secret;

    /** Token opaco per l'id dato. */
    public String tokenFor(UUID subscriberId) {
        try {
            Mac mac = Mac.getInstance(ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGO));
            byte[] out = mac.doFinal(subscriberId.toString().getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC di disiscrizione non calcolabile", e);
        }
    }

    /** Verifica in tempo costante che il token corrisponda all'id (nessun indizio dai tempi). */
    public boolean verify(UUID subscriberId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        byte[] expected = tokenFor(subscriberId).getBytes(StandardCharsets.UTF_8);
        byte[] given = token.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, given);
    }
}
