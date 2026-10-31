package co.edu.javeriana.recursos.services.client;

import jakarta.ejb.NoSuchEJBException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/** Identifica fallos de disponibilidad sin clasificar errores internos como caidas. */
public final class DependencyAvailability {
    private DependencyAvailability() {
    }

    public static boolean isUnavailable(Throwable failure) {
        Set<Throwable> visited =
                Collections.newSetFromMap(new IdentityHashMap<>());
        return search(failure, visited);
    }

    private static boolean search(Throwable failure, Set<Throwable> visited) {
        if (failure == null || !visited.add(failure)) {
            return false;
        }

        if (failure instanceof NoSuchEJBException
                || failure instanceof ConnectException
                || failure instanceof NoRouteToHostException
                || failure instanceof SocketTimeoutException
                || failure instanceof UnknownHostException
                || failure instanceof TimeoutException) {
            return true;
        }

        if (search(failure.getCause(), visited)) {
            return true;
        }

        for (Throwable suppressed : failure.getSuppressed()) {
            if (search(suppressed, visited)) {
                return true;
            }
        }

        return false;
    }
}