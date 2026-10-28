package co.edu.javeriana.recursos.contracts.exception;

import jakarta.ejb.ApplicationException;

/**
 * Se intentó atender una solicitud que ya está ATENDIDA (HTTP 409).
 */
@ApplicationException(rollback = true)
public class AlreadyAttendedException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "ALREADY_ATTENDED";

    public AlreadyAttendedException(Long id) {
        super(CODE, "La solicitud con id " + id + " ya fue atendida");
    }
}
