package app.appgrove.core.billing;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Firma HMAC dei webhook (#09 D18a) nel formato reale di Paddle:
 * {@code Paddle-Signature: ts=<unix>;h1=<hmac-sha256(secret, ts + ":" + body)>}. Il secret di firma è
 * per-ambiente (in dev un secret di test; in prod Secrets Manager — packaging cloud differito, UC 0025
 * "Punti aperti"). Verifica a tempo costante; firma assente/errata → l'ingest risponde 401 senza alcun
 * processing.
 *
 * <p><b>Anti-replay</b> (hardening UC 0025): il {@code ts} è incluso nel payload firmato; una firma con
 * {@code ts} fuori dalla finestra {@code appgrove.payments.webhook-max-age} è rifiutata (difesa contro il
 * re-invio malevolo di un payload catturato). Finestra ≤ 0 → controllo disattivato (utile in scenari
 * deterministici). La validazione del formato/segreto contro il vero Paddle è L3 (UC 0029, gated #14).
 */
@ApplicationScoped
public class PaddleSignature {

    private final byte[] secret;
    private final long maxAgeSeconds;

    public PaddleSignature(
            @ConfigProperty(name = "appgrove.payments.webhook-secret") String secret,
            @ConfigProperty(name = "appgrove.payments.webhook-max-age", defaultValue = "PT5M")
                    Duration maxAge) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.maxAgeSeconds = maxAge.getSeconds();
    }

    /** Firma il body all'istante corrente → header {@code ts=...;h1=...}. */
    public String sign(String body) {
        return sign(body, Instant.now().getEpochSecond());
    }

    /** Firma il body a un {@code ts} esplicito (per i test di replay/out-of-order). */
    public String sign(String body, long ts) {
        return "ts=" + ts + ";h1=" + hmacHex(ts + ":" + body);
    }

    /**
     * Verifica a tempo costante e anti-replay; header assente/malformato/errato o {@code ts} fuori
     * finestra → {@code false} (l'ingest scarterà con 401).
     */
    public boolean verify(String body, String header) {
        if (header == null || header.isBlank()) {
            return false;
        }
        String ts = null;
        String h1 = null;
        for (String part : header.split(";")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = part.substring(0, eq).trim();
            String value = part.substring(eq + 1).trim();
            if ("ts".equals(key)) {
                ts = value;
            } else if ("h1".equals(key)) {
                h1 = value;
            }
        }
        if (ts == null || h1 == null) {
            return false;
        }
        long tsEpoch;
        try {
            tsEpoch = Long.parseLong(ts);
        } catch (NumberFormatException e) {
            return false;
        }
        if (maxAgeSeconds > 0
                && Math.abs(Instant.now().getEpochSecond() - tsEpoch) > maxAgeSeconds) {
            return false; // replay / clock skew oltre la finestra
        }
        byte[] expected = hmacHex(tsEpoch + ":" + body).getBytes(StandardCharsets.UTF_8);
        byte[] provided = h1.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    private String hmacHex(String signedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] raw = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile firmare il webhook", e);
        }
    }
}
