#!/usr/bin/env bash
# RN-5 bajo concurrencia (D-16): N peticiones simultáneas atienden la MISMA solicitud.
# Medida: exactamente 1 respuesta 200 y N-1 respuestas 409 ALREADY_ATTENDED; la solicitud queda ATENDIDA.
# Va directo a services (sin gateway) para que el rate-limiting de Kong no interfiera.
source "$(dirname "$0")/lib.sh"
evidencia RN-5-CONCURRENCIA

N="${N:-10}"
creada=$(curl -s -X POST "$SERVICES/requests" -H 'Content-Type: application/json' \
    -d '{"disasterDescription":"Prueba de concurrencia","resourceType":"Agua","quantity":1,"priority":"ALTA"}')
id=$(sed -E 's/.*"id":([0-9]+).*/\1/' <<<"$creada")
echo "Solicitud creada: id=$id"

tmp=$(mktemp -d)
for i in $(seq 1 "$N"); do
    curl -s -o "$tmp/$i.json" -w '%{http_code}\n' -X PUT "$SERVICES/requests/$id/attend" >"$tmp/$i.code" &
done
wait

ok=$(cat "$tmp"/*.code | grep -c '^200$' || true)
conflicto=$(cat "$tmp"/*.code | grep -c '^409$' || true)
otros=$((N - ok - conflicto))
codigos=$(cat "$tmp"/*.json | grep -o '"code":"[A-Z_]*"' | sort | uniq -c | tr '\n' ' ')
estado=$(curl -s "$SERVICES/requests/$id" | grep -o '"status":"[A-Z]*"')
rm -rf "$tmp"

echo "$N atenciones simultáneas -> 200: $ok | 409: $conflicto | otros: $otros"
echo "Códigos de error: ${codigos:-ninguno}"
echo "Estado final: $estado"

r=1
if [ "$ok" -eq 1 ] && [ "$conflicto" -eq $((N - 1)) ] && [ "$estado" = '"status":"ATENDIDA"' ]; then r=0; fi
resultado $r "una sola atención gana; las demás reciben 409 ALREADY_ATTENDED"
exit $r
