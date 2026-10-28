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
| DTO de Datos | `dto.ResourceRequestData` | Mismos campos que la vista; contrato Lógica→Datos |
| Enums | `dto.Priority {BAJA, MEDIA, ALTA}`, `dto.RequestStatus {PENDIENTE, ATENDIDA}` | Se movieron desde `domain.model` del T2 |
| Excepciones | `exception.BusinessValidationException`, `RequestNotFoundException`, `AlreadyAttendedException` | Heredan de `BusinessException` con `getCode()`; `@ApplicationException(rollback = true)` |
| Constantes | `JndiNames.LOGIC_SERVICE`, `JndiNames.DATA_SERVICE` | Ver §2 |

**Cambio respecto al T2:** los DTO son clases JavaBean (constructor vacío + getters/setters), no `record`.
Motivos: la EL de Tomcat 10.1 (EL 5.0) no lee componentes de `record` en JSF (`#{r.id}` falla), JAXB/SOAP exige constructor vacío y setters, y los DTO viajan por EJB remoto. En Lógica, `command.quantity()` pasa a `command.getQuantity()`.

## 2. EJB remoto (etapa ①, y Servicios → Lógica en ②③)

| Bean | WAR (= módulo) | Nombre JNDI |
|---|---|---|
| `ResourceRequestServiceBean` | `logic.war` | `ejb:/logic/ResourceRequestServiceBean!co.edu.javeriana.recursos.contracts.ResourceRequestService` |
| `ResourceRequestDataServiceBean` | `data.war` | `ejb:/data/ResourceRequestDataServiceBean!co.edu.javeriana.recursos.contracts.ResourceRequestDataService` |

- Protocolo `remote+http` sobre el puerto 8080 interno del contenedor; autenticación con usuario de aplicación de WildFly (`EJB_USER` / `EJB_PASSWORD`).
- Transacciones: JTA **local a Datos**; cada llamada remota a Datos es su propia transacción (D-11). La forma exacta de evitar la propagación (p. ej. `NOT_SUPPORTED` en Lógica) se fija con el spike S-2.

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
- Operaciones: `registerRequest`, `listRequests`, `getRequest`, `attendRequest`, `listPendingRequests`.
- Errores como SOAP Fault con el mismo `code` de la tabla REST (en `detail`) y el mensaje de negocio.

## 5. Despliegue

| Servicio Compose | Imagen base | Puerto host | URL |
|---|---|---|---|
| `presentation` | `tomcat:10.1-jdk17-temurin` | 8080 | `http://localhost:8080/recursos/` |
| `gateway` (Kong 3.9 DB-less) | `kong:3.9` | 8000 | admin 8001 no expuesto |
| `services` | `quay.io/wildfly/wildfly:38.0.1.Final-jdk17` | 8082 | REST directo + SOAP |
| `logic` | ídem | — | solo red interna |
| `data` | ídem | — | solo red interna |
| `postgres` | `postgres:16-alpine` | 5434 | — |

Variables de entorno: `LOGIC_URL`, `DATA_URL`, `EJB_USER`, `EJB_PASSWORD`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `BACKEND_MODE` (`ejb` | `rest`), `GATEWAY_URL`, `API_KEY`.
