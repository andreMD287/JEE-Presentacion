package co.edu.javeriana.recursos.integration;

import co.edu.javeriana.recursos.contracts.JndiNames;
import co.edu.javeriana.recursos.contracts.RemoteEjbLocator;
import co.edu.javeriana.recursos.contracts.ResourceRequestDataService;
import co.edu.javeriana.recursos.contracts.ResourceRequestService;

/** Prueba de lectura sin escrituras para medir la primera llamada EJB tras un reinicio. */
public final class DiagnosticoPrimeraInvocacion {
    private DiagnosticoPrimeraInvocacion() { }

    public static void main(String[] args) {
        String target = System.getenv("PROBE_TARGET");
        if (!"data".equals(target) && !"logic".equals(target)) {
            throw new IllegalArgumentException("PROBE_TARGET debe ser data o logic");
        }

        long beforeLookup = System.nanoTime();
        final Object proxy;
        if ("data".equals(target)) {
            proxy = RemoteEjbLocator.lookup(
                    "DATA_URL", JndiNames.DATA_SERVICE, ResourceRequestDataService.class);
        } else {
            proxy = RemoteEjbLocator.lookup(
                    "LOGIC_URL", JndiNames.LOGIC_SERVICE, ResourceRequestService.class);
        }
        System.out.printf("[DIAG] %s: lookup/proxy en %d ms%n", target, elapsed(beforeLookup));

        boolean firstPassed = false;
        for (int i = 1; i <= 2; i++) {
            long beforeInvocation = System.nanoTime();
            try {
                int rows = "data".equals(target)
                        ? ((ResourceRequestDataService) proxy).findAll().size()
                        : ((ResourceRequestService) proxy).findAll().size();
                System.out.printf("[DIAG] %s: llamada %d OK; %d filas; %d ms%n",
                        target, i, rows, elapsed(beforeInvocation));
                if (i == 1) firstPassed = true;
            } catch (Exception error) {
                System.out.printf("[DIAG] %s: llamada %d FALLO tras %d ms%n",
                        target, i, elapsed(beforeInvocation));
                Throwable cause = error;
                for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
                    System.out.printf("       causa %d: %s: %s%n", depth,
                            cause.getClass().getSimpleName(), cause.getMessage());
                    if (cause.getCause() == cause) break;
                }
            }
        }

        if (!firstPassed) {
            throw new IllegalStateException("La primera invocacion de " + target + " no paso (diagnostico)");
        }
    }

    private static long elapsed(long begin) {
        return (System.nanoTime() - begin) / 1_000_000L;
    }
}
