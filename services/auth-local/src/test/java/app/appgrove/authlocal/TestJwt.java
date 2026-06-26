package app.appgrove.authlocal;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

/** Util di test per ispezionare e verificare i JWT emessi da auth-local. */
final class TestJwt {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final String ISSUER = "https://local.appgrove.app";

    private TestJwt() {}

    /** Decodifica (senza verifica) i claim del payload del JWT. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> claims(String token) {
        try {
            String payload = token.split("\\.")[1];
            byte[] json = Base64.getUrlDecoder().decode(payload);
            return MAPPER.readValue(new String(json, StandardCharsets.UTF_8), Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Verifica firma/issuer/scadenza del token contro il JWKS pubblicato (prova che il core lo accetta). */
    static void verifyAgainstJwks(String token, String jwksJson) throws Exception {
        JsonWebKeySet set = new JsonWebKeySet(jwksJson);
        JsonWebKey jwk = set.getJsonWebKeys().get(0);
        JwtConsumer consumer = new JwtConsumerBuilder()
                .setVerificationKey(jwk.getKey())
                .setExpectedIssuer(ISSUER)
                .setRequireExpirationTime()
                .build();
        consumer.processToClaims(token); // lancia se non valido
    }
}
