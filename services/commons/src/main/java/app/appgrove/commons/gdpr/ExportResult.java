package app.appgrove.commons.gdpr;

import java.util.List;
import java.util.Map;

/**
 * Esito di {@link AppDataContract#exportData(GdprScope)}: i dati del tenant per ogni entità, più gli
 * <b>step dichiarati</b> per il progress (#13 L69). {@code entities} mappa nome-entità → righe
 * (ogni riga è una mappa colonna→valore, già serializzabile).
 */
public record ExportResult(
        String appId,
        List<String> steps,
        Map<String, List<Map<String, Object>>> entities) {}
