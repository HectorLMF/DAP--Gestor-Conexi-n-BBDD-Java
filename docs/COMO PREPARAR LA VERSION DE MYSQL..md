# COMO PREPARAR LA VERSIÓN DE MySQL

## ¿Qué es este proyecto y qué es un middleware?

- El proyecto es un pequeño "gestor"/middleware que expone un endpoint HTTP (`/query`) y
  actúa como intermediario entre clientes web y distintas bases de datos (Postgres y en
  futuro MySQL).

- Middleware: es una capa de software que medía o conecta dos sistemas. En nuestro caso,
  el middleware recibe peticiones HTTP desde un cliente web, transforma y valida la petición,
  ejecuta una consulta en la base de datos y devuelve el resultado al cliente en formato JSON.

- Arquitectura básica en este repo:
  - `WebServer` / `SimpleWebServer` : servidor HTTP que expone `/query`.
  - `DBFactory`, `DBConnection`, `DBQuery` : contratos (interfaces) que permiten
    tener implementaciones por proveedor (Postgres, MySQL...).
  - `DBClient` : cliente genérico que usa una fábrica para crear una conexión y ejecutar queries.
  - Implementación actual: Postgres (cliente por sockets y demo). Debemos añadir MySQL (JDBC).

---

## Objetivo

Proporcionar una versión operativa para MySQL con funcionalidad equivalente a la de Postgres.
La versión MySQL debe:
- Permitir recibir peticiones POST a `/query` con JSON `{ "db":"mysql","sql":"..." }`.
- Ejecutar la consulta en MySQL y devolver las filas como JSON (lista de mapas columna->valor).
- Ser fácil de arrancar para desarrollo (script, Docker) y de probar con herramientas como
  PowerShell o curl.

---

## Pasos desde 0 (To Do) — ordenados y detallados

1) Preparar el entorno local
   - Instalar Java (JDK 17 recomendado), Maven, Docker Desktop (opcional, para pruebas con contenedor MySQL).
   - Verificar con:
     ```powershell
     java -version
     mvn -v
     docker --version
     ```

2) Añadir la dependencia MySQL Connector/J a `pom.xml` (en la rama MySQL)
   - Editar `pom.xml` y añadir dentro de `<dependencies>`:
     ```xml
     <dependency>
       <groupId>mysql</groupId>
       <artifactId>mysql-connector-java</artifactId>
       <version>8.1.0</version> <!-- usar versión estable actual -->
     </dependency>
     ```
   - Commit y push en la rama MySQL.

3) Implementar `MySQLConnection` usando JDBC (o adaptar la actual simulada)
   - Recomendación: crear una nueva clase `MySQLJdbcConnection` en paquete `org.example.db.mysql`.
   - Contrato a respetar (según `DBConnection`):
     - `getName()` debe devolver algo como `mysql:demo`.
     - `connect()` debe abrir una conexión JDBC (`DriverManager.getConnection(url,user,password)`).
     - `disconnect()` debe cerrar la conexión JDBC.
     - `isConnected()` debe reflejar el estado real.
     - `execute(String sql)` debe ejecutar la consulta y devolver `List<Map<String,Object>>`:
       - Para `SELECT` devolver una lista con cada fila como `LinkedHashMap` para preservar orden de columnas.
       - Para `INSERT/UPDATE/DELETE` puede devolver lista vacía o el número de filas afectadas encapsulado según convenga.

   - Ejemplo mínimo (pseudocódigo):
     ```java
     Connection c = DriverManager.getConnection(url,user,password);
     try (Statement st = c.createStatement()) {
       boolean hasResultSet = st.execute(sql);
       if (hasResultSet) {
         try (ResultSet rs = st.getResultSet()) {
           // iterar columnas y filas, construir List<Map<String,Object>>
         }
       } else {
         return Collections.emptyList();
       }
     }
     ```

   - Configuración (URL): construir URL JDBC a partir de variables de entorno o del fichero `src/main/resources/db.properties`.
     - Ejemplo: `jdbc:mysql://localhost:3306/<database>?serverTimezone=UTC&useSSL=false`

4) Actualizar `MySQLFactory` para devolver la conexión JDBC
   - Modificar `createConnection` para devolver `new MySQLJdbcConnection(name)`.
   - `createQuery` puede devolver una implementación simple que delega en la conexión.

5) Crear un contenedor Docker para MySQL (opcional pero recomendado para pruebas)
   - Comando rápido (PowerShell):
     ```powershell
     docker run --name dap-mysql -e MYSQL_ROOT_PASSWORD=postgres -e MYSQL_DATABASE=demo -p 3306:3306 -d mysql:8.1
     ```
   - Crear un script `scripts/start-mysql-and-run.ps1` o reutilizar `scripts/start.ps1` añadiendo soporte para MySQL.
   - Inicializar esquema y datos de prueba (puedes usar `docker exec -i` con un fichero SQL):
     ```sql
     CREATE TABLE users (
       id INT AUTO_INCREMENT PRIMARY KEY,
       name VARCHAR(100),
       email VARCHAR(200)
     );
     INSERT INTO users (name,email) VALUES
       ('Alice','alice@example.com'),
       ('Bob','bob@example.com'),
       ('Eve','eve@example.com');
     ```

6) Probar la integración manualmente (PowerShell)
   - Arrancar MySQL en Docker (si lo usas)
   - Arrancar el servidor (desde la rama MySQL):
     ```powershell
     # compilar
     mvn -DskipTests package
     # arrancar en foreground
     mvn -DskipTests -Dexec.mainClass=org.example.Main exec:java
     ```
   - En otra terminal, enviar un POST a `/query`:
     ```powershell
     $body = '{"db":"mysql","sql":"SELECT * FROM users"}'
     Invoke-RestMethod -Uri 'http://127.0.0.1:8000/query' -Method Post -Body $body -ContentType 'application/json'
     ```
   - Deberías recibir JSON con las filas.

7) Manejo de errores y seguridad
   - Sanitizar o validar SQL si es necesario (evitar ejecuciones peligrosas en producción).
   - Añadir timeouts en JDBC (setQueryTimeout) y límites de tamaño del request body.
   - Añadir autenticación básica o token si el servicio estará expuesto.

8) Pruebas automatizadas (opcional)
   - Crear pruebas de integración que arranquen un contenedor MySQL (usando Testcontainers o `docker-compose`) y realicen peticiones HTTP al servidor.
   - Tests mínimos:
     - SELECT devuelve filas
     - INSERT añade fila y subsequent SELECT la devuelve
     - DELETE elimina fila

9) Documentación y entrega
   - Actualizar `README.md` o `docs/COMO PREPARAR LA VERSION DE MYSQL..md` indicando cómo arrancar y probar.
   - Hacer PR en la rama MySQL con implementación y documentación detallada.

---

## Checklist rápido (To Do para tu compañero)

- [ ] Crear rama `feature/mysql-integration` a partir de `DEV-POSTGRESS`.
- [ ] Añadir dependencia `mysql-connector-java` en `pom.xml`.
- [ ] Implementar `MySQLJdbcConnection` que cumpla `DBConnection`.
- [ ] Actualizar `MySQLFactory` para devolver la conexión JDBC.
- [ ] Asegurar que `SimpleWebServer` maneja `db: "mysql"` (ya soportado en código actual).
- [ ] Escribir script/README para levantar MySQL y ejecutar pruebas.
- [ ] Añadir un pequeño conjunto de datos de ejemplo (fichero SQL) para inicializar la DB.
- [ ] Escribir pruebas de integración (opcional pero muy recomendado).

---

## Consejos prácticos y trampas comunes

- Usa `LinkedHashMap` cuando construyas filas para mantener el orden de columnas en la respuesta JSON.
- Gestiona los recursos JDBC con try-with-resources para evitar fugas de conexiones.
- No hardcodees contraseñas en el código: usa variables de entorno o `db.properties` en `src/main/resources`.
- Si usas Docker, mapea puertos y revisa que no haya conflictos (ej. 3306 ya en uso).
- Para debugging remoto desde el IDE, `start.ps1` soporta `-Debug` que habilita `MAVEN_OPTS`.

---

## Contacto y ayudas

Si te atascas, deja comentarios en la PR con:
- Qué pasos seguiste
- Logs relevantes
- El `pom.xml` y la versión del conector usada

Con esto tu compañero debería tener una guía completa y práctica para avanzar con la versión MySQL.

---

_Fin del documento._
