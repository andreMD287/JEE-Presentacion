package co.edu.javeriana.recursos.services.rest;

import co.edu.javeriana.recursos.services.client.DependencyAvailability;
import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.contracts.exception.BusinessException;
import co.edu.javeriana.recursos.contracts.exception.BusinessValidationException;
import co.edu.javeriana.recursos.contracts.exception.RequestNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Centraliza el formato HTTP; evita filtrar trazas o detalles de infraestructura. */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        String code;
        String message;
        int status;

        if (exception instanceof BusinessException error) {
            code = error.getCode();
            message = error.getMessage();
            status = switch (code) {
                case BusinessValidationException.CODE -> 400;
                case RequestNotFoundException.CODE -> 404;
                case AlreadyAttendedException.CODE -> 409;
                default -> 500;
            };
        } else if (exception instanceof PayloadException error) {
            code = error.getCode();
            message = error.getMessage();
            status = 400;
        } else if (exception instanceof WebApplicationException error) {
            // Respeta las respuestas HTTP propias de JAX-RS (404, 405, 415, ...).
            status = error.getResponse().getStatus();
            code = status == 400 ? "MALFORMED_JSON" : "HTTP_ERROR";
            message = "La solicitud HTTP no es válida";
        } else if (DependencyAvailability.isUnavailable(exception)) {
            status = 503;
            code = "SERVICE_UNAVAILABLE";
            message = "El servicio no esta disponible temporalmente";
            LOG.log(Level.WARNING, "Dependencia no disponible", exception);
        } else {
            status = 500;
            code = "INTERNAL_ERROR";
            message = "No se pudo completar la operación";
            LOG.log(Level.SEVERE, "Error al acceder a la capa de Lógica", exception);
        }

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ApiError(code, message, OffsetDateTime.now(ZoneOffset.UTC).toString()))
                .build();
    }
}
