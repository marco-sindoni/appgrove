package app.appgrove.commons.messaging;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link MessageQueues} in-memory per i test (nel test-jar di commons, UC 0032): nessun
 * ElasticMQ → deterministico e offline. Ogni servizio la espone via una sottoclasse
 * {@code @io.quarkus.test.Mock}. Code create al primo uso; FIFO; simula il <b>redrive/DLQ</b> di
 * SQS/ElasticMQ come la {@code InMemoryWebhookQueue} di core (maxReceiveCount=5, allineato a
 * {@code dev/elasticmq.conf}).
 */
public class InMemoryMessageQueues implements MessageQueues {

    /** Pari a {@code dev/elasticmq.conf} (DLQ dopo 5 consegne senza delete). */
    public static final int MAX_RECEIVE_COUNT = 5;

    private final Map<String, Map<String, String>> queues = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> receiveCounts = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> dlqs = new LinkedHashMap<>();
    private final AtomicLong seq = new AtomicLong();

    @Override
    public synchronized void send(String queueName, String body) {
        queue(queueName).put("h-" + seq.incrementAndGet(), body);
    }

    @Override
    public synchronized List<Message> receive(String queueName, int max) {
        Map<String, String> queue = queue(queueName);
        Map<String, Integer> counts = receiveCounts.computeIfAbsent(queueName, q -> new LinkedHashMap<>());
        List<Message> batch = new ArrayList<>();
        for (Map.Entry<String, String> e : List.copyOf(queue.entrySet())) {
            if (batch.size() >= max) {
                break;
            }
            String handle = e.getKey();
            int count = counts.merge(handle, 1, Integer::sum);
            if (count > MAX_RECEIVE_COUNT) {
                dlqs.computeIfAbsent(queueName, q -> new LinkedHashMap<>()).put(handle, e.getValue());
                queue.remove(handle);
                counts.remove(handle);
                continue;
            }
            batch.add(new Message(handle, e.getValue()));
        }
        return batch;
    }

    @Override
    public synchronized void delete(String queueName, Message message) {
        queue(queueName).remove(message.handle());
        receiveCounts.getOrDefault(queueName, Map.of()).remove(message.handle());
    }

    private Map<String, String> queue(String queueName) {
        return queues.computeIfAbsent(queueName, q -> new LinkedHashMap<>());
    }

    /** Numero di messaggi in coda (assert nei test). */
    public synchronized int size(String queueName) {
        return queue(queueName).size();
    }

    /** Corpi dei messaggi in coda, in ordine FIFO (assert nei test, senza consumarli). */
    public synchronized List<String> bodies(String queueName) {
        return List.copyOf(queue(queueName).values());
    }

    /** Numero di messaggi finiti in DLQ (assert nei test). */
    public synchronized int dlqSize(String queueName) {
        return dlqs.getOrDefault(queueName, Map.of()).size();
    }

    /** Svuota tutte le code, DLQ e contatori tra un test e l'altro. */
    public synchronized void clear() {
        queues.clear();
        receiveCounts.clear();
        dlqs.clear();
    }
}
