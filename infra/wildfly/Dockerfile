# syntax=docker/dockerfile:1
# Imagen de las capas que corren en WildFly (data, logic, services).
# Uso: docker build -f infra/wildfly/Dockerfile --build-arg MODULE=data .   (contexto = raíz del repo)

# Etapa 1: compilar el WAR del módulo (y contracts) con Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY contracts/pom.xml contracts/
COPY data/pom.xml data/
COPY logic/pom.xml logic/
COPY services/pom.xml services/
COPY presentation-jsf/pom.xml presentation-jsf/
COPY contracts/src contracts/src
ARG MODULE
COPY ${MODULE}/ ${MODULE}/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am package -DskipTests
# Driver JDBC (solo lo usa data; pesa ~1 MB)
ARG POSTGRES_DRIVER_VERSION=42.7.7
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:copy -Dartifact=org.postgresql:postgresql:${POSTGRES_DRIVER_VERSION} \
        -DoutputDirectory=/build/drivers -Dmdep.stripVersion=true

# Etapa 2: WildFly con la configuración del módulo aplicada por CLI
FROM quay.io/wildfly/wildfly:38.0.1.Final-jdk17
ARG MODULE
COPY --from=build --chown=jboss:root /build/drivers/ /tmp/drivers/
COPY --chown=jboss:root infra/wildfly/common/ /tmp/cli/
COPY --chown=jboss:root infra/wildfly/${MODULE}/ /tmp/cli/
RUN for f in /tmp/cli/*.cli; do \
        if [ -e "$f" ]; then /opt/jboss/wildfly/bin/jboss-cli.sh --file="$f" || exit 1; fi; \
    done \
    && rm -rf /tmp/cli /tmp/drivers /opt/jboss/wildfly/standalone/configuration/standalone_xml_history
COPY --from=build --chown=jboss:root /build/${MODULE}/target/${MODULE}.war /opt/jboss/wildfly/standalone/deployments/
COPY --chown=jboss:root --chmod=755 infra/wildfly/entrypoint.sh /opt/jboss/entrypoint.sh
ENTRYPOINT ["/opt/jboss/entrypoint.sh"]
