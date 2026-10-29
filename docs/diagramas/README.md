Diagramas de `docs/arquitectura.md`.

| Archivo | Figura | Fuente |
|---|---|---|
| `00-tablero.jpg` | 1. Consigna (foto del tablero, recortada) | foto original |
| `01-contexto.svg` | 2. Contexto | `generar.py` |
| `02-etapa1.svg` | 3. Etapa ①: componentes y conectores | `generar.py` |
| `03-etapas2-3.svg` | 4. Etapas ② y ③ | `generar.py` |
| `04-vista-logica.svg` | 5. Vista lógica | `generar.py` |
| `05-vista-desarrollo.svg` | 6. Vista de desarrollo | `generar.py` |
| `06-secuencia-registrar-jsf.*` | 7. Registrar desde JSF | `.mmd` (Mermaid) |
| `07-secuencia-atender-rest.*` | 8. Atender por REST vía gateway | `.mmd` (Mermaid) |
| `08-secuencia-registrar-soap.*` | 9. Registrar por SOAP | `.mmd` (Mermaid) |
| `09-despliegue.svg` | 10. Vista física | `generar.py` |

Los diagramas estructurales se dibujan con coordenadas fijas en `generar.py` (nodos UML 3D, componentes, paquetes,
puertos) para controlar la estructura y que ninguna flecha se cruce; el acomodo automático de Mermaid no lo garantiza.
Las secuencias siguen en Mermaid porque su disposición ya es ordenada.

Regenerar (requiere Python 3 y `@mermaid-js/mermaid-cli`, que trae puppeteer; usa el Chrome instalado):

```bash
python generar.py                                   # escribe los .svg estructurales
NODE_PATH=<ruta a node_modules con mermaid-cli> node render.js   # .mmd -> .svg/.png y .svg -> .png (escala 2)
```
