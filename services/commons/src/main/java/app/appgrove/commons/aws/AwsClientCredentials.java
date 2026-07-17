package app.appgrove.commons.aws;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * Selezione delle credenziali dei client AWS (SQS/S3) in base alla presenza dell'endpoint override
 * (UC 0005, "adeguamento cloud dei client"). Endpoint override presente = emulatore locale
 * (ElasticMQ/MinIO, #11) → credenziali statiche di comodo; assente = AWS reale → catena di default
 * dell'SDK, che in ECS risolve il <b>task role IAM</b> del modulo {@code microsaas_app} (nessuna
 * chiave statica nei servizi, #07 26).
 */
public final class AwsClientCredentials {

    private AwsClientCredentials() {}

    public static AwsCredentialsProvider forEndpoint(
            String endpointOverride, String accessKey, String secretKey) {
        return endpointOverride != null
                ? StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
                : DefaultCredentialsProvider.create();
    }
}
