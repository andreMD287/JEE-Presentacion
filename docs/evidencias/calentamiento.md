# Primera llamada EJB tras un reinicio y precalentamiento (D-17)

Fecha: 2026-10-29. Antecedente: durante la integración se observó que, tras reiniciar `data` y `logic`, algunas primeras
llamadas EJB superaban el timeout de 5 s (D-14), aunque las siguientes tardaban 12–13 ms.

## Método

Todas las sondas corren dentro de la red de Compose con `EJB_INVOCATION_TIMEOUT_MS=5000` y llaman a
`ResourceRequestDataService.findAll()` por `remote+http://data:8080`.

1. **Cliente nuevo en cada medición** (`DiagnosticoPrimeraInvocacion`, en `tests/integration-ejb`): un JVM recién arrancado
   llama dos veces a `data`, con `data` recién reiniciado (frío) y con `data` ya usado (caliente). La diferencia entre
   frío y caliente es el costo de arranque del servidor; lo que queda en "caliente" es el costo del cliente nuevo.
2. **Cliente de larga vida** (equivale a `logic` ya en marcha, el caso de QA-DIS-1): un JVM llama a `data` cada
   500 ms; a los 15 s se reinicia solo `data` y se mide la llamada exitosa más lenta después del reinicio.

Cada caso se corrió 3 veces con la imagen de `data` sin precalentamiento (commit `45a93ef`) y con precalentamiento.

## Resultados

**1. Cliente nuevo** (1.ª llamada; la 2.ª siempre tardó 15–34 ms)

| Imagen de `data` | `data` recién reiniciado | `data` caliente |
|---|---|---|
| Sin precalentamiento | 3.713, 3.025, 2.978 ms | 2.140, 2.019, 2.005 ms |
| Con precalentamiento | 2.492, 2.259 ms y **1 timeout (5.399 ms)** | 2.020, 3.070, 1.995 ms |

**2. Cliente de larga vida, se reinicia solo `data`**

| Imagen de `data` | Llamada exitosa más lenta tras el reinicio | Fallas por timeout |
|---|---|---|
| Sin precalentamiento | 718, 813, 783 ms | 0 |
| Con precalentamiento | 74, 88, 29 ms | 0 |

En el log de `data`: `Calentamiento de data completo en 868–943 ms` durante el despliegue.

## Conclusiones

- El costo de arranque **del servidor `data`** es ~0,8–1,7 s. El precalentamiento lo traslada al despliegue: con un
  cliente caliente, la primera llamada tras volver `data` baja de ~800 ms a < 100 ms.
- El costo mayor (2–3,9 s) es **del cliente que llama por primera vez** (carga del cliente EJB, conexión y
  autenticación). Sumado al costo del servidor puede superar 5 s: es la falla intermitente observada, y por eso
  persiste un timeout en la medición 1 aun con `data` precalentado.
- Por lo tanto cada cliente EJB (`logic` → `data`, `services` → `logic`, `presentation` → `logic`) debe hacer una
  llamada de calentamiento al arrancar, de mejor esfuerzo, antes de declararse sano.
- La sonda 1 sobrestima el costo del cliente: carga 116 jars desde una carpeta de Windows montada en el contenedor.
  En las imágenes reales los jars están dentro de la imagen.

## Gateway: primera petición tras reiniciar Kong

En la primera corrida oficial de QA-INT-1 (2026-10-29), una petición por el gateway tardó 5,5 s aunque `services` la
atendió en 1,3 s. Al reiniciar solo `gateway` y medir con `curl`:

| Configuración de Kong | 401 sin clave (no sale de Kong) | 1.ª petición con clave | 2.ª petición |
|---|---|---|---|
| Por defecto (`dns_order` con SRV primero) | 13–35 ms | 4,13 s y 4,05 s | 33–99 ms |
| `KONG_DNS_ORDER=LAST,A,CNAME` | — | 0,07 s, 0,66 s y 0,05 s | 34–253 ms |

Causa: Kong 3.x resuelve el upstream `services` consultando primero un registro SRV. El DNS interno de Docker no lo
tiene, así que Kong espera un timeout antes de pedir el registro A. Sin SRV en el orden de búsqueda, la primera
petición ya no paga esa espera. Es la misma familia de problema que D-17: un costo de arranque que no debe pagar el
primer cliente.
