# syntax=docker/dockerfile:1
# Imagen de la capa de Presentación (JSF en Tomcat 10.1).
# Uso: docker build -f infra/tomcat/Dockerfile .   (contexto = raíz del repo)

# Etapa 1: compilar recursos.war (incluye JSF, Weld y el cliente EJB de WildFly)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY contracts/pom.xml contracts/
COPY data/pom.xml data/
COPY logic/pom.xml logic/
COPY services/pom.xml services/
COPY presentation-jsf/pom.xml presentation-jsf/
COPY contracts/src contracts/src
COPY presentation-jsf/ presentation-jsf/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl presentation-jsf -am package -DskipTests

# Etapa 2: Tomcat (no trae JSF ni CDI; van dentro del WAR)
FROM tomcat:10.1-jdk17-temurin
COPY --from=build /build/presentation-jsf/target/recursos.war /usr/local/tomcat/webapps/recursos.war
