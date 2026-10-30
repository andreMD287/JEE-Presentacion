#!/usr/bin/env bash
# QA-INT-1 (interoperabilidad): RF-1..RF-5 y sus errores de negocio por REST (vía Kong y directo) y por SOAP.
# Medida: 100 % de las aserciones de Newman en verde en las tres corridas.
source "$(dirname "$0")/lib.sh"
evidencia QA-INT-1

total_fallas=0
correr() {
    # correr <etiqueta> <colección> <entorno>
    local salida fallas
    echo
    echo "== $1"
    salida=$(newman "$2" "$3")
    sed -n '/┌/,/┘/p' <<<"$salida"
    grep -E '^\s+[0-9]+\. ' <<<"$salida" || true          # detalle de cada aserción fallida, si la hay
    fallas=$(grep -E '│\s+assertions' <<<"$salida" | awk -F'│' '{ gsub(/ /, "", $4); print $4 }')
    total_fallas=$((total_fallas + ${fallas:-1}))
    sleep 1                                                # no mezclar con la ventana del límite de Kong
}

correr "REST vía gateway (etapa ③)" rest.postman_collection.json gateway.postman_environment.json
correr "REST directo a services (etapa ②)" rest.postman_collection.json directo.postman_environment.json
correr "SOAP directo a services (etapas ② y ③)" soap.postman_collection.json directo.postman_environment.json

ok=1
[ "$total_fallas" -eq 0 ] && ok=0
resultado $ok "RF-1..RF-5 y sus errores iguales por REST (gateway y directo) y SOAP; aserciones fallidas: $total_fallas"
exit $ok
