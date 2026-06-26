package app.appgrove.authlocal;

import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;

/**
 * Firma i token (access/id/refresh) con la chiave RSA locale, espone il JWKS (chiave pubblica) e
 * verifica il refresh token. Replica lo shape JWT di prod (claim {@code sub}/{@code tenant_id}/
 * {@code groups}+{@code roles}); cambia solo l'issuer e la sorgente chiavi.
 */
@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "auth.local.issuer")
    String issuer;

    @ConfigProperty(name = "auth.local.kid")
    String kid;

    @ConfigProperty(name = "auth.local.access-ttl-seconds", defaultValue = "900")
    long accessTtl;

    @ConfigProperty(name = "auth.local.refresh-ttl-seconds", defaultValue = "86400")
    long refreshTtl;

    @ConfigProperty(name = "auth.local.verify-ttl-seconds", defaultValue = "86400")
    long verifyTtl;

    @ConfigProperty(name = "auth.local.reset-ttl-seconds", defaultValue = "3600")
    long resetTtl;

    @ConfigProperty(name = "auth.local.mfa-challenge-ttl-seconds", defaultValue = "300")
    long mfaChallengeTtl;

    @ConfigProperty(name = "auth.local.platform-admin-subjects", defaultValue = "seed-platform-admin")
    List<String> platformAdminSubjects;

    @ConfigProperty(name = "auth.local.private-key-location")
    String privateKeyLocation;

    @ConfigProperty(name = "auth.local.public-key-location")
    String publicKeyLocation;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String jwksJson;
    private JwtConsumer consumer;

    @PostConstruct
    void init() {
        try {
            this.privateKey = decodePrivate(readPem(privateKeyLocation));
            this.publicKey = decodePublic(readPem(publicKeyLocation));
            this.jwksJson = buildJwks(publicKey, kid);
            this.consumer = new JwtConsumerBuilder()
                    .setVerificationKey(publicKey)
                    .setExpectedIssuer(issuer)
                    .setRequireExpirationTime()
                    .setRequireSubject()
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Inizializzazione chiavi auth-local fallita", e);
        }
    }

    public long accessTtlSeconds() {
        return accessTtl;
    }

    public boolean isPlatformAdmin(String sub) {
        return platformAdminSubjects.contains(sub);
    }

    /** Gruppi/ruoli del token: ruolo tenant (+ platform-admin se il subject è configurato). */
    public Set<String> groupsFor(AuthUser user) {
        Set<String> groups = new java.util.LinkedHashSet<>();
        groups.add(user.role());
        if (isPlatformAdmin(user.sub())) {
            groups.add("platform-admin");
        }
        return groups;
    }

    public String accessToken(AuthUser user, Set<String> groups) {
        return Jwt.issuer(issuer)
                .subject(user.sub())
                .upn(user.email())
                .claim("tenant_id", user.tenantId())
                .groups(groups)
                .claim("roles", new ArrayList<>(groups))
                .claim("token_use", "access")
                .expiresIn(accessTtl)
                .jws().keyId(kid).sign(privateKey);
    }

    public String idToken(AuthUser user) {
        // smallrye-jwt rifiuta claim null → fallback per displayName mancante (signup senza nome).
        String name = user.displayName() != null ? user.displayName() : user.email();
        return Jwt.issuer(issuer)
                .subject(user.sub())
                .upn(user.email())
                .claim("email", user.email())
                .claim("name", name)
                .claim("token_use", "id")
                .expiresIn(accessTtl)
                .jws().keyId(kid).sign(privateKey);
    }

    public String refreshToken(AuthUser user) {
        return signTyped(user.sub(), "refresh", refreshTtl);
    }

    public String emailVerifyToken(String sub) {
        return signTyped(sub, "email_verify", verifyTtl);
    }

    public String passwordResetToken(String sub) {
        return signTyped(sub, "pwd_reset", resetTtl);
    }

    public String mfaChallengeToken(String sub) {
        return signTyped(sub, "mfa_challenge", mfaChallengeTtl);
    }

    private String signTyped(String sub, String use, long ttl) {
        return Jwt.issuer(issuer)
                .subject(sub)
                .claim("token_use", use)
                .expiresIn(ttl)
                .jws().keyId(kid).sign(privateKey);
    }

    public String jwks() {
        return jwksJson;
    }

    public long refreshTtlSeconds() {
        return refreshTtl;
    }

    public String verifyRefreshSubject(String token) {
        return verifyTokenSubject(token, "refresh");
    }

    /** Verifica firma/issuer/scadenza + {@code token_use} atteso, e ritorna il subject. Lancia se non valido. */
    public String verifyTokenSubject(String token, String expectedUse) {
        try {
            JwtClaims claims = consumer.processToClaims(token);
            if (!expectedUse.equals(claims.getClaimValueAsString("token_use"))) {
                throw new IllegalArgumentException("token_use atteso: " + expectedUse);
            }
            return claims.getSubject();
        } catch (Exception e) {
            throw new InvalidTokenException();
        }
    }

    // ── helper chiavi ────────────────────────────────────────────────────────
    private static String readPem(String location) throws IOException {
        Path p = Path.of(location);
        if (Files.exists(p)) {
            return Files.readString(p);
        }
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(location)) {
            if (is == null) {
                throw new IOException("chiave non trovata: " + location);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] derBytes(String pem) {
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private static PrivateKey decodePrivate(String pem) throws Exception {
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(derBytes(pem)));
    }

    private static PublicKey decodePublic(String pem) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(derBytes(pem)));
    }

    private static String buildJwks(PublicKey publicKey, String kid) throws Exception {
        RsaJsonWebKey jwk = new RsaJsonWebKey((RSAPublicKey) publicKey);
        jwk.setKeyId(kid);
        jwk.setUse("sig");
        jwk.setAlgorithm("RS256");
        return new JsonWebKeySet(jwk).toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY);
    }
}
