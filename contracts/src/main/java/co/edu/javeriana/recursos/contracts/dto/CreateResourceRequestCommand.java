package co.edu.javeriana.recursos.contracts.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * Datos de entrada para registrar una solicitud (igual que en el Taller 2).
 *
 * <p>No lleva id ni estado: los asigna el sistema. La prioridad llega como texto y
 * la cantidad como {@code Integer} para que la Lógica pueda reportar como error de
 * negocio un valor inválido o ausente.</p>
 *
 * <p>Es una clase JavaBean (constructor vacío + getters/setters) y no un
 * {@code record}: la EL de Tomcat 10.1 (EL 5.0) no lee componentes de records,
 * JAXB (SOAP) exige constructor vacío y setters, y viaja por EJB remoto.</p>
 */
public class CreateResourceRequestCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    private String disasterDescription;
    private String resourceType;
    private Integer quantity;
    private String priority;

    public CreateResourceRequestCommand() {
    }

    public CreateResourceRequestCommand(String disasterDescription, String resourceType,
                                        Integer quantity, String priority) {
        this.disasterDescription = disasterDescription;
        this.resourceType = resourceType;
        this.quantity = quantity;
        this.priority = priority;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CreateResourceRequestCommand other)) {
            return false;
        }
        return Objects.equals(disasterDescription, other.disasterDescription)
                && Objects.equals(resourceType, other.resourceType)
                && Objects.equals(quantity, other.quantity)
                && Objects.equals(priority, other.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(disasterDescription, resourceType, quantity, priority);
    }

    @Override
    public String toString() {
        return "CreateResourceRequestCommand{disasterDescription='" + disasterDescription + '\''
                + ", resourceType='" + resourceType + '\''
                + ", quantity=" + quantity
                + ", priority='" + priority + "'}";
    }
}
