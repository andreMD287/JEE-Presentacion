package co.edu.javeriana.recursos.contracts;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Map;
import java.util.Properties;

/**
 * Obtiene el proxy de un EJB remoto de WildFly ({@code remote+http}) con la misma política en todos los
 * clientes: presentación (Tomcat → logic), servicios (services → logic) y lógica (logic → data).
 *
 * <p>Política común (probada en los spikes S-1 y S-2, ver {@code docs/evidencias/spikes.md}):</p>
 * <ul>
 *   <li>Contexto JNDI programático con {@code WildFlyInitialContextFactory}, URL y usuario de aplicación.</li>
 *   <li>Timeout de invocación ({@code EJB_INVOCATION_TIMEOUT_MS}, 5000 ms por defecto). Sin él, la primera
 *       llamada tras la caída de una capa tarda ~135 s en fallar; con él falla en el tiempo configurado
 *       (escenario QA-DIS-1).</li>
 * </ul>
 *
 * <p>El proxy es seguro para varios hilos y se reconecta solo cuando la capa vuelve: se puede guardar y
 * reutilizar.</p>
 */
public final class RemoteEjbLocator {

    static final String FACTORY = "org.wildfly.naming.client.WildFlyInitialContextFactory";
    static final String INVOCATION_TIMEOUT = "invocation.timeout";
    static final String DEFAULT_TIMEOUT_MS = "5000";

    private RemoteEjbLocator() {
    }

    /**
     * Busca un EJB remoto leyendo las credenciales y el timeout de las variables de entorno.
     *
     * @param urlVariable variable con la URL del servidor, p. ej. {@code "DATA_URL"} o {@code "LOGIC_URL"}
     * @param jndiName    nombre JNDI, p. ej. {@link JndiNames#DATA_SERVICE}
     * @param type        interfaz remota esperada
     */
    public static <T> T lookup(String urlVariable, String jndiName, Class<T> type) {
        return lookup(environment(System.getenv(), urlVariable), jndiName, type);
    }

    static <T> T lookup(Properties environment, String jndiName, Class<T> type) {
        try {
            Context context = new InitialContext(environment);
            return type.cast(context.lookup(jndiName));
        } catch (NamingException e) {
            throw new IllegalStateException("No se pudo obtener " + jndiName + " en "
                    + environment.getProperty(Context.PROVIDER_URL), e);
        }
    }

    /** Propiedades del contexto JNDI a partir de las variables de entorno. */
    static Properties environment(Map<String, String> env, String urlVariable) {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, FACTORY);
        props.put(Context.PROVIDER_URL, required(env, urlVariable));
        props.put(Context.SECURITY_PRINCIPAL, required(env, "EJB_USER"));
        props.put(Context.SECURITY_CREDENTIALS, required(env, "EJB_PASSWORD"));
        props.put(INVOCATION_TIMEOUT, env.getOrDefault("EJB_INVOCATION_TIMEOUT_MS", DEFAULT_TIMEOUT_MS));
        return props;
    }

    private static String required(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + name);
        }
        return value;
    }
}
