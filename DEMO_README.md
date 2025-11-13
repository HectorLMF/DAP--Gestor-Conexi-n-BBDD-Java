# 🚀 Script de Demostración

## Descripción

Este script automatiza completamente el proceso de demostración del Gestor de Conexiones BBDD. Con un solo comando, levanta todas las dependencias necesarias y la aplicación lista para usar.

## 📋 Prerrequisitos

- **Docker**: Para contenedores de MySQL y PostgreSQL
- **Maven**: Para compilar el proyecto Java
- **Java 17+**: Para ejecutar la aplicación
- **Bash**: Para ejecutar el script (Linux/Mac/WSL/Git Bash)

## 🎯 Uso

### Opción 1: Linux/Mac
```bash
chmod +x demo.sh
./demo.sh
```

### Opción 2: Windows (Git Bash o WSL)
```bash
bash demo.sh
```

## 🔧 ¿Qué hace el script?

1. ✅ Verifica que Docker, Maven y Java estén instalados
2. 🧹 Limpia contenedores anteriores (mysql-demo, postgres-demo)
3. 🐬 Levanta MySQL 8.0 en puerto 3306
4. 🐘 Levanta PostgreSQL 15 en puerto 5432
5. ⏳ Espera a que ambas bases de datos estén listas
6. 📦 Compila la aplicación Java con Maven
7. 🚀 Inicia el servidor web en puerto 8080
8. 📊 Muestra información de conexión y consultas de ejemplo

## 🌐 Acceso

Una vez iniciado, accede a:
- **Aplicación Web**: http://localhost:8080
- **MySQL**: localhost:3306 (usuario: root, password: root)
- **PostgreSQL**: localhost:5432 (usuario: postgres, password: postgres)

## 📝 Consultas de Ejemplo

### MySQL
```sql
SELECT VERSION();
CREATE TABLE test (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(50));
INSERT INTO test (nombre) VALUES ('Demo MySQL');
SELECT * FROM test;
```

### PostgreSQL
```sql
SELECT version();
CREATE TABLE test (id SERIAL PRIMARY KEY, nombre VARCHAR(50));
INSERT INTO test (nombre) VALUES ('Demo PostgreSQL');
SELECT * FROM test;
```

## 🛑 Detener la Demostración

```bash
# Detener contenedores Docker
docker rm -f mysql-demo postgres-demo

# Detener aplicación Java
pkill -f 'org.example.Main'
```

## 🎓 Características Demostradas

- ✅ **Patrón Abstract Factory**: Creación de conexiones específicas por proveedor
- ✅ **Conexión Socket Nativa**: Implementación del protocolo MySQL/PostgreSQL
- ✅ **Fallback JDBC**: Si falla socket, usa JDBC automáticamente
- ✅ **Servidor Web Embebido**: Jetty para interfaz HTTP
- ✅ **API REST**: Endpoint `/query` para ejecutar SQL
- ✅ **Gestión de Configuración**: Variables de entorno y db.properties
- ✅ **Utilidades Desacopladas**: SQLCleaner, ResultSetConverter, etc.

## 📚 Arquitectura

```
org.example
├── Main.java                    # Punto de entrada
├── db/
│   ├── DBClient.java           # Cliente principal
│   ├── DBConnection.java       # Interfaz conexión
│   ├── DBFactory.java          # Abstract Factory
│   ├── DBQuery.java            # Interfaz query
│   ├── mysql/                  # Implementación MySQL
│   │   ├── MySQLFactory.java
│   │   ├── MySQLConnection.java
│   │   └── MySQLQuery.java
│   ├── postgres/               # Implementación PostgreSQL
│   │   ├── PostgressFactory.java
│   │   ├── PostgressConnection.java
│   │   └── PostgressQuery.java
│   └── utilities/              # Utilidades compartidas
│       ├── SQLCleaner.java
│       ├── ConnectionConfig.java
│       ├── JDBCConnectionHelper.java
│       ├── ResultSetConverter.java
│       └── QueryResponseBuilder.java
└── web/                        # Servidor web
    ├── WebServer.java
    └── servlet/
        └── QueryServlet.java
```

## 🐛 Solución de Problemas

### Puerto ya en uso
```bash
# Verificar qué está usando el puerto
netstat -ano | grep 3306
netstat -ano | grep 5432
netstat -ano | grep 8080

# Detener procesos si es necesario
docker rm -f mysql-demo postgres-demo
pkill -f 'org.example.Main'
```

### Contenedores no inician
```bash
# Ver logs de Docker
docker logs mysql-demo
docker logs postgres-demo

# Reiniciar Docker si es necesario
```

### Aplicación no compila
```bash
# Limpiar y recompilar
mvn clean install -DskipTests
```

## 📞 Contacto

Para dudas o problemas, revisar la documentación en `docs/` o consultar el código fuente.

---

**¡Listo para la demostración!** 🎉
