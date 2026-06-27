package app.appgrove.commons.quota;

/**
 * Natura di una metrica di quota (#09 A).
 * <ul>
 *   <li>{@link #FLOW}: conteggio di eventi in una finestra che si azzera (es. "n. fatture/mese");</li>
 *   <li>{@link #STOCK}: livello istantaneo che non si azzera (es. "n. posti/seats").</li>
 * </ul>
 */
public enum QuotaNature {
    FLOW,
    STOCK
}
