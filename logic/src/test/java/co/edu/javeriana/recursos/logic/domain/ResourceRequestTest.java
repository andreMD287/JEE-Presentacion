package co.edu.javeriana.recursos.logic.domain;

import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceRequestTest {
    @Test void versionIsOpaqueAndPreservedWhenChangingStatus() {
        ResourceRequest original = new ResourceRequest(5L, "Sismo", "Agua", 10,
                Priority.ALTA, RequestStatus.PENDIENTE, 17L);
        ResourceRequest changed = original.withStatus(RequestStatus.ATENDIDA);
        assertEquals(Long.valueOf(17L), original.getVersion());
        assertEquals(Long.valueOf(17L), changed.getVersion());
        assertEquals(RequestStatus.PENDIENTE, original.getStatus());
        assertEquals(RequestStatus.ATENDIDA, changed.getStatus());
    }

    @Test void newRequestsStartWithoutVersion() {
        ResourceRequest newRequest = new ResourceRequest(null, "Sismo", "Agua", 10,
                Priority.ALTA, RequestStatus.PENDIENTE);
        assertNull(newRequest.getVersion());
    }
}
