Scripts CLI de WildFly para la capa de Servicios (`*.cli`, se aplican al construir la imagen).

Por ahora no necesita ninguno: JAX-RS y JAX-WS vienen en `standalone.xml`, y el usuario de aplicación para
EJB remoto lo crea `infra/wildfly/entrypoint.sh` al arrancar.
