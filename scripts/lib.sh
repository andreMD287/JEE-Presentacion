#!/usr/bin/env bash
# Funciones comunes de los scripts de escenarios de calidad. Se usan con: source "$(dirname "$0")/lib.sh"
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATEWAY="${GATEWAY:-http://localhost:8000/api/v1}"
SERVICES="${SERVICES:-http://localhost:8082/services/api/v1}"
JSF="${JSF:-http://localhost:8080/recursos}"
API_KEY="${API_KEY:-clave-taller4}"
EVIDENCIAS="$ROOT/docs/evidencias/qa"
mkdir -p "$EVIDENCIAS"

# Guarda todo lo que imprime el script en docs/evidencias/qa/<id>-<fecha>.txt
evidencia() {
    local id="$1"
    local file="$EVIDENCIAS/${id}-$(date +%Y%m%d-%H%M%S).txt"
    exec > >(tee "$file") 2>&1
    echo "== $id | $(date '+%Y-%m-%d %H:%M:%S') | $(git -C "$ROOT" rev-parse --short HEAD 2>/dev/null || echo sin-git)"
    echo "   evidencia: ${file#"$ROOT"/}"
}

# Ruta para montar en Docker (en Git Bash, Docker Desktop necesita la ruta de Windows)
ruta_docker() {
    if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else echo "$1"; fi
}

# Corre una colección de Newman dentro de la red de Compose: newman <colección> <entorno>
newman() {
    docker run --rm --network jee-presentacion_default -v "$(ruta_docker "$ROOT/tests/postman"):/etc/newman" \
        postman/newman:6-alpine run "$1" -e "$2" --color off
}

compose() {
    docker compose --project-directory "$ROOT" "$@"
}

# Espera a que un servicio de Compose esté healthy (máx. $2 segundos)
esperar_sano() {
    local servicio="$1" max="${2:-180}" i=0
    until compose ps "$servicio" --format '{{.Status}}' | grep -q '(healthy)'; do
        sleep 2; i=$((i + 2))
        if [ "$i" -ge "$max" ]; then echo "   $servicio no quedó sano en ${max}s"; return 1; fi
    done
}

# Milisegundos desde la época (Git Bash y Linux)
ahora_ms() {
    date +%s%3N
}

# Lee números (uno por línea) y muestra n, p50, p95 y máximo
percentiles() {
    sort -n | awk '{ v[NR] = $1 } END {
        if (NR == 0) { print "   sin datos"; exit }
        p50 = v[int((NR - 1) * 0.50) + 1]; p95 = v[int((NR - 1) * 0.95) + 1]
        printf "   n=%d  p50=%.1f ms  p95=%.1f ms  max=%.1f ms\n", NR, p50, p95, v[NR]
    }'
}

resultado() {
    # resultado <ok:0|1> <texto>
    if [ "$1" -eq 0 ]; then echo "RESULTADO: CUMPLE - $2"; else echo "RESULTADO: NO CUMPLE - $2"; fi
}
