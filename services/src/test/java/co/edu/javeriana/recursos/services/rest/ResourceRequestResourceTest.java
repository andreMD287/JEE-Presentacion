package co.edu.javeriana.recursos.services.rest;

import co.edu.javeriana.recursos.contracts.ResourceRequestService;
import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceRequestResourceTest {
    private final ResourceRequestService logic = mock(ResourceRequestService.class);
    private final ResourceRequestResource resource = new ResourceRequestResource(
            () -> logic, body -> new co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand(
                    "Sismo", "Carpas", 2, "ALTA"));
    private final ResourceRequestView view = new ResourceRequestView(8L, "Sismo", "Carpas",
            2, Priority.ALTA, RequestStatus.PENDIENTE);

    @Test void registerReturns201AndLocation() {
        when(logic.register(any())).thenReturn(view);
        Response response = resource.register("{\"disasterDescription\":\"Sismo\","
                + "\"resourceType\":\"Carpas\",\"quantity\":2,\"priority\":\"ALTA\"}");
        assertEquals(201, response.getStatus());
        assertEquals(view, response.getEntity());
        assertTrue(response.getLocation().toString().endsWith("/requests/8"));
        verify(logic).register(argThat(cmd -> cmd.getQuantity() == 2 && "ALTA".equals(cmd.getPriority())));
    }

    @Test void delegatesFindAll() {
        when(logic.findAll()).thenReturn(List.of(view));
        assertEquals(List.of(view), resource.list());
    }

    @Test void delegatesFindPending() {
        when(logic.findPending()).thenReturn(List.of(view));
        assertEquals(List.of(view), resource.pending());
    }

    @Test void delegatesFindById() {
        when(logic.findById(8L)).thenReturn(view);
        assertEquals(view, resource.get(8L));
    }

    @Test void delegatesAttend() {
        when(logic.attend(8L)).thenReturn(view);
        assertEquals(view, resource.attend(8L));
    }

    @Test void emptyBodyDoesNotInvokeLogic() {
        PayloadException ex = assertThrows(PayloadException.class, () -> resource.register("  "));
        assertEquals("EMPTY_BODY", ex.getCode());
        verifyNoInteractions(logic);
    }

    @Test void malformedBodyDoesNotInvokeLogic() {
        ResourceRequestResource malformed = new ResourceRequestResource(
                () -> logic, body -> { throw new jakarta.json.bind.JsonbException("JSON inválido"); });
        PayloadException ex = assertThrows(PayloadException.class, () -> malformed.register("{malformed"));
        assertEquals("MALFORMED_JSON", ex.getCode());
        verifyNoInteractions(logic);
    }
}
