#!/usr/bin/env bash
# QA-MOD-1 (modificabilidad): la presentación pasa de consumir la Lógica por EJB (etapa 1) a consumir los
# Servicios por el gateway (etapa 3) solo cambiando BACKEND_MODE.
# Medida: misma imagen de presentation en los dos modos (sin recompilar), 0 cambios en logic y data, y el camino
# observable cambia: en modo rest la pantalla JSF deja una línea en el log de acceso de services; en modo ejb, ninguna.
source "$(dirname "$0")/lib.sh"
evidencia QA-MOD-1

imagen() {
    docker inspect -f '{{.Image}}' "$(compose ps -q presentation)"
}

# Abre la pantalla JSF en el modo pedido y cuenta cuántas peticiones llegaron a services por esa visita
probar_modo() {
    local modo="$1" desde lineas codigo
    BACKEND_MODE="$modo" compose up -d --no-build --no-deps presentation >/dev/null 2>&1
    esperar_sano presentation 180
    desde=$(date -u +%Y-%m-%dT%H:%M:%SZ)   # con Z: sin ella Docker la toma como hora local
    sleep 1
    codigo=$(curl -s -m 30 -o /dev/null -w '%{http_code}' "$JSF/")
    sleep 1
    lineas=$(compose logs --no-log-prefix --since "$desde" services 2>/dev/null \
        | grep -c '"GET /services/api/v1/requests' || true)
    printf "   BACKEND_MODE=%-4s | JSF HTTP %s | peticiones en el log de services: %s | imagen %s\n" \
        "$modo" "$codigo" "$lineas" "$(imagen | cut -c8-19)"
    echo "$modo $codigo $lineas $(imagen)" >> /tmp/qa-mod-1.res
}

: > /tmp/qa-mod-1.res
echo "1) Etapa 1: presentation -> logic por EJB remoto"
probar_modo ejb
echo "2) Etapa 3: presentation -> gateway -> services (REST), sin recompilar"
probar_modo rest
echo "3) Se vuelve al modo por defecto"
probar_modo ejb

read -r _ c1 l1 i1 <<<"$(sed -n 1p /tmp/qa-mod-1.res)"
read -r _ c2 l2 i2 <<<"$(sed -n 2p /tmp/qa-mod-1.res)"
read -r _ c3 l3 i3 <<<"$(sed -n 3p /tmp/qa-mod-1.res)"

cambios=$(git -C "$ROOT" status --porcelain -- logic data | wc -l)
echo "Archivos modificados en logic y data para hacer el cambio: $cambios"
[ "$i1" = "$i2" ] && [ "$i2" = "$i3" ] && echo "La imagen de presentation es la misma en los tres pasos" \
    || echo "ATENCIÓN: la imagen de presentation cambió"

ok=1
if [ "$c1" = 200 ] && [ "$c2" = 200 ] && [ "$c3" = 200 ] && [ "$l1" -eq 0 ] && [ "$l2" -ge 1 ] && [ "$l3" -eq 0 ] \
   && [ "$i1" = "$i2" ] && [ "$i2" = "$i3" ] && [ "$cambios" -eq 0 ]; then ok=0; fi
resultado $ok "cambio de etapa solo por BACKEND_MODE: misma imagen, 0 cambios en logic y data, camino REST verificado"
exit $ok
