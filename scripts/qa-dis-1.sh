#!/usr/bin/env bash
# QA-DIS-1: al detener data, los clientes presentan errores controlados <= 5000 ms;
# al restaurar data, JSF y REST recuperan la respuesta sin reiniciar otras capas.
source "$(dirname "$0")/lib.sh"
evidencia QA-DIS-1

LIMITE_MS=5000
body=$(mktemp)
res=$(mktemp)
data_detenida=0

# Asegura restauracion incluso cuando falla una comprobacion o se recibe Ctrl+C.
limpiar() {
    local estado=$?
    trap - EXIT
    if [ "$data_detenida" -eq 1 ]; then
        echo "Restaurando data tras interrupcion o error..."
        if ! compose start data >/dev/null || ! esperar_sano data 180; then
            echo "ATENCION: data no pudo restaurarse; verificar docker compose ps" >&2
            estado=1
        fi
    fi
    rm -f "$body" "$res"
    exit "$estado"
}
trap limpiar EXIT

medir() {
    # medir <etiqueta> <url> <normal|caida> [header]
    local etiqueta="$1" url="$2" fase="$3" header="${4:-}"
    local t0 t code traza="no" contenido="normal" codigo_interno="" salida=0
    t0=$(ahora_ms)
    if [ -n "$header" ]; then
        code=$(curl --silent --show-error --max-time 15 -o "$body" -w '%{http_code}' -H "$header" "$url") || salida=$?
    else
        code=$(curl --silent --show-error --max-time 15 -o "$body" -w '%{http_code}' "$url") || salida=$?
    fi
    t=$(( $(ahora_ms) - t0 ))
    if [ "$salida" -ne 0 ]; then
        echo "ERROR: curl fallo en $etiqueta con codigo $salida" >&2
        return 1
    fi
    if grep -qiE 'Exception|at [a-z]+\.[a-z]+\.' "$body"; then traza="SI"; fi
    if grep -q 'No se pudo acceder al servicio' "$body"; then
        contenido="error JSF"
    elif grep -q '"code":"SERVICE_UNAVAILABLE"' "$body"; then
        contenido="SERVICE_UNAVAILABLE"
    elif grep -q '"code":"INTERNAL_ERROR"' "$body"; then
        contenido="INTERNAL_ERROR"
    fi
    local correcto=0
    if [ "$fase" = "normal" ]; then
        [ "$code" = 200 ] && [ "$traza" = "no" ] && correcto=1
    elif [ "$etiqueta" = "JSF (etapa 1, EJB)" ]; then
        [ "$code" = 200 ] && [ "$contenido" = "error JSF" ] && [ "$traza" = "no" ] && [ "$t" -le "$LIMITE_MS" ] && correcto=1
    else
        # main devuelve 500/INTERNAL_ERROR; la mejora opcional cambia a 503/SERVICE_UNAVAILABLE.
        if { [ "$code" = 500 ] && [ "$contenido" = "INTERNAL_ERROR" ]; } ||
           { [ "$code" = 503 ] && [ "$contenido" = "SERVICE_UNAVAILABLE" ]; }; then
            [ "$traza" = "no" ] && [ "$t" -le "$LIMITE_MS" ] && correcto=1
        fi
    fi
    printf "   %-28s HTTP %-3s en %5d ms | contenido: %-20s | traza: %-2s | %s\n" \
        "$etiqueta" "$code" "$t" "$contenido" "$traza" "$([ "$correcto" -eq 1 ] && echo OK || echo FALLO)"
    echo "$t $code $traza $correcto" >> "$res"
    [ "$correcto" -eq 1 ]
}

probar_todo() {
    local fase="$1"
    : > "$res"
    local fallo=0
    medir "JSF (etapa 1, EJB)" "$JSF/" "$fase" || fallo=1
    medir "REST via gateway (etapa 3)" "$GATEWAY/requests" "$fase" "apikey: $API_KEY" || fallo=1
    return "$fallo"
}

fallo=0
echo "1) Antes de la caida:"
probar_todo normal || fallo=1
reinicios_antes=$(docker inspect -f '{{.RestartCount}} {{.State.StartedAt}}' \
    "$(compose ps -q logic)" "$(compose ps -q services)" "$(compose ps -q presentation)")

echo "2) Deteniendo data:"
data_detenida=1
compose stop data >/dev/null
probar_todo caida || fallo=1
falla_max=$(awk '{ if ($1 > m) m = $1 } END { print m + 0 }' "$res")
trazas=$(awk '$3 != "no" {n++} END {print n+0}' "$res")

echo "3) Restaurando solo data:"
compose start data >/dev/null
esperar_sano data 180
data_detenida=0
probar_todo normal || fallo=1
recuperado=$(awk '$2 == 200 && $4 == 1 {n++} END {print n+0}' "$res")
reinicios_despues=$(docker inspect -f '{{.RestartCount}} {{.State.StartedAt}}' \
    "$(compose ps -q logic)" "$(compose ps -q services)" "$(compose ps -q presentation)")

echo "Mayor tiempo con data caido: ${falla_max} ms (limite ${LIMITE_MS} ms) | trazas: $trazas"
echo "Clientes recuperados: $recuperado de 2"
if [ "$reinicios_antes" != "$reinicios_despues" ]; then
    echo "ATENCION: otra capa se reinicio"
    fallo=1
fi
if [ "$falla_max" -gt "$LIMITE_MS" ] || [ "$trazas" -ne 0 ] || [ "$recuperado" -ne 2 ]; then
    fallo=1
fi
resultado "$fallo" "errores controlados <= 5 s y recuperacion sin reiniciar otras capas"
exit "$fallo"
