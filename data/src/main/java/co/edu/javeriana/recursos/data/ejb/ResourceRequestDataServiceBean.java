package co.edu.javeriana.recursos.data.ejb;

import co.edu.javeriana.recursos.contracts.ResourceRequestDataService;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestData;
import co.edu.javeriana.recursos.data.entity.ResourceRequestEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

/**
 * Implementación de la capa de Datos: persiste solicitudes en PostgreSQL con JPA.
 *
 * <p>Cada método corre en una transacción JTA gestionada por el contenedor
 * ({@code REQUIRED} por defecto). Como la Lógica llama con {@code NOT_SUPPORTED},
 * la transacción nace y termina aquí: es local a esta capa (decisión D-11).</p>
 */
@Stateless
public class ResourceRequestDataServiceBean implements ResourceRequestDataService {

    @PersistenceContext(unitName = "recursosPU")
    EntityManager em;

    @Override
    public ResourceRequestData save(ResourceRequestData request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud a guardar no puede ser null");
        }
        if (request.getId() == null) {
            ResourceRequestEntity entity = new ResourceRequestEntity();
            copy(request, entity);
            em.persist(entity);
            em.flush(); // asigna el id generado por la base de datos
            return toData(entity);
        }
        ResourceRequestEntity entity = em.find(ResourceRequestEntity.class, request.getId());
        if (entity == null) {
            throw new IllegalArgumentException("No existe la solicitud con id " + request.getId());
        }
        copy(request, entity);
        return toData(entity);
    }

    @Override
    public ResourceRequestData findById(Long id) {
        if (id == null) {
            return null;
        }
        ResourceRequestEntity entity = em.find(ResourceRequestEntity.class, id);
        return entity == null ? null : toData(entity);
    }

    @Override
    public List<ResourceRequestData> findAll() {
        return em.createQuery("SELECT r FROM ResourceRequestEntity r ORDER BY r.id", ResourceRequestEntity.class)
                .getResultList()
                .stream()
                .map(ResourceRequestDataServiceBean::toData)
                .toList();
    }

    private static void copy(ResourceRequestData from, ResourceRequestEntity to) {
        to.setDisasterDescription(from.getDisasterDescription());
        to.setResourceType(from.getResourceType());
        to.setQuantity(from.getQuantity());
        to.setPriority(from.getPriority());
        to.setStatus(from.getStatus());
    }

    static ResourceRequestData toData(ResourceRequestEntity e) {
        return new ResourceRequestData(e.getId(), e.getDisasterDescription(), e.getResourceType(),
                e.getQuantity(), e.getPriority(), e.getStatus());
    }
}
