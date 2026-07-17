package app.appgrove.auth.cognito;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * Client AWS del provider Cognito (pattern commons: UrlConnection, catena credenziali di default →
 * ruolo IAM della Lambda; endpoint raggiunti via VPC endpoint, #06 18). Beans lazy: in profilo
 * locale nessuno li usa e non vengono mai istanziati.
 */
@ApplicationScoped
public class CognitoClients {

    @ConfigProperty(name = "auth.cognito.region", defaultValue = "eu-west-1")
    String region;

    @Produces
    @ApplicationScoped
    CognitoIdentityProviderClient cognito() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Produces
    @ApplicationScoped
    SsmClient ssm() {
        return SsmClient.builder()
                .region(Region.of(region))
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Produces
    @ApplicationScoped
    SesV2Client ses() {
        return SesV2Client.builder()
                .region(Region.of(region))
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }

    @Produces
    @ApplicationScoped
    software.amazon.awssdk.services.secretsmanager.SecretsManagerClient secretsManager() {
        return software.amazon.awssdk.services.secretsmanager.SecretsManagerClient.builder()
                .region(Region.of(region))
                .httpClient(UrlConnectionHttpClient.create())
                .build();
    }
}
