package co.edu.javeriana.recursos.contracts.exception;

import jakarta.ejb.ApplicationException;

/**
 * No existe una solicitud con el identificador indicado (HTTP 404).
 */
@ApplicationException(rollback = true)
public class RequestNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "REQUEST_NOT_FOUND";

    public RequestNotFoundException(Long id) {
        super(CODE, "No existe la solicitud con id " + id);
    }
}
