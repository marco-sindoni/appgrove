package app.appgrove.commons.entitlement.projection;

import app.appgrove.commons.entitlement.EntitlementView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Accesso alla <b>proiezione locale in sola lettura</b> degli entitlement, nello schema del servizio
 * (UC 0046). Sostituisce, sul percorso caldo, la chiamata sincrona app→core introdotta da UC 0027.
 *
 * <p>Scrive e legge via <b>JDBC diretto</b> (come {@code GdprPurgeAuditWriter}/{@code SubscriptionWriter}):
 * il consumer di invalidazione gira <b>fuori</b> da una richiesta autenticata, quindi senza JWT, e il
 * {@code TenantResolver} di Hibernate non potrebbe risolvere il tenant. Il {@code tenant_id} è sempre
 * <b>esplicito</b> nelle query: in lettura arriva dal JWT verificato (invariante #1) e vincola ogni
 * {@code where} (invariante #2); in scrittura dal contenuto dell'evento pubblicato da core.
 *
 * <p>La tabella è indicata da {@code appgrove.entitlement.projection.table}. <b>Config assente = store
 * inerte</b>: i servizi che non sono app di marketplace (core, auth) non hanno la tabella e non devono
 * fallire all'avvio.
 */
@ApplicationScoped
public class EntitlementProjectionStore {

    /** Identificatore SQL qualificato: la config non può iniettare SQL arbitrario. */
    private static final Pattern TABLE_NAME = Pattern.compile("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)?");

    @Inject
    AgroalDataSource ds;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "appgrove.entitlement.projection.table")
    Optional<String> table;

    /** {@code true} se il servizio ha una proiezione configurata (è un'app di marketplace). */
    public boolean enabled() {
        return table.isPresent();
    }

    private String table() {
        String name = table.orElseThrow(
                () -> new IllegalStateException("config appgrove.entitlement.projection.table mancante"));
        if (!TABLE_NAME.matcher(name).matches()) {
            throw new IllegalStateException("appgrove.entitlement.projection.table non è un identificatore valido: " + name);
        }
        return name;
    }

    /**
     * Cancella <b>fisicamente</b> ogni riga di proiezione del tenant: fa parte dell'erasure
     * (#13 L70), non è un dettaglio di cache.
     *
     * <p>Senza questa cancellazione, dopo l'esercizio del diritto di cancellazione sopravvivrebbe in
     * ogni schema applicativo una riga con l'<b>identificativo dell'account</b> e il piano che aveva:
     * una traccia residua di un soggetto che ha chiesto di sparire. Vive qui, in {@code commons},
     * e non nel contratto della singola app, proprio perché la proiezione è infrastruttura condivisa:
     * se ogni app dovesse ricordarsene, prima o poi una se ne dimenticherebbe — e la dimenticanza
     * sarebbe silenziosa.
     *
     * @return righe cancellate (entra nell'audit della purge come prova)
     */
    public int purgeTenant(String tenantId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement("delete from " + table() + " where tenant_id = ?")) {
            ps.setString(1, tenantId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("purge della proiezione entitlement fallita per il tenant " + tenantId, e);
        }
    }

    /**
     * Riga di proiezione per il tenant e l'app indicati, se presente.
     *
     * <p>{@code entitlement} è {@code null} quando la proiezione registra un <b>diniego noto</b>
     * (il tenant non ha accesso): è un'informazione utile quanto il permesso, e distinguerla
     * dall'assenza di riga evita di rifare la chiamata di rete a ogni richiesta di chi non ha accesso.
     */
    public Optional<ProjectedEntitlement> find(String tenantId, String appSlug) {
        String sql = "select entitlement, stale, refreshed_at from " + table()
                + " where tenant_id = ? and app_slug = ?";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, appSlug);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String json = rs.getString("entitlement");
                Timestamp refreshed = rs.getTimestamp("refreshed_at");
                return Optional.of(new ProjectedEntitlement(
                        json == null ? null : read(json),
                        rs.getBoolean("stale"),
                        refreshed == null ? Instant.EPOCH : refreshed.toInstant()));
            }
        } catch (SQLException e) {
            throw new RuntimeException("lettura della proiezione entitlement fallita per il tenant " + tenantId, e);
        }
    }

    /**
     * Registra l'esito di un rinfresco: {@code view} valorizzata = accesso con i suoi tetti;
     * {@code null} = diniego noto. In entrambi i casi la riga torna <b>fresca</b>.
     */
    public void save(String tenantId, String appSlug, EntitlementView view) {
        String sql = "insert into " + table()
                + " (tenant_id, app_slug, entitlement, stale, refreshed_at, invalidated_at)"
                + " values (?, ?, ?::jsonb, false, ?, null)"
                + " on conflict (tenant_id, app_slug) do update set"
                + "   entitlement = excluded.entitlement,"
                + "   stale = false,"
                + "   refreshed_at = excluded.refreshed_at,"
                + "   invalidated_at = null";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, appSlug);
            ps.setString(3, view == null ? null : write(view));
            ps.setTimestamp(4, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("scrittura della proiezione entitlement fallita per il tenant " + tenantId, e);
        }
    }

    /**
     * Marca come <b>da rinfrescare</b> tutte le righe del tenant, senza cancellarle: il valore
     * vecchio resta disponibile come ultima verità nota se al momento del rinfresco core non
     * risponde (postura "cache con rete di sicurezza": un guasto di core non deve mai bloccare un
     * cliente di cui sappiamo qualcosa). Idempotente: rimarcare una riga già marcata non cambia
     * nulla, quindi un messaggio consegnato due volte è innocuo.
     *
     * @return righe marcate (0 = nessuna proiezione per quel tenant: la prima richiesta la creerà)
     */
    public int markStale(String tenantId) {
        String sql = "update " + table() + " set stale = true, invalidated_at = ? where tenant_id = ?";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, tenantId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("invalidazione della proiezione entitlement fallita per il tenant " + tenantId, e);
        }
    }

    private EntitlementView read(String json) {
        try {
            return mapper.readValue(json, EntitlementView.class);
        } catch (Exception e) {
            // Riga illeggibile (formato evoluto, scrittura corrotta): la si tratta come assente
            // invece di far fallire la richiesta — il chiamante ricadrà sulla rete di sicurezza.
            return null;
        }
    }

    private String write(EntitlementView view) {
        try {
            return mapper.writeValueAsString(view);
        } catch (Exception e) {
            throw new IllegalStateException("serializzazione della proiezione entitlement fallita", e);
        }
    }

    /**
     * Stato proiettato di un'app per un tenant.
     *
     * @param view entitlement noto, o {@code null} se la proiezione registra un diniego
     * @param stale {@code true} se un evento ha invalidato la riga e serve un rinfresco
     * @param refreshedAt ultimo rinfresco riuscito (per la misura di scostamento)
     */
    public record ProjectedEntitlement(EntitlementView view, boolean stale, Instant refreshedAt) {

        /** {@code true} se la proiezione registra un accesso concesso. */
        public boolean hasAccess() {
            return view != null;
        }
    }
}
