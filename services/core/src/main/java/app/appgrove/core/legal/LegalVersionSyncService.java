package app.appgrove.core.legal;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Motore della <b>sync delle versioni legali</b> (UC 0056): legge il frontmatter dei componenti da
 * {@code content/legal/} (bundlato in classpath, lingua IT facente fede) e fa <b>upsert</b> in
 * {@code platform.legal_version} — la fonte di verità delle versioni correnti per la derivazione
 * "accettata &lt; corrente". Idempotente. Scrittura in <b>SQL nativo</b> con id deterministico
 * per-componente: opera <b>fuori da una richiesta</b> (command-mode / startup), niente TenantResolver,
 * come {@code PricingSyncService} (tabella platform-level, non tenant-scoped).
 */
@ApplicationScoped
public class LegalVersionSyncService {

    private static final Logger LOG = Logger.getLogger(LegalVersionSyncService.class);

    private static final String UPSERT =
            """
            insert into platform.legal_version
              (id, component, major, version, effective_date, created_at, updated_at, created_by, updated_by)
            values (?, ?, ?, ?, ?, now(), now(), 'sync-legal', 'sync-legal')
            on conflict (component) do update set
              major = excluded.major, version = excluded.version, effective_date = excluded.effective_date,
              updated_at = now(), updated_by = 'sync-legal', deleted_at = null
            """;

    @Inject
    LegalContentLoader loader;

    @Inject
    AgroalDataSource ds;

    /** Esito della sync: quanti componenti riconciliati. */
    public record Report(int components) {}

    /** Id deterministico per-componente (stabile tra ri-sync). */
    private static UUID versionId(LegalComponent component) {
        return UUID.nameUUIDFromBytes(("legal_version:" + component.name()).getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public Report sync() {
        int n = 0;
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(UPSERT)) {
            for (LegalComponent component : LegalComponent.values()) {
                LegalContentLoader.LegalMeta meta = loader.metaOf(component);
                ps.setObject(1, versionId(component));
                ps.setString(2, component.name());
                ps.setInt(3, meta.major());
                ps.setString(4, meta.version());
                ps.setObject(5, Date.valueOf(meta.effectiveDate()));
                ps.executeUpdate();
                n++;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("sync-legal fallita: " + e.getMessage(), e);
        }
        LOG.infof("sync-legal completata: componenti=%d", n);
        return new Report(n);
    }
}
