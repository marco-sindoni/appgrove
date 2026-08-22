package app.appgrove.@@APP_ID@@;

import app.appgrove.commons.access.AppRole;
import app.appgrove.commons.access.AppRoleOutcome;
import app.appgrove.commons.access.AppRoleService;
import app.appgrove.commons.entitlement.SafetyNet;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sta al posto della <b>rete di sicurezza</b> del ruolo per applicazione (la chiamata REST reale a
 * {@code /me/app-access}) nei test: offline e deterministica. Predefinito = ruolo {@code admin}, così i
 * collaudi che non parlano di ruoli continuano a esercitare il dominio e non il varco.
 *
 * <p><b>Perché è qualificata {@link SafetyNet} e non predefinita</b> (stessa ragione di
 * {@link MockEntitlementService}): sostituendo il bean predefinito, i test scavalcherebbero la copia
 * locale — cioè proprio il percorso che gira in produzione. Qualificandola come rete di sicurezza,
 * l'intera suite attraversa {@code ProjectedAppRoleService} → copia locale → (all'occorrenza) questa
 * finta sorgente.
 *
 * <p>{@link #calls} conta gli accessi: è ciò che permette di dimostrare che una copia fresca <b>non</b>
 * genera traffico verso il core. {@link #unreachable} simula il core giù.
 */
@Mock
@SafetyNet
@ApplicationScoped
public class MockAppRoleService implements AppRoleService {

    /** Ruolo restituito; {@code null} = la persona non ha accesso (diniego noto). */
    static volatile AppRole role = AppRole.admin;

    /** {@code true} = core irraggiungibile: ogni accesso alla rete di sicurezza fallisce. */
    static volatile boolean unreachable = false;

    /** Accessi alla rete di sicurezza dall'ultimo {@link #reset()}. */
    static final AtomicInteger calls = new AtomicInteger();

    static void reset() {
        role = AppRole.admin;
        unreachable = false;
        calls.set(0);
    }

    @Override
    public AppRoleOutcome roleOf(String appSlug) {
        calls.incrementAndGet();
        if (unreachable) {
            throw new IllegalStateException("core irraggiungibile (simulato)");
        }
        return role == null ? new AppRoleOutcome.NoAccess() : new AppRoleOutcome.Granted(role);
    }

    @Override
    public AppRoleOutcome roleFresh(String appSlug) {
        return roleOf(appSlug);
    }
}
