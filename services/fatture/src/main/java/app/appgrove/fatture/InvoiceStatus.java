package app.appgrove.fatture;

/** Stato di una fattura. {@code voided} = annullata ("void" è keyword Java). */
public enum InvoiceStatus {
    draft,
    issued,
    paid,
    voided
}
