package app.appgrove.authlocal;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** Hash/verifica password con BCrypt (cost 12). Lo stato vive in auth_local (dev-only). */
public final class Passwords {

    private static final int COST = 12;

    private Passwords() {}

    public static String hash(String raw) {
        return BCrypt.withDefaults().hashToString(COST, raw.toCharArray());
    }

    public static boolean verify(String raw, String hash) {
        return BCrypt.verifyer().verify(raw.toCharArray(), hash).verified;
    }
}
