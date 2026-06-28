package app.appgrove.core.billing;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Firma HMAC dei webhook (#09 D18a): il secret di firma è per-ambiente (in dev un secret di test;
 * in prod Secrets Manager, UC 0025/infra). Verifica a tempo costante; firma non valida → l'ingest
 * risponde 401 senza alcun processing.
 *
 * <p>Schema minimo (UC 0023): {@code HMAC-SHA256(secret, body)} in hex. Il formato header reale di
 * Paddle ({@code ts=...;h1=...}) sarà allineato in UC 0025 (pipeline prod); qui conta esercitare la
 * catena verifica → ingest → consumer in modo deterministico.
 */
@ApplicationScoped
public class PaddleSignature {

    private final byte[] secret;

    public PaddleSignature(@ConfigProperty(name = "appgrove.payments.webhook-secret") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Firma il corpo grezzo del webhook (hex di HMAC-SHA256). */
    public String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile firmare il webhook", e);
        }
    }

    /** Verifica a tempo costante; firma assente/errata → false (l'ingest scarterà con 401). */
    public boolean verify(String body, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        byte[] expected = sign(body).getBytes(StandardCharsets.UTF_8);
        byte[] provided = signature.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }
}
