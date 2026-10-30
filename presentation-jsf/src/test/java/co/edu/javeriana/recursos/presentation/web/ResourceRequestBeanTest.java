package co.edu.javeriana.recursos.presentation.web;

import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.presentation.client.RequestClient;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceRequestBeanTest {
    private ResourceRequestView row(long id, RequestStatus status) {
        return new ResourceRequestView(id, "Sismo", "Agua", 2, Priority.MEDIA, status);
    }
    @Test void listsAndFiltersPending() {
        RequestClient client = mock(RequestClient.class);
        when(client.findAll()).thenReturn(List.of(row(1, RequestStatus.PENDIENTE),
                row(2, RequestStatus.ATENDIDA)));
        ResourceRequestBean bean = new ResourceRequestBean(client);
        bean.load();
        assertEquals(2, bean.getTotalCount());
        assertEquals(1, bean.getPendingCount());
        bean.togglePending();
        assertEquals(1, bean.getShownRequests().size());
        assertEquals(1L, bean.getShownRequests().get(0).getId());
    }
    @Test void registersAndRefreshes() {
        RequestClient client = mock(RequestClient.class);
        when(client.register(any())).thenReturn(row(5, RequestStatus.PENDIENTE));
        when(client.findAll()).thenReturn(List.of(row(5, RequestStatus.PENDIENTE)));
        ResourceRequestBean bean = new ResourceRequestBean(client);
        bean.setDescription("Sismo"); bean.setResourceType("Agua");
        bean.setQuantity(2); bean.setPriority("MEDIA");
        bean.register();
        assertEquals("Solicitud #5 registrada correctamente.", bean.getNotice());
        assertNull(bean.getDescription());
        assertEquals(1, bean.getTotalCount());
    }
    @Test void showsBusinessConflictWithoutTechnicalDetails() {
        RequestClient client = mock(RequestClient.class);
        when(client.attend(7L)).thenThrow(new AlreadyAttendedException(7L));
        when(client.findAll()).thenReturn(List.of(row(7, RequestStatus.ATENDIDA)));
        ResourceRequestBean bean = new ResourceRequestBean(client);
        bean.attend(7L);
        assertTrue(bean.getError().contains("ya fue atendida"));
    }
    @Test void technicalFailureShowsControlledMessageWithoutStackTrace() {
        RequestClient client = mock(RequestClient.class);
        when(client.findAll()).thenThrow(new IllegalStateException("contraseña interna"));
        ResourceRequestBean bean = new ResourceRequestBean(client);
        assertDoesNotThrow(bean::load);
        assertTrue(bean.getError().contains("No se pudo acceder al servicio"));
        assertFalse(bean.getError().contains("Exception"));
        assertFalse(bean.getError().contains("contraseña interna"));
        assertEquals(0, bean.getTotalCount());
    }
    @Test void searchRejectsInvalidIdWithoutRemoteCall() {
        RequestClient client = mock(RequestClient.class);
        ResourceRequestBean bean = new ResourceRequestBean(client);
        bean.setSearchId(0L);
        bean.search();
        assertTrue(bean.getError().contains("ID válido"));
        verifyNoInteractions(client);
    }
}
