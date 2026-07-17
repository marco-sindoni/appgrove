package app.appgrove.commons.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * Selezione credenziali dei client AWS (UC 0005): endpoint override presente = emulatore locale →
 * credenziali statiche; assente = AWS reale → catena di default (task role IAM in ECS).
 */
class AwsClientCredentialsTest {

    @Test
    void endpointOverridePresenteUsaCredenzialiStatiche() {
        var provider = AwsClientCredentials.forEndpoint("http://localhost:9324", "local", "local");
        assertInstanceOf(StaticCredentialsProvider.class, provider);
        assertEquals(AwsBasicCredentials.create("local", "local"), provider.resolveCredentials());
    }

    @Test
    void senzaEndpointOverrideUsaLaCatenaDiDefault() {
        var provider = AwsClientCredentials.forEndpoint(null, "local", "local");
        assertInstanceOf(DefaultCredentialsProvider.class, provider);
    }
}
