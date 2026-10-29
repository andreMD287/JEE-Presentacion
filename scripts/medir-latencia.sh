#!/usr/bin/env bash
# QA-REN-1 (rendimiento comparativo): N consultas RF-2 (consultar todas) por cada camino, medidas de punta a punta
# por HTTP desde el equipo, para que los tres caminos se comparen con el mismo instrumento:
#   etapa 1: JSF en Tomcat  -> EJB remoto -> logic -> data
#   etapa 2: REST directo   -> services  -> EJB remoto -> logic -> data
#   etapa 3: REST por Kong  -> services  -> EJB remoto -> logic -> data
# Se informa p50/p95; no se inventan cifras: lo que imprime este script es lo que va al documento.
source "$(dirname "$0")/lib.sh"
evidencia QA-REN-1

N="${N:-100}"
CALENTAMIENTO="${CALENTAMIENTO:-10}"

camino() {
    # camino <etiqueta> <url> [header]
    local h=()
    [ -n "${3:-}" ] && h=(-H "$3")
    for _ in $(seq 1 "$CALENTAMIENTO"); do curl -s -o /dev/null "${h[@]}" "$2"; done
    echo "$1"
    for _ in $(seq 1 "$N"); do
        curl -s -o /dev/null -w '%{http_code} %{time_total}\n' "${h[@]}" "$2"
    done > /tmp/qa-ren-1.txt
    local errores
    errores=$(awk '$1 != 200' /tmp/qa-ren-1.txt | wc -l)
    awk '$1 == 200 { printf "%.3f\n", $2 * 1000 }' /tmp/qa-ren-1.txt | percentiles
    [ "$errores" -gt 0 ] && echo "   respuestas distintas de 200: $errores"
    return 0
}

registros=$(curl -s -H "apikey: $API_KEY" "$GATEWAY/requests" | grep -o '"id"' | wc -l)
echo "Solicitudes en la base: $registros | N=$N por camino (+$CALENTAMIENTO de calentamiento)"
camino "Etapa 1: JSF -> EJB (Tomcat -> logic -> data)" "$JSF/"
camino "Etapa 2: REST directo a services" "$SERVICES/requests"
camino "Etapa 3: REST vía Kong" "$GATEWAY/requests" "apikey: $API_KEY"
echo "Nota: el límite de Kong es 20 peticiones/s; las peticiones son secuenciales y no lo alcanzan."
