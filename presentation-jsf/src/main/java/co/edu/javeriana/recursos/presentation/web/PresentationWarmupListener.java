package co.edu.javeriana.recursos.presentation.web;

import co.edu.javeriana.recursos.presentation.client.RequestClient;
import co.edu.javeriana.recursos.presentation.client.RequestClientSelector;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Primera llamada del cliente EJB o REST durante el arranque de Tomcat. */
public class PresentationWarmupListener implements ServletContextListener {
    private static final Logger LOG = Logger.getLogger(PresentationWarmupListener.class.getName());

    @Override public void contextInitialized(ServletContextEvent event) {
        try {
            warmUp(RequestClientSelector.get());
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudo iniciar el calentamiento de presentation", e);
        }
    }

    void warmUp(RequestClient client) {
        long start = System.nanoTime();
        try {
            client.findAll();
            LOG.info(() -> "Calentamiento de presentation completo en " + elapsedMs(start) + " ms");
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Calentamiento de presentation fallido tras " + elapsedMs(start)
                    + " ms; la primera petición puede ser más lenta", e);
        }
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000L;
    }
}
