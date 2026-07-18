package app.appgrove.commons.security;

import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Difesa in profondità sull'access token (UC 0016, decisione #02 11). smallrye-jwt verifica
 * già firma ed emittente; questo filtro aggiunge, per TUTTI i servizi, due controlli che la
 * verifica di serie non copre sugli access token Cognito:
 *
 * <ul>
 *   <li><b>{@code token_use = access}</b>: i servizi accettano SOLO access token. Un id token
 *       (firmato dallo stesso pool, {@code token_use=id}) viene rifiutato — è per il client,
 *       non per autorizzare le API.</li>
 *   <li><b>{@code client_id}</b> atteso: gli access token Cognito portano {@code client_id}
 *       (non {@code aud}), quindi la verifica "destinatario" va fatta su quel claim. Attiva solo
 *       se {@code appgrove.auth.expected-client-id} è configurato (in cloud = id del client
 *       confidenziale; in locale = client del provider Local, per parità).</li>
 * </ul>
 *
 * <p>Le richieste anonime (nessun token: endpoint pubblici, webhook) passano oltre — l'accesso
 * è deciso da {@code @RolesAllowed}/{@code @Authenticated}. Il filtro non introduce nuove fonti
 * per {@code tenant_id}/{@code roles}: quelli restano letti dal solo JWT verificato.
 */
@Provider
@ApplicationScoped
public class AccessTokenGuardFilter implements ContainerRequestFilter {

    static final String TOKEN_USE_ACCESS = "access";

    @Inject
    JsonWebToken jwt;

    @Inject
    @ConfigProperty(name = "appgrove.auth.expected-client-id")
    Optional<String> expectedClientId;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String rawToken = jwt == null ? null : jwt.getRawToken();
        if (rawToken == null || rawToken.isBlank()) {
            return; // richiesta anonima: l'autorizzazione la decide @RolesAllowed
        }

        if (!TOKEN_USE_ACCESS.equals(claim("token_use"))) {
            throw new UnauthorizedException("token_use non valido: atteso access token");
        }

        if (expectedClientId.isPresent() && !expectedClientId.get().equals(claim("client_id"))) {
            throw new UnauthorizedException("client_id del token non atteso");
        }
    }

    private String claim(String name) {
        try {
            Object value = jwt.getClaim(name);
            return value == null ? null : value.toString();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
