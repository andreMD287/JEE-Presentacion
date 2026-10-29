#!/usr/bin/env bash
# QA-DIS-1 (disponibilidad / manejo de fallos): se detiene la capa data.
# Medida: los clientes reciben un error controlado (sin stack trace) en <= 5 s (EJB_INVOCATION_TIMEOUT_MS) y, al
# reiniciar solo data, el sistema vuelve a responder sin reiniciar logic, services ni presentation.
source "$(dirname "$0")/lib.sh"
evidencia QA-DIS-1

medir() {
    # medir <etiqueta> <url> [header]
    local t0 code t
    t0=$(ahora_ms)
    if [ -n "${3:-}" ]; then
        code=$(curl -s -m 60 -o /tmp/qa-dis-1.body -w '%{http_code}' -H "$3" "$2")
    else
        code=$(curl -s -m 60 -o /tmp/qa-dis-1.body -w '%{http_code}' "$2")
    fi
    t=$(( $(ahora_ms) - t0 ))
    local traza="no"
    grep -qiE 'Exception|at [a-z]+\.[a-z]+\.' /tmp/qa-dis-1.body && traza="SÍ"
    printf "   %-28s HTTP %s en %5d ms | stack trace visible: %s\n" "$1" "$code" "$t" "$traza"
    echo "$t $code $traza" >> /tmp/qa-dis-1.res
}

probar_todo() {
    : > /tmp/qa-dis-1.res
    medir "JSF (etapa 1, EJB)" "$JSF/"
    medir "REST vía gateway (etapa 3)" "$GATEWAY/requests" "apikey: $API_KEY"
}

echo "1) Todo arriba:"
probar_todo
reinicios_antes=$(docker inspect -f '{{.RestartCount}} {{.State.StartedAt}}' \
    "$(compose ps -q logic)" "$(compose ps -q services)" "$(compose ps -q presentation)")

echo "2) Se detiene data:"
compose stop data >/dev/null 2>&1
probar_todo
falla_max=$(awk '{ if ($1 > m) m = $1 } END { print m + 0 }' /tmp/qa-dis-1.res)
trazas=$(grep -c ' SÍ$' /tmp/qa-dis-1.res || true)

echo "3) Se reinicia solo data:"
compose start data >/dev/null 2>&1
esperar_sano data 180
probar_todo
recuperado=$(awk '$2 == 200' /tmp/qa-dis-1.res | wc -l)
reinicios_despues=$(docker inspect -f '{{.RestartCount}} {{.State.StartedAt}}' \
    "$(compose ps -q logic)" "$(compose ps -q services)" "$(compose ps -q presentation)")

echo "Peor tiempo de respuesta con data caído: $falla_max ms | respuestas con stack trace: $trazas"
echo "Clientes que volvieron a responder 200: $recuperado de 2"
[ "$reinicios_antes" = "$reinicios_despues" ] && echo "logic, services y presentation no se reiniciaron" \
    || echo "ATENCIÓN: alguna otra capa se reinició"

ok=1
if [ "$falla_max" -le 6000 ] && [ "$trazas" -eq 0 ] && [ "$recuperado" -eq 2 ] \
   && [ "$reinicios_antes" = "$reinicios_despues" ]; then ok=0; fi
resultado $ok "falla controlada en <= 5 s (+1 s de margen) y recuperación sin reiniciar las otras capas"
exit $ok
