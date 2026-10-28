package co.edu.javeriana.recursos.contracts;

/**
 * Nombres JNDI de los EJB remotos.
 *
 * <p>Formato de WildFly para un WAR: {@code ejb:/<módulo>/<bean>!<interfaz>}. El
 * módulo es el nombre del WAR ({@code logic.war}, {@code data.war}), por eso cada
 * pom fija {@code finalName}.</p>
 */
public final class JndiNames {

    public static final String LOGIC_SERVICE =
            "ejb:/logic/ResourceRequestServiceBean!" + ResourceRequestService.class.getName();

    public static final String DATA_SERVICE =
            "ejb:/data/ResourceRequestDataServiceBean!" + ResourceRequestDataService.class.getName();

    private JndiNames() {
    }
}
