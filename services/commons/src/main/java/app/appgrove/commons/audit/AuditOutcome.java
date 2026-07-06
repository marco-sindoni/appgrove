package app.appgrove.commons.audit;

/** Esito di un evento audit/sicurezza (UC 0006/#08 29). */
public enum AuditOutcome {
    /** Azione completata. */
    SUCCESS,
    /** Azione tentata e fallita (errore applicativo/tecnico). */
    FAILURE,
    /** Azione rifiutata da un controllo (autorizzazione, stato incompatibile). */
    DENIED
}
