package app.appgrove.core.example;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/** DTO dell'harness. {@link CreateWidget} NON espone {@code tenant_id}: il tenant viene solo dal JWT (anti-override). */
public final class WidgetDtos {

    private WidgetDtos() {}

    public record CreateWidget(@NotBlank String name) {}

    public record WidgetView(UUID id, String name, String tenantId) {
        public static WidgetView from(Widget w) {
            return new WidgetView(w.getId(), w.getName(), w.getTenantId());
        }
    }
}
