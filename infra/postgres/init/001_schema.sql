-- Esquema de la capa de Datos. Lo ejecuta PostgreSQL solo la primera vez que se crea el volumen.
-- Hibernate lo valida al desplegar data.war (hibernate.hbm2ddl.auto=validate).
-- No hay CHECK de cantidad ni de campos vacíos: esas son reglas de negocio y viven en la capa de Lógica.

CREATE TABLE IF NOT EXISTS resource_request (
    id                   BIGSERIAL    PRIMARY KEY,
    disaster_description VARCHAR(500) NOT NULL,
    resource_type        VARCHAR(200) NOT NULL,
    quantity             INTEGER      NOT NULL,
    priority             VARCHAR(10)  NOT NULL,
    status               VARCHAR(10)  NOT NULL
);
