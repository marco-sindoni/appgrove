package app.appgrove.commons.quota;

/**
 * Contratto SPI di quota per-app (#09 A5/E23/F30 gate 5). L'app implementa la faccia runtime e la
 * invoca <b>prima</b> dell'azione che consuma quota. Il tetto arriva da {@link QuotaLimitSource};
 * l'uso corrente è calcolato dall'app (per una metrica {@link QuotaNature#FLOW}, conteggio nella
 * finestra). A tetto raggiunto si lancia {@link QuotaExceededException} (→ 429).
 */
public interface QuotaService {

    /**
     * Verifica che ci sia capienza per un'unità della metrica e la "prenota" implicitamente
     * lasciando procedere l'azione (per le metriche flow la prenotazione è l'azione stessa).
     *
     * @throws QuotaExceededException se l'uso corrente ha raggiunto il tetto.
     */
    void checkAndReserve(String metric);

    /** Uso corrente della metrica nella finestra (per ispezione/test). */
    long currentUsage(String metric);
}
