package app.appgrove.fatture;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;

/** Repository fatture. Tenant-scoped automatico (discriminator): nessun filtro manuale su tenant. */
@ApplicationScoped
public class InvoiceRepository implements PanacheRepositoryBase<Invoice, UUID> {

    @Inject
    EntityManager em;

    /** Conteggio fatture create dal tenant corrente da {@code since} (uso quota flow, finestra mensile). */
    public long countCreatedSince(Instant since) {
        return count("createdAt >= ?1", since);
    }

    /**
     * Prossimo numero progressivo per il tenant nell'anno (es. {@code 2026-0001}). Calcolato sul
     * massimo suffisso esistente <b>incluse</b> le fatture soft-deleted (query nativa, niente
     * restriction): la numerazione è monotòna e non riusa numeri liberati da cancellazioni.
     * Il tenant è esplicito (= quello del chiamante) per coerenza con il discriminator.
     */
    public String nextNumber(String tenantId, int year) {
        Number max = (Number) em.createNativeQuery(
                        "select coalesce(max(cast(split_part(number, '-', 2) as int)), 0)"
                                + " from app_fatture.invoice where tenant_id = :t and number like :p")
                .setParameter("t", tenantId)
                .setParameter("p", year + "-%")
                .getSingleResult();
        return String.format("%d-%04d", year, max.intValue() + 1);
    }
}
