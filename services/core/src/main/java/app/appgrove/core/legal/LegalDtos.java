package app.appgrove.core.legal;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** DTO degli endpoint legali (UC 0056). */
public final class LegalDtos {

    private LegalDtos() {}

    /** Stato di un componente da (ri-)accettare o notificare. */
    public record ComponentStatusView(String component, String version, String effectiveDate, String act) {
        static ComponentStatusView from(LegalService.ComponentStatus s) {
            return new ComponentStatusView(
                    s.component().name(), s.version(), s.effectiveDate().toString(), s.act().name());
        }
    }

    /** Stato di (ri-)accettazione derivato per il chiamante. */
    public record LegalStatusView(List<ComponentStatusView> pending, List<ComponentStatusView> notices) {
        static LegalStatusView from(LegalService.LegalStatus s) {
            return new LegalStatusView(
                    s.pending().stream().map(ComponentStatusView::from).toList(),
                    s.notices().stream().map(ComponentStatusView::from).toList());
        }
    }

    /** Corpo del POST di accettazione: i componenti che l'utente accetta/prende atto (alle versioni correnti lato server). */
    public record AcceptRequest(@NotEmpty List<String> components) {}

    /** Documento legale reso (markdown coi token {{titolare.*}} risolti). */
    public record LegalDocView(String component, String lang, String version, String effectiveDate, String markdown) {
        static LegalDocView from(LegalContentLoader.LegalDoc d) {
            return new LegalDocView(
                    d.component().name(), d.lang(), d.version(), d.effectiveDate().toString(), d.markdown());
        }
    }
}
