package co.edu.javeriana.recursos.contracts.exception;

import jakarta.ejb.ApplicationException;

/**
 * Los datos de una solicitud no cumplen las reglas de negocio (HTTP 400).
 *
 * <p>{@code @ApplicationException} evita que el contenedor la envuelva en
 * {@code EJBException}: llega tal cual al cliente remoto.</p>
 */
@ApplicationException(rollback = true)
public class BusinessValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "VALIDATION_ERROR";

    public BusinessValidationException(String message) {
        super(CODE, message);
    }
}
