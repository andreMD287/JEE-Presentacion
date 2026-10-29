package co.edu.javeriana.recursos.logic.ejb;

import co.edu.javeriana.recursos.contracts.ResourceRequestService;
import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.contracts.exception.BusinessValidationException;
import co.edu.javeriana.recursos.contracts.exception.RequestNotFoundException;
import co.edu.javeriana.recursos.logic.domain.ResourceRequest;
import co.edu.javeriana.recursos.logic.port.ResourceRequestRepository;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Locale;

/** Reglas de negocio únicas, compartidas por los clientes JSF, REST y SOAP. */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ResourceRequestServiceBean implements ResourceRequestService {

    @Inject
    ResourceRequestRepository repository;

    @Override
    public ResourceRequestView register(CreateResourceRequestCommand command) {
        if (command == null) {
            throw new BusinessValidationException("Los datos de la solicitud son obligatorios");
        }
        String description = requireText(command.getDisasterDescription(),
                "La descripción del desastre es obligatoria");
        String resourceType = requireText(command.getResourceType(),
                "El tipo de recurso es obligatorio");
        Integer quantity = command.getQuantity();
        if (quantity == null || quantity <= 0) {
            throw new BusinessValidationException("La cantidad debe ser mayor que cero");
        }
        Priority priority = parsePriority(command.getPriority());
        ResourceRequest request = new ResourceRequest(null, description, resourceType,
                quantity, priority, RequestStatus.PENDIENTE);
        return toView(repository.save(request));
    }

    @Override
    public List<ResourceRequestView> findAll() {
        return repository.findAll().stream().map(ResourceRequestServiceBean::toView).toList();
    }

    @Override
    public ResourceRequestView findById(Long id) {
        return toView(findExisting(id));
    }

    @Override
    public ResourceRequestView attend(Long id) {
        ResourceRequest current = findExisting(id);
        if (current.getStatus() == RequestStatus.ATENDIDA) {
            throw new AlreadyAttendedException(id);
        }
        return toView(repository.save(current.withStatus(RequestStatus.ATENDIDA)));
    }

    @Override
    public List<ResourceRequestView> findPending() {
        return repository.findAll().stream()
                .filter(request -> request.getStatus() == RequestStatus.PENDIENTE)
                .map(ResourceRequestServiceBean::toView)
                .toList();
    }

    private ResourceRequest findExisting(Long id) {
        return repository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessValidationException(message);
        }
        return value.trim();
    }

    private static Priority parsePriority(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessValidationException("La prioridad es obligatoria");
        }
        try {
            return Priority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessValidationException("La prioridad debe ser BAJA, MEDIA o ALTA");
        }
    }

    private static ResourceRequestView toView(ResourceRequest request) {
        return new ResourceRequestView(request.getId(), request.getDisasterDescription(),
                request.getResourceType(), request.getQuantity(), request.getPriority(), request.getStatus());
    }
}
