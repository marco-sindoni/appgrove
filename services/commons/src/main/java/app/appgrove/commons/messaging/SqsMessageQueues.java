package app.appgrove.commons.messaging;

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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
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
 */
@ApplicationScoped
@UnlessBuildProfile("test")
public class SqsMessageQueues implements MessageQueues {

    private final String endpoint;
    private final String region;
    private final Map<String, String> queueUrls = new ConcurrentHashMap<>();
    private SqsClient client;

    public SqsMessageQueues(
            @ConfigProperty(name = "appgrove.sqs.endpoint") Optional<String> endpoint,
            @ConfigProperty(name = "appgrove.sqs.region", defaultValue = "eu-south-1") String region) {
        this.endpoint = endpoint.orElse(null);
        this.region = region;
    }

    @PostConstruct
    void init() {
        var builder = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")))
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

    private String queueUrl(String queueName) {
        return queueUrls.computeIfAbsent(
                queueName, name -> client.getQueueUrl(b -> b.queueName(name)).queueUrl());
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
