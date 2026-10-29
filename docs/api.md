# Contratos entre capas (BORRADOR — pendiente de confirmar por ambos roles)

> Este archivo es la fuente única de los contratos. Si algo cambia, se cambia aquí primero.
> Al final del taller se completa con ejemplos de petición/respuesta.

## 1. Módulo `contracts` (Java, compartido por todas las capas)

Paquete base `co.edu.javeriana.recursos.contracts`.

| Tipo | Clase | Notas |
|---|---|---|
| Interfaz `@Remote` de Lógica | `ResourceRequestService` | Mismas 5 operaciones del T2: `register`, `findAll`, `findById`, `attend`, `findPending` |
| Interfaz `@Remote` de Datos | `ResourceRequestDataService` | `save`, `findById` (**devuelve `null` si no existe**: `Optional` no es serializable), `findAll` (orden por id) |
| DTO entrada | `dto.CreateResourceRequestCommand` | `disasterDescription`, `resourceType`, `Integer quantity`, `String priority` |
| DTO salida de Lógica | `dto.ResourceRequestView` | `id`, `disasterDescription`, `resourceType`, `quantity`, `priority`, `status` |
| DTO de Datos | `dto.ResourceRequestData` | Mismos campos que la vista **más `Long version`** (bloqueo optimista, D-16); contrato Lógica→Datos |
| Enums | `dto.Priority {BAJA, MEDIA, ALTA}`, `dto.RequestStatus {PENDIENTE, ATENDIDA}` | Se movieron desde `domain.model` del T2 |
| Excepciones | `exception.BusinessValidationException`, `RequestNotFoundException`, `AlreadyAttendedException` | Heredan de `BusinessException` con `getCode()`; `@ApplicationException(rollback = true)` |
| Excepción interna Datos→Lógica | `exception.ConcurrentUpdateException` (`CONCURRENT_UPDATE`) | La lanza `save` si la fila cambió desde que se leyó. **No sale de la Lógica:** en `attend` se traduce a `AlreadyAttendedException` (otra petición atendió primero) |
| Constantes | `JndiNames.LOGIC_SERVICE`, `JndiNames.DATA_SERVICE` | Ver §2 |

**Cambio respecto al T2:** los DTO son clases JavaBean (constructor vacío + getters/setters), no `record`.
Motivos: la EL de Tomcat 10.1 (EL 5.0) no lee componentes de `record` en JSF (`#{r.id}` falla), JAXB/SOAP exige constructor vacío y setters, y los DTO viajan por EJB remoto. En Lógica, `command.quantity()` pasa a `command.getQuantity()`.

## 2. EJB remoto (etapa ①, y Servicios → Lógica en ②③)

| Bean | WAR (= módulo) | Nombre JNDI |
|---|---|---|
| `ResourceRequestServiceBean` | `logic.war` | `ejb:/logic/ResourceRequestServiceBean!co.edu.javeriana.recursos.contracts.ResourceRequestService` |
| `ResourceRequestDataServiceBean` | `data.war` | `ejb:/data/ResourceRequestDataServiceBean!co.edu.javeriana.recursos.contracts.ResourceRequestDataService` |

- Protocolo `remote+http` sobre el puerto 8080 interno del contenedor; autenticación con usuario de aplicación de WildFly (`EJB_USER` / `EJB_PASSWORD`).
- Conexión: **usar siempre `RemoteEjbLocator`** (en `contracts`), que arma el contexto JNDI programático (`WildFlyInitialContextFactory` + URL + credenciales) y fija el timeout de invocación:
  ```java
  ResourceRequestDataService data = RemoteEjbLocator.lookup("DATA_URL", JndiNames.DATA_SERVICE, ResourceRequestDataService.class);
  ResourceRequestService logic = RemoteEjbLocator.lookup("LOGIC_URL", JndiNames.LOGIC_SERVICE, ResourceRequestService.class);
  ```
  El proxy se puede guardar y reutilizar (es seguro entre hilos y se reconecta cuando la capa vuelve).
- Timeout: `EJB_INVOCATION_TIMEOUT_MS` (5000 por defecto). Si la capa remota cae, la llamada lanza `jakarta.ejb.EJBException` (causa `TimeoutException`) o `org.jboss.ejb.client.RequestSendFailedException` en ≤ 5 s; el cliente debe mostrar un mensaje controlado (QA-DIS-1). Sin timeout la primera falla tardaba ~135 s.
- Bloqueo optimista (D-16): `save` de una solicitud existente exige la `version` leída (si viene `null` lanza
  `IllegalArgumentException`). Lo más simple en Lógica es conservar la versión que trae `findById` y reenviarla al
  guardar; si el modelo de dominio se reconstruye en el adaptador, el dominio debe llevar `version` como dato opaco
  para que el adaptador la copie en ambos sentidos. En `attend`, capturar `ConcurrentUpdateException` y lanzar
  `AlreadyAttendedException(id)`.
- Validación de prioridad (RN-6): `CreateResourceRequestCommand.priority` es `String`; la Lógica la exige y la
  convierte a `Priority`; si falta o no es `BAJA`/`MEDIA`/`ALTA`, lanza `BusinessValidationException`
  (`VALIDATION_ERROR`, HTTP 400).
- Transacciones: JTA **local a Datos**; cada llamada remota a Datos es su propia transacción (D-11). El bean de Lógica lleva `@TransactionAttribute(NOT_SUPPORTED)` a nivel de clase: el spike S-2 mostró que con `REQUIRED` la transacción se propaga a Datos.

## 3. REST (`services`, etapa ②③)

Base directa: `http://localhost:8082/services/api/v1` · Vía gateway: `http://localhost:8000/api/v1` (header `apikey`).

| RF | Método y ruta | Éxito |
|---|---|---|
| RF-1 Registrar | `POST /requests` (JSON `CreateResourceRequestCommand`) | 201 + `Location` |
| RF-2 Todas | `GET /requests` | 200 |
| RF-3 Por id | `GET /requests/{id}` | 200 |
| RF-4 Atender | `PUT /requests/{id}/attend` | 200 |
| RF-5 Pendientes | `GET /requests/pending` | 200 |

Se conserva `/requests/pending` del T2 (en lugar de `?status=PENDIENTE`) para reutilizar el recurso JAX-RS sin cambios de ruta.

Errores con el **mismo cuerpo del T2**: `{"code": "...", "message": "...", "timestamp": "..."}`.

| Excepción | HTTP | `code` |
|---|---|---|
| `BusinessValidationException` | 400 | `VALIDATION_ERROR` |
| `RequestNotFoundException` | 404 | `REQUEST_NOT_FOUND` |
| `AlreadyAttendedException` | 409 | `ALREADY_ATTENDED` |
| JSON vacío / mal formado | 400 | `EMPTY_BODY` / `MALFORMED_JSON` |
| Cualquier otra | 500 | `INTERNAL_ERROR` |

## 4. SOAP (`services`, etapa ③, sin gateway)

- Servicio `ResourceRequestSoapService`, WSDL: `http://localhost:8082/services/ResourceRequestSoapService?wsdl`
- Estilo document/literal wrapped (el de JAX-WS por defecto), SOAP 1.1, `targetNamespace = "http://recursos.javeriana.edu.co/soap"`.
- Operaciones y nombres de elementos (`@WebMethod(operationName)`, `@WebParam(name)`, `@WebResult(name)`):

| RF | Operación | Parámetro | Resultado |
|---|---|---|---|
| RF-1 | `registerRequest` | `request` (`CreateResourceRequestCommand`) | `request` (`ResourceRequestView`) |
| RF-2 | `listRequests` | — | `request` repetido (lista de `ResourceRequestView`) |
| RF-3 | `getRequest` | `id` | `request` |
| RF-4 | `attendRequest` | `id` | `request` |
| RF-5 | `listPendingRequests` | — | `request` repetido |

  Los campos de los DTO viajan con sus nombres Java (`id`, `disasterDescription`, `resourceType`, `quantity`, `priority`, `status`).
- Errores: SOAP Fault (HTTP 500) con `faultstring` = mensaje de negocio y en `detail` un elemento con `<code>` y `<message>`
  (p. ej. `@WebFault(name = "BusinessFault")` con un bean `{code, message}`). Mismos `code` que la tabla REST.
- Ejemplo de petición (`registerRequest`):

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:r="http://recursos.javeriana.edu.co/soap">
  <soapenv:Body>
    <r:registerRequest>
      <request>
        <disasterDescription>Sismo en Pasto</disasterDescription>
        <resourceType>Medicamentos</resourceType>
        <quantity>40</quantity>
        <priority>ALTA</priority>
      </request>
    </r:registerRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

Las colecciones de Newman (`tests/postman/`) prueban exactamente este contrato; ver §5.

## 5. Pruebas de API (Newman)

Newman corre en un contenedor dentro de la red de Compose (`jee-presentacion_default`):

```bash
docker run --rm --network jee-presentacion_default -v "$PWD/tests/postman:/etc/newman" postman/newman:6-alpine \
  run rest.postman_collection.json -e gateway.postman_environment.json
```

| Colección | Qué prueba | Entorno |
|---|---|---|
| `rest.postman_collection.json` | RF-1..RF-5 y las 3 reglas de error por REST (13 peticiones) | `gateway` (vía Kong) o `directo` (services) |
| `soap.postman_collection.json` | WSDL, RF-1..RF-5 y SOAP Faults (10 peticiones) | `gateway` o `directo` (SOAP siempre va directo) |
| `gateway.postman_collection.json` | QA-SEG-1: 401 sin clave o con clave inválida; `X-Correlation-ID` y límite | `gateway` |

`local.postman_environment.json` sirve para abrir las colecciones en Postman desde el equipo (`localhost`).

## 6. Despliegue

| Servicio Compose | Imagen base | Puerto host | URL |
|---|---|---|---|
| `presentation` | `tomcat:10.1-jdk17-temurin` | 8080 | `http://localhost:8080/recursos/` |
| `gateway` (Kong 3.9.4 DB-less) | `kong:3.9.4` | 8000 | admin 8001 no expuesto; API key `clave-taller4` en el header `apikey`; 20 peticiones/s y 600/min por consumidor |
| `services` | `quay.io/wildfly/wildfly:38.0.1.Final-jdk17` | 8082 | REST directo + SOAP |
| `logic` | ídem | — | solo red interna |
| `data` | ídem | — | solo red interna |
| `postgres` | `postgres:16-alpine` | 5434 | — |

Variables de entorno: `LOGIC_URL`, `DATA_URL`, `EJB_USER`, `EJB_PASSWORD`, `EJB_INVOCATION_TIMEOUT_MS`, `TX_NODE_ID`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `BACKEND_MODE` (`ejb` | `rest`), `GATEWAY_URL`, `API_KEY`.
