package app.appgrove.core.support;

/**
 * Stato di un ticket. Persistito come stringa.
 *
 * <p>Il ciclo di vita (UC 0075): nasce {@code open} (aspetta la piattaforma); chi assiste risponde
 * e passa a {@code waiting_user} (aspetta il cliente); la replica del cliente riporta a
 * {@code open}. {@code in_progress} è lo stato che l'operatore mette a mano quando sta lavorando
 * alla richiesta senza aver ancora risposto. {@code resolved} e {@code closed} sono terminali.
 */
public enum TicketStatus {
    open,
    in_progress,
    /** Risposta data: la palla è al cliente (UC 0075). Una sua replica riporta a {@link #open}. */
    waiting_user,
    resolved,
    closed;

    /** Stato terminale: da qui decorre la retention di 24 mesi (#13 E). */
    public boolean isTerminal() {
        return this == resolved || this == closed;
    }

    /**
     * Lo stato aspetta una mossa della piattaforma (e quindi corre la scadenza di legge sui ticket
     * privacy). Usato per portare in cima alla coda ciò su cui l'operatore deve agire.
     */
    public boolean awaitsPlatform() {
        return this == open || this == in_progress;
    }
}
