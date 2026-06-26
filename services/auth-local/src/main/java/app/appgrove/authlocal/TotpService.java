package app.appgrove.authlocal;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import jakarta.enterprise.context.ApplicationScoped;

/** 2FA TOTP (authenticator): genera il segreto, l'URI otpauth:// e verifica i codici (#02 dec.18). */
@ApplicationScoped
public class TotpService {

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);

    public String newSecret() {
        return secretGenerator.generate();
    }

    public String otpauthUri(String secret, String accountEmail) {
        QrData data = new QrData.Builder()
                .label(accountEmail)
                .secret(secret)
                .issuer("appgrove")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        return data.getUri();
    }

    public boolean verify(String secret, String code) {
        return secret != null && code != null && verifier.isValidCode(secret, code);
    }
}
