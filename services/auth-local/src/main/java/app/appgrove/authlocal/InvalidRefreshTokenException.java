package app.appgrove.authlocal;

/** Refresh token assente/scaduto/forgiato → fail-closed (401). */
public class InvalidRefreshTokenException extends RuntimeException {
}
