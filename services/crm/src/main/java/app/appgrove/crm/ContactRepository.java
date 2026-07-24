package app.appgrove.crm;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

/** Repository dei contatti. Tenant-scoped automatico (discriminator): nessun filtro manuale. */
@ApplicationScoped
public class ContactRepository implements PanacheRepositoryBase<Contact, UUID> {

    /**
     * Ricerca per testo (nome o organizzazione, senza distinzione di maiuscole) e/o per stato. Un
     * parametro nullo non filtra: {@code find(null, null)} ritorna tutti i contatti del tenant.
     */
    public io.quarkus.hibernate.orm.panache.PanacheQuery<Contact> search(String text, ContactStage stage) {
        StringBuilder ql = new StringBuilder("1 = 1");
        Parameters params = new Parameters();
        if (text != null && !text.isBlank()) {
            ql.append(" and (lower(displayName) like :q or lower(organization) like :q)");
            params.and("q", "%" + text.toLowerCase() + "%");
        }
        if (stage != null) {
            ql.append(" and stage = :stage");
            params.and("stage", stage);
        }
        return find(ql.toString(), params);
    }
}
