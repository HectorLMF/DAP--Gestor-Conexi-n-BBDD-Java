#!/bin/bash
###############################################################################
# Comandos útiles para la demostración
###############################################################################

# 1. INICIAR DEMOSTRACIÓN COMPLETA
./demo.sh

# 2. VER LOGS EN TIEMPO REAL
tail -f nohup.out

# 3. DETENER TODO
docker rm -f mysql-demo postgres-demo
pkill -f 'org.example.Main'

# 4. SOLO REINICIAR APLICACIÓN (sin tocar Docker)
pkill -f 'org.example.Main'
sleep 2
mvn exec:java &

# 5. SOLO REINICIAR BASES DE DATOS
docker restart mysql-demo postgres-demo

# 6. VER ESTADO DE CONTENEDORES
docker ps --filter "name=demo"

# 7. CONECTARSE A MYSQL (desde terminal)
docker exec -it mysql-demo mysql -uroot -proot test

# 8. CONECTARSE A POSTGRESQL (desde terminal)
docker exec -it postgres-demo psql -U postgres -d postgres

# 9. VER LOGS DE MYSQL
docker logs mysql-demo

# 10. VER LOGS DE POSTGRESQL
docker logs postgres-demo

# 11. RECOMPILAR SIN EJECUTAR
mvn clean compile

# 12. LIMPIAR TODO Y EMPEZAR DE CERO
docker rm -f mysql-demo postgres-demo
pkill -f 'org.example.Main'
mvn clean
./demo.sh

# 13. PROBAR CONECTIVIDAD A MYSQL
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "provider=mysql&sql=SELECT VERSION();"

# 14. PROBAR CONECTIVIDAD A POSTGRESQL
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "provider=postgres&sql=SELECT version();"

# 15. VER PUERTOS EN USO
netstat -tuln | grep -E '3306|5432|8080'

# 16. VERIFICAR JAVA Y MAVEN
java -version
mvn -version

# 17. VER PROCESOS JAVA
ps aux | grep java

# 18. MATAR TODOS LOS PROCESOS JAVA (cuidado!)
pkill -9 java

# 19. CREAR TABLA DE PRUEBA EN MYSQL
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "provider=mysql&sql=CREATE TABLE demo (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(50));"

# 20. INSERTAR DATOS EN MYSQL
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "provider=mysql&sql=INSERT INTO demo (nombre) VALUES ('Test');"

# 21. CONSULTAR DATOS EN MYSQL
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "provider=mysql&sql=SELECT * FROM demo;"

# 22. BACKUP DE BASE DE DATOS MYSQL
docker exec mysql-demo mysqldump -uroot -proot test > backup_mysql.sql

# 23. BACKUP DE BASE DE DATOS POSTGRESQL
docker exec postgres-demo pg_dump -U postgres postgres > backup_postgres.sql

# 24. VER USO DE MEMORIA DE CONTENEDORES
docker stats mysql-demo postgres-demo --no-stream

# 25. VERIFICAR CONEXIÓN A MYSQL (health check)
docker exec mysql-demo mysqladmin ping -h localhost -uroot -proot

# 26. VERIFICAR CONEXIÓN A POSTGRESQL (health check)
docker exec postgres-demo pg_isready -U postgres

# 27. ABRIR NAVEGADOR EN LA APLICACIÓN (Linux)
xdg-open http://localhost:8080

# 28. ABRIR NAVEGADOR EN LA APLICACIÓN (Mac)
open http://localhost:8080

# 29. VER THREADS JAVA
jstack $(pgrep -f 'org.example.Main')

# 30. MONITOREO DE RECURSOS EN TIEMPO REAL
watch -n 2 'docker stats mysql-demo postgres-demo --no-stream'

###############################################################################
# CONSULTAS SQL DE DEMOSTRACIÓN
###############################################################################

# MySQL - Crear tabla y datos
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "provider=mysql" \
  --data-urlencode "sql=CREATE TABLE IF NOT EXISTS empleados (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(100), salario DECIMAL(10,2));"

curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "provider=mysql" \
  --data-urlencode "sql=INSERT INTO empleados (nombre, salario) VALUES ('Juan', 50000), ('María', 60000), ('Pedro', 55000);"

curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "provider=mysql" \
  --data-urlencode "sql=SELECT * FROM empleados WHERE salario > 52000;"

# PostgreSQL - Crear tabla y datos
curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "provider=postgres" \
  --data-urlencode "sql=CREATE TABLE IF NOT EXISTS productos (id SERIAL PRIMARY KEY, nombre VARCHAR(100), precio NUMERIC(10,2));"

curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "provider=postgres" \
  --data-urlencode "sql=INSERT INTO productos (nombre, precio) VALUES ('Laptop', 899.99), ('Mouse', 25.50), ('Teclado', 75.00);"

curl -X POST http://localhost:8080/query \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data-urlencode "provider=postgres" \
  --data-urlencode "sql=SELECT * FROM productos ORDER BY precio DESC;"

###############################################################################
# NOTAS IMPORTANTES
###############################################################################

# - Asegúrate de tener Docker corriendo antes de ejecutar los comandos
# - Los puertos 3306, 5432 y 8080 deben estar libres
# - Usa Ctrl+C para detener procesos en foreground
# - Los contenedores se eliminan con "docker rm -f"
# - Los datos se pierden al eliminar contenedores (no hay volúmenes persistentes)
