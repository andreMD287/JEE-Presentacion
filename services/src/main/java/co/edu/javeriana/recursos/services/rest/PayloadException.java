package co.edu.javeriana.recursos.services.rest;

/** Fallos de representación JSON (no son reglas del negocio). */
public class PayloadException extends RuntimeException {
    private final String code;

    public PayloadException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
