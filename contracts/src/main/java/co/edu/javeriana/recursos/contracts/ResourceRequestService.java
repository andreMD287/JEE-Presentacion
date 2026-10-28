package co.edu.javeriana.recursos.contracts;

import co.edu.javeriana.recursos.contracts.dto.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.contracts.dto.ResourceRequestView;
import jakarta.ejb.Remote;

import java.util.List;

/**
 * Contrato de la capa de Lógica (mismas operaciones del Taller 2).
 *
 * <p>Lo implementa un EJB {@code @Stateless} desplegado en el WildFly "logic".
 * Lo consumen la presentación JSF (etapa 1) y la capa de Servicios (etapas 2 y 3)
 * por EJB remoto. Nombre JNDI en {@link JndiNames#LOGIC_SERVICE}.</p>
 *
 * <p>Errores de negocio: {@link co.edu.javeriana.recursos.contracts.exception.BusinessValidationException},
 * {@link co.edu.javeriana.recursos.contracts.exception.RequestNotFoundException} y
 * {@link co.edu.javeriana.recursos.contracts.exception.AlreadyAttendedException}.</p>
 */
@Remote
public interface ResourceRequestService {

    ResourceRequestView register(CreateResourceRequestCommand command);

    List<ResourceRequestView> findAll();

    ResourceRequestView findById(Long id);

    ResourceRequestView attend(Long id);

    List<ResourceRequestView> findPending();
}
