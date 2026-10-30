# Recorrido de la presentación JSF

Fecha: 2026-10-30. Sistema completo de `main` (`dccad91`) con los seis contenedores sanos; capturas tomadas en Chrome
sobre `http://localhost:8080/recursos/` (página reducida al 62 % para que quepa más contenido en cada imagen).

| Captura | Qué muestra | Requisito o escenario |
|---|---|---|
| `01-registro-correcto.jpg` | Registro de "Deslizamiento en Mocoa", 30 mantas, prioridad alta: "Solicitud #12 registrada correctamente"; los contadores pasan a 12 en total y 2 pendientes | RF-1, RN-3 |
| `02-error-cantidad-0.jpg` | El mismo formulario con cantidad 0: "La cantidad debe ser mayor que cero", el mensaje de la Lógica, sin trazas | RN-1 |
| `03-filtro-pendientes.jpg` | "Solo pendientes": quedan #2 y #12, ambas `PENDIENTE` con su botón Atender | RF-5 |
| `04-atender.jpg` | Tras confirmar la atención: "Solicitud #12 atendida correctamente"; 1 pendiente y 11 atendidas | RF-4, RN-5 |
| `05-modo-rest.jpg` | La misma imagen de `presentation` reiniciada con `BACKEND_MODE=rest`: el indicador dice "Conexión REST" | QA-MOD-1 |
| `06-modo-rest-listado.jpg` | En modo REST (por Kong), la tabla con las 12 solicitudes y #12 `ATENDIDA` | RF-2, QA-MOD-1 |
| `07-data-caido.jpg` | De vuelta en modo EJB y con `data` detenido: "No se pudo acceder al servicio. Comprueba las capas y vuelve a intentar.", sin trazas | QA-DIS-1 |
| `08-data-recuperado.jpg` | `data` reiniciado, sin reiniciar `logic` ni `presentation`: la página vuelve a mostrar los datos | QA-DIS-1 |

Observaciones durante el recorrido:

- Una sola vez, la primera página cargada justo cuando `data` volvió a estar sano mostró el error
  (`UnknownHostException: data`: el nombre del contenedor aún no se resolvía). En dos repeticiones controladas la
  recuperación fue inmediata (desde 350 ms después de quedar sano) y `scripts/qa-dis-1.sh` cumple revisando el contenido
  de la página.
- Con `data` caído los contadores muestran 0 en lugar de indicar que no hay datos disponibles.
- Al filtrar después de un error, el mensaje anterior sigue visible hasta la siguiente acción.
- El botón "Atender" pide confirmación con un cuadro del navegador.
