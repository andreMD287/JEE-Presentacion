package co.edu.javeriana.recursos.presentation.client;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RequestClientSelectorTest {
    @Test void usesEjbByDefault() {
        assertInstanceOf(EjbRequestClient.class, RequestClientSelector.create(Map.of()));
    }
    @Test void supportsRestThroughGateway() {
        assertInstanceOf(RestRequestClient.class, RequestClientSelector.create(Map.of(
                "BACKEND_MODE", "REST", "GATEWAY_URL", "http://gateway:8000/api/v1",
                "API_KEY", "clave-taller4")));
    }
    @Test void rejectsUnknownMode() {
        assertThrows(IllegalArgumentException.class,
                () -> RequestClientSelector.create(Map.of("BACKEND_MODE", "soap")));
    }
    @Test void restRequiresApiKey() {
        assertThrows(IllegalArgumentException.class,
                () -> RequestClientSelector.create(Map.of("BACKEND_MODE", "rest")));
    }
}
