package app.appgrove.core.billing.seats;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository delle versioni del listino dei posti (UC 0102). Di <b>piattaforma</b>: nessun filtro per
 * account, perché il listino non ne ha uno.
 *
 * <p><b>C'è una sola via di lettura, e non è «prendi l'ultima»</b>: è «prendi quella vigente a questa
 * data». La differenza si vede il giorno in cui UC 0105 inserisce una versione con decorrenza futura —
 * cosa che il governo del listino fa per costruzione, perché una tariffa nuova vige dal ciclo successivo:
 * «l'ultima creata» risponderebbe con il listino che non è ancora in vigore, cioè fatturerebbe in
 * anticipo un prezzo non ancora dovuto. Per questo il metodo che prende l'ultima non esiste.
 */
@ApplicationScoped
public class SeatPricingRepository implements PanacheRepositoryBase<SeatPricingVersion, UUID> {

    /**
     * La versione vigente all'istante indicato: quella con la decorrenza più recente fra quelle già
     * decorse. Vuota se nessuna versione era in vigore a quella data.
     *
     * <p>Il secondo criterio di ordinamento (identificativo decrescente) non serve al dominio — due
     * versioni con la stessa decorrenza sono impedite da {@code ux_seat_pricing_version_effective_from} —
     * ma rende la lettura <b>deterministica</b> comunque, e una lettura sul denaro che dipende dall'ordine
     * fisico delle righe è un difetto che si manifesta una volta all'anno.
     */
    public Optional<SeatPricingVersion> findVigenteAl(Instant when) {
        Optional<SeatPricingVersion> version =
                find("effectiveFrom <= ?1 order by effectiveFrom desc, id desc", when).firstResultOptional();
        version.ifPresent(SeatPricingVersion::loadBands);
        return version;
    }

    /**
     * La versione vigente all'istante indicato, o un errore esplicito.
     *
     * <p>È la forma da usare quando si sta per calcolare del denaro: {@link NoSeatPricingVersionException}
     * ferma il calcolo invece di lasciar passare uno zero che nessuno distinguerebbe da un dovuto nullo
     * legittimo (UC 0102 §5).
     */
    public SeatPricingVersion requireVigenteAl(Instant when) {
        return findVigenteAl(when).orElseThrow(() -> new NoSeatPricingVersionException(when));
    }
}
