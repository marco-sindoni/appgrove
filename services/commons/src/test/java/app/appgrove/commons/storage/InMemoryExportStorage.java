package app.appgrove.commons.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * {@link ExportStorage} in-memory per i test (nel test-jar di commons, UC 0032): nessun MinIO →
 * deterministico e offline. Ogni servizio la espone via una sottoclasse {@code @io.quarkus.test.Mock}.
 * Il presigned URL è fittizio ma porta chiave e scadenza, così i test possono asserire il TTL di
 * 7 giorni (#13 D22) senza firma reale.
 */
public class InMemoryExportStorage implements ExportStorage {

    private final Map<String, byte[]> objects = new LinkedHashMap<>();

    @Override
    public synchronized void put(String key, byte[] content, String contentType) {
        objects.put(key, content.clone());
    }

    @Override
    public synchronized byte[] get(String key) {
        byte[] content = objects.get(key);
        if (content == null) {
            throw new NoSuchElementException("oggetto assente nello storage di test: " + key);
        }
        return content.clone();
    }

    @Override
    public synchronized PresignedLink presignGet(String key, Duration ttl) {
        if (!objects.containsKey(key)) {
            throw new NoSuchElementException("oggetto assente nello storage di test: " + key);
        }
        Instant expiresAt = Instant.now().plus(ttl);
        return new PresignedLink("https://storage.local/" + key + "?expires=" + expiresAt, expiresAt);
    }

    /** Chiavi presenti (assert nei test). */
    public synchronized Set<String> keys() {
        return Set.copyOf(objects.keySet());
    }

    public synchronized boolean contains(String key) {
        return objects.containsKey(key);
    }

    /** Svuota lo storage tra un test e l'altro. */
    public synchronized void clear() {
        objects.clear();
    }
}
