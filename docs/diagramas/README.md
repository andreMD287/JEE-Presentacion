Diagramas de `docs/arquitectura.md`: fuente Mermaid (`.mmd`) y exportación (`.png`).

La fuente de verdad son los bloques `mermaid` del documento; cada `.mmd` es una copia de uno de ellos, numerada en
orden de aparición. Los `.png` se generaron con `@mermaid-js/mermaid-cli` 11.4.2, tema `neutral`, fondo blanco y escala 2:

```bash
mmdc -t neutral -b white -s 2 -i 06-logica-contratos.mmd -o 06-logica-contratos.png
```
