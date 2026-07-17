package app.appgrove.commons.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Nome fisico delle code (UC 0005): nel cloud i nomi logici sono prefissati per-ambiente
 * ({@code appgrove.sqs.queue-prefix} ← env {@code APPGROVE_SQS_QUEUE_PREFIX} del modulo
 * {@code microsaas_app}); in locale il prefisso è vuoto e i nomi coincidono con ElasticMQ.
 */
class SqsMessageQueuesTest {

    @Test
    void inLocaleSenzaPrefissoIlNomeFisicoCoincideColLogico() {
        var queues = new SqsMessageQueues(Optional.of("http://localhost:9324"), "eu-south-1", "");
        assertEquals("gdpr-export-fatture", queues.physicalName("gdpr-export-fatture"));
    }

    @Test
    void nelCloudIlPrefissoPerAmbientePrecedeIlNomeLogico() {
        var queues = new SqsMessageQueues(Optional.empty(), "eu-west-1", "appgrove-test-");
        assertEquals("appgrove-test-gdpr-export-fatture", queues.physicalName("gdpr-export-fatture"));
    }
}
