package app.appgrove.core.legal;

import app.appgrove.core.legal.LegalDtos.AcceptRequest;
import app.appgrove.core.legal.LegalDtos.LegalStatusView;
import app.appgrove.core.platform.CallerContext;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * (Ri-)accettazione legale runtime dell'utente corrente (UC 0056). Aperta a qualunque ruolo autenticato:
 * l'accettazione è dell'utente, non del ruolo. Tenant/utente derivati <b>solo dal JWT</b>
 * ({@link CallerContext}); il log è tenant-scoped (discriminator). Root {@code /me/legal} distinto da
 * {@code MeResource} ({@code /me}) per non sovrapporsi nel routing.
 */
@Path("/api/platform/v1/me/legal")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LegalResource {

    private static final Logger LOG = Logger.getLogger(LegalResource.class);

    @Inject
    LegalService legal;

    @Inject
    CallerContext caller;

    // Commit dei legali su cui si accetta (prod lo imposta al deploy); opzionale in locale/test.
    @ConfigProperty(name = "appgrove.legal.commit-hash")
    Optional<String> commitHash;

    /** Stato di (ri-)accettazione derivato per il chiamante (pending = blocco, notices = minor). */
    @GET
    @Path("/status")
    public LegalStatusView status() {
        return LegalStatusView.from(legal.statusFor(caller.subject()));
    }

    /** Registra l'accettazione/presa d'atto dei componenti indicati (alle versioni correnti). Idempotente. */
    @POST
    @Path("/acceptance")
    public LegalStatusView accept(@Valid AcceptRequest body) {
        List<LegalComponent> components = body.components().stream()
                .map(LegalResource::parseComponent)
                .toList();
        LOG.infof("legal.accept tenant_id=%s user_id=%s components=%s",
                caller.tenantId(), caller.subject(), components);
        String hash = commitHash.filter(s -> !s.isBlank()).orElse(null);
        return LegalStatusView.from(legal.accept(caller.subject(), components, hash));
    }

    static LegalComponent parseComponent(String value) {
        try {
            return LegalComponent.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Componente legale non valido: " + value);
        }
    }
}
