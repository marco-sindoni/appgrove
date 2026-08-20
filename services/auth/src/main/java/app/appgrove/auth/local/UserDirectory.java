package app.appgrove.auth.local;

import app.appgrove.auth.AuthUser;
import app.appgrove.commons.membership.ActiveAccount;
import app.appgrove.commons.membership.ActiveAccount.Candidate;
import app.appgrove.commons.membership.ActiveAccount.Choice;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lettura delle persone dallo schema {@code platform} via JDBC diretto. Il login è <b>pre-tenant</b>:
 * NON si usa l'entità {@code Membership} tenant-scoped del core (il discriminator richiederebbe un
 * tenant già noto). Replica la lettura DB del Pre-Token-Gen (#02 dec.9).
 *
 * <p>Dopo UC 0116 la lettura è identità ⋈ appartenenze: si cerca la <b>persona</b> (unica globalmente
 * per indirizzo e per identificativo di autenticazione) e si guardano <b>tutte</b> le sue appartenenze
 * vive. Con UC 0117 quale di esse diventa l'account della sessione non è più un ripiego («la più
 * antica») ma la regola condivisa {@link ActiveAccount#choose}, applicata al riferimento conservato in
 * {@code identity.active_membership_id}. <b>Il valore conservato non è creduto</b>: vale solo se
 * corrisponde a un'appartenenza attiva trovata adesso.
 *
 * <p><b>Parità col Pre-Token-Gen</b> (infra/modules/platform_shared/lambda/pre_token_gen/handler.py):
 * stessa tabella di casi, attuata in Java qui e in Python là, con la stessa tabella di casi eseguita
 * dai collaudi di entrambe. Se una delle due implementazioni cambia, l'altra cambia con essa —
 * altrimenti i collaudi locali dicono una cosa e l'ambiente reale un'altra.
 *
 * <p>Lo stato risultante è {@code suspended} se lo è una qualsiasi delle due — a chiusura in caso di
 * dubbio: la sospensione della persona (limitazione del trattamento) e quella decisa dall'owner del
 * singolo account valgono entrambe.
 */
@ApplicationScoped
public class UserDirectory {

    /**
     * Una riga per <b>appartenenza viva</b> della persona (non solo per quelle attive: le sospese
     * servono a distinguere «persona sospesa» da «persona che non esiste», che è la differenza fra un
     * rifiuto e un silenzio nei flussi che partono da un indirizzo).
     */
    private static final String SELECT =
            "select i.cognito_sub, i.email, i.display_name, i.locale, i.status as identity_status,"
                    + " i.active_membership_id,"
                    + " m.id as membership_id, m.tenant_id, m.role, m.status as membership_status"
                    + " from platform.identity i"
                    + " join platform.membership m on m.identity_id = i.id and m.deleted_at is null"
                    + " where %s and i.deleted_at is null"
                    + " order by m.created_at, m.id";

    @Inject
    AgroalDataSource ds;

    /**
     * Esito della risoluzione di una persona.
     *
     * @param user la persona con l'account della sessione già scelto ({@code tenantId}/{@code role}
     *     nulli soltanto quando {@code mustChooseAccount} è vero)
     * @param mustChooseAccount vero quando la persona ha più appartenenze attive e nessuna scelta
     *     valida: <b>nessun token</b> può essere emesso, e la richiesta va rifiutata dicendo cosa
     *     fare invece di «credenziali non valide»
     */
    public record Resolution(AuthUser user, boolean mustChooseAccount) {}

    public Optional<AuthUser> findByEmail(String email) {
        return resolveByEmail(email).map(Resolution::user);
    }

    public Optional<AuthUser> findBySub(String sub) {
        return resolveBySub(sub).map(Resolution::user);
    }

    public Optional<Resolution> resolveByEmail(String email) {
        return resolve("lower(i.email) = lower(?)", email);
    }

    public Optional<Resolution> resolveBySub(String sub) {
        return resolve("i.cognito_sub = ?", sub);
    }

    private Optional<Resolution> resolve(String condition, String value) {
        String sql = SELECT.formatted(condition);
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return decide(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<Resolution> decide(ResultSet rs) throws SQLException {
        String sub = null;
        String email = null;
        String displayName = null;
        String locale = null;
        boolean identitySuspended = false;
        UUID stored = null;
        List<Candidate> active = new ArrayList<>();
        List<Candidate> live = new ArrayList<>();

        while (rs.next()) {
            if (sub == null) {
                sub = rs.getString("cognito_sub");
                email = rs.getString("email");
                displayName = rs.getString("display_name");
                locale = rs.getString("locale");
                identitySuspended = "suspended".equals(rs.getString("identity_status"));
                stored = rs.getObject("active_membership_id", UUID.class);
            }
            Candidate candidate = new Candidate(
                    rs.getObject("membership_id", UUID.class), rs.getString("tenant_id"), rs.getString("role"));
            live.add(candidate);
            // La persona sospesa non ha appartenenze CANDIDATE: nessun account è utilizzabile da lei,
            // qualunque cosa dica il valore conservato (a chiusura in caso di dubbio).
            if (!identitySuspended && "active".equals(rs.getString("membership_status"))) {
                active.add(candidate);
            }
        }

        if (sub == null) {
            return Optional.empty(); // nessuna persona con quell'indirizzo/identificativo
        }

        Choice choice = ActiveAccount.choose(active, stored);
        if (choice instanceof Choice.Chosen chosen) {
            return Optional.of(new Resolution(
                    user(sub, chosen.candidate().tenantId(), chosen.candidate().role(),
                            "active", email, displayName, locale),
                    false));
        }
        if (choice instanceof Choice.MustChoose) {
            // Nessun account nel token: la persona esiste e può lavorare, ma nessuno può decidere al
            // suo posto per conto di chi. `tenantId`/`role` restano nulli DI PROPOSITO — chi riceve
            // questo esito non deve poter costruire una sessione per errore.
            return Optional.of(new Resolution(user(sub, null, null, "active", email, displayName, locale), true));
        }
        // Choice.None — nessuna appartenenza attiva. Se ne esiste una viva ma sospesa (o è sospesa la
        // persona) si risponde con quella e lo stato `suspended`, così i flussi che partono da un
        // indirizzo distinguono «sospesa» da «non esiste», come prima di questa storia.
        if (live.isEmpty()) {
            return Optional.empty();
        }
        Candidate oldest = live.get(0);
        return Optional.of(new Resolution(
                user(sub, oldest.tenantId(), oldest.role(), "suspended", email, displayName, locale), false));
    }

    private static AuthUser user(
            String sub, String tenantId, String role, String status,
            String email, String displayName, String locale) {
        return new AuthUser(sub, tenantId, role, status, email, displayName, locale);
    }
}
