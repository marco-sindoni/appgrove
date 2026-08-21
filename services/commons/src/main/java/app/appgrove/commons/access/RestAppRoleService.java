package app.appgrove.commons.access;

import app.appgrove.commons.entitlement.SafetyNet;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Lettura del ruolo dalla <b>fonte di verità</b>: il core, via {@link AppAccessClient}, propagando il JWT
 * del chiamante (invariante #1). Qualificata {@link SafetyNet} perché <b>non</b> è il percorso normale: il
 * bean predefinito è {@link ProjectedAppRoleService}, che legge la copia locale e ricade qui solo quando la
 * copia non basta a decidere (assente, scaduta o invalidata) e quando un'operazione irreversibile chiede
 * esplicitamente di rileggere.
 *
 * <p>{@code @RequestScoped}: l'elenco è <b>memoizzato per richiesta</b>, così più varchi nella stessa
 * richiesta non ripetono la chiamata di rete.
 *
 * <p><b>Un guasto non viene tradotto in un diniego qui.</b> Se il core non risponde, l'eccezione risale:
 * solo il chiamante sa se ha una copia vecchia da usare come ultima verità nota o se non ha nulla e deve
 * negare. Tradurre qui il guasto in «nessun accesso» cancellerebbe quella distinzione — ed è la distinzione
 * fra «non hai i permessi» e «non è colpa tua».
 */
@RequestScoped
@SafetyNet
public class RestAppRoleService implements AppRoleService {

    @Inject
    @RestClient
    AppAccessClient client;

    @Inject
    JsonWebToken jwt;

    private List<MyAppAccessView> cache;

    /** Lettura memoizzata per-richiesta. {@code protected} per i test (override). */
    protected List<MyAppAccessView> myAppAccess() {
        if (cache == null) {
            cache = client.getMyAppAccess("Bearer " + jwt.getRawToken());
        }
        return cache;
    }

    @Override
    public AppRoleOutcome roleOf(String appSlug) {
        List<MyAppAccessView> all = myAppAccess();
        if (all == null) {
            return new AppRoleOutcome.NoAccess();
        }
        return all.stream()
                .filter(entry -> appSlug != null && appSlug.equals(entry.appSlug()))
                .findFirst()
                .flatMap(entry -> AppRole.parse(entry.role()))
                // Voce presente con un ruolo che non sappiamo interpretare: non si concede nulla
                // (fail-closed). Un valore ignoto è un'informazione che non abbiamo, non un permesso.
                .<AppRoleOutcome>map(AppRoleOutcome.Granted::new)
                .orElseGet(AppRoleOutcome.NoAccess::new);
    }

    /** Questa attuazione legge sempre dalla fonte di verità: qui «fresco» non aggiunge nulla. */
    @Override
    public AppRoleOutcome roleFresh(String appSlug) {
        return roleOf(appSlug);
    }
}
