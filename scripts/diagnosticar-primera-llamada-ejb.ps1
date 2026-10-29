$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Set-Location $repo

if (-not (Test-Path '.\tests\integration-ejb\pom.xml')) {
    throw 'Falta tests/integration-ejb/pom.xml de la prueba anterior.'
}

$cfgJson = docker compose config --format json
if ($LASTEXITCODE -ne 0) { throw 'No se pudo obtener la configuracion Compose.' }
$cfg = $cfgJson | ConvertFrom-Json
$ejbUser = $cfg.services.logic.environment.EJB_USER
$ejbPassword = $cfg.services.logic.environment.EJB_PASSWORD

Write-Host "`n===== DIAGNOSTICO: PRIMERA INVOCACION EJB (LECTURAS) ====="
Write-Host 'Se reinician Datos y Logica antes de cada caso; no se borra PostgreSQL.'
$run = 'mvn -B -q -pl contracts -am install -DskipTests && mvn -B -q -f tests/integration-ejb/pom.xml compile exec:java -Dexec.mainClass=co.edu.javeriana.recursos.integration.DiagnosticoPrimeraInvocacion'
$results = @{}

foreach ($target in @('data','logic')) {
    Write-Host "`n===== PRUEBA EN FRIO: $target ====="
    docker compose restart data logic
    if ($LASTEXITCODE -ne 0) { throw 'No fue posible reiniciar los dos servidores.' }
    docker compose up -d --no-build --wait postgres data logic
    if ($LASTEXITCODE -ne 0) { throw 'No quedaron saludables los tres servicios.' }

    docker run --rm `
      --network jee-presentacion_default `
      --mount "type=bind,source=$repo,target=/workspace" `
      -w /workspace `
      -e "PROBE_TARGET=$target" `
      -e 'DATA_URL=remote+http://data:8080' `
      -e 'LOGIC_URL=remote+http://logic:8080' `
      -e 'EJB_INVOCATION_TIMEOUT_MS=5000' `
      -e "EJB_USER=$ejbUser" `
      -e "EJB_PASSWORD=$ejbPassword" `
      maven:3.9-eclipse-temurin-17 `
      sh -ec $run
    $results[$target] = $LASTEXITCODE
    Write-Host "[DIAG] Proceso $target termino con codigo $LASTEXITCODE"
}

Write-Host "`n===== RESUMEN ====="
Write-Host "Data:  $($results['data']) (0 significa primera llamada exitosa)"
Write-Host "Logic: $($results['logic']) (0 significa primera llamada exitosa)"
Write-Host 'Los tiempos de llamada estan arriba. Copia toda la salida para interpretarlos.'
Write-Host "`n===== ERRORES DEL SERVIDOR TRAS EL ULTIMO CASO ====="
docker compose logs --since=5m logic data | Select-String -Pattern 'WFLYEJB0034|TimeoutException|ERROR' -Context 0,2
Write-Host "`n===== GIT ====="
git status --short --branch
