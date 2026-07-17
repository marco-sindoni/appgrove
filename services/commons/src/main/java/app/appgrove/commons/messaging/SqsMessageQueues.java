package app.appgrove.commons.messaging;

import app.appgrove.commons.aws.AwsClientCredentials;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * {@link MessageQueues} su SQS (in dev = ElasticMQ, #11; in prod = SQS reale). Stesso client e
 * stesse chiavi di config del {@code SqsWebhookQueue} di core ({@code appgrove.sqs.endpoint} /
 * {@code appgrove.sqs.region}); gli URL delle code sono risolti in lazy e cachati per nome, così
 * l'avvio non dipende dalla raggiungibilità di ElasticMQ. Attiva fuori dal profilo {@code test}.
 *
 * <p>Profilo cloud (UC 0005): senza endpoint override le credenziali vengono dalla catena di
 * default (task role IAM) e i nomi logici delle code sono prefissati con
 * {@code appgrove.sqs.queue-prefix} (← env {@code APPGROVE_SQS_QUEUE_PREFIX} iniettata dal modulo
 * {@code microsaas_app}: le code fisiche sono {@code appgrove-<env>-...}). In locale il prefisso è
 * vuoto e i nomi logici coincidono con quelli fisici di ElasticMQ.
 */
@ApplicationScoped
@UnlessBuildProfile("test")
public class SqsMessageQueues implements MessageQueues {

    private final String endpoint;
    private final String region;
    private final String queuePrefix;
    private final Map<String, String> queueUrls = new ConcurrentHashMap<>();
    private SqsClient client;

    public SqsMessageQueues(
            @ConfigProperty(name = "appgrove.sqs.endpoint") Optional<String> endpoint,
            @ConfigProperty(name = "appgrove.sqs.region", defaultValue = "eu-south-1") String region,
            @ConfigProperty(name = "appgrove.sqs.queue-prefix") Optional<String> queuePrefix) {
        this.endpoint = endpoint.orElse(null);
        this.region = region;
        // Optional (non defaultValue=""): un default stringa-vuota non supera la validazione
        // config di Quarkus all'avvio (SRCFG00014) nei profili dove il bean è attivo (dev).
        this.queuePrefix = queuePrefix.orElse("");
    }

    @PostConstruct
    void init() {
        var builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(AwsClientCredentials.forEndpoint(endpoint, "local", "local"))
                .httpClient(UrlConnectionHttpClient.create());
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint));
        }
        this.client = builder.build();
    }

    /** Nome fisico della coda: prefisso per-ambiente + nome logico (testabile senza client). */
    String physicalName(String queueName) {
        return queuePrefix + queueName;
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }

    private String queueUrl(String queueName) {
        return queueUrls.computeIfAbsent(
                queueName, name -> client.getQueueUrl(b -> b.queueName(physicalName(name))).queueUrl());
    }

    @Override
    public void send(String queueName, String body) {
        client.sendMessage(
                SendMessageRequest.builder().queueUrl(queueUrl(queueName)).messageBody(body).build());
    }

    @Override
    public List<Message> receive(String queueName, int max) {
        return client
                .receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl(queueName))
                        .maxNumberOfMessages(max)
                        .waitTimeSeconds(0)
                        .build())
                .messages()
                .stream()
                .map(m -> new Message(m.receiptHandle(), m.body()))
                .toList();
    }

    @Override
    public void delete(String queueName, Message message) {
        client.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl(queueName))
                .receiptHandle(message.handle())
                .build());
    }
}
