package app.appgrove.commons.membership;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import app.appgrove.commons.membership.ActiveAccount.Candidate;
import app.appgrove.commons.membership.ActiveAccount.Choice;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * La tabella dei casi di UC 0117 §4.2, caso per caso. È il collaudo più importante della storia: la
 * stessa tabella gira sulla funzione Python che compone il token in cloud
 * ({@code infra/modules/platform_shared/lambda/pre_token_gen/test_handler.py}) — se le due divergono,
 * i collaudi locali dicono una cosa e l'ambiente reale un'altra.
 */
class ActiveAccountTest {

    private static final UUID M1 = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID M2 = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID M_ALTRUI = UUID.fromString("d0000000-0000-4000-8000-0000000000ff");

    private static final Candidate ACME = new Candidate(M1, "tenant-acme", "owner");
    private static final Candidate BETA = new Candidate(M2, "tenant-beta", "member");

    @Test
    void nessunaAppartenenzaAttivaNonProduceAlcunClaim() {
        assertInstanceOf(Choice.None.class, ActiveAccount.choose(List.of(), null));
        assertInstanceOf(Choice.None.class, ActiveAccount.choose(null, M1));
        assertInstanceOf(
                Choice.None.class,
                ActiveAccount.choose(List.of(), M1),
                "senza appartenenze attive il valore conservato non può resuscitare nulla");
    }

    @Test
    void unaSolaAppartenenzaVinceSulValoreConservato() {
        // Il caso di tutti gli utenti di oggi: deve restare a costo zero e senza modi di sbagliare.
        assertEquals(ACME, chosen(ActiveAccount.choose(List.of(ACME), null)));
        assertEquals(ACME, chosen(ActiveAccount.choose(List.of(ACME), M_ALTRUI)),
                "con una sola appartenenza il valore conservato è irrilevante, anche se manomesso");
        assertEquals(ACME, chosen(ActiveAccount.choose(List.of(ACME), M1)));
    }

    @Test
    void piuAppartenenzeConValoreConservatoValido() {
        assertEquals(BETA, chosen(ActiveAccount.choose(List.of(ACME, BETA), M2)));
        assertEquals(ACME, chosen(ActiveAccount.choose(List.of(ACME, BETA), M1)));
    }

    @Test
    void piuAppartenenzeSenzaScelta() {
        Choice choice = ActiveAccount.choose(List.of(ACME, BETA), null);
        Choice.MustChoose mustChoose = assertInstanceOf(Choice.MustChoose.class, choice);
        assertEquals(List.of(ACME, BETA), mustChoose.candidates(), "l'elenco fra cui scegliere è completo");
    }

    /**
     * La prova che conta: un valore conservato che NON corrisponde a un'appartenenza attiva — perché
     * l'appartenenza è stata revocata, perché è di un altro account, o perché la colonna è stata
     * manomessa — non produce mai un claim. È l'unica prova che il valore conservato non è creduto.
     */
    @Test
    void valoreConservatoNonCorrispondenteNonProduceMaiUnClaim() {
        assertInstanceOf(
                Choice.MustChoose.class,
                ActiveAccount.choose(List.of(ACME, BETA), M_ALTRUI),
                "un'appartenenza che non è fra quelle attive non può diventare l'account della sessione");
    }

    private static Candidate chosen(Choice choice) {
        return assertInstanceOf(Choice.Chosen.class, choice).candidate();
    }
}
