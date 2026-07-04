package app.appgrove.commons.storage;

import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * {@link ExportStorage} su S3 (in dev = MinIO, #11; in prod = S3 reale). La cifratura at rest è una
 * proprietà del <b>bucket</b> (SSE di default, UC 0003 — MinIO locale non la applica), non del
 * singolo put; l'auto-cancellazione a 7 giorni degli oggetti è la lifecycle rule del bucket (UC 0003).
 * Path-style forzato con endpoint override (richiesto da MinIO). Attiva fuori dal profilo {@code test}.
 */
@ApplicationScoped
@UnlessBuildProfile("test")
public class S3ExportStorage implements ExportStorage {

    private final String endpoint;
    private final String region;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private S3Client client;
    private S3Presigner presigner;

    public S3ExportStorage(
            @ConfigProperty(name = "appgrove.s3.endpoint") Optional<String> endpoint,
            @ConfigProperty(name = "appgrove.s3.region", defaultValue = "eu-south-1") String region,
            @ConfigProperty(name = "appgrove.gdpr.export-bucket", defaultValue = "gdpr-export") String bucket,
            @ConfigProperty(name = "appgrove.s3.access-key", defaultValue = "local") String accessKey,
            @ConfigProperty(name = "appgrove.s3.secret-key", defaultValue = "local") String secretKey) {
        this.endpoint = endpoint.orElse(null);
        this.region = region;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @PostConstruct
    void init() {
        var credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        var clientBuilder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .httpClient(UrlConnectionHttpClient.create());
        var presignerBuilder = S3Presigner.builder().region(Region.of(region)).credentialsProvider(credentials);
        if (endpoint != null) {
            clientBuilder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
            presignerBuilder.endpointOverride(URI.create(endpoint))
                    .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }
        this.client = clientBuilder.build();
        this.presigner = presignerBuilder.build();
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public byte[] get(String key) {
        return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .asByteArray();
    }

    @Override
    public PresignedLink presignGet(String key, Duration ttl) {
        PresignedGetObjectRequest presigned = presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build());
        return new PresignedLink(presigned.url().toString(), presigned.expiration());
    }
}
