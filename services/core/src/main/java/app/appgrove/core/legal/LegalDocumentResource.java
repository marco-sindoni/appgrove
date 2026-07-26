package app.appgrove.core.legal;

import app.appgrove.core.legal.LegalDtos.LegalDocView;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.Set;

/**
 * Testo di un documento legale reso in-app (UC 0056): markdown coi token {@code {{titolare.*}}} risolti
 * lato core da {@code content/legal/entity.yaml} (il frontend rende markdown→HTML). Aperto agli autenticati.
 */
@Path("/api/platform/v1/legal")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class LegalDocumentResource {

    private static final Set<String> SUPPORTED_LANGS = Set.of("en", "it", "fr", "es", "de");
    private static final String DEFAULT_LANG = "en";

    @Inject
    LegalContentLoader content;

    @GET
    @Path("/{component}")
    public LegalDocView document(@PathParam("component") String component, @QueryParam("lang") String lang) {
        LegalComponent c = LegalResource.parseComponent(component);
        String resolvedLang = (lang != null && SUPPORTED_LANGS.contains(lang)) ? lang : DEFAULT_LANG;
        return LegalDocView.from(content.load(c, resolvedLang));
    }
}
