package app.appgrove.core.newsletter;

import app.appgrove.core.newsletter.NewsletterDtos.PreferenceRequest;
import app.appgrove.core.newsletter.NewsletterDtos.PreferenceView;
import app.appgrove.core.newsletter.NewsletterDtos.SubscribeRequest;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Identity;
import app.appgrove.core.platform.IdentityRepository;
import io.quarkus.security.Authenticated;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

/**
 * API newsletter (UC 0039). Due nature nello stesso path:
 * <ul>
 *   <li><b>pubblica</b> (senza JWT, come il webhook Paddle): iscrizione dal sito/signup, conferma
 *       double opt-in e disiscrizione one-click — protette da campo esca + limite di frequenza per IP;
 *   <li><b>autenticata</b> ({@code tenant_id}/{@code sub} dal JWT): la preferenza dell'utente loggato.
 * </ul>
 * Le pagine di conferma/disiscrizione, aperte dal browser dall'email, rispondono in HTML localizzato.
 */
@Path("/api/platform/v1/newsletter")
public class NewsletterResource {

    @Inject
    NewsletterService service;

    @Inject
    SubscribeRateLimiter rateLimiter;

    @Inject
    UnsubscribeTokens unsubscribeTokens;

    @Inject
    CallerContext caller;

    @Inject
    IdentityRepository identities;

    // ── pubblico ───────────────────────────────────────────────────────────────

    @POST
    @Path("/subscriptions")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subscribe(
            @Valid SubscribeRequest body,
            @HeaderParam("X-Forwarded-For") String forwardedFor,
            @Context HttpServerRequest request) {
        // Campo esca riempito = bot: si risponde 202 come per un'iscrizione, senza fare nulla.
        if (body.website() != null && !body.website().isBlank()) {
            return Response.accepted().build();
        }
        if (!Boolean.TRUE.equals(body.consent())) {
            throw new BadRequestException("Consenso mancante: il consenso alla newsletter è obbligatorio.");
        }
        if (!rateLimiter.tryAcquire(clientIp(forwardedFor, request), System.currentTimeMillis())) {
            return Response.status(Response.Status.TOO_MANY_REQUESTS).build();
        }
        service.subscribeAnonymous(body.email(), body.locale(), parseAnonymousChannel(body.channel()));
        // Neutra (anti-enumeration): non rivela se l'indirizzo era già noto/confermato.
        return Response.accepted().build();
    }

    @GET
    @Path("/confirm")
    @Produces(MediaType.TEXT_HTML)
    public Response confirm(@QueryParam("token") String token) {
        NewsletterService.ConfirmOutcome outcome = service.confirm(token);
        return htmlPage(outcome.locale(),
                outcome.confirmed() ? MessageKind.confirmed : MessageKind.invalidConfirm);
    }

    @GET
    @Path("/unsubscribe")
    @Produces(MediaType.TEXT_HTML)
    public Response unsubscribe(@QueryParam("sid") String sid, @QueryParam("t") String token) {
        UUID subscriberId = parseUuidOrNull(sid);
        NewsletterService.UnsubscribeOutcome outcome = service.unsubscribeByToken(subscriberId, token);
        return htmlPage(outcome.locale(),
                outcome.done() ? MessageKind.unsubscribed : MessageKind.invalidUnsub);
    }

    // ── autenticato (preferenza dell'utente) ─────────────────────────────────────

    @GET
    @Path("/preference")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public PreferenceView getPreference() {
        return new PreferenceView(service.isSubscribed(callerIdentity().getEmail()));
    }

    @PUT
    @Path("/preference")
    @Authenticated
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public PreferenceView setPreference(@Valid PreferenceRequest body) {
        Identity user = callerIdentity();
        boolean subscribed = service.setPreference(user.getEmail(), body.subscribed(), user.getLocale(), user.getId());
        return new PreferenceView(subscribed);
    }

    // ── helper ─────────────────────────────────────────────────────────────────

    /**
     * Identità del chiamante (indirizzo autoritativo + lingua + provenienza). Dopo UC 0116 il
     * consenso alla newsletter si lega alla <b>persona</b>, non alla sua appartenenza a un account:
     * l'indirizzo e la lingua sono suoi e non cambiano da un account all'altro.
     */
    private Identity callerIdentity() {
        return identities.findByCognitoSub(caller.subject())
                .orElseThrow(() -> new NotFoundException("Utente del chiamante non trovato."));
    }

    private static ConsentChannel parseAnonymousChannel(String raw) {
        if (raw == null || raw.isBlank() || "site".equals(raw)) {
            return ConsentChannel.site;
        }
        if ("signup".equals(raw)) {
            return ConsentChannel.signup;
        }
        throw new BadRequestException("Canale non valido: " + raw);
    }

    private static UUID parseUuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** IP del client: primo elemento di X-Forwarded-For (dietro Caddy/ALB), altrimenti remote address. */
    private static String clientIp(String forwardedFor, HttpServerRequest request) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request != null && request.remoteAddress() != null ? request.remoteAddress().hostAddress() : null;
    }

    private enum MessageKind {
        confirmed, invalidConfirm, unsubscribed, invalidUnsub
    }

    private static Response htmlPage(String locale, MessageKind kind) {
        boolean it = "it".equals(NewsletterEmailRenderer.normalize(locale));
        String title;
        String body;
        Response.Status status = Response.Status.OK;
        switch (kind) {
            case confirmed -> {
                title = it ? "Iscrizione confermata" : "Subscription confirmed";
                body = it ? "Grazie: la tua iscrizione alla newsletter è confermata."
                        : "Thanks — your newsletter subscription is confirmed.";
            }
            case unsubscribed -> {
                title = it ? "Disiscrizione completata" : "Unsubscribed";
                body = it ? "Sei stato disiscritto dalla newsletter. Non riceverai più queste email."
                        : "You have been unsubscribed. You will no longer receive these emails.";
            }
            case invalidUnsub -> {
                title = it ? "Collegamento non valido" : "Invalid link";
                body = it ? "Questo collegamento di disiscrizione non è valido."
                        : "This unsubscribe link is not valid.";
                status = Response.Status.BAD_REQUEST;
            }
            default -> {
                title = it ? "Collegamento non valido o scaduto" : "Invalid or expired link";
                body = it ? "Questo collegamento di conferma non è più valido. Prova a iscriverti di nuovo."
                        : "This confirmation link is no longer valid. Please subscribe again.";
                status = Response.Status.BAD_REQUEST;
            }
        }
        String html = """
                <!doctype html><html lang="%s"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s — appgrove</title>
                <style>body{font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;
                margin:0;display:grid;place-items:center;min-height:100vh;background:#0b0c10;color:#e8eaf0}
                main{max-width:32rem;padding:2.5rem;text-align:center}
                h1{font-size:1.4rem;margin:0 0 .75rem}p{color:#aab;line-height:1.5;margin:0}</style>
                </head><body><main><h1>%s</h1><p>%s</p></main></body></html>
                """.formatted(it ? "it" : "en", esc(title), esc(title), esc(body));
        return Response.status(status).entity(html).build();
    }

    private static String esc(String v) {
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
