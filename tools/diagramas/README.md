Fuentes de los diagramas de `docs/diagramas/` (allí solo quedan los PNG que usa el documento).

| PNG | Figura | Fuente |
|---|---|---|
| `01-contexto.png` | Contexto del sistema | `generar.py` |
| `02-etapa1.png` | Etapa 1: componentes y conectores | `generar.py` |
| `03-etapas2-3.png` | Etapas 2 y 3 | `generar.py` |
| `04-vista-logica.png` | Vista lógica | `generar.py` |
| `05-vista-desarrollo.png` | Vista de desarrollo | `generar.py` |
| `06-secuencia-registrar-jsf.png` | Registro desde JSF | `06-secuencia-registrar-jsf.mmd` |
| `07-secuencia-atender-rest.png` | Atención por REST a través del gateway | `07-secuencia-atender-rest.mmd` |
| `08-secuencia-registrar-soap.png` | Registro por SOAP | `08-secuencia-registrar-soap.mmd` |
| `09-despliegue.png` | Vista física | `generar.py` |

Los diagramas estructurales se dibujan con coordenadas fijas en `generar.py` para controlar la estructura y evitar
cruces de flechas; las secuencias se escriben en Mermaid.

Regenerar (requiere Python 3 y `@mermaid-js/mermaid-cli`, que trae puppeteer; usa el Chrome instalado):

```bash
python generar.py                                                  # escribe los SVG intermedios en build/
NODE_PATH=<ruta a node_modules con mermaid-cli> node render.js     # exporta todo a PNG en docs/diagramas/
```
