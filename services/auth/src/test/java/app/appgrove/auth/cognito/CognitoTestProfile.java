package app.appgrove.auth.cognito;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

/** Profilo test del provider Cognito: porta selezionata via config, client AWS mockati. */
public class CognitoTestProfile implements QuarkusTestProfile {

    static final String CLIENT_ID = "test-client-id";
    static final String CLIENT_SECRET = "test-client-secret";
    static final String USER_POOL_ID = "eu-west-1_testpool";

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "auth.provider", "cognito",
                "auth.cognito.user-pool-id", USER_POOL_ID,
                "auth.cognito.client-id", CLIENT_ID,
                "auth.cognito.client-secret", CLIENT_SECRET);
    }
}
