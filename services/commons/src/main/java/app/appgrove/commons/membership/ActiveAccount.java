package app.appgrove.commons.membership;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Scelta dell'account attivo di una sessione (UC 0117): dato l'insieme delle appartenenze
 * <b>attive</b> di una persona e il riferimento conservato in {@code platform.identity
 * .active_membership_id}, dice quale account deve finire nel claim del token — o che non deve
 * finirci nessuno.
 *
 * <p><b>È il cuore della storia, e sta in {@code commons} per una ragione precisa</b>: la regola è
 * usata dal servizio che compone i token in locale ({@code services/auth}) e dal servizio di
 * piattaforma che scrive la scelta ({@code services/core}), che non si vedono fra loro. Il piano di
 * lavoro la collocava in {@code core}: {@code core} però non è raggiungibile da {@code auth}, e il
 * percorso di accesso non deve acquisire una dipendenza di rete verso un altro servizio. {@code
 * commons} è l'unico posto in cui la regola può essere scritta <b>una volta</b> per entrambi.
 *
 * <p><b>Parità con la funzione che compone il token in cloud</b>
 * ({@code infra/modules/platform_shared/lambda/pre_token_gen/handler.py}): quella gira dentro
 * l'infrastruttura e non può chiamare Java, quindi la regola è attuata <b>due volte</b>, qui e in
 * Python. Due attuazioni della stessa regola sono un debito: si tiene onesto con la stessa tabella
 * di casi eseguita su entrambe ({@code ActiveAccountTest} e {@code test_handler.py}). Se una delle
 * due cambia, l'altra cambia con essa — altrimenti i collaudi locali dicono una cosa e l'ambiente
 * reale un'altra.
 *
 * <p><b>Il valore conservato non è creduto.</b> Vale solo se corrisponde a un'appartenenza attiva
 * trovata <i>adesso</i>. È la riga che impedisce che una manomissione di quella colonna diventi un
 * varco fra due aziende: l'invariante «account solo dal token verificato» resta intatta, perché
 * cambia la funzione che <i>calcola</i> il claim, non chi se ne fida.
 */
public final class ActiveAccount {

    private ActiveAccount() {}

    /**
     * Un'appartenenza attiva candidata a diventare l'account della sessione.
     *
     * @param membershipId identificativo dell'appartenenza (quello conservato in {@code
     *     active_membership_id})
     * @param tenantId account (= {@code tenant_id} del claim)
     * @param role ruolo della persona in quell'account
     */
    public record Candidate(UUID membershipId, String tenantId, String role) {}

    /** Esito della scelta: uno dei tre, mai un quarto implicito. */
    public sealed interface Choice {

        /**
         * Nessuna appartenenza attiva: <b>nessun claim</b>. Comportamento di oggi, da conservare —
         * una persona che è uscita dal suo ultimo account resta un'identità senza appartenenze,
         * stato non proibito ma inutilizzabile.
         */
        record None() implements Choice {}

        /** L'account della sessione è deciso. */
        record Chosen(Candidate candidate) implements Choice {}

        /**
         * Più appartenenze attive e nessuna scelta valida: <b>nessun claim</b> e richiesta esplicita
         * di scegliere. Non è un errore del sistema ed è distinto da {@link None}: la persona può
         * lavorare, ma nessuno può decidere al suo posto per conto di chi.
         */
        record MustChoose(List<Candidate> candidates) implements Choice {}
    }

    /**
     * Applica la tabella dei casi di UC 0117 §4.2, in quest'ordine:
     *
     * <table border="1">
     *   <caption>Tabella dei casi</caption>
     *   <tr><th>Appartenenze attive</th><th>Valore conservato</th><th>Esito</th></tr>
     *   <tr><td>nessuna</td><td>qualunque</td><td>{@link Choice.None}</td></tr>
     *   <tr><td>una sola</td><td>qualunque, anche assente</td><td>{@link Choice.Chosen} — quella</td></tr>
     *   <tr><td>più di una</td><td>corrisponde a una di esse</td><td>{@link Choice.Chosen} — quella</td></tr>
     *   <tr><td>più di una</td><td>assente o non corrispondente</td><td>{@link Choice.MustChoose}</td></tr>
     * </table>
     *
     * <p>Il caso «una sola» <b>ignora</b> il valore conservato: è il caso di tutti gli utenti di
     * oggi e deve restare a costo zero — nessun passaggio in più, nessun modo di sbagliare.
     *
     * @param activeMemberships appartenenze <b>attive</b> della persona, in ordine deterministico
     *     (anzianità); la funzione non filtra per stato: chi la chiama passa solo ciò che è attivo
     * @param storedActiveMembershipId valore conservato, eventualmente {@code null} o non più valido
     */
    public static Choice choose(List<Candidate> activeMemberships, UUID storedActiveMembershipId) {
        List<Candidate> candidates = activeMemberships == null ? List.of() : List.copyOf(activeMemberships);
        if (candidates.isEmpty()) {
            return new Choice.None();
        }
        if (candidates.size() == 1) {
            return new Choice.Chosen(candidates.get(0));
        }
        if (storedActiveMembershipId != null) {
            for (Candidate candidate : candidates) {
                if (Objects.equals(candidate.membershipId(), storedActiveMembershipId)) {
                    return new Choice.Chosen(candidate);
                }
            }
        }
        return new Choice.MustChoose(candidates);
    }
}
