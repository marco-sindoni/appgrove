package app.appgrove.commons.storage;

import java.time.Duration;
import java.time.Instant;

/**
 * Storage del bucket export GDPR (≈ S3): frammenti per-app e ZIP finale dei job di export
 * (UC 0032, #13 D22). In dev/prod = {@link S3ExportStorage} (MinIO/S3); nei test un'implementazione
 * in-memory ({@code @Mock} nel servizio). Il download utente passa <b>solo</b> da link firmati a
 * scadenza ({@link #presignGet}) generati dal core: il bucket non è mai pubblico.
 */
public interface ExportStorage {

    /** Carica {@code content} alla chiave {@code key} (sovrascrive: i worker sono idempotenti). */
    void put(String key, byte[] content, String contentType);

    /** Legge l'oggetto alla chiave {@code key} (per l'aggregazione ZIP del core). */
    byte[] get(String key);

    /** Genera un link firmato in GET valido {@code ttl} (export: 7 giorni, #13 D22). */
    PresignedLink presignGet(String key, Duration ttl);

    /** Link firmato: URL + istante di scadenza (mostrato in UI con data/ora, #13 D22.4). */
    record PresignedLink(String url, Instant expiresAt) {}
}
