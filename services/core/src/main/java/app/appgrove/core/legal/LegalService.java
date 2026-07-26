package app.appgrove.core.legal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Derivazione runtime dello stato di (ri-)accettazione (UC 0056): confronta, per ogni componente
 * <b>vincolante</b>, la major accettata dall'utente (log) con la major corrente ({@link LegalVersion},
 * popolata dalla sync). Nessun flag/job di massa. Registra le accettazioni (append-only, idempotente).
 *
 * <p>Regola: <b>major corrente &gt; max major accettata</b> → componente <b>pendente</b> (schermata bloccante);
 * stessa major ma versione corrente non ancora accettata (minor) → <b>notifica</b> non bloccante; versione
 * corrente già accettata → nulla. Se una versione corrente non è nota (sync non ancora eseguita) il componente
 * è ignorato (fail-open: non si blocca l'accesso su dati mancanti).
 */
@ApplicationScoped
public class LegalService {

    @Inject
    LegalVersionRepository versions;

    @Inject
    LegalAcceptanceRepository acceptances;

    /** Voce di stato per un componente. */
    public record ComponentStatus(LegalComponent component, String version, LocalDate effectiveDate, LegalActType act) {}

    /** Stato derivato: componenti da (ri-)accettare (bloccanti) + componenti con soli cambi minor (notifica). */
    public record LegalStatus(List<ComponentStatus> pending, List<ComponentStatus> notices) {}

    public LegalStatus statusFor(String userId) {
        List<LegalAcceptance> mine = acceptances.findByUser(userId);
        List<ComponentStatus> pending = new ArrayList<>();
        List<ComponentStatus> notices = new ArrayList<>();

        for (LegalComponent component : LegalComponent.BINDING) {
            Optional<LegalVersion> currentOpt = versions.findByComponent(component);
            if (currentOpt.isEmpty()) {
                continue; // versione corrente non nota → non si richiede nulla
            }
            LegalVersion current = currentOpt.get();
            int maxAcceptedMajor = mine.stream()
                    .filter(a -> a.getComponent() == component)
                    .mapToInt(LegalAcceptance::getMajor)
                    .max()
                    .orElse(-1);
            boolean acceptedCurrentVersion = mine.stream()
                    .anyMatch(a -> a.getComponent() == component && a.getVersion().equals(current.getVersion()));

            ComponentStatus cs = new ComponentStatus(
                    component, current.getVersion(), current.getEffectiveDate(), component.requiredAct());
            if (current.getMajor() > maxAcceptedMajor) {
                pending.add(cs); // major nuova → blocco
            } else if (!acceptedCurrentVersion) {
                notices.add(cs); // stessa major, versione (minor/patch) diversa → notifica
            }
        }
        return new LegalStatus(pending, notices);
    }

    /**
     * Registra l'accettazione/presa d'atto dei componenti indicati alle <b>versioni correnti</b> (mai da
     * versione fornita dal client): idempotente per (utente, componente, versione). Ritorna lo stato aggiornato.
     */
    @Transactional
    public LegalStatus accept(String userId, List<LegalComponent> components, String commitHash) {
        for (LegalComponent component : components) {
            LegalVersion current = versions.findByComponent(component).orElse(null);
            if (current == null) {
                continue; // niente versione corrente → nulla da registrare
            }
            if (acceptances.exists(userId, component, current.getVersion())) {
                continue; // già accettata questa versione (idempotente)
            }
            acceptances.persist(new LegalAcceptance(
                    userId,
                    component,
                    current.getVersion(),
                    current.getMajor(),
                    component.requiredAct(),
                    Instant.now(),
                    commitHash));
        }
        return statusFor(userId);
    }
}
