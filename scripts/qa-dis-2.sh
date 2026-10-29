#!/usr/bin/env bash
# QA-DIS-2 (disponibilidad ante picos): ráfaga de 50 peticiones REST simultáneas por el gateway.
# Medida: el gateway responde 429 a lo que supera el límite (20/s por consumidor) y services sigue respondiendo.
source "$(dirname "$0")/lib.sh"
evidencia QA-DIS-2

N="${N:-50}"
args=()
for _ in $(seq 1 "$N"); do args+=(-o /dev/null "$GATEWAY/requests"); done

inicio=$(ahora_ms)
# tr quita el \r que agrega curl en Windows
codigos=$(curl -s --parallel --parallel-max "$N" -H "apikey: $API_KEY" -w '%{http_code}\n' "${args[@]}" | tr -d '\r')
duracion=$(( $(ahora_ms) - inicio ))

echo "Ráfaga de $N peticiones en $duracion ms. Respuestas por código HTTP:"
sort <<<"$codigos" | uniq -c | sed 's/^/   /'
rechazadas=$(grep -c '^429$' <<<"$codigos" || true)
atendidas=$(grep -vc '^429$' <<<"$codigos" || true)

servicio=$(curl -s -o /dev/null -w '%{http_code}' "$SERVICES/requests")
echo "Atendidas por services: $atendidas | rechazadas por Kong (429): $rechazadas"
echo "services responde directamente justo después: HTTP $servicio"

ok=1
if [ "$rechazadas" -gt 0 ] && [ "$servicio" != 000 ] && [ "$servicio" -lt 500 ]; then ok=0; fi
resultado $ok "Kong corta la ráfaga con 429 y services sigue disponible"
exit $ok
