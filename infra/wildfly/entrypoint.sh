#!/bin/sh
# Crea el usuario de aplicación para EJB remoto (ApplicationRealm) con las credenciales del entorno
# y arranca WildFly. Se hace al arrancar y no al construir para no dejar la contraseña en la imagen.
set -e

: "${EJB_USER:?Falta EJB_USER}"
: "${EJB_PASSWORD:?Falta EJB_PASSWORD}"

USERS=/opt/jboss/wildfly/standalone/configuration/application-users.properties
if ! grep -q "^${EJB_USER}=" "$USERS"; then
    /opt/jboss/wildfly/bin/add-user.sh -a -s -u "$EJB_USER" -p "$EJB_PASSWORD"
fi

# Borra los marcadores de despliegue de un arranque anterior (docker compose stop/start): el healthcheck
# usa <war>.deployed y no debe dar "sano" antes de que el WAR vuelva a desplegarse.
rm -f /opt/jboss/wildfly/standalone/deployments/*.deployed \
      /opt/jboss/wildfly/standalone/deployments/*.failed \
      /opt/jboss/wildfly/standalone/deployments/*.isdeploying \
      /opt/jboss/wildfly/standalone/deployments/*.pending

exec /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 "$@"
