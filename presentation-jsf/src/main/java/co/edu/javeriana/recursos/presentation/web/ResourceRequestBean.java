package co.edu.javeriana.recursos.presentation.web;

import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import co.edu.javeriana.recursos.contracts.exception.BusinessException;
import co.edu.javeriana.recursos.presentation.client.RequestClient;
import co.edu.javeriana.recursos.presentation.client.RequestClientSelector;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Interacción de la página JSF con los cinco casos de uso. */
@Named("requests")
@ViewScoped
public class ResourceRequestBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ResourceRequestBean.class.getName());

    private transient RequestClient injectedClient;
    private String description;
    private String resourceType;
    private Integer quantity;
    private String priority = "MEDIA";
    private Long searchId;
    private ResourceRequestView selected;
    private List<ResourceRequestView> requests = List.of();
    private boolean onlyPending;
    private String notice;
    private String error;

    public ResourceRequestBean() { }
    ResourceRequestBean(RequestClient client) { this.injectedClient = client; }

    private RequestClient backend() {
        return injectedClient != null ? injectedClient : RequestClientSelector.get();
    }

    public void load() {
        refresh();
    }

    public void register() {
        clearMessages();
        try {
            ResourceRequestView result = backend().register(new CreateResourceRequestCommand(
                    description, resourceType, quantity, priority));
            notice = "Solicitud #" + result.getId() + " registrada correctamente.";
            description = null;
            resourceType = null;
            quantity = null;
            priority = "MEDIA";
            refresh();
        } catch (RuntimeException e) {
            report(e);
        }
    }

    public void search() {
        clearMessages();
        selected = null;
        if (searchId == null || searchId <= 0) {
            error = "Ingresa un ID válido para buscar.";
            return;
        }
        try {
            selected = backend().findById(searchId);
        } catch (RuntimeException e) {
            report(e);
        }
    }

    public void attend(Long id) {
        clearMessages();
        try {
            ResourceRequestView result = backend().attend(id);
            notice = "Solicitud #" + result.getId() + " atendida correctamente.";
            if (selected != null && id.equals(selected.getId())) selected = result;
            refresh();
        } catch (RuntimeException e) {
            report(e);
            refresh();
        }
    }

    public void togglePending() {
        onlyPending = !onlyPending;
        refresh();
    }

    public void reload() {
        clearMessages();
        refresh();
    }

    private void refresh() {
        try {
            requests = List.copyOf(backend().findAll());
        } catch (RuntimeException e) {
            requests = List.of();
            report(e);
        }
    }

    private void report(RuntimeException e) {
        if (e instanceof BusinessException) {
            error = e.getMessage();
        } else {
            LOG.log(Level.WARNING, "No se pudo completar la consulta desde presentación", e);
            error = "No se pudo acceder al servicio. Comprueba las capas y vuelve a intentar.";
        }
    }

    private void clearMessages() { notice = null; error = null; }

    public List<ResourceRequestView> getShownRequests() {
        return onlyPending ? requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDIENTE).toList() : requests;
    }
    public long getPendingCount() {
        return requests.stream().filter(r -> r.getStatus() == RequestStatus.PENDIENTE).count();
    }
    public long getAttendedCount() {
        return requests.stream().filter(r -> r.getStatus() == RequestStatus.ATENDIDA).count();
    }
    public int getTotalCount() { return requests.size(); }
    public String getMode() { return RequestClientSelector.mode().toUpperCase(java.util.Locale.ROOT); }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getSearchId() { return searchId; }
    public void setSearchId(Long searchId) { this.searchId = searchId; }
    public ResourceRequestView getSelected() { return selected; }
    public boolean isOnlyPending() { return onlyPending; }
    public String getNotice() { return notice; }
    public String getError() { return error; }
}
