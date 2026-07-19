package app.appgrove.core.gdpr;

import app.appgrove.commons.audit.AuditLogger;
import app.appgrove.core.billing.EntitlementInvalidationPublisher;
import app.appgrove.core.platform.Account;
import app.appgrove.core.platform.AccountDtos.AccountView;
import app.appgrove.core.platform.AccountRepository;
import app.appgrove.core.platform.AccountStatus;
import app.appgrove.core.platform.CallerContext;
import app.appgrove.core.platform.Roles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Map;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.ResponseStatus;

/**
 * Eliminazione account self-service con grace 14gg (UC 0033, #13 E25): la richiesta disattiva
 * subito l'account ({@code pending_deletion} → zero entitlement, vedi
 * {@code EntitlementReadModel}) e persiste l'istante; l'hard-purge lo esegue
 * {@link AccountDeletionSweeper} a grace scaduta, ed è <b>annullabile</b> fin lì. Diritto GDPR
 * esente dai gate di enforcement (#09 F31): NIENTE {@code @RequiresEntitlement} (guardia:
 * {@code GdprGateExemptionTest}). OWNER-only: eliminare l'account è un atto dispositivo del
 * titolare dell'account, come le mutazioni billing (UC 0028 §8).
 */
@Path("/api/platform/v1/accounts/me/deletion")
@RolesAllowed(Roles.OWNER)
@Produces(MediaType.APPLICATION_JSON)
public class AccountDeletionResource {

    private static final Logger LOG = Logger.getLogger(AccountDeletionResource.class);

    @Inject
    AccountRepository accounts;

    @Inject
    CallerContext caller;

    @Inject
    AuditLogger audit;

    @Inject
    EntitlementInvalidationPublisher invalidation;

    /** Richiede l'eliminazione: disattivazione immediata + avvio della grace. 409 se già in corso. */
    @POST
    @Transactional
    @ResponseStatus(202)
    public AccountView request() {
        Account account = currentAccount();
        if (account.getStatus() == AccountStatus.pending_deletion) {
            throw new ClientErrorException("Eliminazione già richiesta", Response.Status.CONFLICT);
        }
        account.setStatus(AccountStatus.pending_deletion);
        account.setDeletionRequestedAt(Instant.now());
        LOG.infof("gdpr.account-deletion.request tenant_id=%s user_id=%s effective_at=%s",
                caller.tenantId(), caller.subject(), account.deletionEffectiveAt());
        audit.success("gdpr.account-deletion.requested", Map.of(
                "tenant_id", caller.tenantId().toString(),
                "user_id", caller.subject(),
                "effective_at", account.deletionEffectiveAt().toString()));
        // Disattivazione immediata = zero entitlement (#13 E25): senza invalidazione le app
        // continuerebbero a servire dalla propria proiezione un accesso che non esiste più.
        invalidation.invalidateAllApps(caller.tenantId().toString(), "account.pending_deletion");
        return AccountView.from(account);
    }

    /** Annulla l'eliminazione entro la grace: l'account torna attivo (#13 E25). 409 se non in corso. */
    @DELETE
    @Transactional
    public AccountView cancel() {
        Account account = currentAccount();
        if (account.getStatus() != AccountStatus.pending_deletion) {
            throw new ClientErrorException("Nessuna eliminazione da annullare", Response.Status.CONFLICT);
        }
        account.setStatus(AccountStatus.active);
        account.setDeletionRequestedAt(null);
        LOG.infof("gdpr.account-deletion.cancel tenant_id=%s user_id=%s",
                caller.tenantId(), caller.subject());
        audit.success("gdpr.account-deletion.canceled", Map.of(
                "tenant_id", caller.tenantId().toString(),
                "user_id", caller.subject()));
        // Ripristino dell'accesso: va propagato con la stessa urgenza della revoca, altrimenti il
        // cliente resta bloccato fino alla scadenza naturale della proiezione (che non c'è).
        invalidation.invalidateAllApps(caller.tenantId().toString(), "account.deletion_canceled");
        return AccountView.from(account);
    }

    private Account currentAccount() {
        Account account = accounts.findById(caller.tenantId());
        if (account == null) {
            throw new NotFoundException("Account non trovato");
        }
        return account;
    }
}
