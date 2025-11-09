@echo off
REM Script para Windows cmd.exe: arranca Postgres en Docker, espera a que esté listo, compila y ejecuta la demo
setlocal enabledelayedexpansion

:: Configuración
set CONTAINER_NAME=dap-postgres
set PGUSER=postgres
set PGPASSWORD=postgres
set PGDATABASE=postgres
set PGHOST=localhost
set PGPORT=5432

:: Procesar argumentos: /FORCE o /Y para no preguntar, /SIMULATE para forzar modo simulacion
set NO_PROMPT=0
set FORCE_SIM=0
if "%~1"=="/FORCE" set NO_PROMPT=1
if "%~1"=="/Y" set NO_PROMPT=1
if /I "%~1"=="/SIMULATE" set FORCE_SIM=1

:: Si se pide forzar simulación, saltar comprobaciones de Docker
if "%FORCE_SIM%"=="1" goto SIMULATE_ONLY

:: Comprobar si el comando docker está disponible
where docker >nul 2>&1
if errorlevel 1 (
  echo Docker no encontrado en PATH.
  set DOCKER_AVAILABLE=0
) else (
  set DOCKER_AVAILABLE=1
)

:: Si docker está instalado, comprobar que el daemon responde
if "%DOCKER_AVAILABLE%"=="1" (
  docker info >nul 2>&1
  if errorlevel 1 (
    echo Docker instalado pero el daemon no responde. Asegurate de que Docker Desktop o el servicio está en marcha.
    set DOCKER_AVAILABLE=0
  )
)

:: Si Docker no está disponible y no se fuerza, preguntar o salir según NO_PROMPT
if "%DOCKER_AVAILABLE%"=="0" (
  if "%NO_PROMPT%"=="1" (
    echo Docker no disponible y se solicitó ejecucion no interactiva. Abortando.
    endlocal
    exit /b 1
  ) else (
    echo.
    echo No es posible usar Docker. Deseas continuar en modo simulacion local sin Postgres nativo? [Y/N]
    set /p ANSWER=Respuesta:
    if /I "%ANSWER%"=="Y" goto SIMULATE_ONLY
    echo Cancelling.
    endlocal
    exit /b 1
  )
)

:: Eliminar contenedor anterior si existe
echo Buscando contenedor previo %CONTAINER_NAME%...
for /f "usebackq" %%i in (`docker ps -a --filter "name=%CONTAINER_NAME%" -q`) do set EXISTING=%%i
if defined EXISTING (
  echo Parando y eliminando contenedor previo %CONTAINER_NAME%...
  docker rm -f %CONTAINER_NAME%
  set EXISTING=
)

echo Iniciando contenedor Postgres...
docker run --name %CONTAINER_NAME% -e POSTGRES_PASSWORD=%PGPASSWORD% -e POSTGRES_USER=%PGUSER% -e POSTGRES_DB=%PGDATABASE% -e POSTGRES_HOST_AUTH_METHOD=trust -p %PGPORT%:5432 -d postgres:16
if errorlevel 1 (
  echo Error al lanzar el contenedor. Revisa que la imagen 'postgres:16' está disponible o que Docker puede descargarla.
  if "%NO_PROMPT%"=="1" (
    echo Modo no interactivo: abortando.
    endlocal
    exit /b 1
  ) else (
    echo Deseas continuar en modo simulacion local? [Y/N]
    set /p ANSWER=Respuesta:
    if /I "%ANSWER%"=="Y" goto SIMULATE_ONLY
    endlocal
    exit /b 1
  )
)

echo Esperando a que Postgres acepte conexiones...
set tries=0
:waitloop
set /a tries+=1
docker exec %CONTAINER_NAME% pg_isready -U %PGUSER% -d %PGDATABASE%
if %ERRORLEVEL%==0 goto ready
if %tries% GEQ 30 (
  echo Timeout esperando a Postgres (aprox 60s). Revisa logs: docker logs %CONTAINER_NAME%
  if "%NO_PROMPT%"=="1" (
    echo Modo no interactivo: abortando tras timeout.
    endlocal
    exit /b 1
  ) else (
    echo Deseas continuar en modo simulacion local? [Y/N]
    set /p ANSWER=Respuesta:
    if /I "%ANSWER%"=="Y" goto SIMULATE_ONLY
    endlocal
    exit /b 1
  )
)
ping -n 2 127.0.0.1>nul
goto waitloop

:ready
echo Postgres listo.

:: Establecer variables de entorno para el proceso actual
set PGHOST=%PGHOST%
set PGPORT=%PGPORT%
set PGDATABASE=%PGDATABASE%
set PGUSER=%PGUSER%
set PGPASSWORD=%PGPASSWORD%

echo Compilando proyecto...
mvn -DskipTests package
if errorlevel 1 (
  echo Maven build falló.
  endlocal
  exit /b 1
)

echo Ejecutando demo Postgres...
mvn -DskipTests -Dexec.mainClass=org.example.db.postgres.PostgresDemo exec:java

goto FIN

:SIMULATE_ONLY
echo Ejecutando en modo simulacion local (sin Docker/Postgres). Se usará la DB simulada en memoria.

echo Compilando proyecto...
mvn -DskipTests package
if errorlevel 1 (
  echo Maven build falló.
  endlocal
  exit /b 1
)
echo Ejecutando demo Postgres (modo simulacion)...
mvn -DskipTests -Dexec.mainClass=org.example.db.postgres.PostgresDemo exec:java

:FIN
endlocal
exit /b 0
