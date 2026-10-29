# Prueba de integración EJB del Taller 4

Valida con un cliente Java 17, dentro de la red interna de Docker Compose, el recorrido
`cliente -> logic (EJB remoto) -> data (EJB remoto) -> PostgreSQL`.

Prueba las cinco operaciones, tres errores de negocio y verifica mediante lectura SQL
la fila creada y atendida. **Inserta una solicitud de prueba y la deja ATENDIDA en la BD.**

Desde la raíz del proyecto, con `postgres`, `data` y `logic` saludables:

```powershell
.\scripts\probar-integracion-ejb.ps1
```

El script prepara `contracts` e inicia Maven en un contenedor temporal. No requiere
publicar el puerto 5432/5434 ni editar los módulos de producción. Las dependencias
Maven pueden descargarse durante su primera ejecución.

No subir credenciales ni logs que contengan datos secretos.
