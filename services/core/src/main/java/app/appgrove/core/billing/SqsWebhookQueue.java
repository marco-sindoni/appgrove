package app.appgrove.core.billing;

import app.appgrove.commons.aws.AwsClientCredentials;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Coda webhook su SQS (in dev = ElasticMQ, #11; in prod = SQS reale, endpoint da config UC 0025/infra).
 * Attiva fuori dal profilo {@code test} (dove la rimpiazza una coda in-memory). Il client è AWS SDK v2
 * con HTTP sync url-connection (leggero); l'URL della coda è risolto in lazy alla prima operazione, così
 * l'avvio non dipende dalla raggiungibilità di ElasticMQ.
 *
 * <p>Profilo cloud (UC 0005): senza endpoint override le credenziali vengono dalla catena di default
 * (task role IAM) e il nome coda è prefissato con {@code appgrove.sqs.queue-prefix}
 * (← env {@code APPGROVE_SQS_QUEUE_PREFIX}), come {@code SqsMessageQueues} del commons.
 */
@ApplicationScoped
@UnlessBuildProfile("test")
public class SqsWebhookQueue implements WebhookQueue {

    private final String endpoint;
    private final String region;
    private final String queueName;
    private SqsClient client;
    private volatile String queueUrl;

    public SqsWebhookQueue(
            @ConfigProperty(name = "appgrove.sqs.endpoint") Optional<String> endpoint,
            @ConfigProperty(name = "appgrove.sqs.region", defaultValue = "eu-south-1") String region,
            @ConfigProperty(name = "appgrove.sqs.queue-prefix", defaultValue = "") String queuePrefix,
            @ConfigProperty(name = "appgrove.payments.webhook-queue-name") String queueName) {
        this.endpoint = endpoint.orElse(null);
        this.region = region;
        this.queueName = queuePrefix + queueName;
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

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }

    private String queueUrl() {
        if (queueUrl == null) {
            queueUrl = client.getQueueUrl(b -> b.queueName(queueName)).queueUrl();
        }
        return queueUrl;
    }

    @Override
    public void send(String body) {
        client.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl()).messageBody(body).build());
    }

    @Override
    public List<Message> receive(int max) {
        return client
                .receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl())
                        .maxNumberOfMessages(max)
                        .waitTimeSeconds(0)
                        .build())
                .messages()
                .stream()
                .map(m -> new Message(m.receiptHandle(), m.body()))
                .toList();
    }

    @Override
    public void delete(Message message) {
        client.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl())
                .receiptHandle(message.handle())
                .build());
    }
}
