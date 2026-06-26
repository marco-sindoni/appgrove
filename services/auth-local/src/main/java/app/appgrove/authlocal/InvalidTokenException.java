package app.appgrove.authlocal;

/** Token (refresh/verifica/reset/challenge/access) assente, scaduto o forgiato → fail-closed (401). */
public class InvalidTokenException extends RuntimeException {
}
