package app.appgrove.core.billing.seats;

import java.time.Instant;
import java.util.List;

/** Contratto di rete del listino dei posti (UC 0102 §8). */
public final class SeatPricingDtos {

    private SeatPricingDtos() {}

    /**
     * Il listino vigente: valuta, decorrenza e fasce.
     *
     * <p>Non contiene il dovuto di nessuno: il calcolo del <b>proprio</b> dovuto è di UC 0103, insieme al
     * riquadro dei posti. Qui c'è solo il listino, che è pubblico dentro il prodotto.
     */
    public record SeatPricingView(String currency, Instant effectiveFrom, List<SeatBandView> bands) {}

    /**
     * Una fascia.
     *
     * @param toSeat ultimo posto della fascia; {@code null} per l'ultima, aperta verso l'alto
     */
    public record SeatBandView(int fromSeat, Integer toSeat, int unitPriceCents) {}
}
