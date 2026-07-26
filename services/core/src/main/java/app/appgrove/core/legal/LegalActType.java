package app.appgrove.core.legal;

/**
 * Tipo di atto registrato nel log accettazioni (UC 0056):
 * <ul>
 *   <li>{@code accept} — accettazione esplicita (contratto: Termini di Servizio);</li>
 *   <li>{@code acknowledge} — presa d'atto di un'informativa (Privacy, Cookie).</li>
 * </ul>
 */
public enum LegalActType {
    accept,
    acknowledge
}
