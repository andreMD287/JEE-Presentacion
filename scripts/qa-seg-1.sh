#!/usr/bin/env bash
# QA-SEG-1 (seguridad): una petición REST sin API key se rechaza en el gateway con 401 y no llega a services.
# Medida: 401 para "sin clave" y "clave inválida"; 0 líneas nuevas en el log de acceso de services por esas peticiones.
source "$(dirname "$0")/lib.sh"
evidencia QA-SEG-1

marca="qa-seg-1-$(date +%s)"
antes=$(compose logs services 2>&1 | grep -c "io.undertow.accesslog" || true)

sin=$(curl -s -o /dev/null -w '%{http_code}' -H "X-Correlation-ID: $marca-sin" "$GATEWAY/requests")
mala=$(curl -s -o /dev/null -w '%{http_code}' -H "apikey: clave-equivocada" -H "X-Correlation-ID: $marca-mala" "$GATEWAY/requests")
con=$(curl -s -o /dev/null -w '%{http_code}' -H "apikey: $API_KEY" -H "X-Correlation-ID: $marca-con" "$GATEWAY/requests")
sleep 1
despues=$(compose logs services 2>&1 | grep -c "io.undertow.accesslog" || true)
llegaron=$(compose logs services 2>&1 | grep "corr=$marca" | sed -E 's/.*corr=//' || true)

echo "Sin API key       -> HTTP $sin"
echo "API key inválida  -> HTTP $mala"
echo "Con API key       -> HTTP $con"
echo "Líneas nuevas en el log de acceso de services: $((despues - antes))"
echo "Correlation-ID que llegaron a services: ${llegaron:-ninguno}" | tr '\n' ' '; echo

ok=1
if [ "$sin" = 401 ] && [ "$mala" = 401 ] && [ "$con" != 401 ] \
   && ! grep -q "$marca-sin\|$marca-mala" <<<"$llegaron" && grep -q "$marca-con" <<<"$llegaron"; then ok=0; fi
resultado $ok "las peticiones sin clave o con clave inválida se rechazan en Kong (401) y no llegan a services"
echo
echo "Colección de Newman del gateway:"
newman gateway.postman_collection.json gateway.postman_environment.json | sed -n '/┌/,/┘/p'
exit $ok
