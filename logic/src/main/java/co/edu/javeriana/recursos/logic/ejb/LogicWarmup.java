package co.edu.javeriana.recursos.logic.ejb;

import co.edu.javeriana.recursos.logic.port.ResourceRequestRepository;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Prepara el cliente EJB remoto hacia Datos durante el arranque de Logica. */
@Singleton
@Startup
public class LogicWarmup {
    private static final Logger LOG = Logger.getLogger(LogicWarmup.class.getName());

    @Inject
    ResourceRequestRepository repository;

    @PostConstruct
    void warmUp() {
        long start = System.nanoTime();
        try {
            repository.findAll();
            LOG.info(() -> "Calentamiento de logic completo en " + elapsedMs(start) + " ms");
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Calentamiento de logic fallido tras " + elapsedMs(start)
                    + " ms; se intentara de nuevo con la primera peticion", e);
        }
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000L;
    }
}
