Scripts CLI de WildFly para la capa de Servicios (`*.cli`, se aplican al construir la imagen).

- `access-log.cli`: log de acceso de Undertow en la consola, con el `X-Correlation-ID` que agrega Kong
  (evidencia de QA-SEG-1 y trazabilidad de peticiones).

JAX-RS y JAX-WS vienen en `standalone.xml`. El usuario de aplicación para EJB remoto lo crea
`infra/wildfly/entrypoint.sh` al arrancar.
