package co.edu.javeriana.recursos.integration;

import co.edu.javeriana.recursos.contracts.JndiNames;
import co.edu.javeriana.recursos.contracts.RemoteEjbLocator;
import co.edu.javeriana.recursos.contracts.ResourceRequestService;
import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.Priority;
import co.edu.javeriana.recursos.contracts.dto.RequestStatus;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.contracts.exception.BusinessValidationException;
import co.edu.javeriana.recursos.contracts.exception.RequestNotFoundException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

/** Prueba integrada: cliente -> EJB logic -> EJB data -> PostgreSQL. */
public final class SmokeEjbIntegration {
    private SmokeEjbIntegration() { }

    public static void main(String[] args) throws Exception {
        ResourceRequestService service = RemoteEjbLocator.lookup(
                "LOGIC_URL", JndiNames.LOGIC_SERVICE, ResourceRequestService.class);
        System.out.println("[OK] Proxy EJB de Logica obtenido");

        // Una solicitud unica para no depender de si la base ya tiene filas.
        String description = "IT-EJB-" + UUID.randomUUID();
        List<ResourceRequestView> initial = service.findAll();
        ensure(initial != null, "findAll devolvio null");
        ResourceRequestView created = service.register(
                new CreateResourceRequestCommand(description, "Agua", 7, "MEDIA"));
        ensure(created != null && created.getId() != null && created.getId() > 0,
                "register no devolvio un ID valido");
        Long id = created.getId();
        ensure(created.getStatus() == RequestStatus.PENDIENTE, "Estado inicial incorrecto");
        ensure(created.getPriority() == Priority.MEDIA && created.getQuantity() == 7,
                "Cantidad o prioridad incorrecta");
        System.out.println("[OK] RF-1 registrar: id=" + id + ", estado=PENDIENTE");

        ensure(service.findAll().stream().anyMatch(item -> id.equals(item.getId())),
                "RF-2 no encuentra la solicitud");
        System.out.println("[OK] RF-2 listar todas");

        ResourceRequestView loaded = service.findById(id);
        ensure(description.equals(loaded.getDisasterDescription()), "RF-3 datos distintos");
        System.out.println("[OK] RF-3 consultar por ID");

        ensure(service.findPending().stream().anyMatch(item -> id.equals(item.getId())),
                "RF-5 no encuentra la solicitud pendiente");
        System.out.println("[OK] RF-5 listar pendientes");

        try {
            service.register(new CreateResourceRequestCommand("Prueba", "Agua", 0, "ALTA"));
            throw new AssertionError("Se acepto cantidad cero");
        } catch (BusinessValidationException expected) {
            ensure("VALIDATION_ERROR".equals(expected.getCode()), "Codigo de validacion incorrecto");
        }
        System.out.println("[OK] Validacion cantidad cero y codigo de negocio");

        try {
            service.findById(-1L);
            throw new AssertionError("Una solicitud inexistente no genero error");
        } catch (RequestNotFoundException expected) {
            ensure("REQUEST_NOT_FOUND".equals(expected.getCode()), "Codigo no encontrado incorrecto");
        }
        System.out.println("[OK] Solicitud inexistente y codigo de negocio");

        ResourceRequestView attended = service.attend(id);
        ensure(attended.getStatus() == RequestStatus.ATENDIDA, "RF-4 no cambio el estado");
        ensure(service.findById(id).getStatus() == RequestStatus.ATENDIDA,
                "RF-4 no persistio el cambio en el servicio");
        ensure(service.findPending().stream().noneMatch(item -> id.equals(item.getId())),
                "La solicitud atendida sigue pendiente");
        System.out.println("[OK] RF-4 atender y excluir de pendientes");

        try {
            service.attend(id);
            throw new AssertionError("Se acepto atender dos veces");
        } catch (AlreadyAttendedException expected) {
            ensure("ALREADY_ATTENDED".equals(expected.getCode()), "Codigo duplicado incorrecto");
        }
        System.out.println("[OK] Doble atencion rechazada correctamente");

        // Comprobacion de lectura SQL: sin hacer INSERT/UPDATE/DELETE por fuera del EJB.
        String url = "jdbc:postgresql://postgres:5432/recursos";
        try (Connection conn = DriverManager.getConnection(
                url, required("DB_USER"), required("DB_PASSWORD"));
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT disaster_description, resource_type, quantity, priority, status "
                             + "FROM resource_request WHERE id = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                ensure(rs.next(), "El registro no existe en PostgreSQL");
                ensure(description.equals(rs.getString("disaster_description")), "Descripcion SQL incorrecta");
                ensure("Agua".equals(rs.getString("resource_type")), "Recurso SQL incorrecto");
                ensure(rs.getInt("quantity") == 7, "Cantidad SQL incorrecta");
                ensure("MEDIA".equals(rs.getString("priority")), "Prioridad SQL incorrecta");
                ensure("ATENDIDA".equals(rs.getString("status")), "Estado SQL incorrecto");
                ensure(!rs.next(), "El ID aparece duplicado en PostgreSQL");
            }
        }
        System.out.println("[OK] Persistencia verificada directamente en PostgreSQL");
        System.out.println("[EXITO] Integracion EJB completa. Registro de prueba conservado: id=" + id);
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + key);
        }
        return value;
    }

    private static void ensure(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
