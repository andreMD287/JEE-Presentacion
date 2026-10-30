package co.edu.javeriana.recursos.presentation.client;

import co.edu.javeriana.recursos.contracts.JndiNames;
import co.edu.javeriana.recursos.contracts.RemoteEjbLocator;
import co.edu.javeriana.recursos.contracts.ResourceRequestService;
import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import java.util.List;

/** La presentación consume Lógica por EJB remoto, sin hablar directamente con Datos. */
public final class EjbRequestClient implements RequestClient {
    private volatile ResourceRequestService remote;

    private ResourceRequestService service() {
        ResourceRequestService current = remote;
        if (current == null) {
            synchronized (this) {
                current = remote;
                if (current == null) {
                    current = RemoteEjbLocator.lookup(
                            "LOGIC_URL", JndiNames.LOGIC_SERVICE, ResourceRequestService.class);
                    remote = current;
                }
            }
        }
        return current;
    }

    @Override public ResourceRequestView register(CreateResourceRequestCommand command) {
        return service().register(command);
    }
    @Override public List<ResourceRequestView> findAll() { return service().findAll(); }
    @Override public ResourceRequestView findById(Long id) { return service().findById(id); }
    @Override public ResourceRequestView attend(Long id) { return service().attend(id); }
    @Override public List<ResourceRequestView> findPending() { return service().findPending(); }
}
