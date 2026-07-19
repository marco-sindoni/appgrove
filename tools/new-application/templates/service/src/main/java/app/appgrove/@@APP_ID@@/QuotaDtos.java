package app.appgrove.@@APP_ID@@;

/** DTO di sola lettura per lo stato di quota di una metrica (banner consumo/limite nel modulo frontend). */
public final class QuotaDtos {

    private QuotaDtos() {}

    /**
     * Stato di quota della metrica nella finestra corrente. {@code limit}/{@code remaining} sono
     * <b>nullable</b>: {@code null} = nessun tetto applicato (illimitato). Quando il tetto esiste,
     * {@code remaining = max(0, limit - used)}.
     */
    public record QuotaStatusView(String metric, long used, Long limit, Long remaining) {

        /** Costruisce la vista dal {@code used} corrente e dal tetto grezzo ({@code cap < 0} = illimitato). */
        public static QuotaStatusView of(String metric, long used, long cap) {
            if (cap < 0) {
                return new QuotaStatusView(metric, used, null, null);
            }
            return new QuotaStatusView(metric, used, cap, Math.max(0, cap - used));
        }
    }
}
