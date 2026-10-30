package co.edu.javeriana.recursos.services.client;

import co.edu.javeriana.recursos.contracts.ResourceRequestService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Prepara el cliente remoto hacia Logica al desplegar Servicios. */
@Singleton
@Startup
public class ServicesWarmup {
    private static final Logger LOG = Logger.getLogger(ServicesWarmup.class.getName());

    @PostConstruct
    void warmUp() {
        warmUp(LogicClient::get);
    }

    void warmUp(Supplier<ResourceRequestService> serviceSupplier) {
        long start = System.nanoTime();
        try {
            serviceSupplier.get().findAll();
            LOG.info(() -> "Calentamiento de services completo en " + elapsedMs(start) + " ms");
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Calentamiento de services fallido tras " + elapsedMs(start)
                    + " ms; se intentara de nuevo con la primera peticion", e);
        }
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000L;
    }
}
