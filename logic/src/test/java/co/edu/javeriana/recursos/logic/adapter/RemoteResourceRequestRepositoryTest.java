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
            assertNull(sent.getVersion());
            assertEquals("Sismo", sent.getDisasterDescription());
            assertEquals("Agua", sent.getResourceType());
            assertEquals(10, sent.getQuantity());
            assertEquals(Priority.ALTA, sent.getPriority());
            assertEquals(RequestStatus.PENDIENTE, sent.getStatus());
            sent.setId(8L);
            sent.setVersion(0L);
            return sent;
        });
        ResourceRequest inserted = repository.save(pending(null));
        assertEquals(8L, inserted.getId());
        assertEquals(Long.valueOf(0L), inserted.getVersion());
    }

    @Test void updatePreservesIdStatusAndVersion() {
        when(remote.save(any())).thenAnswer(i -> {
            ResourceRequestData sent = i.getArgument(0);
            assertEquals(4L, sent.getId());
            assertEquals(Long.valueOf(5L), sent.getVersion());
            assertEquals(RequestStatus.ATENDIDA, sent.getStatus());
            sent.setVersion(6L);
            return sent;
        });
        ResourceRequest current = new ResourceRequest(4L, "Sismo", "Agua", 10,
                Priority.ALTA, RequestStatus.PENDIENTE, 5L);
        ResourceRequest updated = repository.save(current.withStatus(RequestStatus.ATENDIDA));
        assertEquals(4L, updated.getId());
        assertEquals(RequestStatus.ATENDIDA, updated.getStatus());
        assertEquals(Long.valueOf(6L), updated.getVersion());
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
        assertEquals(Long.valueOf(2L), found.getVersion());
    }

    @Test void findAllConvertsDtoListPreservingOrder() {
        when(remote.findAll()).thenReturn(List.of(data(1L, RequestStatus.PENDIENTE),
                data(2L, RequestStatus.ATENDIDA)));
        var items = repository.findAll();
        assertEquals(List.of(1L, 2L), items.stream().map(ResourceRequest::getId).toList());
        assertEquals(RequestStatus.ATENDIDA, items.get(1).getStatus());
    }

    @Test void propagatesConcurrentUpdateToBusinessLayer() {
        when(remote.save(any())).thenThrow(
                new co.edu.javeriana.recursos.contracts.exception.ConcurrentUpdateException(4L));
        ResourceRequest current = new ResourceRequest(4L, "Sismo", "Agua", 10,
                Priority.ALTA, RequestStatus.PENDIENTE, 0L);
        assertThrows(co.edu.javeriana.recursos.contracts.exception.ConcurrentUpdateException.class,
                () -> repository.save(current.withStatus(RequestStatus.ATENDIDA)));
    }

    @Test void forwardsRemoteErrorsInsteadOfSwallowingThem() {
        when(remote.findAll()).thenThrow(new IllegalStateException("Servidor de Datos no disponible"));
        assertThrows(IllegalStateException.class, () -> repository.findAll());
    }

    private ResourceRequest pending(Long id) {
        return new ResourceRequest(id, "Sismo", "Agua", 10, Priority.ALTA, RequestStatus.PENDIENTE);
    }

    private ResourceRequestData data(Long id, RequestStatus status) {
        return new ResourceRequestData(id, "Sismo", "Agua", 10, Priority.ALTA, status, 2L);
    }
}
