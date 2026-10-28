package co.edu.javeriana.recursos.contracts;

import co.edu.javeriana.recursos.contracts.dto.ResourceRequestData;
import jakarta.ejb.Remote;

import java.util.List;

/**
 * Contrato de la capa de Datos.
 *
 * <p>Lo implementa un EJB {@code @Stateless} desplegado en el WildFly "data" y lo
 * consume el adaptador remoto del puerto {@code ResourceRequestRepository} de la
 * capa de Lógica. Nombre JNDI en {@link JndiNames#DATA_SERVICE}.</p>
 *
 * <p>Mismo contrato del puerto del Taller 2, con dos diferencias impuestas por la
 * llamada remota: viaja un DTO serializable en lugar de la clase de dominio, y
 * {@link #findById} devuelve {@code null} en lugar de {@code Optional} (que no es
 * serializable). El adaptador de Lógica vuelve a envolverlo en {@code Optional}.</p>
 *
 * <ul>
 *   <li>{@link #save}: si el id es {@code null} crea la solicitud y asigna id; si no,
 *       reemplaza la almacenada. Devuelve lo que quedó guardado.</li>
 *   <li>{@link #findAll}: ordenadas por id ascendente.</li>
 *   <li>No contiene reglas de negocio ni filtra por estado.</li>
 * </ul>
 */
@Remote
public interface ResourceRequestDataService {

    ResourceRequestData save(ResourceRequestData request);

    /** @return la solicitud, o {@code null} si no existe o el id es {@code null}. */
    ResourceRequestData findById(Long id);

    List<ResourceRequestData> findAll();
}
