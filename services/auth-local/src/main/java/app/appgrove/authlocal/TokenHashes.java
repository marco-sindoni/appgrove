package app.appgrove.authlocal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** SHA-256(hex) — stesso schema del core ({@code InvitationTokens.hash}) per confrontare i token d'invito. */
public final class TokenHashes {

    private TokenHashes() {}

    public static String sha256Hex(String token) {
        try {
            byte[] out = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 non disponibile", e);
        }
    }
}
