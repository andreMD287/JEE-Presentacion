package co.edu.javeriana.recursos.presentation.client;

import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.contracts.exception.BusinessValidationException;
import co.edu.javeriana.recursos.contracts.exception.RequestNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** Modo REST: todas las solicitudes pasan por Kong con la API key configurada. */
public final class RestRequestClient implements RequestClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofMillis(4500);
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;
    private final String apiKey;

    public RestRequestClient(String baseUrl, String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(), baseUrl, apiKey);
    }

    RestRequestClient(HttpClient http, String baseUrl, String apiKey) {
        if (baseUrl == null || baseUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Faltan GATEWAY_URL o API_KEY");
        }
        this.http = http;
        this.baseUrl = baseUrl.replaceAll("/+$", "") + "/requests";
        this.apiKey = apiKey;
    }

    @Override public ResourceRequestView register(CreateResourceRequestCommand command) {
        return readView(call("POST", "", command, null));
    }
    @Override public List<ResourceRequestView> findAll() {
        try {
            return json.readValue(call("GET", "", null, null), new TypeReference<List<ResourceRequestView>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("La respuesta de listado no tiene el formato esperado", e);
        }
    }
    @Override public ResourceRequestView findById(Long id) {
        return readView(call("GET", "/" + id, null, id));
    }
    @Override public ResourceRequestView attend(Long id) {
        return readView(call("PUT", "/" + id + "/attend", null, id));
    }
    @Override public List<ResourceRequestView> findPending() {
        try {
            return json.readValue(call("GET", "/pending", null, null),
                    new TypeReference<List<ResourceRequestView>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("La respuesta de pendientes no tiene el formato esperado", e);
        }
    }

    private ResourceRequestView readView(String body) {
        try {
            return json.readValue(body, ResourceRequestView.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("La respuesta de la solicitud no tiene el formato esperado", e);
        }
    }

    private String call(String method, String suffix, Object payload, Long id) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + suffix))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("apikey", apiKey);
        if ("POST".equals(method)) {
            try {
                request.header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload), StandardCharsets.UTF_8));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("No se pudo preparar la solicitud", e);
            }
        } else if ("PUT".equals(method)) {
            request.PUT(HttpRequest.BodyPublishers.noBody());
        } else {
            request.GET();
        }
        try {
            HttpResponse<String> response = http.send(request.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status >= 200 && status < 300) return response.body();
            throw businessOrTechnicalError(status, response.body(), id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La consulta fue interrumpida", e);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo contactar el gateway", e);
        }
    }

    private RuntimeException businessOrTechnicalError(int status, String body, Long id) {
        String message = "Error al procesar la solicitud";
        try {
            JsonNode error = json.readTree(body);
            if (error != null && error.path("message").isTextual()) {
                message = error.path("message").asText();
            }
        } catch (JsonProcessingException ignored) {
            // Kong puede devolver un cuerpo distinto al contrato REST.
        }
        if (status == 400) return new BusinessValidationException(message);
        if (status == 404 && id != null) return new RequestNotFoundException(id);
        if (status == 409 && id != null) return new AlreadyAttendedException(id);
        if (status == 429) return new IllegalStateException("Se alcanzó el límite de consultas. Inténtalo de nuevo.");
        return new IllegalStateException("El servicio no está disponible (HTTP " + status + ")");
    }
}
