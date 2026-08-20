package app.appgrove.core.platform;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository inviti. Tenant-scoped automatico (discriminator) per le letture dell'account che invita.
 *
 * <p>Le letture <b>della persona invitata</b> (UC 0118) sono di natura opposta e stanno qui sotto,
 * separate e dichiarate: «quali inviti sono indirizzati a me?» ha per soggetto la persona e per
 * costruzione attraversa gli account. Usano query native perché il discriminatore filtrerebbe per
 * l'account della sessione, cioè risponderebbe alla domanda sbagliata — è la stessa ragione, e la
 * stessa forma, di {@link MembershipRepository#activeAccountsOf(UUID)}.
 */
@ApplicationScoped
public class InvitationRepository implements PanacheRepositoryBase<Invitation, UUID> {

    @Inject
    EntityManager em;

    /** True se esiste già un invito pending per quell'email nel tenant corrente. */
    public boolean existsPendingForEmail(String email) {
        return count("lower(email) = ?1 and status = ?2", email.toLowerCase(), InvitationStatus.pending) > 0;
    }

    /**
     * Un invito in attesa indirizzato a una persona, con il nome dell'azienda che la invita.
     *
     * @param accountName serve all'interfaccia per dire «l'azienda X ti invita»: senza il nome, il
     *     consenso che si chiede sarebbe un consenso alla cieca
     */
    public record InviteForIdentity(
            UUID id, String tenantId, String accountName, String role, Instant expiresAt) {}

    /**
     * Gli inviti <b>in attesa e non scaduti</b> indirizzati a quell'indirizzo, in qualunque account,
     * con il nome dell'account che invita.
     *
     * <p><b>Nessun filtro per account, ed è corretto così</b>: è la domanda «chi mi ha invitato?».
     * Riservata al percorso «di me stesso» — il perimetro è l'indirizzo dell'identità del token, mai
     * un indirizzo che arrivi dal chiamante, altrimenti questa lettura diventerebbe il modo di sapere
     * chi è stato invitato da chi.
     *
     * <p>Gli account cancellati sono esclusi: un invito di un'azienda che non c'è più non è un invito.
     * Gli account <b>sospesi o in eliminazione</b> restano invece inclusi, per la stessa ragione per
     * cui restano selezionabili nel selettore (UC 0117): non è questa lettura a decidere se un account
     * è utilizzabile.
     */
    public List<InviteForIdentity> pendingFor(String email) {
        return rows(
                "select i.id, i.tenant_id, a.name, i.role, i.expires_at"
                        + " from platform.invitations i"
                        + " join platform.accounts a on a.id = i.tenant_id::uuid"
                        + " where lower(i.email) = lower(:email) and i.status = 'pending'"
                        + " and i.expires_at > now() and i.deleted_at is null and a.deleted_at is null"
                        + " order by i.created_at, i.id",
                email,
                null);
    }

    /**
     * Un singolo invito in attesa, purché indirizzato <b>proprio a quell'indirizzo</b>.
     *
     * <p>L'identificativo arriva dal chiamante: è un candidato, non un permesso. La condizione
     * sull'indirizzo è ciò che impedisce a un invito di diventare <b>trasferibile</b> (UC 0118 §6) —
     * un invito inoltrato ad altri non vale. Assente = «non è tuo», «non c'è più», «è scaduto» o «non
     * esiste», indistinguibili di proposito.
     */
    public Optional<InviteForIdentity> pendingFor(String email, UUID invitationId) {
        return rows(
                        "select i.id, i.tenant_id, a.name, i.role, i.expires_at"
                                + " from platform.invitations i"
                                + " join platform.accounts a on a.id = i.tenant_id::uuid"
                                + " where i.id = :id and lower(i.email) = lower(:email)"
                                + " and i.status = 'pending' and i.expires_at > now()"
                                + " and i.deleted_at is null and a.deleted_at is null",
                        email,
                        invitationId)
                .stream()
                .findFirst();
    }

    /**
     * Chiude un invito, con lo stato che dice <b>chi</b> l'ha chiuso: {@code accepted} (con l'identità
     * che ha accettato) oppure {@code rejected}. Scrittura di piattaforma: l'invito appartiene a un
     * account che non è quello della sessione, quindi il {@code tenant_id} non si desume dal contesto —
     * la riga è già stata individuata e verificata da {@link #pendingFor(String, UUID)}.
     *
     * <p>La condizione {@code status = 'pending'} nella scrittura non è ridondante: è ciò che rende
     * l'operazione sicura contro due richieste simultanee — la seconda aggiorna zero righe, e chi
     * chiama lo vede.
     */
    public int close(UUID invitationId, InvitationStatus status, UUID acceptedIdentityId) {
        return em.createNativeQuery(
                        "update platform.invitations set status = :status, accepted_user_id = :identity,"
                                + " updated_at = now() where id = :id and status = 'pending'")
                .setParameter("status", status.name())
                .setParameter("identity", acceptedIdentityId)
                .setParameter("id", invitationId)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    private List<InviteForIdentity> rows(String sql, String email, UUID invitationId) {
        var query = em.createNativeQuery(sql).setParameter("email", email);
        if (invitationId != null) {
            query.setParameter("id", invitationId);
        }
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(r -> new InviteForIdentity(
                        (UUID) r[0],
                        (String) r[1],
                        (String) r[2],
                        (String) r[3],
                        (Instant) r[4]))
                .toList();
    }
}
