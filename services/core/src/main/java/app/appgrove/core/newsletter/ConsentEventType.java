package app.appgrove.core.newsletter;

/**
 * Tipo di evento nel registro dei consensi (UC 0039): {@code grant} = consenso espresso (intento
 * iniziale), {@code confirm} = conferma del double opt-in (la prova decisiva ex art. 7), {@code revoke}
 * = revoca/disiscrizione. Il registro è append-only: ogni evento resta, non si aggiorna né si cancella.
 */
public enum ConsentEventType {
    grant,
    confirm,
    revoke
}
