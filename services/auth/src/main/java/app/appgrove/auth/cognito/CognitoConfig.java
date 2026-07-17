package app.appgrove.auth.cognito;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * Config del provider Cognito. Il client secret arriva o direttamente (test) o da SSM Parameter
 * Store a runtime ({@code auth.cognito.client-secret-param}, SecureString — #02 15: zero secret
 * nel codice o nelle env della Lambda), letto in lazy e cachato per la vita dell'istanza.
 */
@ApplicationScoped
public class CognitoConfig {

    // Optional: in profilo locale il provider Cognito non è attivo e la config può mancare.
    @ConfigProperty(name = "auth.cognito.user-pool-id")
    Optional<String> userPoolId;

    @ConfigProperty(name = "auth.cognito.client-id")
    Optional<String> clientId;

    @ConfigProperty(name = "auth.cognito.client-secret")
    Optional<String> clientSecret;

    @ConfigProperty(name = "auth.cognito.client-secret-param")
    Optional<String> clientSecretParam;

    @ConfigProperty(name = "auth.cognito.refresh-ttl-seconds", defaultValue = "86400")
    long refreshTtlSeconds;

    @Inject
    Instance<SsmClient> ssm;

    private final AtomicReference<String> cachedSecret = new AtomicReference<>();

    public String userPoolId() {
        return userPoolId.orElseThrow(() -> new IllegalStateException("Config mancante: auth.cognito.user-pool-id"));
    }

    public String clientId() {
        return clientId.orElseThrow(() -> new IllegalStateException("Config mancante: auth.cognito.client-id"));
    }

    public long refreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public String clientSecret() {
        String cached = cachedSecret.get();
        if (cached != null) {
            return cached;
        }
        String resolved = clientSecret.orElseGet(() -> clientSecretParam
                .map(param -> ssm.get().getParameter(b -> b.name(param).withDecryption(true))
                        .parameter().value())
                .orElseThrow(() -> new IllegalStateException(
                        "Config mancante: auth.cognito.client-secret o auth.cognito.client-secret-param")));
        cachedSecret.set(resolved);
        return resolved;
    }
}
