package co.edu.javeriana.recursos.services.soap;

import co.edu.javeriana.recursos.contracts.ResourceRequestService;
import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.contracts.exception.BusinessValidationException;
import co.edu.javeriana.recursos.contracts.exception.RequestNotFoundException;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceRequestSoapServiceTest {
    private final ResourceRequestService logic = mock(ResourceRequestService.class);
    private final ResourceRequestSoapService soap = new ResourceRequestSoapService(() -> logic);
    private final ResourceRequestView view = new ResourceRequestView(10L, "Inundación", "Agua",
            30, Priority.MEDIA, RequestStatus.PENDIENTE);

    @Test void namespaceAndMethodsMatchContract() {
        WebService annotation = ResourceRequestSoapService.class.getAnnotation(WebService.class);
        assertEquals("http://recursos.javeriana.edu.co/soap", annotation.targetNamespace());
        assertEquals("ResourceRequestSoapService", annotation.serviceName());
        var names = Arrays.stream(ResourceRequestSoapService.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(WebMethod.class))
                .map(m -> m.getAnnotation(WebMethod.class).operationName())
                .collect(Collectors.toSet());
        assertEquals(java.util.Set.of("registerRequest", "listRequests", "getRequest",
                "attendRequest", "listPendingRequests"), names);
    }

    @Test void delegatesEveryBusinessOperation() throws BusinessFault {
        when(logic.register(any())).thenReturn(view);
        when(logic.findAll()).thenReturn(List.of(view));
        when(logic.findById(10L)).thenReturn(view);
        when(logic.attend(10L)).thenReturn(view);
        when(logic.findPending()).thenReturn(List.of(view));
        var command = new CreateResourceRequestCommand("Inundación", "Agua", 30, "MEDIA");
        assertEquals(view, soap.registerRequest(command));
        assertEquals(List.of(view), soap.listRequests());
        assertEquals(view, soap.getRequest(10L));
        assertEquals(view, soap.attendRequest(10L));
        assertEquals(List.of(view), soap.listPendingRequests());
        verify(logic).register(command);
    }

    @Test void validationFaultPreservesCodeAndMessage() {
        when(logic.register(any())).thenThrow(new BusinessValidationException("La cantidad debe ser mayor que cero"));
        BusinessFault fault = assertThrows(BusinessFault.class,
                () -> soap.registerRequest(new CreateResourceRequestCommand()));
        assertEquals("VALIDATION_ERROR", fault.getFaultInfo().getCode());
        assertEquals("La cantidad debe ser mayor que cero", fault.getFaultInfo().getMessage());
    }

    @Test void missingRequestFaultPreservesCode() {
        when(logic.findById(99L)).thenThrow(new RequestNotFoundException(99L));
        BusinessFault fault = assertThrows(BusinessFault.class, () -> soap.getRequest(99L));
        assertEquals("REQUEST_NOT_FOUND", fault.getFaultInfo().getCode());
    }

    @Test void repeatedAttendFaultPreservesCode() {
        when(logic.attend(10L)).thenThrow(new AlreadyAttendedException(10L));
        BusinessFault fault = assertThrows(BusinessFault.class, () -> soap.attendRequest(10L));
        assertEquals("ALREADY_ATTENDED", fault.getFaultInfo().getCode());
    }

    @Test void systemFailureDoesNotExposeCredentials() {
        when(logic.findAll()).thenThrow(new IllegalStateException("pass123"));
        BusinessFault fault = assertThrows(BusinessFault.class, soap::listRequests);
        assertEquals("INTERNAL_ERROR", fault.getFaultInfo().getCode());
        assertFalse(fault.getMessage().contains("pass123"));
    }
}
