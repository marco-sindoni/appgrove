package app.appgrove.core.billing;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coda webhook in-memory per i test (sostituisce {@code SqsWebhookQueue} via {@code @Mock}): nessun
 * ElasticMQ/LocalStack → test deterministici e offline. Preserva l'<b>ordine FIFO</b> di inserimento,
 * coerente col rigore Minimo del consumer (l'out-of-order è gestito da UC 0025).
 */
@Mock
@ApplicationScoped
public class InMemoryWebhookQueue implements WebhookQueue {

    private final Map<String, String> messages = Collections.synchronizedMap(new LinkedHashMap<>());
    private final AtomicLong seq = new AtomicLong();

    @Override
    public void send(String body) {
        messages.put("h-" + seq.incrementAndGet(), body);
    }

    @Override
    public List<Message> receive(int max) {
        synchronized (messages) {
            return messages.entrySet().stream()
                    .limit(max)
                    .map(e -> new Message(e.getKey(), e.getValue()))
                    .toList();
        }
    }

    @Override
    public void delete(Message message) {
        messages.remove(message.handle());
    }

    /** Numero di messaggi in coda (assert nei test). */
    public int size() {
        return messages.size();
    }

    /** Svuota la coda tra un test e l'altro. */
    public void clear() {
        messages.clear();
    }
}
