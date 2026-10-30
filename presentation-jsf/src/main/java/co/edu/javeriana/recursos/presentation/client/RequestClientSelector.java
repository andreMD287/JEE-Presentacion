package co.edu.javeriana.recursos.presentation.client;

import java.util.Locale;
import java.util.Map;

/** Selecciona una sola implementación durante la vida de la aplicación. */
public final class RequestClientSelector {
    private static volatile RequestClient instance;
    private static final String DEFAULT_GATEWAY = "http://gateway:8000/api/v1";

    private RequestClientSelector() { }

    public static RequestClient get() {
        RequestClient current = instance;
        if (current == null) {
            synchronized (RequestClientSelector.class) {
                current = instance;
                if (current == null) {
                    current = create(System.getenv());
                    instance = current;
                }
            }
        }
        return current;
    }

    static RequestClient create(Map<String, String> env) {
        String mode = env.getOrDefault("BACKEND_MODE", "ejb").trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "ejb" -> new EjbRequestClient();
            case "rest" -> new RestRequestClient(
                    env.getOrDefault("GATEWAY_URL", DEFAULT_GATEWAY),
                    env.get("API_KEY"));
            default -> throw new IllegalArgumentException("BACKEND_MODE debe ser ejb o rest");
        };
    }

    public static String mode() {
        return System.getenv().getOrDefault("BACKEND_MODE", "ejb").trim().toLowerCase(Locale.ROOT);
    }
}
