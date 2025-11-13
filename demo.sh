#!/bin/bash

###############################################################################
# Script de Demostración - Gestor de Conexiones BBDD
# 
# Este script realiza una demostración completa del sistema:
# 1. Levanta contenedores Docker de MySQL y PostgreSQL
# 2. Espera a que las bases de datos estén listas
# 3. Compila y ejecuta la aplicación Java
# 4. Proporciona información para probar el sistema
#
# Uso: ./demo.sh
###############################################################################

set -e  # Salir si hay algún error

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}"
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║   DEMOSTRACIÓN - GESTOR DE CONEXIONES BBDD               ║"
echo "║   Abstract Factory Pattern + JDBC + Sockets              ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 1. Verificar dependencias
echo -e "${YELLOW}[1/6] Verificando dependencias...${NC}"
command -v docker >/dev/null 2>&1 || { echo -e "${RED}❌ Docker no está instalado${NC}"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo -e "${RED}❌ Maven no está instalado${NC}"; exit 1; }
command -v java >/dev/null 2>&1 || { echo -e "${RED}❌ Java no está instalado${NC}"; exit 1; }
echo -e "${GREEN}✓ Todas las dependencias están instaladas${NC}"

# 2. Limpiar contenedores existentes
echo -e "\n${YELLOW}[2/6] Limpiando contenedores existentes...${NC}"
docker rm -f mysql-demo 2>/dev/null || true
docker rm -f postgres-demo 2>/dev/null || true
echo -e "${GREEN}✓ Contenedores limpiados${NC}"

# 3. Levantar MySQL
echo -e "\n${YELLOW}[3/6] Iniciando MySQL...${NC}"
docker run -d \
    --name mysql-demo \
    -e MYSQL_ROOT_PASSWORD=root \
    -e MYSQL_DATABASE=test \
    -p 3306:3306 \
    mysql:8.0 \
    --default-authentication-plugin=mysql_native_password

echo "   Esperando a que MySQL esté listo..."
for i in {1..30}; do
    if docker exec mysql-demo mysqladmin ping -h localhost -uroot -proot &>/dev/null; then
        echo -e "${GREEN}✓ MySQL está listo${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ MySQL no se inició correctamente${NC}"
        exit 1
    fi
    echo -n "."
    sleep 2
done

# 4. Levantar PostgreSQL
echo -e "\n${YELLOW}[4/6] Iniciando PostgreSQL...${NC}"
docker run -d \
    --name postgres-demo \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_DB=postgres \
    -p 5432:5432 \
    postgres:15-alpine

echo "   Esperando a que PostgreSQL esté listo..."
for i in {1..30}; do
    if docker exec postgres-demo pg_isready -U postgres &>/dev/null; then
        echo -e "${GREEN}✓ PostgreSQL está listo${NC}"
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ PostgreSQL no se inició correctamente${NC}"
        exit 1
    fi
    echo -n "."
    sleep 2
done

# 5. Compilar aplicación
echo -e "\n${YELLOW}[5/6] Compilando aplicación Java...${NC}"
mvn clean compile -q
echo -e "${GREEN}✓ Aplicación compilada${NC}"

# 6. Iniciar aplicación
echo -e "\n${YELLOW}[6/6] Iniciando aplicación...${NC}"
echo -e "${BLUE}════════════════════════════════════════════════${NC}"

# Matar cualquier proceso Java anterior
pkill -f "org.example.Main" 2>/dev/null || true
sleep 2

# Iniciar aplicación en segundo plano
mvn exec:java -Dexec.mainClass="org.example.Main" &
APP_PID=$!

# Esperar a que la aplicación esté lista
echo "   Esperando a que el servidor web esté listo..."
for i in {1..15}; do
    if curl -s http://localhost:8080 >/dev/null 2>&1; then
        echo -e "${GREEN}✓ Servidor web está listo${NC}"
        break
    fi
    if [ $i -eq 15 ]; then
        echo -e "${RED}❌ El servidor no se inició correctamente${NC}"
        kill $APP_PID 2>/dev/null || true
        exit 1
    fi
    sleep 2
done

# Información de la demostración
echo -e "\n${GREEN}"
echo "╔═══════════════════════════════════════════════════════════╗"
echo "║              ✅ SISTEMA LISTO PARA DEMOSTRAR              ║"
echo "╚═══════════════════════════════════════════════════════════╝"
echo -e "${NC}"

echo -e "${BLUE}🌐 URL de la aplicación:${NC}"
echo -e "   ${GREEN}http://localhost:8080${NC}"
echo ""

echo -e "${BLUE}📊 Estado de los contenedores:${NC}"
docker ps --filter "name=mysql-demo" --filter "name=postgres-demo" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""

echo -e "${BLUE}🔧 Consultas SQL de ejemplo:${NC}"
echo ""
echo -e "${YELLOW}Para MySQL:${NC}"
echo "   SELECT VERSION();"
echo "   CREATE TABLE test (id INT AUTO_INCREMENT PRIMARY KEY, nombre VARCHAR(50));"
echo "   INSERT INTO test (nombre) VALUES ('Demo');"
echo "   SELECT * FROM test;"
echo ""
echo -e "${YELLOW}Para PostgreSQL:${NC}"
echo "   SELECT version();"
echo "   CREATE TABLE test (id SERIAL PRIMARY KEY, nombre VARCHAR(50));"
echo "   INSERT INTO test (nombre) VALUES ('Demo');"
echo "   SELECT * FROM test;"
echo ""

echo -e "${BLUE}📝 Logs de la aplicación:${NC}"
echo "   tail -f nohup.out"
echo ""

echo -e "${BLUE}🛑 Para detener todo:${NC}"
echo "   docker rm -f mysql-demo postgres-demo"
echo "   pkill -f 'org.example.Main'"
echo ""

echo -e "${GREEN}✨ Presiona Ctrl+C para detener el script (la app seguirá corriendo)${NC}"
echo -e "${GREEN}✨ O abre tu navegador en http://localhost:8080${NC}"
echo ""

# Mantener el script vivo y mostrar logs
wait $APP_PID
