package app.appgrove.commons.gdpr;

import java.util.List;

/**
 * Manifesto dati per-app (#13 C15/L69): l'inventario dei campi con dati personali e la loro
 * classificazione. È la dichiarazione che la RoPA automation (UC 0030) industrializzerà e che il
 * test di compliance usa per verificare che export/erasure coprano ogni dato personale.
 */
public record DataManifest(String appId, List<Entry> entries) {

    /** Una voce del manifesto: un campo personale di un'entità con la sua classificazione. */
    public record Entry(
            String entity,
            String field,
            String category,
            String purpose,
            String legalBasis,
            String retention) {}
}
