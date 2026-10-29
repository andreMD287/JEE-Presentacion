# Spikes de EJB remoto (Fase 1)

Fecha: 2026-10-28. Entorno: Docker Desktop 28.4.0 en Windows 11; imágenes `quay.io/wildfly/wildfly:38.0.1.Final-jdk17` y `tomcat:10.1-jdk17-temurin`.

Objetivo: comprobar antes de construir las capas que los dos tramos remotos de la etapa ① funcionan con los
contratos reales del módulo `contracts`, y fijar la forma de conectarse.

## Montaje

Tres contenedores con los contratos reales y una implementación mínima (no es la Lógica del T2):

| Contenedor | Contenido |
|---|---|
| `data` (WildFly) | `ResourceRequestDataServiceBean` en memoria + `TxProbeBean` (método `MANDATORY`) |
| `logic` (WildFly) | `ResourceRequestServiceBean` (`NOT_SUPPORTED`) que llama a `data` por EJB remoto |
| `web` (Tomcat 10.1) | JSF 4.0.28 + Weld 5.1.7 + cliente EJB de WildFly 38.0.1; página Facelets y servlet de prueba |

Usuario de aplicación en ambos WildFly: `add-user.sh -a -s -u <EJB_USER> -p <EJB_PASSWORD>` (ApplicationRealm).
Configuración de WildFly: `standalone.xml` por defecto, sin scripts CLI.

## Resultados

| ID | Prueba | Resultado |
|---|---|---|
| S-1 | Tomcat → `logic` por `remote+http://logic:8080` con contexto JNDI programático | **OK**: RF-1..RF-5 responden |
| S-1 | JSF + Weld arrancan en Tomcat; tabla Facelets lee los DTO (`#{r.id}`, `#{r.status}`) | **OK** (Weld 5.1.7, Mojarra 4.0.28) |
| S-1 | Error de negocio mostrado en JSF (`h:messages`) al registrar cantidad 0 | **OK**: `VALIDATION_ERROR: La cantidad debe ser mayor que cero` |
| S-2 | `logic` → `data` por `remote+http://data:8080` con contexto JNDI programático (opción A) | **OK**: no hizo falta `remote-outbound-connection` |
| S-2 | Serialización de `CreateResourceRequestCommand`, `ResourceRequestView`, `ResourceRequestData` y enums | **OK** |
| S-2 | Excepciones de negocio llegan al cliente con su clase y `getCode()` | **OK**: `VALIDATION_ERROR`, `REQUEST_NOT_FOUND`, `ALREADY_ATTENDED` |
| S-2 | Propagación de transacción `logic` → `data` con `REQUIRED` | **Se propaga** (el bean `MANDATORY` de `data` la recibe) |
| S-2 | Propagación con `NOT_SUPPORTED` en `logic` | **No se propaga** (`EJBTransactionRequiredException` en el bean `MANDATORY`) |
| QA-DIS-1 (previo) | Se detiene `data` y se consulta desde Tomcat | Error controlado `EJBCLIENT000409: No more destinations are available`, pero tarda **~72 s** |
| QA-DIS-1 (previo) | Se reinicia solo `data` | **OK**: el sistema vuelve sin reiniciar `logic` ni `web` |
| QA-REN-1 (previo) | 200 × `findAll` Tomcat → `logic` → `data` (en memoria), tras 10 de calentamiento | p50 = 6,16 ms; p95 = 12,90 ms (medición indicativa; la oficial se hace en F5) |

## Decisiones que salen de los spikes

1. **Conexión remota por contexto JNDI programático** en los tres clientes (Tomcat→logic, services→logic, logic→data):

   ```java
   Properties p = new Properties();
   p.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
   p.put(Context.PROVIDER_URL, providerUrl);          // LOGIC_URL o DATA_URL, p. ej. remote+http://data:8080
   p.put(Context.SECURITY_PRINCIPAL, ejbUser);        // EJB_USER
   p.put(Context.SECURITY_CREDENTIALS, ejbPassword);  // EJB_PASSWORD
   T proxy = (T) new InitialContext(p).lookup(JndiNames.DATA_SERVICE);
   ```

   En WildFly no requiere `jboss-deployment-structure.xml` ni CLI. Se descarta la contingencia `remote-outbound-connection`.
2. **D-11 confirmada:** el bean de Lógica lleva `@TransactionAttribute(NOT_SUPPORTED)` a nivel de clase. Así cada
   llamada a Datos abre y cierra su propia transacción JTA local, y no hay transacción distribuida entre servidores.
   Costo: `attend` hace `findById` + `save` en dos transacciones, así que dos atenciones simultáneas de la misma
   solicitud podrían pasar ambas la validación (riesgo documentado; se acota en Datos con bloqueo optimista si hace falta).
3. **DTO como JavaBeans:** confirmados con JSF/EL en Tomcat y con el marshalling de WildFly.
4. **Tomcat:** el WAR lleva `WEB-INF/beans.xml` (`annotated`), `web.xml` con `FacesServlet` en `*.xhtml` y
   `META-INF/context.xml` con el recurso `BeanManager` de Weld.
5. **Pendiente (Rol 1, F2):** configurar los timeouts del cliente EJB para que la caída de una capa se reporte en
   pocos segundos y no en ~72 s (QA-DIS-1).
