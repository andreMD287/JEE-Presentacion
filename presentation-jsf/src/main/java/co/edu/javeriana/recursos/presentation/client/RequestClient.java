package co.edu.javeriana.recursos.presentation.client;

import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import java.util.List;

/** Acceso a las operaciones de negocio, independiente del protocolo elegido. */
public interface RequestClient {
    ResourceRequestView register(CreateResourceRequestCommand command);
    List<ResourceRequestView> findAll();
    ResourceRequestView findById(Long id);
    ResourceRequestView attend(Long id);
    List<ResourceRequestView> findPending();
}
