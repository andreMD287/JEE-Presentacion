package co.edu.javeriana.recursos.logic.adapter;

import co.edu.javeriana.recursos.contracts.JndiNames;
import co.edu.javeriana.recursos.contracts.RemoteEjbLocator;
import co.edu.javeriana.recursos.contracts.ResourceRequestDataService;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestData;
import co.edu.javeriana.recursos.logic.domain.ResourceRequest;
import co.edu.javeriana.recursos.logic.port.ResourceRequestRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

/** Implementación del puerto que traduce entre dominio local y DTO remoto de Datos. */
@ApplicationScoped
public class RemoteResourceRequestRepository implements ResourceRequestRepository {
    private volatile ResourceRequestDataService remote;

    public RemoteResourceRequestRepository() {
        // Constructor requerido por CDI. El proxy se obtiene en la primera llamada.
    }

    /** Constructor para pruebas sin servidor ni conexión de red. */
    RemoteResourceRequestRepository(ResourceRequestDataService remote) {
        this.remote = remote;
    }

    @Override
    public ResourceRequest save(ResourceRequest request) {
        ResourceRequestData saved = dataService().save(toData(request));
        return toDomain(saved);
    }

    @Override
    public Optional<ResourceRequest> findById(Long id) {
        return Optional.ofNullable(dataService().findById(id)).map(RemoteResourceRequestRepository::toDomain);
    }

    @Override
    public List<ResourceRequest> findAll() {
        return dataService().findAll().stream().map(RemoteResourceRequestRepository::toDomain).toList();
    }

    private ResourceRequestDataService dataService() {
        ResourceRequestDataService existing = remote;
        if (existing == null) {
            synchronized (this) {
                existing = remote;
                if (existing == null) {
                    existing = RemoteEjbLocator.lookup(
                            "DATA_URL", JndiNames.DATA_SERVICE, ResourceRequestDataService.class);
                    remote = existing;
                }
            }
        }
        return existing;
    }

    private static ResourceRequestData toData(ResourceRequest value) {
        return new ResourceRequestData(value.getId(), value.getDisasterDescription(),
                value.getResourceType(), value.getQuantity(), value.getPriority(), value.getStatus(), value.getVersion());
    }

    private static ResourceRequest toDomain(ResourceRequestData value) {
        return new ResourceRequest(value.getId(), value.getDisasterDescription(),
                value.getResourceType(), value.getQuantity(), value.getPriority(), value.getStatus(), value.getVersion());
    }
}
