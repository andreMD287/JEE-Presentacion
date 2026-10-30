package co.edu.javeriana.recursos.logic.domain;

import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;

/** Modelo del negocio: no depende de JPA ni de protocolos remotos. */
public class ResourceRequest {
    private final Long id;
    private final String disasterDescription;
    private final String resourceType;
    private final int quantity;
    private final Priority priority;
    private final RequestStatus status;
    private final Long version;

    public ResourceRequest(Long id, String disasterDescription, String resourceType,
                           int quantity, Priority priority, RequestStatus status) {
        this(id, disasterDescription, resourceType, quantity, priority, status, null);
    }

    public ResourceRequest(Long id, String disasterDescription, String resourceType,
                           int quantity, Priority priority, RequestStatus status, Long version) {
        this.id = id;
        this.disasterDescription = disasterDescription;
        this.resourceType = resourceType;
        this.quantity = quantity;
        this.priority = priority;
        this.status = status;
        this.version = version;
    }

    public Long getId() { return id; }
    public String getDisasterDescription() { return disasterDescription; }
    public String getResourceType() { return resourceType; }
    public int getQuantity() { return quantity; }
    public Priority getPriority() { return priority; }
    public RequestStatus getStatus() { return status; }
    public Long getVersion() { return version; }

    public ResourceRequest withStatus(RequestStatus newStatus) {
        return new ResourceRequest(id, disasterDescription, resourceType, quantity, priority, newStatus, version);
    }
}
