# TODO y Plan de trabajo (2 personas)

Objetivo del milestone inicial
- Crear esqueleto del proyecto con interfaces y clases vacías (stubs) con documentación Doxygen/Javadoc.
- Preparar `Main` que arranque servidor web (esqueleto).
- Dividir el trabajo entre dos personas: una se encargará de la parte MySQL (rama `MYSQL`) y la otra de Postgres (rama `POSTGRESS`).

Reglas de ramas
- Rama principal: `main` (o `master`) con el esqueleto y documentación.
- Cada desarrollador trabajará en su rama (`MYSQL`, `POSTGRESS`) y abrirá Pull Requests a `main` cuando tenga completadas y probadas las funcionalidades.

Tareas por persona (reparto 50/50)

Persona A (rama `MYSQL`):
- Implementar `MySQLFactory`.
- Implementar `MySQLConnection`:
  - Simulación de tablas en memoria.
  - Métodos: `connect()`, `disconnect()`, `isConnected()`, `execute(String sql)`.
  - Soportar operaciones básicas: SELECT, INSERT, DELETE (mínimo).
- Implementar `MySQLQuery`:
  - Parser sencillo/ejecutor que delegue en `MySQLConnection`.
- Tests unitarios para las operaciones anteriores.
- Documentación específica y ejemplos en `docs/mysql.md`.

Persona B (rama `POSTGRESS`):
- Implementar `PostgressFactory`.
- Implementar `PostgressConnection` (similares requerimientos a MySQLConnection pero separado para demostrar patrón):
  - Simulación de tablas en memoria.
  - Métodos: `connect()`, `disconnect()`, `isConnected()`, `execute(String sql)`.
  - Soportar operaciones básicas: SELECT, INSERT, DELETE (mínimo).
- Implementar `PostgressQuery` que delegue en `PostgressConnection`.
- Tests unitarios para las operaciones anteriores.
- Documentación específica y ejemplos en `docs/postgres.md`.

Tareas compartidas / integradas
- Implementar servidor web en `Main` con formularios para crear conexiones y ejecutar queries.
- Crear clases helper comunes si procede (parser SQL simple, utilidades de serialización, etc.).
- Revisiones de código cruzadas y PRs.
- PR final de integración a `main` con tests pasados.

Plan de milestones
- Día 1: Esqueleto y reparto de ramas (hecho).
- Día 2-3: Implementación básica de conexiones y queries en cada rama.
- Día 4: Tests y documentación por cada rama.
- Día 5: Integración, resolver conflictos, PRs y merge a `main`.

Notas
- No usar JDBC ni librerías externas para la simulación (salvo las del JDK). Para la parte web se puede usar `com.sun.net.httpserver.HttpServer` o similar.
- Mantener API simple y documentada con Javadoc/Doxygen.

