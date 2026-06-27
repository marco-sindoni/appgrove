package app.appgrove.commons.gdpr;

import java.util.Map;

/**
 * Esito di {@link AppDataContract#purgeData(GdprScope)}: numero di righe cancellate per entità
 * (cancellazione <b>fisica</b>, erasure GDPR #13 L70). Serve all'audit dell'orchestratore purge.
 */
public record PurgeResult(String appId, Map<String, Integer> deletedByEntity) {

    public int total() {
        return deletedByEntity.values().stream().mapToInt(Integer::intValue).sum();
    }
}
