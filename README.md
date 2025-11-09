# DAP-Gestor-BBDD

Esqueleto para un gestor de conexiones y queries a bases de datos usando el patrón Abstract Factory.

Objetivo
- Proveer una implementación didáctica sin usar JDBC/Hibernate: simular conexiones y ejecución de sentencias usando Java "vanilla".
- Interfaz web mínima para crear conexiones y ejecutar consultas.

Estructura
- `src/main/java/org/example/` : código fuente Java.

Cómo compilar
- Proyecto Maven. Para compilar:

```bash
mvn -q -DskipTests package
```

Cómo ejecutar (esqueleto)
- La clase `org.example.Main` contendrá el arranque del servidor web. En esta versión inicial los métodos están sin implementar.

Probar conexión nativa a Postgres (docker)

Este proyecto incluye `PostgressConnection`, que intenta establecer una conexión nativa por sockets con un servidor Postgres (protocolo frontend/backend). Si falla, usa una DB simulada en memoria.

1) Levantar Postgres con Docker (ejemplo):

```bash
# arranca postgres en el puerto 5432, usuario/postgres/password=postgres
docker run --rm -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres -e POSTGRES_DB=postgres -p 5432:5432 postgres:16
```

2) Exportar variables de entorno (Windows cmd.exe):

```cmd
set PGHOST=localhost
set PGPORT=5432
set PGDATABASE=postgres
set PGUSER=postgres
set PGPASSWORD=postgres
```

3) Ejecutar la demo de Postgres (clase `org.example.db.postgres.PostgresDemo`):

```bash
mvn -q -DskipTests exec:java -Dexec.mainClass=org.example.db.postgres.PostgresDemo
```

Notas
- La implementación nativa es mínima: soporta autenticación trust/cleartext/md5 y queries en modo texto. No soporta SSL ni funciones avanzadas.
- Si la conexión nativa falla, el código informa por consola y continúa usando la DB simulada.

Contribución
- Ramas de trabajo previstas: `MYSQL` y `POSTGRESS` (una por persona) según el plan en `TODO.md`.

Licencia: MIT
