package co.edu.javeriana.recursos.services.rest;

import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.contracts.exception.BusinessValidationException;
import co.edu.javeriana.recursos.contracts.exception.RequestNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionMapperTest {
    private final ApiExceptionMapper mapper = new ApiExceptionMapper();

    @Test void validationReturns400() {
        assertError(new BusinessValidationException("Datos inválidos"), 400, "VALIDATION_ERROR", "Datos inválidos");
    }

    @Test void notFoundReturns404() {
        assertError(new RequestNotFoundException(77L), 404, "REQUEST_NOT_FOUND", "No existe la solicitud con id 77");
    }

    @Test void alreadyAttendedReturns409() {
        assertError(new AlreadyAttendedException(5L), 409, "ALREADY_ATTENDED", "La solicitud con id 5 ya fue atendida");
    }

    @Test void emptyBodyReturns400() {
        assertError(new PayloadException("EMPTY_BODY", "El cuerpo de la solicitud es obligatorio"),
                400, "EMPTY_BODY", "El cuerpo de la solicitud es obligatorio");
    }

    @Test void invalidJsonReturns400() {
        assertError(new PayloadException("MALFORMED_JSON", "El JSON enviado no es válido"),
                400, "MALFORMED_JSON", "El JSON enviado no es válido");
    }

    @Test void technicalFailuresHideDetails() {
        Response response = mapper.toResponse(new IllegalStateException("contraseña interna"));
        assertEquals(500, response.getStatus());
        ApiError error = (ApiError) response.getEntity();
        assertEquals("INTERNAL_ERROR", error.getCode());
        assertFalse(error.getMessage().contains("contraseña"));
    }

    private void assertError(Exception exception, int status, String code, String message) {
        Response response = mapper.toResponse(exception);
        assertEquals(status, response.getStatus());
        assertEquals("application/json", response.getMediaType().toString());
        ApiError body = (ApiError) response.getEntity();
        assertEquals(code, body.getCode());
        assertEquals(message, body.getMessage());
        assertNotNull(body.getTimestamp());
        assertFalse(body.getTimestamp().isBlank());
    }
}
