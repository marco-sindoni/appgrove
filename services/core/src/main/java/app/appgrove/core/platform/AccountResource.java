package app.appgrove.core.platform;

import app.appgrove.core.platform.AccountDtos.AccountView;
import app.appgrove.core.platform.AccountDtos.UpdateAccount;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** API dell'account corrente (tenant). L'account è individuato da {@code id = tenant_id} (dal JWT). */
@Path("/api/platform/v1/accounts")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    @Inject
    AccountRepository repository;

    @Inject
    CallerContext caller;

    @GET
    @Path("/me")
    public AccountView me() {
        return AccountView.from(currentAccount());
    }

    @PATCH
    @Path("/me")
    @RolesAllowed(Roles.OWNER)
    @Transactional
    public AccountView update(@Valid UpdateAccount body) {
        Account account = currentAccount();
        account.setName(body.name());
        return AccountView.from(account);
    }

    private Account currentAccount() {
        Account account = repository.findById(caller.tenantId());
        if (account == null) {
            throw new NotFoundException("Account non trovato");
        }
        return account;
    }
}
