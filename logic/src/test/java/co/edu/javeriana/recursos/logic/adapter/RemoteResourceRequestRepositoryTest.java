package co.edu.javeriana.recursos.logic.adapter;

import co.edu.javeriana.recursos.contracts.ResourceRequestDataService;
import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestData;
import co.edu.javeriana.recursos.logic.domain.ResourceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoteResourceRequestRepositoryTest {
    @Mock ResourceRequestDataService remote;
    RemoteResourceRequestRepository repository;

    @BeforeEach void setUp() {
        repository = new RemoteResourceRequestRepository(remote);
    }

    @Test void convertsDomainToDataAndBackOnInsert() {
        when(remote.save(any())).thenAnswer(i -> {
            ResourceRequestData sent = i.getArgument(0);
            assertNull(sent.getId());
            assertEquals("Sismo", sent.getDisasterDescription());
            assertEquals("Agua", sent.getResourceType());
            assertEquals(10, sent.getQuantity());
            assertEquals(Priority.ALTA, sent.getPriority());
            assertEquals(RequestStatus.PENDIENTE, sent.getStatus());
            sent.setId(8L);
            return sent;
        });
        assertEquals(8L, repository.save(pending(null)).getId());
    }

    @Test void updatePreservesIdAndStatus() {
        when(remote.save(any())).thenAnswer(i -> i.getArgument(0));
        ResourceRequest updated = repository.save(pending(4L).withStatus(RequestStatus.ATENDIDA));
        assertEquals(4L, updated.getId());
        assertEquals(RequestStatus.ATENDIDA, updated.getStatus());
    }

    @Test void missingRemoteRequestReturnsEmptyOptional() {
        when(remote.findById(404L)).thenReturn(null);
        assertTrue(repository.findById(404L).isEmpty());
    }

    @Test void existingRemoteRequestReturnsDomainModel() {
        when(remote.findById(5L)).thenReturn(data(5L, RequestStatus.ATENDIDA));
        var found = repository.findById(5L).orElseThrow();
        assertEquals(5L, found.getId());
        assertEquals(RequestStatus.ATENDIDA, found.getStatus());
    }

    @Test void findAllConvertsDtoListPreservingOrder() {
        when(remote.findAll()).thenReturn(List.of(data(1L, RequestStatus.PENDIENTE),
                data(2L, RequestStatus.ATENDIDA)));
        var items = repository.findAll();
        assertEquals(List.of(1L, 2L), items.stream().map(ResourceRequest::getId).toList());
        assertEquals(RequestStatus.ATENDIDA, items.get(1).getStatus());
    }

    @Test void forwardsRemoteErrorsInsteadOfSwallowingThem() {
        when(remote.findAll()).thenThrow(new IllegalStateException("Servidor de Datos no disponible"));
        assertThrows(IllegalStateException.class, () -> repository.findAll());
    }

    private ResourceRequest pending(Long id) {
        return new ResourceRequest(id, "Sismo", "Agua", 10, Priority.ALTA, RequestStatus.PENDIENTE);
    }

    private ResourceRequestData data(Long id, RequestStatus status) {
        return new ResourceRequestData(id, "Sismo", "Agua", 10, Priority.ALTA, status);
    }
}
