package app.appgrove.core.legal;

import app.appgrove.commons.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

/**
 * Versione <b>corrente</b> di un documento legale (UC 0056). PLATFORM-LEVEL (non tenant-scoped):
 * identità del prodotto, non dell'utente → nessun dato personale. Una riga per componente. Scritta
 * dal comando {@code sync-legal} (CI/startup) leggendo il frontmatter di {@code content/legal/}.
 * {@code major} è la prima cifra del semver {@code version}: è la soglia di ri-accettazione.
 */
@Entity
@Table(schema = "platform", name = "legal_version")
@SQLRestriction("deleted_at is null")
public class LegalVersion extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LegalComponent component;

    @Column(nullable = false)
    private int major;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    protected LegalVersion() {
        // richiesto da JPA
    }

    public LegalComponent getComponent() {
        return component;
    }

    public int getMajor() {
        return major;
    }

    public String getVersion() {
        return version;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }
}
