package app.appgrove.core.billing;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coda webhook in-memory per i test (sostituisce {@code SqsWebhookQueue} via {@code @Mock}): nessun
 * ElasticMQ/LocalStack → test deterministici e offline. Preserva l'<b>ordine FIFO</b> di inserimento.
 *
 * <p>Simula il <b>redrive/DLQ</b> di SQS/ElasticMQ (UC 0025): ogni {@link #receive} incrementa il
 * receive-count del messaggio; superato {@code maxReceiveCount} consegne senza {@link #delete} (= il
 * consumer ha fallito ripetutamente), il messaggio è instradato nella <b>DLQ</b> e non viene più
 * consegnato. Allineato a {@code dev/elasticmq.conf} (maxReceiveCount=5). Così l'L1 può asserire
 * "messaggio velenoso → finisce in DLQ, non perso né processato all'infinito".
 */
@Mock
@ApplicationScoped
public class InMemoryWebhookQueue implements WebhookQueue {

    /** Pari a {@code dev/elasticmq.conf} (paddle-webhooks → DLQ dopo 5 consegne). */
    static final int MAX_RECEIVE_COUNT = 5;

    private final Map<String, String> messages = new LinkedHashMap<>();
    private final Map<String, Integer> receiveCount = new LinkedHashMap<>();
    private final Map<String, String> dlq = new LinkedHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    @Override
    public synchronized void send(String body) {
        messages.put("h-" + seq.incrementAndGet(), body);
    }

    @Override
    public synchronized List<Message> receive(int max) {
        List<Message> batch = new ArrayList<>();
        for (Map.Entry<String, String> e : List.copyOf(messages.entrySet())) {
            if (batch.size() >= max) {
                break;
            }
            String handle = e.getKey();
            int count = receiveCount.merge(handle, 1, Integer::sum);
            if (count > MAX_RECEIVE_COUNT) {
                // troppi tentativi falliti senza delete → DLQ (non più consegnato dalla coda principale)
                dlq.put(handle, e.getValue());
                messages.remove(handle);
                receiveCount.remove(handle);
                continue;
            }
            batch.add(new Message(handle, e.getValue()));
        }
        return batch;
    }

    @Override
    public synchronized void delete(Message message) {
        messages.remove(message.handle());
        receiveCount.remove(message.handle());
    }

    /** Numero di messaggi nella coda principale (assert nei test). */
    public synchronized int size() {
        return messages.size();
    }

    /** Numero di messaggi finiti in DLQ (assert nei test). */
    public synchronized int dlqSize() {
        return dlq.size();
    }

    /** Svuota coda, DLQ e contatori tra un test e l'altro. */
    public synchronized void clear() {
        messages.clear();
        receiveCount.clear();
        dlq.clear();
    }
}
