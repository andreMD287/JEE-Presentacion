package co.edu.javeriana.recursos.logic.port;

import co.edu.javeriana.recursos.logic.domain.ResourceRequest;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia definido por la Lógica, independiente de JPA/EJB remoto. */
public interface ResourceRequestRepository {
    ResourceRequest save(ResourceRequest request);
    Optional<ResourceRequest> findById(Long id);
    List<ResourceRequest> findAll();
}
