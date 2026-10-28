package co.edu.javeriana.recursos.contracts.exception;

/**
 * Base de los errores de negocio que la capa de Lógica lanza a sus clientes.
 *
 * <p>Lleva un código estable ({@link #getCode()}) para que REST, SOAP y JSF
 * reporten el mismo error con el mismo código, sin depender del texto del mensaje.
 * Los códigos son los del Taller 2.</p>
 */
public abstract class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    protected BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
