package co.edu.javeriana.recursos.contracts.exception;

import jakarta.ejb.ApplicationException;

/**
 * La capa de Datos rechazó una actualización porque la solicitud cambió desde que se leyó
 * (bloqueo optimista, D-16).
 *
 * <p>Solo cruza el contrato Lógica → Datos: no es un error de negocio para los clientes.
 * La Lógica la traduce; en {@code attend} significa que otra petición atendió la misma
 * solicitud primero, así que se reporta como {@link AlreadyAttendedException} (RN-5).</p>
 */
@ApplicationException(rollback = true)
public class ConcurrentUpdateException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public static final String CODE = "CONCURRENT_UPDATE";

    public ConcurrentUpdateException(Long id) {
        super(CODE, "La solicitud con id " + id + " fue modificada por otra operación");
    }
}
