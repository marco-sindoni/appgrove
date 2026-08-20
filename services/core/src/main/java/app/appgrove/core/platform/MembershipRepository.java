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
}
