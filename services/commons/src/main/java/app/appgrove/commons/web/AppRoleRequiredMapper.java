package app.appgrove.commons.web;

import app.appgrove.commons.access.AppRoleRequiredException;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Varco del ruolo per applicazione non superato → problem+json col codice di stato del caso (UC 0099):
 * <b>403</b> per «non entri» e per «ruolo insufficiente», <b>503</b> per «non decidibile».
 *
 * <p>Il corpo porta l'identificativo <b>stabile</b> del caso nel campo {@code type} (RFC 9457) e — quando
 * esistono — il ruolo richiesto e quello posseduto come estensioni: l'interfaccia deve poter disabilitare
 * un comando o proporre «chiedi l'abilitazione» senza interpretare una frase in italiano.
 */
@Provider
public class AppRoleRequiredMapper implements ExceptionMapper<AppRoleRequiredException> {

    @Override
    public Response toResponse(AppRoleRequiredException exception) {
        AppRoleRequiredException.Case kind = exception.kind();
        AppRoleProblem problem = new AppRoleProblem(
                kind.type(),
                kind.title(),
                kind.status(),
                exception.getMessage(),
                exception.appSlug(),
                exception.required() == null ? null : exception.required().name(),
                exception.held() == null ? null : exception.held().name());
        return Response.status(kind.status())
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problem)
                .build();
    }

    /**
     * Corpo problem+json del varco, con le estensioni che servono all'interfaccia. Un record dedicato
     * invece di {@link ProblemDetail}: i campi {@code requiredRole}/{@code role} sono estensioni proprie
     * di questo errore e non appartengono al corpo di errore generale della piattaforma.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AppRoleProblem(
            String type,
            String title,
            int status,
            String detail,
            String appSlug,
            String requiredRole,
            String role) {}
}
