package app.appgrove.core.platform;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository delle appartenenze (UC 0116). Tenant-scoped automatico (discriminator): ogni metodo
 * Panache filtra {@code WHERE tenant_id = ?} senza codice manuale.
 *
 * <p>Le due domande della storia vanno tenute distinte:
 * <ul>
 *   <li>«chi sono le persone di questo account?» → {@link #membersOf()}, con il filtro per account;</li>
 *   <li>«a quali account appartiene questa persona?» → {@link #membershipsOf(UUID)}, <b>senza</b>
 *       filtro per account.</li>
 * </ul>
 */
@ApplicationScoped
public class MembershipRepository implements PanacheRepositoryBase<Membership, UUID> {

    @Inject
    EntityManager em;

    /** Le persone dell'account corrente (filtro per account automatico). */
    public List<Membership> membersOf() {
        return listAll();
    }

    /** L'appartenenza dell'account corrente per quell'identità, se esiste. */
    public Optional<Membership> findByIdentity(UUID identityId) {
        return find("identityId", identityId).firstResultOptional();
    }

    /**
     * Gli account a cui appartiene una persona, in ordine di anzianità dell'appartenenza.
     *
     * <p><b>Questa lettura NON porta il filtro per account, ed è corretto così</b>: è la domanda «a
     * quali account appartiene questa persona?», che per costruzione attraversa gli account — non un
     * difetto di isolamento. Per questo usa una query nativa: il discriminator di Hibernate
     * filtrerebbe per l'account corrente, cioè risponderebbe alla domanda sbagliata.
     *
     * <p>È riservata al percorso di accesso (chi compone il token) e alla console di piattaforma.
     * <b>Nessun percorso di account la usa</b>, perché nessuna interfaccia di account deve poter
     * dedurre le altre appartenenze di una persona (UC 0116 §8).
     */
    @SuppressWarnings("unchecked")
    public List<String> tenantsOf(UUID identityId) {
        return em.createNativeQuery(
                        "select tenant_id from platform.membership"
                                + " where identity_id = :id and status = 'active' and deleted_at is null"
                                + " order by created_at, id")
                .setParameter("id", identityId)
                .getResultList();
    }

    /**
     * Un'appartenenza attiva di una persona, con il nome dell'account: quanto serve al selettore
     * dell'account attivo (UC 0117) e nulla di più. Nessun ruolo: l'interfaccia di piattaforma non
     * mostra etichette di ruolo, perché il ruolo è per applicazione (UC 0117 §4.6).
     */
    public record AccountOfIdentity(UUID membershipId, String tenantId, String role, String accountName) {}

    /**
     * Le appartenenze <b>attive</b> di una persona con il nome dell'account, in ordine di anzianità
     * (UC 0117). Stessa natura di {@link #tenantsOf(UUID)}: lettura di <b>piattaforma</b>, senza
     * filtro per account, riservata al percorso di accesso e al selettore della persona stessa —
     * nessuna interfaccia di account la usa.
     *
     * <p><b>Lo stato dell'account NON è un filtro, ed è deliberato.</b> Un account in eliminazione
     * (periodo di grazia, UC 0033) resta selezionabile perché è <i>da dentro</i> quell'account che
     * l'eliminazione si annulla: escluderlo qui vorrebbe dire chiudere fuori la persona proprio
     * quando deve poter tornare indietro. È lo stesso insieme di candidati che vedeva la funzione del
     * token prima di questa storia, quindi nessun accesso che funzionava smette di funzionare.
     */
    @SuppressWarnings("unchecked")
    public List<AccountOfIdentity> activeAccountsOf(UUID identityId) {
        List<Object[]> rows = em.createNativeQuery(
                        "select m.id, m.tenant_id, m.role, a.name"
                                + " from platform.membership m"
                                + " join platform.accounts a on a.id = m.tenant_id::uuid"
                                + " where m.identity_id = :id and m.status = 'active' and m.deleted_at is null"
                                + " and a.deleted_at is null"
                                + " order by m.created_at, m.id")
                .setParameter("id", identityId)
                .getResultList();
        return rows.stream()
                .map(r -> new AccountOfIdentity(
                        (UUID) r[0], (String) r[1], (String) r[2], (String) r[3]))
                .toList();
    }

    /**
     * Esiste già un'appartenenza <b>viva</b> di questa persona a quell'account? Lettura di piattaforma
     * (nessun filtro per account: l'account è un parametro, non il contesto), usata prima di far
     * entrare qualcuno da un percorso d'ingresso — il vincolo
     * {@code ux_membership_tenant_identity} lo impedirebbe comunque, ma con una violazione di indice
     * invece che con un esito comprensibile, ed è esattamente il difetto che UC 0118 chiude.
     */
    public boolean existsIn(String tenantId, UUID identityId) {
        return !em.createNativeQuery(
                        "select 1 from platform.membership"
                                + " where tenant_id = :tenant and identity_id = :id and deleted_at is null")
                .setParameter("tenant", tenantId)
                .setParameter("id", identityId)
                .getResultList()
                .isEmpty();
    }

    /**
     * Crea l'appartenenza di una persona a un account e la restituisce.
     *
     * <p><b>Scrittura di piattaforma, deliberatamente fuori dal discriminatore.</b> L'appartenenza
     * nasce in un account che <b>non è</b> quello della sessione — è il senso stesso di accettare un
     * invito da dentro l'applicazione, o di aprirsi un account nuovo (UC 0118): con l'entità
     * tenant-scoped Hibernate scriverebbe il {@code tenant_id} del token, cioè l'account sbagliato.
     * Per questo il {@code tenant_id} è un parametro <b>esplicito</b>, e chi chiama ha il dovere di
     * averlo ricavato da una riga di invito verificata o da un account appena creato per la persona
     * del token — <b>mai</b> da un valore arrivato dal chiamante.
     */
    public UUID createMembership(String tenantId, UUID identityId, MembershipRole role, String createdBy) {
        UUID id = UUID.randomUUID();
        em.createNativeQuery(
                        "insert into platform.membership"
                                + " (id, tenant_id, identity_id, role, status, created_at, updated_at, created_by)"
                                + " values (:id, :tenant, :identity, :role, 'active', now(), now(), :by)")
                .setParameter("id", id)
                .setParameter("tenant", tenantId)
                .setParameter("identity", identityId)
                .setParameter("role", role.name())
                .setParameter("by", createdBy)
                .executeUpdate();
        return id;
    }
}
