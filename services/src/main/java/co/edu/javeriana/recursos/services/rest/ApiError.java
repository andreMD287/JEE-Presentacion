package co.edu.javeriana.recursos.services.rest;

/** Error estable acordado con los consumidores REST. */
public class ApiError {
    private String code;
    private String message;
    private String timestamp;

    public ApiError() {
    }

    public ApiError(String code, String message, String timestamp) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
