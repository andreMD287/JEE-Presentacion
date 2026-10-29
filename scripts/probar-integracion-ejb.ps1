$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $repo

if (-not (Test-Path ".\tests\integration-ejb\pom.xml")) {
    throw "No se encontro el proyecto de prueba tests/integration-ejb"
}

# Obtenemos las credenciales de Compose sin imprimirlas en consola.
$cfgText = docker compose config --format json
if ($LASTEXITCODE -ne 0) { throw "No se pudo leer la configuracion de Docker Compose" }
$cfg = $cfgText | ConvertFrom-Json
$ejbUser = $cfg.services.logic.environment.EJB_USER
$ejbPassword = $cfg.services.logic.environment.EJB_PASSWORD
$dbUser = $cfg.services.postgres.environment.POSTGRES_USER
$dbPassword = $cfg.services.postgres.environment.POSTGRES_PASSWORD

Write-Host "`n===== CONTENEDORES ====="
docker compose ps
if ($LASTEXITCODE -ne 0) { throw "Docker Compose no responde" }

Write-Host "`n===== PRUEBA REAL EJB Y PERSISTENCIA ====="
# El cliente usa Java 17 y la misma red interna que logic, data y postgres.
# No se editan los archivos de produccion ni se publican nuevos puertos.
$run = 'mvn -B -q -pl contracts -am install -DskipTests && mvn -B -q -f tests/integration-ejb/pom.xml compile exec:java -Dexec.mainClass=co.edu.javeriana.recursos.integration.SmokeEjbIntegration'
docker run --rm `
  --network jee-presentacion_default `
  --mount "type=bind,source=$repo,target=/workspace" `
  -w /workspace `
  -e "LOGIC_URL=remote+http://logic:8080" `
  -e "EJB_USER=$ejbUser" `
  -e "EJB_PASSWORD=$ejbPassword" `
  -e "DB_USER=$dbUser" `
  -e "DB_PASSWORD=$dbPassword" `
  maven:3.9-eclipse-temurin-17 `
  sh -ec $run
if ($LASTEXITCODE -ne 0) { throw "La prueba de integracion fallo (codigo $LASTEXITCODE)" }
Write-Host "`n===== INTEGRACION VALIDADA ====="
