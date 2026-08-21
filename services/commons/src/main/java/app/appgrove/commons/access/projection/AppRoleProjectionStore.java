package app.appgrove.commons.access.projection;

import app.appgrove.commons.access.AppRole;
import app.appgrove.commons.projection.LocalProjection;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Accesso alla <b>copia locale del ruolo</b> della persona su questa applicazione, nello schema del
 * servizio (UC 0099). È la <b>fotocopia</b> della copia locale dei diritti d'accesso
 * ({@code EntitlementProjectionStore}): stesso schema di invalidazione, stessa scrittura via JDBC diretto,
 * stessa postura «configurazione assente = copia inerte». Non si inventa un secondo meccanismo: si riusa
 * la forma di quello che funziona.
 *
 * <p><b>Una differenza voluta, e una sola: qui la copia scade.</b> La copia dei diritti d'accesso non ha
 * soglia di scadenza, e con ragione: un abbonamento cambia di rado ed è l'<b>evento</b> — non il tempo —
 * a dire che qualcosa è cambiato. Un ruolo su una applicazione cambia molto più spesso e la conseguenza di
 * un evento perso è diversa: non un cliente pagante bloccato, ma un <b>permesso revocato che sopravvive</b>.
 * La durata massima ({@code appgrove.app-role.projection.max-age}) è la rete che tiene anche quando il
 * canale degli eventi è rotto — cioè l'unico caso in cui l'invalidazione, da sola, non protegge nulla.
 *
 * <p><b>Cosa contiene, e cosa non contiene.</b> La riga è indicizzata sull'<b>identificativo di
 * autenticazione</b> della persona ({@code sub} del token verificato): nessuna email, nessun nome. È la
 * copia di un dato già dichiarato nel manifesto della piattaforma ({@code app_access.identity_id}), e come
 * ogni copia locale viene <b>cancellata fisicamente</b> quando l'account esercita il diritto di
 * cancellazione — vedi {@link LocalProjection}.
 *
 * <p><b>Invarianti.</b> In lettura l'account arriva dal JWT verificato (#1) e vincola ogni {@code where}
 * (#2); in scrittura dal contenuto dell'evento pubblicato dal core, mai da un input di un client.
 */
@ApplicationScoped
public class AppRoleProjectionStore implements LocalProjection {

    /** Identificatore SQL qualificato: la configurazione non può iniettare SQL arbitrario. */
    private static final Pattern TABLE_NAME = Pattern.compile("[a-z_][a-z0-9_]*(\\.[a-z_][a-z0-9_]*)?");

    @Inject
    AgroalDataSource ds;

    @ConfigProperty(name = "appgrove.app-role.projection.table")
    Optional<String> table;

    @Override
    public String name() {
        return "app_role_projection";
    }

    @Override
    public boolean enabled() {
        return table.isPresent();
    }

    private String table() {
        String name = table.orElseThrow(
                () -> new IllegalStateException("config appgrove.app-role.projection.table mancante"));
        if (!TABLE_NAME.matcher(name).matches()) {
            throw new IllegalStateException(
                    "appgrove.app-role.projection.table non è un identificatore valido: " + name);
        }
        return name;
    }

    /**
     * Riga della copia per quella persona su quella applicazione, se presente.
     *
     * <p>{@code role} è {@code null} quando la copia registra un <b>diniego noto</b> (la persona non ha
     * accesso): è un'informazione utile quanto il permesso, e distinguerla dall'assenza di riga evita di
     * rifare la chiamata di rete a ogni richiesta di chi non ha accesso.
     */
    public Optional<ProjectedAppRole> find(String tenantId, String subject, String appSlug) {
        String sql = "select role, stale, refreshed_at from " + table()
                + " where tenant_id = ? and subject = ? and app_slug = ?";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, subject);
            ps.setString(3, appSlug);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Timestamp refreshed = rs.getTimestamp("refreshed_at");
                return Optional.of(new ProjectedAppRole(
                        AppRole.parse(rs.getString("role")).orElse(null),
                        rs.getBoolean("stale"),
                        refreshed == null ? Instant.EPOCH : refreshed.toInstant()));
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "lettura della copia locale del ruolo fallita per l'account " + tenantId, e);
        }
    }

    /**
     * Registra l'esito di un rinfresco: {@code role} valorizzato = accesso con quel ruolo; {@code null} =
     * diniego noto. In entrambi i casi la riga torna <b>fresca</b>.
     */
    public void save(String tenantId, String subject, String appSlug, AppRole role) {
        String sql = "insert into " + table()
                + " (tenant_id, subject, app_slug, role, stale, refreshed_at, invalidated_at)"
                + " values (?, ?, ?, ?, false, ?, null)"
                + " on conflict (tenant_id, subject, app_slug) do update set"
                + "   role = excluded.role,"
                + "   stale = false,"
                + "   refreshed_at = excluded.refreshed_at,"
                + "   invalidated_at = null";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            ps.setString(2, subject);
            ps.setString(3, appSlug);
            ps.setString(4, role == null ? null : role.name());
            ps.setTimestamp(5, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "scrittura della copia locale del ruolo fallita per l'account " + tenantId, e);
        }
    }

    /**
     * Marca come <b>da rinfrescare</b> tutte le righe dell'account — comprese quelle delle altre persone.
     * È volutamente grossolano: il messaggio di invalidazione è sottile e identifica il solo account, e
     * invalidare più del necessario è innocuo (il rinfresco è pigro e per persona), mentre invalidare meno
     * del necessario è un permesso che sopravvive.
     */
    @Override
    public int markStale(String tenantId) {
        String sql = "update " + table() + " set stale = true, invalidated_at = ? where tenant_id = ?";
        try (Connection c = ds.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, tenantId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "invalidazione della copia locale del ruolo fallita per l'account " + tenantId, e);
        }
    }

    @Override
    public int purgeTenant(String tenantId) {
        try (Connection c = ds.getConnection();
                PreparedStatement ps =
                        c.prepareStatement("delete from " + table() + " where tenant_id = ?")) {
            ps.setString(1, tenantId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("purga della copia locale del ruolo fallita per l'account " + tenantId, e);
        }
    }

    /**
     * Stato copiato del ruolo di una persona su una applicazione.
     *
     * @param role ruolo noto, o {@code null} se la copia registra un diniego
     * @param stale {@code true} se un evento ha invalidato la riga e serve un rinfresco
     * @param refreshedAt ultimo rinfresco riuscito (da cui si misura la scadenza)
     */
    public record ProjectedAppRole(AppRole role, boolean stale, Instant refreshedAt) {

        /** La riga è usabile senza rinfresco? Non marcata da rinfrescare e non più vecchia di {@code maxAge}. */
        public boolean usable(Duration maxAge, Instant now) {
            if (stale) {
                return false;
            }
            if (maxAge == null || maxAge.isZero() || maxAge.isNegative()) {
                return false;
            }
            return !refreshedAt.plus(maxAge).isBefore(now);
        }
    }
}
