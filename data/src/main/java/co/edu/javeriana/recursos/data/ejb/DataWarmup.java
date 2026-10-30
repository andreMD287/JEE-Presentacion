package co.edu.javeriana.recursos.data.ejb;

import co.edu.javeriana.recursos.contracts.ResourceRequestDataService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Precalentamiento al arrancar (D-17).
 *
 * <p>La primera llamada a un servidor recién iniciado paga trabajo de arranque: conexión JDBC, primera consulta de
 * Hibernate, pool del bean y carga de clases. Aquí se hace ese trabajo durante el despliegue, antes de que WildFly
 * marque {@code data.war} como desplegado. Así el healthcheck solo reporta "sano" cuando la capa ya responde en
 * milisegundos, y la primera petición real no consume el timeout de 5 s de sus clientes (QA-DIS-1).</p>
 *
 * <p>Es de mejor esfuerzo: si falla (por ejemplo, PostgreSQL no responde), se registra y el despliegue sigue.</p>
 */
@Singleton
@Startup
public class DataWarmup {

    private static final Logger LOG = Logger.getLogger(DataWarmup.class.getName());

    @EJB
    ResourceRequestDataService data;

    @PostConstruct
    void warmUp() {
        long start = System.nanoTime();
        try {
            data.findAll();      // consulta JPQL de listado + conexión del pool
            data.findById(0L);   // camino de búsqueda por id (no existe: devuelve null)
            LOG.info(() -> "Calentamiento de data completo en " + elapsedMs(start) + " ms");
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Calentamiento de data fallido tras " + elapsedMs(start)
                    + " ms; la primera petición pagará el arranque", e);
        }
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000L;
    }
}
