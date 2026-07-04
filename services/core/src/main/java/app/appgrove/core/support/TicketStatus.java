package app.appgrove.core.support;

/** Stato di un ticket. Persistito come stringa. */
public enum TicketStatus {
    open,
    in_progress,
    resolved,
    closed;

    /** Stato terminale: da qui decorre la retention di 24 mesi (#13 E). */
    public boolean isTerminal() {
        return this == resolved || this == closed;
    }
}
