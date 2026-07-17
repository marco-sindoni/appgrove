package app.appgrove.auth.cognito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.quarkus.credentials.CredentialsProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Credenziali del datasource (RDS Proxy) lette da Secrets Manager nel profilo cloud
 * ({@code quarkus.datasource.credentials-provider=aws-secrets}): niente password nelle env della
 * Lambda (#02 15). Lettura a ogni apertura di connessione fisica (rare, pool Agroal) → la
 * rotazione del secret non richiede riavvii.
 */
@Unremovable
@ApplicationScoped
public class SecretsManagerCredentialsProvider implements CredentialsProvider {

    @ConfigProperty(name = "auth.db-secret-arn")
    Optional<String> secretArn;

    @Inject
    Instance<SecretsManagerClient> secrets;

    @Inject
    ObjectMapper mapper;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        String arn = secretArn.orElseThrow(
                () -> new IllegalStateException("Config mancante: auth.db-secret-arn"));
        try {
            String json = secrets.get().getSecretValue(b -> b.secretId(arn)).secretString();
            JsonNode node = mapper.readTree(json);
            return Map.of(
                    USER_PROPERTY_NAME, node.get("username").asText(),
                    PASSWORD_PROPERTY_NAME, node.get("password").asText());
        } catch (Exception e) {
            throw new IllegalStateException("Lettura credenziali DB da Secrets Manager fallita", e);
        }
    }
}
