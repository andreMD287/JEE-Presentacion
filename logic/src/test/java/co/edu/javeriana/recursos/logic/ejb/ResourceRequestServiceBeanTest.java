package co.edu.javeriana.recursos.logic.ejb;

import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.contracts.exception.BusinessValidationException;
import co.edu.javeriana.recursos.contracts.exception.RequestNotFoundException;
import co.edu.javeriana.recursos.logic.domain.ResourceRequest;
import co.edu.javeriana.recursos.logic.port.ResourceRequestRepository;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceRequestServiceBeanTest {
    @Mock ResourceRequestRepository repository;
    ResourceRequestServiceBean service;

    @BeforeEach
    void setUp() {
        service = new ResourceRequestServiceBean();
        service.repository = repository;
    }

    private CreateResourceRequestCommand valid() {
        return new CreateResourceRequestCommand("Sismo en Pasto", "Medicamentos", 40, "ALTA");
    }

    @Test void registerCreatesPendingWithAllFields() {
        when(repository.save(any())).thenAnswer(invocation -> {
            ResourceRequest request = invocation.getArgument(0);
            assertNull(request.getId());
            assertEquals(RequestStatus.PENDIENTE, request.getStatus());
            return new ResourceRequest(12L, request.getDisasterDescription(), request.getResourceType(),
                    request.getQuantity(), request.getPriority(), request.getStatus());
        });
        var result = service.register(valid());
        assertEquals(12L, result.getId());
        assertEquals("Sismo en Pasto", result.getDisasterDescription());
        assertEquals("Medicamentos", result.getResourceType());
        assertEquals(40, result.getQuantity());
        assertEquals(Priority.ALTA, result.getPriority());
        assertEquals(RequestStatus.PENDIENTE, result.getStatus());
        verify(repository).save(any());
    }

    @Test void rejectsNullRequest() {
        var e = assertThrows(BusinessValidationException.class, () -> service.register(null));
        assertEquals("VALIDATION_ERROR", e.getCode());
        verifyNoInteractions(repository);
    }

    @Test void rejectsNullQuantity() {
        var request = valid(); request.setQuantity(null);
        assertMessage("La cantidad debe ser mayor que cero", () -> service.register(request));
    }

    @Test void rejectsZeroQuantity() {
        var request = valid(); request.setQuantity(0);
        assertMessage("La cantidad debe ser mayor que cero", () -> service.register(request));
    }

    @Test void rejectsNegativeQuantity() {
        var request = valid(); request.setQuantity(-1);
        assertMessage("La cantidad debe ser mayor que cero", () -> service.register(request));
    }

    @Test void rejectsNullDescription() {
        var request = valid(); request.setDisasterDescription(null);
        assertMessage("La descripción del desastre es obligatoria", () -> service.register(request));
    }

    @Test void rejectsBlankDescription() {
        var request = valid(); request.setDisasterDescription("   ");
        assertMessage("La descripción del desastre es obligatoria", () -> service.register(request));
    }

    @Test void rejectsNullResourceType() {
        var request = valid(); request.setResourceType(null);
        assertMessage("El tipo de recurso es obligatorio", () -> service.register(request));
    }

    @Test void rejectsBlankResourceType() {
        var request = valid(); request.setResourceType(" ");
        assertMessage("El tipo de recurso es obligatorio", () -> service.register(request));
    }

    @Test void rejectsMissingPriority() {
        var request = valid(); request.setPriority(null);
        assertMessage("La prioridad es obligatoria", () -> service.register(request));
    }

    @Test void rejectsBlankPriority() {
        var request = valid(); request.setPriority(" ");
        assertMessage("La prioridad es obligatoria", () -> service.register(request));
    }

    @Test void rejectsInvalidPriority() {
        var request = valid(); request.setPriority("URGENTE");
        assertMessage("La prioridad debe ser BAJA, MEDIA o ALTA", () -> service.register(request));
    }

    @Test void acceptsCaseInsensitivePriority() {
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var request = valid(); request.setPriority(" media ");
        assertEquals(Priority.MEDIA, service.register(request).getPriority());
    }

    @Test void findAllDelegatesAndConvertsViews() {
        when(repository.findAll()).thenReturn(List.of(pending(1L), attended(2L)));
        var results = service.findAll();
        assertEquals(List.of(1L, 2L), results.stream().map(r -> r.getId()).toList());
        assertEquals(RequestStatus.ATENDIDA, results.get(1).getStatus());
        verify(repository).findAll();
    }

    @Test void findAllEmptyReturnsEmpty() {
        when(repository.findAll()).thenReturn(List.of());
        assertTrue(service.findAll().isEmpty());
    }

    @Test void findByIdReturnsExisting() {
        when(repository.findById(2L)).thenReturn(Optional.of(pending(2L)));
        assertEquals(2L, service.findById(2L).getId());
    }

    @Test void findByIdNotFoundThrows404Error() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        var error = assertThrows(RequestNotFoundException.class, () -> service.findById(9L));
        assertEquals("REQUEST_NOT_FOUND", error.getCode());
        assertEquals("No existe la solicitud con id 9", error.getMessage());
    }

    @Test void attendChangesStatusAndPersists() {
        when(repository.findById(7L)).thenReturn(Optional.of(pending(7L)));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = service.attend(7L);
        assertEquals(RequestStatus.ATENDIDA, result.getStatus());
        verify(repository).save(argThat(r -> r.getId().equals(7L) && r.getStatus() == RequestStatus.ATENDIDA));
    }

    @Test void attendNotFoundThrows404WithoutSaving() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        var error = assertThrows(RequestNotFoundException.class, () -> service.attend(9L));
        assertEquals("REQUEST_NOT_FOUND", error.getCode());
        verify(repository, never()).save(any());
    }

    @Test void attendAlreadyAttendedThrows409WithoutSaving() {
        when(repository.findById(7L)).thenReturn(Optional.of(attended(7L)));
        var error = assertThrows(AlreadyAttendedException.class, () -> service.attend(7L));
        assertEquals("ALREADY_ATTENDED", error.getCode());
        assertEquals("La solicitud con id 7 ya fue atendida", error.getMessage());
        verify(repository, never()).save(any());
    }

    @Test void onlyPendingAreReturned() {
        when(repository.findAll()).thenReturn(List.of(pending(1L), attended(2L), pending(3L)));
        assertEquals(List.of(1L, 3L), service.findPending().stream().map(r -> r.getId()).toList());
    }

    @Test void filteringAnEmptyRepositoryReturnsEmpty() {
        when(repository.findAll()).thenReturn(List.of());
        assertTrue(service.findPending().isEmpty());
    }

    @Test void transactionIsNotSupported() {
        var annotation = ResourceRequestServiceBean.class.getAnnotation(TransactionAttribute.class);
        assertNotNull(annotation);
        assertEquals(TransactionAttributeType.NOT_SUPPORTED, annotation.value());
    }

    private void assertMessage(String expected, org.junit.jupiter.api.function.Executable run) {
        var error = assertThrows(BusinessValidationException.class, run);
        assertEquals(expected, error.getMessage());
        assertEquals("VALIDATION_ERROR", error.getCode());
        verifyNoInteractions(repository);
    }

    private ResourceRequest pending(Long id) {
        return new ResourceRequest(id, "Sismo", "Agua", 10, Priority.ALTA, RequestStatus.PENDIENTE);
    }

    private ResourceRequest attended(Long id) {
        return pending(id).withStatus(RequestStatus.ATENDIDA);
    }
}
