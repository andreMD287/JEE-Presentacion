package co.edu.javeriana.recursos.contracts.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Vista de solo lectura de una solicitud, devuelta por la capa de Lógica.
 *
 * <p>JavaBean serializable (ver {@link CreateResourceRequestCommand} para el porqué
 * de no usar {@code record}). Los setters existen solo para JAXB/JSON-B.</p>
 */
public class ResourceRequestView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String disasterDescription;
    private String resourceType;
    private int quantity;
    private Priority priority;
    private RequestStatus status;

    public ResourceRequestView() {
    }

    public ResourceRequestView(Long id, String disasterDescription, String resourceType,
                               int quantity, Priority priority, RequestStatus status) {
        this.id = id;
        this.disasterDescription = disasterDescription;
        this.resourceType = resourceType;
        this.quantity = quantity;
        this.priority = priority;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisasterDescription() {
        return disasterDescription;
    }

    public void setDisasterDescription(String disasterDescription) {
        this.disasterDescription = disasterDescription;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceRequestView other)) {
            return false;
        }
        return quantity == other.quantity
                && Objects.equals(id, other.id)
                && Objects.equals(disasterDescription, other.disasterDescription)
                && Objects.equals(resourceType, other.resourceType)
                && priority == other.priority
                && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, disasterDescription, resourceType, quantity, priority, status);
    }

    @Override
    public String toString() {
        return "ResourceRequestView{id=" + id
                + ", disasterDescription='" + disasterDescription + '\''
                + ", resourceType='" + resourceType + '\''
                + ", quantity=" + quantity
                + ", priority=" + priority
                + ", status=" + status + '}';
    }
}
