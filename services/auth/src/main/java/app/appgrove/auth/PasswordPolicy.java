package app.appgrove.auth;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Policy password (#02 dec.19): ≥10 caratteri, almeno una maiuscola, una minuscola, una cifra. */
public final class PasswordPolicy {

    private PasswordPolicy() {}

    public static void validate(String pw) {
        boolean ok = pw != null
                && pw.length() >= 10
                && pw.chars().anyMatch(Character::isUpperCase)
                && pw.chars().anyMatch(Character::isLowerCase)
                && pw.chars().anyMatch(Character::isDigit);
        if (!ok) {
            throw new WebApplicationException(
                    "La password deve avere almeno 10 caratteri, con maiuscola, minuscola e numero.",
                    Response.Status.BAD_REQUEST);
        }
    }
}
