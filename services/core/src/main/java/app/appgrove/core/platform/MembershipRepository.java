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
}
