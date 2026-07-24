package app.appgrove.crm;

/** Avanzamento della trattativa con un contatto del mini-CRM. Persistito come stringa. */
public enum ContactStage {
    /** Contatto acquisito, non ancora qualificato. */
    lead,
    /** Contatto qualificato (interesse reale, budget plausibile). */
    qualified,
    /** Trattativa in corso. */
    negotiating,
    /** Trattativa chiusa positivamente. */
    won,
    /** Trattativa chiusa senza esito. */
    lost
}
