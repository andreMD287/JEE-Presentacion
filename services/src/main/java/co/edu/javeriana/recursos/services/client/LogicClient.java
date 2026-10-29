package co.edu.javeriana.recursos.services.client;

import co.edu.javeriana.recursos.contracts.JndiNames;
import co.edu.javeriana.recursos.contracts.RemoteEjbLocator;
import co.edu.javeriana.recursos.contracts.ResourceRequestService;

/** Proxy compartido de negocio: no contiene reglas ni acceso a Datos. */
public final class LogicClient {
    private static volatile ResourceRequestService proxy;

    private LogicClient() {
    }

    public static ResourceRequestService get() {
        ResourceRequestService current = proxy;
        if (current == null) {
            synchronized (LogicClient.class) {
                current = proxy;
                if (current == null) {
                    current = RemoteEjbLocator.lookup("LOGIC_URL", JndiNames.LOGIC_SERVICE,
                            ResourceRequestService.class);
                    proxy = current;
                }
            }
        }
        return current;
    }
}
