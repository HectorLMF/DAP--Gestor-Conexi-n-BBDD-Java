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

Contribución
- Ramas de trabajo previstas: `MYSQL` y `POSTGRESS` (una por persona) según el plan en `TODO.md`.

Licencia: MIT

