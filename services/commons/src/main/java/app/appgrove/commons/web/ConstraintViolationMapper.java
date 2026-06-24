package app.appgrove.commons.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/** Bean Validation fallita → 400 problem+json con l'elenco dei campi non validi. */
@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ProblemDetail.FieldError> errors = exception.getConstraintViolations().stream()
                .map(ConstraintViolationMapper::toFieldError)
                .toList();
        ProblemDetail problem = ProblemDetail.of(
                Response.Status.BAD_REQUEST.getStatusCode(),
                "Richiesta non valida",
                "Uno o più campi non superano la validazione.",
                errors);
        return Response.status(Response.Status.BAD_REQUEST)
                .type(ProblemDetail.MEDIA_TYPE)
                .entity(problem)
                .build();
    }

    private static ProblemDetail.FieldError toFieldError(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        int dot = path.lastIndexOf('.');
        String field = dot >= 0 ? path.substring(dot + 1) : path;
        return new ProblemDetail.FieldError(field, violation.getMessage());
    }
}
