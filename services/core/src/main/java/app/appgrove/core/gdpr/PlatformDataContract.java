package app.appgrove.core.gdpr;

import app.appgrove.commons.gdpr.AppDataContract;
import app.appgrove.commons.gdpr.DataManifest;
import app.appgrove.commons.gdpr.DataManifests;
import app.appgrove.commons.gdpr.ExportResult;
import app.appgrove.commons.gdpr.GdprScope;
import app.appgrove.commons.gdpr.PurgeResult;
import app.appgrove.core.billing.BillingTransaction;
import app.appgrove.core.billing.seats.SeatDowngrade;
import app.appgrove.core.billing.seats.SeatDowngradeItem;
import app.appgrove.core.legal.LegalAcceptance;
import app.appgrove.core.newsletter.NewsletterSubscriber;
import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.AppAccess;
import app.appgrove.core.platform.Invitation;
import app.appgrove.core.platform.Membership;
import app.appgrove.core.platform.Identity;
import app.appgrove.core.support.SupportTicket;
import app.appgrove.core.support.SupportTicketMessage;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contratto GDPR della <b>piattaforma</b> (core, UC 0032): i dati personali "di piattaforma" del
 * tenant — account, utenti, inviti. Come {@code FattureDataContract}: JDBC diretto (gira senza JWT,
 * orchestrato via coda), filtro tenant <b>esplicito</b>, manifesto derivato da {@code @PersonalData}.
 *
 * <p>L'export include anche le righe soft-deleted (JDBC bypassa {@code @SQLRestriction}): art. 15 =
 * tutto ciò che è conservato. La purge è <b>fisica</b> (#13 L70) e cancella anche subscription e job
 * di export del tenant (dati derivati); NON tocca {@code gdpr_purge_audit} (la prova dell'erasure,
 * senza dati personali) né {@code webhook_event} (audit di pipeline dichiarato no-PII, UC 0025).
 */
@ApplicationScoped
public class PlatformDataContract implements AppDataContract {

    public static final String APP_ID = "platform";

    /**
     * Gli indirizzi delle persone di <b>questo</b> account (UC 0116): si passa sempre per
     * l'appartenenza, mai per l'identità da sola — è la regola che tiene l'export dentro il confine
     * dell'account. Sotto-interrogazione, non vista: un parametro {@code ?} = il tenant.
     */
    private static final String EMAIL_OF_TENANT =
            "select lower(i.email) from platform.identity i"
                    + " join platform.membership m on m.identity_id = i.id"
                    + " where m.tenant_id = ?";

    @Inject
    AgroalDataSource ds;

    @Override
    public String appId() {
        return APP_ID;
    }

    @Override
    public ExportResult exportData(GdprScope scope) {
        Map<String, List<Map<String, Object>>> entities = new LinkedHashMap<>();
        List<String> steps = List.of(
                "Raccolta account", "Raccolta persone e appartenenze", "Raccolta inviti",
                "Raccolta riduzioni dei posti", "Raccolta ticket di supporto",
                "Raccolta iscrizioni newsletter", "Raccolta accettazioni legali",
                "Raccolta storico pagamenti");

        entities.put("accounts", query(
                "select id, name, status, paddle_customer_id, created_at"
                        + " from platform.accounts where id = ?",
                UUID.fromString(scope.tenantId()),
                "id", "name", "status", "paddle_customer_id", "created_at"));

        // UC 0116 — le persone dell'account si esportano in due entità distinte, perché sono due cose
        // distinte: l'IDENTITÀ (dato di piattaforma: chi è la persona) e l'APPARTENENZA (dato di questo
        // account: che ruolo ha qui). Si parte SEMPRE dall'appartenenza di questo conto e si raggiunge
        // l'identità: così l'export di un account non può contenere nulla di un altro account, e in
        // particolare non rivela a quali altri account quella persona appartenga.
        entities.put("identities", query(
                // `locale` (UC 0018) è un dato personale della persona: va esportato come gli altri,
                // o la portabilità (art. 20) restituirebbe un profilo incompleto.
                //
                // `active_membership_id` (UC 0117) è esportato RISTRETTO a questo account, e la
                // restrizione è la parte importante: il valore grezzo può puntare all'appartenenza
                // della persona in un ALTRO account, e restituirlo qui rivelerebbe l'esistenza di
                // quell'account — esattamente ciò che questo export non deve fare. Il confronto con
                // `m.id` (l'appartenenza di QUESTO account) risponde alla sola domanda lecita: «la
                // persona stava lavorando qui?». Null quando la risposta è no, indistinguibile da
                // «non aveva alcun account attivo».
                "select i.id, i.cognito_sub, i.email, i.display_name, i.locale, i.status, i.created_at,"
                        + " case when i.active_membership_id = m.id then m.id end as active_membership_id"
                        + " from platform.identity i"
                        + " join platform.membership m on m.identity_id = i.id"
                        + " where m.tenant_id = ? order by i.email",
                scope.tenantId(),
                "id", "cognito_sub", "email", "display_name", "locale", "status", "created_at",
                "active_membership_id"));

        entities.put("memberships", query(
                "select id, identity_id, role, status, created_at, deleted_at"
                        + " from platform.membership where tenant_id = ? order by created_at",
                scope.tenantId(),
                "id", "identity_id", "role", "status", "created_at", "deleted_at"));

        // Accessi per applicazione (UC 0098): riga di autorizzazione dell'account, tenant-scoped —
        // nessuna restrizione da applicare, perché non contiene nulla che riguardi altri account.
        entities.put("app_access", query(
                "select aa.id, app.slug as app, aa.identity_id, aa.role, aa.granted_by, aa.created_at, aa.deleted_at"
                        + " from platform.app_access aa"
                        + " join platform.app app on app.id = aa.app_id"
                        + " where aa.tenant_id = ? order by app.slug, aa.created_at",
                scope.tenantId(),
                "id", "app", "identity_id", "role", "granted_by", "created_at", "deleted_at"));

        // `identity_id` è RISTRETTO agli inviti accettati, e la restrizione è la parte importante
        // (UC 0118 §5): a invito accettato quella persona è un membro dell'account, quindi il
        // riferimento non dice nulla di nuovo; su un invito ancora in attesa direbbe che quella
        // persona aveva già un rapporto con la piattaforma — l'informazione che l'invito ha
        // deliberatamente tenuto fuori dalla propria risposta. Stessa forma della restrizione usata
        // per identity.active_membership_id (UC 0117).
        // `seat_charge_ref` (UC 0103) esce SENZA restrizioni, a differenza di `identity_id`: dice che il
        // posto di quella persona è stato pagato e con quale transazione — informazione dell'account che
        // ha pagato, che l'account già conosce dalla propria fatturazione. Non rivela nulla sulla persona
        // invitata che l'account non sappia già, quindi non c'è nulla da tenere fuori.
        entities.put("invitations", query(
                "select id, email, role, status, expires_at, created_at, seat_charge_ref,"
                        + " case when status = 'accepted' then identity_id end as identity_id"
                        + " from platform.invitations where tenant_id = ? order by email",
                scope.tenantId(),
                "id", "email", "role", "status", "expires_at", "created_at", "seat_charge_ref",
                "identity_id"));

        // Riduzione dei posti (UC 0104): la traccia che una persona è stata indicata per la cessazione, e
        // da chi. È un dato dell'account e va esportato senza restrizioni particolari — l'account che
        // esporta è lo stesso che ha deciso la cessazione, quindi non c'è nulla che non sappia già.
        entities.put("seat_reductions", query(
                "select id, execute_at, status, requested_by, executed_at, created_at, deleted_at"
                        + " from platform.seat_downgrade where tenant_id = ? order by created_at",
                scope.tenantId(),
                "id", "execute_at", "status", "requested_by", "executed_at", "created_at", "deleted_at"));

        entities.put("seat_reduction_people", query(
                "select id, downgrade_id, identity_id, created_at, deleted_at"
                        + " from platform.seat_downgrade_item where tenant_id = ? order by created_at",
                scope.tenantId(),
                "id", "downgrade_id", "identity_id", "created_at", "deleted_at"));

        entities.put("support_tickets", query(
                "select id, type, subject, priority, status, due_at, created_at, closed_at"
                        + " from platform.support_ticket where tenant_id = ? order by created_at",
                scope.tenantId(),
                "id", "type", "subject", "priority", "status", "due_at", "created_at", "closed_at"));

        entities.put("support_ticket_messages", query(
                "select id, ticket_id, author, body, created_at"
                        + " from platform.support_ticket_message where tenant_id = ? order by created_at",
                scope.tenantId(),
                "id", "ticket_id", "author", "body", "created_at"));

        // Newsletter (UC 0039): platform-level, collegata al tenant per confronto dell'email (chi si è
        // iscritto con l'email di un utente del tenant). Include gli iscritti anonimi con quella stessa
        // email (signup/sito), non solo il toggle da account.
        entities.put("newsletter_subscribers", query(
                "select id, email, status, locale, origin_channel, confirmed_at, unsubscribed_at, created_at"
                        + " from platform.newsletter_subscriber"
                        + " where lower(email) in (" + EMAIL_OF_TENANT + ")"
                        + " order by email",
                scope.tenantId(),
                "id", "email", "status", "locale", "origin_channel", "confirmed_at", "unsubscribed_at",
                "created_at"));

        entities.put("newsletter_consent_events", query(
                "select ce.id, ce.subscriber_id, ce.event_type, ce.consent_text_version, ce.channel, ce.occurred_at"
                        + " from platform.consent_event ce"
                        + " where ce.subscriber_id in (select ns.id from platform.newsletter_subscriber ns"
                        + "   where lower(ns.email) in (" + EMAIL_OF_TENANT + "))"
                        + " order by ce.occurred_at",
                scope.tenantId(),
                "id", "subscriber_id", "event_type", "consent_text_version", "channel", "occurred_at"));

        // Accettazioni legali (UC 0056): prova di quali documenti l'utente ha accettato/preso atto.
        // Tenant-scoped: fa parte dei dati personali dell'utente (art. 15).
        entities.put("legal_acceptances", query(
                "select id, user_id, component, version, major, act_type, accepted_at"
                        + " from platform.legal_acceptance where tenant_id = ? order by accepted_at",
                scope.tenantId(),
                "id", "user_id", "component", "version", "major", "act_type", "accepted_at"));

        // Storico pagamenti (UC 0096): è ciò che una persona si aspetta di ritrovare in una richiesta di
        // accesso quando chiede "che cosa avete su di me". I documenti fiscali restano del venditore di
        // record (Paddle), che li emette e li conserva: qui si esporta la nostra copia dello storico.
        // Commissione e netto (UC 0071) fanno parte della stessa riga e vanno esportati con essa: sono dati
        // che riguardano quel pagamento, e un'esportazione che ne omette una parte sarebbe incompleta.
        entities.put("billing_transactions", query(
                "select id, app_id, app_tier_id, paddle_transaction_id, status, amount, currency,"
                        + " billing_cycle, receipt_url, billed_at, fee_amount, net_amount, fee_source"
                        + " from platform.billing_transaction where tenant_id = ? order by billed_at",
                scope.tenantId(),
                "id", "app_id", "app_tier_id", "paddle_transaction_id", "status", "amount", "currency",
                "billing_cycle", "receipt_url", "billed_at", "fee_amount", "net_amount", "fee_source"));

        return new ExportResult(APP_ID, steps, entities);
    }

    @Override
    public PurgeResult purgeData(GdprScope scope) {
        // Ordine FK-safe: item → job, poi inviti/subscription/appartenenze, infine l'account (radice).
        // Cancellazione FISICA (erasure #13 L70), atomica sulla singola connessione.
        //
        // UC 0116 — il punto delicato. Cancellare un account cancella le sue APPARTENENZE e i suoi dati,
        // ma NON l'identità di una persona che appartiene anche ad altri account: quella persona è di
        // un'altra azienda tanto quanto di questa, e portarsela via sarebbe cancellare dati di un
        // titolare diverso. Si cancellano quindi solo le identità che restano ORFANE — quelle la cui
        // unica appartenenza era in questo account. Lo stesso vale per ciò che è agganciato alla
        // persona e non all'account: l'iscrizione alla newsletter e la sua prova di consenso.
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            Map<String, Integer> deleted = new LinkedHashMap<>();

            // Le identità orfane, calcolate PRIMA di toccare le appartenenze (dopo non si saprebbe più
            // chi era di questo account). Tabella temporanea che muore col commit: nessuno stato residuo.
            exec(c, "create temp table purge_orphan_identity on commit drop as"
                    + " select i.id, i.email from platform.identity i"
                    + " where i.id in (select m.identity_id from platform.membership m where m.tenant_id = ?)"
                    + "   and not exists (select 1 from platform.membership m2"
                    + "                    where m2.identity_id = i.id and m2.tenant_id <> ?)",
                    scope.tenantId(), scope.tenantId());

            // i ticket referenziano gdpr_export_job (auto-ticket): vanno via prima dei job
            deleted.put("support_ticket_message",
                    delete(c, "delete from platform.support_ticket_message where tenant_id = ?", scope.tenantId()));
            deleted.put("support_ticket",
                    delete(c, "delete from platform.support_ticket where tenant_id = ?", scope.tenantId()));
            deleted.put("gdpr_export_job_item",
                    delete(c, "delete from platform.gdpr_export_job_item where tenant_id = ?", scope.tenantId()));
            deleted.put("gdpr_export_job",
                    delete(c, "delete from platform.gdpr_export_job where tenant_id = ?", scope.tenantId()));
            // Newsletter (UC 0039): legata alla PERSONA per indirizzo, non all'account. Va via solo per
            // le identità orfane: chi resta membro di un altro account resta iscritto, perché il suo
            // consenso non l'aveva dato a questo account. Eventi prima del subscriber (FK).
            deleted.put("consent_event",
                    delete(c, "delete from platform.consent_event where subscriber_id in ("
                            + " select ns.id from platform.newsletter_subscriber ns"
                            + " where lower(ns.email) in (select lower(email) from purge_orphan_identity))"));
            deleted.put("newsletter_subscriber",
                    delete(c, "delete from platform.newsletter_subscriber where lower(email) in ("
                            + " select lower(email) from purge_orphan_identity)"));
            deleted.put("legal_acceptance",
                    delete(c, "delete from platform.legal_acceptance where tenant_id = ?", scope.tenantId()));
            deleted.put("invitations",
                    delete(c, "delete from platform.invitations where tenant_id = ?", scope.tenantId()));
            // Riduzione dei posti (UC 0104): le persone indicate prima dell'atto (chiave esterna), e
            // entrambe prima delle identità, che referenziano.
            deleted.put("seat_downgrade_item",
                    delete(c, "delete from platform.seat_downgrade_item where tenant_id = ?", scope.tenantId()));
            deleted.put("seat_downgrade",
                    delete(c, "delete from platform.seat_downgrade where tenant_id = ?", scope.tenantId()));
            // Storico pagamenti (UC 0096): tabella tenant-scoped, quindi rientra nella cancellazione
            // fisica come le altre — una riga che sopravvive alla purga è un difetto, non una cautela.
            deleted.put("billing_transaction",
                    delete(c, "delete from platform.billing_transaction where tenant_id = ?", scope.tenantId()));
            deleted.put("subscription",
                    delete(c, "delete from platform.subscription where tenant_id = ?", scope.tenantId()));
            // Gli accessi per applicazione (UC 0098) vanno via prima delle appartenenze e delle
            // identità: referenziano l'identità, e la persona rimossa dall'account non conserva
            // permessi su nulla.
            deleted.put("app_access",
                    delete(c, "delete from platform.app_access where tenant_id = ?", scope.tenantId()));
            // Prima le appartenenze di questo account, poi le identità rimaste orfane (FK).
            deleted.put("membership",
                    delete(c, "delete from platform.membership where tenant_id = ?", scope.tenantId()));
            deleted.put("identity",
                    delete(c, "delete from platform.identity where id in ("
                            + " select id from purge_orphan_identity)"));
            deleted.put("accounts",
                    delete(c, "delete from platform.accounts where id = ?", UUID.fromString(scope.tenantId())));
            c.commit();
            return new PurgeResult(APP_ID, deleted);
        } catch (SQLException e) {
            throw new RuntimeException("purge piattaforma fallita per il tenant " + scope.tenantId(), e);
        }
    }

    @Override
    public DataManifest manifest() {
        List<DataManifest.Entry> entries = new ArrayList<>();
        DataManifests.collectPersonalData(Account.class, "accounts", entries);
        DataManifests.collectPersonalData(Identity.class, "identities", entries);
        DataManifests.collectPersonalData(Membership.class, "memberships", entries);
        DataManifests.collectPersonalData(AppAccess.class, "app_access", entries);
        DataManifests.collectPersonalData(Invitation.class, "invitations", entries);
        DataManifests.collectPersonalData(SeatDowngrade.class, "seat_reductions", entries);
        DataManifests.collectPersonalData(SeatDowngradeItem.class, "seat_reduction_people", entries);
        DataManifests.collectPersonalData(SupportTicket.class, "support_tickets", entries);
        DataManifests.collectPersonalData(SupportTicketMessage.class, "support_ticket_messages", entries);
        DataManifests.collectPersonalData(NewsletterSubscriber.class, "newsletter_subscribers", entries);
        DataManifests.collectPersonalData(LegalAcceptance.class, "legal_acceptances", entries);
        DataManifests.collectPersonalData(BillingTransaction.class, "billing_transactions", entries);
        return new DataManifest(APP_ID, entries);
    }

    private static int delete(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    private static void exec(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.execute();
        }
    }

    private List<Map<String, Object>> query(String sql, Object tenantParam, String... columns) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, tenantParam);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> record = new LinkedHashMap<>();
                    for (int i = 0; i < columns.length; i++) {
                        record.put(columns[i], rs.getObject(i + 1));
                    }
                    rows.add(record);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new RuntimeException("export piattaforma fallito per il tenant " + tenantParam, e);
        }
    }
}
