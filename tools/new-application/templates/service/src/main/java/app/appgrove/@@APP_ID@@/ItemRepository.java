package app.appgrove.@@APP_ID@@;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;

/** Repository del dominio segnaposto. Tenant-scoped automatico (discriminator): nessun filtro manuale. */
@ApplicationScoped
public class ItemRepository implements PanacheRepositoryBase<Item, UUID> {

    @Inject
    EntityManager em;

    /** Conteggio record creati dal tenant corrente da {@code since} (uso quota flow, finestra mensile). */
    public long countCreatedSince(Instant since) {
        return count("createdAt >= ?1", since);
    }

    /**
     * Prossimo codice progressivo per il tenant nell'anno (es. {@code 2026-0001}). Calcolato sul
     * massimo suffisso esistente <b>incluse</b> le righe soft-deleted (query nativa, niente
     * restriction): la numerazione è monotòna e non riusa codici liberati da cancellazioni.
     * Il tenant è esplicito (= quello del chiamante) per coerenza con il discriminator.
     */
    public String nextCode(String tenantId, int year) {
        Number max = (Number) em.createNativeQuery(
                        "select coalesce(max(cast(split_part(code, '-', 2) as int)), 0)"
                                + " from @@SCHEMA@@.item where tenant_id = :t and code like :p")
                .setParameter("t", tenantId)
                .setParameter("p", year + "-%")
                .getSingleResult();
        return String.format("%d-%04d", year, max.intValue() + 1);
    }
}
