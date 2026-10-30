package co.edu.javeriana.recursos.presentation.web;

import co.edu.javeriana.recursos.presentation.client.RequestClient;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class PresentationWarmupListenerTest {
    @Test void invokesFindAll() {
        RequestClient client = mock(RequestClient.class);
        when(client.findAll()).thenReturn(List.of());
        new PresentationWarmupListener().warmUp(client);
        verify(client).findAll();
    }
    @Test void failureDoesNotPreventDeployment() {
        RequestClient client = mock(RequestClient.class);
        when(client.findAll()).thenThrow(new IllegalStateException("Sin conexión"));
        assertDoesNotThrow(() -> new PresentationWarmupListener().warmUp(client));
    }
}
