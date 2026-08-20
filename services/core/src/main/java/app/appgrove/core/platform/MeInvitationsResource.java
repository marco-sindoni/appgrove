package app.appgrove.core.platform;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.core.platform.InvitationRepository.InviteForIdentity;
import app.appgrove.core.platform.MeDtos.MyInvitationView;
import app.appgrove.core.platform.MeDtos.MyInvitationsView;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Gli inviti indirizzati alla <b>persona in sessione</b>, e la sua risposta (UC 0118).
 *
 * <p><b>Perché l'accettazione sta qui e non nel percorso di accesso.</b> Chi non ha ancora
 * un'identità accetta l'invito dal collegamento ricevuto per posta, e lì nasce una persona nuova:
 * quello è un percorso di autenticazione ({@code /api/auth/invitations/accept}). Chi ha già
 * un'identità non deve creare nulla — deve solo <b>dare un consenso</b>, e per farlo si autentica
 * come sempre. Chiedergli una parola d'accesso nuova sarebbe fabbricare una seconda identità
 * mascherata, che è il difetto che si paga più tardi in assistenza.
 *
 * <p><b>Perché queste letture attraversano gli account.</b> «Chi mi ha invitato?» ha per soggetto la
 * persona, non l'account: per costruzione attraversa gli account, come «a quali account appartengo?»
 * di UC 0117. Il perimetro è sempre l'identità del {@code sub} del token — e l'indirizzo con cui si
 * cerca è <b>quello dell'identità</b>, mai uno che arrivi dal chiamante: è la riga che impedisce a
 * questa lettura di diventare il modo di sapere chi è stato invitato da chi.
 *
 * <p><b>L'invito non è trasferibile.</b> Si accetta solo se l'indirizzo dell'invito coincide con
 * quello dell'identità autenticata: altrimenti un invito inoltrato a un'altra persona funzionerebbe.
 * Ogni invito che non è proprio, revocato, scaduto o già chiuso risponde «non trovato» —
 * indistinguibile da «non esiste», perché l'esistenza di un invito di un'altra azienda non è
 * un'informazione di chi chiede.
 */
@Path("/api/platform/v1/me/invitations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class MeInvitationsResource {

    private static final Logger LOG = Logger.getLogger(MeInvitationsResource.class);

    @Inject
    IdentityRepository identities;

    @Inject
    MembershipRepository memberships;

    @Inject
    InvitationRepository invitations;

    @Inject
    CallerContext caller;

    @Inject
    AuditLogger audit;

    /**
     * Gli inviti in attesa indirizzati alla persona in sessione, con il nome dell'azienda che invita.
     *
     * <p>Il nome dell'account non è un ornamento: è ciò che rende il consenso consapevole. Un invito
     * senza il nome di chi invita è un consenso alla cieca.
     */
    @GET
    public MyInvitationsView list() {
        Identity identity = currentIdentity();
        return new MyInvitationsView(invitations.pendingFor(identity.getEmail()).stream()
                .map(i -> new MyInvitationView(i.id().toString(), i.tenantId(), i.accountName(), i.expiresAt()))
                .toList());
    }

    /**
     * Accetta l'invito: nasce <b>solo l'appartenenza</b> nell'account che ha invitato, e quella
     * appartenenza diventa l'<b>account attivo</b> della persona — si è appena detto di volerci
     * andare, ed è lì che si atterra (conseguenza operativa di UC 0117, che pretende che ogni
     * appartenenza nuova imposti l'account attivo: senza questa riga la persona si troverebbe, al
     * prossimo accesso, con più appartenenze e nessuna scelta).
     *
     * <p>La chiusura dell'invito avviene <b>prima</b> della creazione dell'appartenenza, e non per
     * gusto dell'ordine: la scrittura è condizionata a {@code status = 'pending'}, quindi due
     * richieste simultanee vedono una sola vincitrice e la seconda non arriva mai a creare
     * un'appartenenza doppia. L'ordine inverso avrebbe lasciato quella corsa aperta.
     */
    @POST
    @Path("/{id}/accept")
    @Transactional
    public Response accept(@PathParam("id") UUID id) {
        Identity identity = currentIdentity();
        InviteForIdentity invite = invitations
                .pendingFor(identity.getEmail(), id)
                .orElseThrow(() -> new NotFoundException("Invito non trovato"));

        boolean alreadyMember = memberships.existsIn(invite.tenantId(), identity.getId());
        if (invitations.close(id, InvitationStatus.accepted, identity.getId()) != 1) {
            // Qualcun altro l'ha chiuso fra la lettura e la scrittura (revoca, o una seconda
            // richiesta della stessa persona): non c'è più nulla da accettare.
            throw new NotFoundException("Invito non trovato");
        }

        if (alreadyMember) {
            // Caso reale e non patologico: invitata, entrata per un'altra via, poi accetta l'invito
            // rimasto. L'invito si chiude — è la verità — ma nessuna appartenenza in più nasce, e
            // l'account attivo non si tocca: non è questo l'atto con cui la persona ha scelto dove
            // lavorare. Il vincolo ux_membership_tenant_identity l'avrebbe fermata comunque, ma con
            // una violazione di indice invece di un esito comprensibile.
            LOG.infof("invitation.accepted.already_member user_id=%s tenant_id=%s", identity.getId(), invite.tenantId());
            return Response.noContent().build();
        }

        UUID membershipId = memberships.createMembership(
                invite.tenantId(), identity.getId(), MembershipRole.valueOf(invite.role()), caller.subject());
        identity.setActiveMembershipId(membershipId);

        LOG.infof("invitation.accepted user_id=%s tenant_id=%s invitation_id=%s",
                identity.getId(), invite.tenantId(), id);
        // Soli identificativi opachi: MAI l'indirizzo dell'invitato, come già `member.invited`.
        audit.success("member.invitation.accepted", Map.of(
                "invitation_id", id.toString(),
                "tenant_id", invite.tenantId(),
                "user_id", identity.getId().toString(),
                "actor", caller.subject()));
        return Response.noContent().build();
    }

    /**
     * Rifiuta l'invito: si chiude come {@code rejected} e il posto si libera. Stato distinto da
     * {@code revoked}, perché revocare è l'atto di chi ha invitato e rifiutare è l'atto della persona
     * invitata: la storia dell'invito deve poter dire chi l'ha chiuso.
     */
    @POST
    @Path("/{id}/reject")
    @Transactional
    public Response reject(@PathParam("id") UUID id) {
        Identity identity = currentIdentity();
        InviteForIdentity invite = invitations
                .pendingFor(identity.getEmail(), id)
                .orElseThrow(() -> new NotFoundException("Invito non trovato"));
        if (invitations.close(id, InvitationStatus.rejected, null) != 1) {
            throw new NotFoundException("Invito non trovato");
        }
        LOG.infof("invitation.rejected user_id=%s tenant_id=%s invitation_id=%s",
                identity.getId(), invite.tenantId(), id);
        audit.success("member.invitation.rejected", Map.of(
                "invitation_id", id.toString(),
                "tenant_id", invite.tenantId(),
                "user_id", identity.getId().toString(),
                "actor", caller.subject()));
        return Response.noContent().build();
    }

    /**
     * La persona del token. Il {@code sub} è l'unica chiave ammessa: mai un identificativo che arrivi
     * dal chiamante, altrimenti questa interfaccia diventerebbe il modo di leggere — e rispondere —
     * agli inviti di qualcun altro.
     */
    private Identity currentIdentity() {
        return identities.findByCognitoSub(caller.subject())
                .orElseThrow(() -> new NotFoundException("Profilo utente non trovato"));
    }
}
