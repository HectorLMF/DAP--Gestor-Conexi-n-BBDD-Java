@echo off
setlocal enabledelayedexpansion
set CONTAINER_NAME=dap-postgres
set PGUSER=postgres
set PGPASSWORD=postgres
set PGDATABASE=postgres
set PGHOST=localhost
set PGPORT=5432
echo Debug: buscar contenedor previo
for /f "usebackq" %%i in (`docker ps -a --filter "name=%CONTAINER_NAME%" -q`) do set EXISTING=%%i
if defined EXISTING (
  echo Debug: removing existing %CONTAINER_NAME%
  docker rm -f %CONTAINER_NAME%
)

echo Debug: starting container (no redirection)
docker run --name %CONTAINER_NAME% -e POSTGRES_PASSWORD=%PGPASSWORD% -e POSTGRES_USER=%PGUSER% -e POSTGRES_DB=%PGDATABASE% -e POSTGRES_HOST_AUTH_METHOD=trust -p %PGPORT%:5432 -d postgres:16
if errorlevel 1 (
  echo Debug: docker run failed with %ERRORLEVEL%
  exit /b 1
)

echo Debug: waiting for pg_isready loop (no redirection)
set tries=0
:dbgloop
set /a tries+=1
echo Debug: iteration %tries%
docker exec %CONTAINER_NAME% pg_isready -U %PGUSER% -d %PGDATABASE%
if %ERRORLEVEL%==0 goto dbgready
if %tries% GEQ 30 (
  echo Debug: timeout
  docker logs %CONTAINER_NAME%
  exit /b 1
)
ping -n 2 127.0.0.1>nul
goto dbgloop

:dbgready
echo Debug: pg_isready returned success
endlocal
exit /b 0

