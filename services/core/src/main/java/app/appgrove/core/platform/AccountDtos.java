package app.appgrove.core.platform;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** DTO dell'account. {@code id} = tenant_id; non è mai accettato dal body (anti-override). */
public final class AccountDtos {

    private AccountDtos() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AccountView(UUID id, String name, String status, String paddleCustomerId) {
        public static AccountView from(Account a) {
            return new AccountView(
                    a.getId(), a.getName(), a.getStatus().name(), a.getPaddleCustomerId());
        }
    }

    public record UpdateAccount(@NotBlank @Size(max = 255) String name) {}
}
