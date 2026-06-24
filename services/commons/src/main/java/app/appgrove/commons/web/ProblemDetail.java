package app.appgrove.commons.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Corpo di errore RFC 9457 (problem+json). Serializzato come {@code application/problem+json}.
 * Il campo {@code errors} è un'estensione per le violazioni di Bean Validation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        List<FieldError> errors) {

    public static final String MEDIA_TYPE = "application/problem+json";

    public record FieldError(String field, String message) {}

    public static ProblemDetail of(int status, String title, String detail) {
        return new ProblemDetail("about:blank", title, status, detail, null, null);
    }

    public static ProblemDetail of(int status, String title, String detail, List<FieldError> errors) {
        return new ProblemDetail("about:blank", title, status, detail, null, errors);
    }
}
