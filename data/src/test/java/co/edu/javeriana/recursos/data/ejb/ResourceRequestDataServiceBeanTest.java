package co.edu.javeriana.recursos.data.ejb;

import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestData;
import co.edu.javeriana.recursos.data.entity.ResourceRequestEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceRequestDataServiceBeanTest {

    @Mock
    EntityManager em;

    @Mock
    TypedQuery<ResourceRequestEntity> query;

    ResourceRequestDataServiceBean bean;

    @BeforeEach
    void setUp() {
        bean = new ResourceRequestDataServiceBean();
        bean.em = em;
    }

    @Test
    void saveWithoutIdPersistsAndReturnsGeneratedId() {
        doAnswer(inv -> {
            inv.<ResourceRequestEntity>getArgument(0).setId(10L); // simula el IDENTITY de PostgreSQL
            return null;
        }).when(em).persist(any(ResourceRequestEntity.class));

        ResourceRequestData saved = bean.save(data(null, RequestStatus.PENDIENTE));

        ArgumentCaptor<ResourceRequestEntity> captor = ArgumentCaptor.forClass(ResourceRequestEntity.class);
        verify(em).persist(captor.capture());
        verify(em).flush();
        assertEquals("Inundacion", captor.getValue().getDisasterDescription());
        assertEquals(10L, saved.getId());
        assertEquals(RequestStatus.PENDIENTE, saved.getStatus());
    }

    @Test
    void saveWithIdUpdatesTheStoredRow() {
        ResourceRequestEntity stored = entity(5L, RequestStatus.PENDIENTE);
        when(em.find(ResourceRequestEntity.class, 5L)).thenReturn(stored);

        ResourceRequestData saved = bean.save(data(5L, RequestStatus.ATENDIDA));

        assertEquals(RequestStatus.ATENDIDA, stored.getStatus());
        assertEquals(RequestStatus.ATENDIDA, saved.getStatus());
        verify(em, never()).persist(any());
    }

    @Test
    void saveWithUnknownIdFails() {
        when(em.find(ResourceRequestEntity.class, 99L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> bean.save(data(99L, RequestStatus.ATENDIDA)));
    }

    @Test
    void saveNullFails() {
        assertThrows(IllegalArgumentException.class, () -> bean.save(null));
        verifyNoInteractions(em);
    }

    @Test
    void findByIdReturnsNullWhenMissingOrIdIsNull() {
        when(em.find(ResourceRequestEntity.class, 7L)).thenReturn(null);

        assertNull(bean.findById(7L));
        assertNull(bean.findById(null));
    }

    @Test
    void findByIdMapsTheEntity() {
        when(em.find(ResourceRequestEntity.class, 3L)).thenReturn(entity(3L, RequestStatus.ATENDIDA));

        assertEquals(new ResourceRequestData(3L, "Inundacion", "Agua", 100, Priority.ALTA, RequestStatus.ATENDIDA),
                bean.findById(3L));
    }

    @Test
    void findAllMapsRowsInQueryOrder() {
        when(em.createQuery("SELECT r FROM ResourceRequestEntity r ORDER BY r.id", ResourceRequestEntity.class))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(entity(1L, RequestStatus.PENDIENTE),
                entity(2L, RequestStatus.ATENDIDA)));

        List<ResourceRequestData> all = bean.findAll();

        assertEquals(List.of(1L, 2L), all.stream().map(ResourceRequestData::getId).toList());
    }

    private static ResourceRequestData data(Long id, RequestStatus status) {
        return new ResourceRequestData(id, "Inundacion", "Agua", 100, Priority.ALTA, status);
    }

    private static ResourceRequestEntity entity(Long id, RequestStatus status) {
        ResourceRequestEntity e = new ResourceRequestEntity();
        e.setId(id);
        e.setDisasterDescription("Inundacion");
        e.setResourceType("Agua");
        e.setQuantity(100);
        e.setPriority(Priority.ALTA);
        e.setStatus(status);
        return e;
    }
}
