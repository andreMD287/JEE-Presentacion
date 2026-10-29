package co.edu.javeriana.recursos.contracts.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Solicitud tal como la guarda y devuelve la capa de Datos.
 *
 * <p>Es distinta de {@link ResourceRequestView} a propósito: una es el contrato
 * Lógica→Datos y la otra el contrato Lógica→clientes. Hoy tienen los mismos campos,
 * pero pueden evolucionar por separado (p. ej. auditoría o versión en Datos).</p>
 */
public class ResourceRequestData implements Serializable {

    private static final long serialVersionUID = 2L;

    private Long id;
    private String disasterDescription;
    private String resourceType;
    private int quantity;
    private Priority priority;
    private RequestStatus status;
    /** Versión de la fila (bloqueo optimista, D-16). {@code null} solo al crear. */
    private Long version;

    public ResourceRequestData() {
    }

    public ResourceRequestData(Long id, String disasterDescription, String resourceType,
                               int quantity, Priority priority, RequestStatus status) {
        this.id = id;
        this.disasterDescription = disasterDescription;
        this.resourceType = resourceType;
        this.quantity = quantity;
        this.priority = priority;
        this.status = status;
    }

    public ResourceRequestData(Long id, String disasterDescription, String resourceType,
                               int quantity, Priority priority, RequestStatus status, Long version) {
        this(id, disasterDescription, resourceType, quantity, priority, status);
        this.version = version;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceRequestData other)) {
            return false;
        }
        return quantity == other.quantity
                && Objects.equals(id, other.id)
                && Objects.equals(disasterDescription, other.disasterDescription)
                && Objects.equals(resourceType, other.resourceType)
                && priority == other.priority
                && status == other.status
                && Objects.equals(version, other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, disasterDescription, resourceType, quantity, priority, status, version);
    }

    @Override
    public String toString() {
        return "ResourceRequestData{id=" + id
                + ", disasterDescription='" + disasterDescription + '\''
                + ", resourceType='" + resourceType + '\''
                + ", quantity=" + quantity
                + ", priority=" + priority
                + ", status=" + status
                + ", version=" + version + '}';
    }
}
