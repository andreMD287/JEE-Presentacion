package co.edu.javeriana.recursos.contracts;

import org.junit.jupiter.api.Test;

import javax.naming.Context;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteEjbLocatorTest {

    private static final Map<String, String> ENV = Map.of(
            "DATA_URL", "remote+http://data:8080",
            "EJB_USER", "ejbuser",
            "EJB_PASSWORD", "secreto");

    @Test
    void buildsWildFlyRemoteContextFromEnvironment() {
        Properties props = RemoteEjbLocator.environment(ENV, "DATA_URL");

        assertEquals("org.wildfly.naming.client.WildFlyInitialContextFactory",
                props.get(Context.INITIAL_CONTEXT_FACTORY));
        assertEquals("remote+http://data:8080", props.get(Context.PROVIDER_URL));
        assertEquals("ejbuser", props.get(Context.SECURITY_PRINCIPAL));
        assertEquals("secreto", props.get(Context.SECURITY_CREDENTIALS));
    }

    @Test
    void usesFiveSecondInvocationTimeoutByDefault() {
        assertEquals("5000", RemoteEjbLocator.environment(ENV, "DATA_URL").get("invocation.timeout"));
    }

    @Test
    void invocationTimeoutCanBeConfigured() {
        var env = new java.util.HashMap<>(ENV);
        env.put("EJB_INVOCATION_TIMEOUT_MS", "2000");

        assertEquals("2000", RemoteEjbLocator.environment(env, "DATA_URL").get("invocation.timeout"));
    }

    @Test
    void failsClearlyWhenAVariableIsMissing() {
        var error = assertThrows(IllegalStateException.class,
                () -> RemoteEjbLocator.environment(ENV, "LOGIC_URL"));

        assertTrue(error.getMessage().contains("LOGIC_URL"));
    }
}
