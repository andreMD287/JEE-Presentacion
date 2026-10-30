package co.edu.javeriana.recursos.presentation.client;

import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class RestRequestClientTest {
    @SuppressWarnings("unchecked")
    @Test void sendsKeyAndParsesRegistration() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(201);
        when(response.body()).thenReturn("{\"id\":5,\"disasterDescription\":\"Sismo\",\"resourceType\":\"Agua\",\"quantity\":2,\"priority\":\"ALTA\",\"status\":\"PENDIENTE\"}");
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        RestRequestClient client = new RestRequestClient(http, "http://gateway:8000/api/v1", "clave-taller4");
        var result = client.register(new CreateResourceRequestCommand("Sismo", "Agua", 2, "ALTA"));
        assertEquals(5L, result.getId());
        assertEquals(Priority.ALTA, result.getPriority());
        assertEquals(RequestStatus.PENDIENTE, result.getStatus());
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("POST", request.getValue().method());
        assertEquals("/api/v1/requests", request.getValue().uri().getPath());
        assertEquals("clave-taller4", request.getValue().headers().firstValue("apikey").orElseThrow());
        assertTrue(request.getValue().timeout().orElseThrow().toMillis() <= 4500);
    }

    @SuppressWarnings("unchecked")
    @Test void mapsConflictToExistingBusinessError() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(409);
        when(response.body()).thenReturn("{\"code\":\"ALREADY_ATTENDED\",\"message\":\"Solicitud ya atendida\"}");
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        RestRequestClient client = new RestRequestClient(http, "http://gateway:8000/api/v1", "clave-taller4");
        var error = assertThrows(AlreadyAttendedException.class, () -> client.attend(5L));
        assertEquals("ALREADY_ATTENDED", error.getCode());
    }

    @SuppressWarnings("unchecked")
    @Test void handlesRateLimitWithoutRetry() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(429);
        when(response.body()).thenReturn("{\"message\":\"Rate limit exceeded\"}");
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        RestRequestClient client = new RestRequestClient(http, "http://gateway:8000/api/v1", "clave-taller4");
        assertThrows(IllegalStateException.class, client::findAll);
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
