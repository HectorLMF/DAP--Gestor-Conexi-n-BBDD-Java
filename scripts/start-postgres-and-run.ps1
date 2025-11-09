# PowerShell script para iniciar Postgres en Docker, esperar a que esté listo, compilar y ejecutar la demo
param(
    [string]$ContainerName = "dap-postgres",
    [string]$PGUser = "postgres",
    [string]$PGPassword = "postgres",
    [string]$PGDatabase = "postgres",
    [string]$PGHost = "localhost",
    [int]$PGPort = 5432
)

function Check-Docker {
    try { docker --version | Out-Null; return $true } catch { return $false }
}

if (-not (Check-Docker)) {
    Write-Warning "Docker no encontrado. Deseas continuar en modo simulacion local (sin Postgres nativo)? [Y/N]"
    $ans = Read-Host "Respuesta"
    if ($ans -match '^[Yy]') {
        Write-Output "Continuando en modo simulacion local."
        goto SimulateOnly
    } else {
        Write-Error "Interrumpo. Instala Docker Desktop y vuelve a intentarlo."
        exit 1
    }
}

# Comprobar que el daemon responde
try {
    docker info > $null 2>&1
} catch {
    Write-Warning "Docker instalado pero el daemon no respondió. Deseas continuar en modo simulacion local? [Y/N]"
    $ans = Read-Host "Respuesta"
    if ($ans -match '^[Yy]') {
        Write-Output "Continuando en modo simulacion local."
        goto SimulateOnly
    } else {
        Write-Error "Interrumpo. Asegurate de que Docker Desktop o el servicio está en marcha."
        exit 1
    }
}

# Eliminar contenedor previo si existe
$existing = docker ps -a --filter "name=$ContainerName" -q
if ($existing) {
    Write-Output "Parando y eliminando contenedor previo $ContainerName..."
    docker rm -f $ContainerName | Out-Null
}

Write-Output "Iniciando contenedor Postgres..."
# Construir argumentos para evitar que PowerShell interprete $PGPort:5432 como operador
$dockerArgs = @(
    'run',
    '--name', $ContainerName,
    '-e', "POSTGRES_PASSWORD=$PGPassword",
    '-e', "POSTGRES_USER=$PGUser",
    '-e', "POSTGRES_DB=$PGDatabase",
    '-e', "POSTGRES_HOST_AUTH_METHOD=trust",
    '-p', "$($PGPort):5432",
    '-d', 'postgres:16'
)

$run = & docker @dockerArgs 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Error al lanzar el contenedor: $run"
    $ans = Read-Host "Deseas continuar en modo simulacion local? [Y/N]"
    if ($ans -match '^[Yy]') {
        Write-Output "Continuando en modo simulacion local."
        goto SimulateOnly
    } else {
        Write-Error "Interrumpo. Revisa Docker o los permisos del usuario."
        exit 1
    }
}

Write-Output "Esperando a que Postgres acepte conexiones..."
$tries = 0
while ($tries -lt 30) {
    $tries++
    docker exec $ContainerName pg_isready -U $PGUser -d $PGDatabase > $null 2>&1
    if ($LASTEXITCODE -eq 0) { break }
    Start-Sleep -Seconds 2
}

if ($tries -ge 30) {
    Write-Error "Timeout esperando a Postgres (aprox 60s). Mostrando logs del contenedor..."
    docker logs $ContainerName
    $ans = Read-Host "Deseas continuar en modo simulacion local? [Y/N]"
    if ($ans -match '^[Yy]') {
        Write-Output "Continuando en modo simulacion local."
        goto SimulateOnly
    } else {
        exit 1
    }
}

Write-Output "Postgres listo."

# Setear variables de entorno para el proceso actual
$env:PGHOST = $PGHost
$env:PGPORT = $PGPort.ToString()
$env:PGDATABASE = $PGDatabase
$env:PGUSER = $PGUser
$env:PGPASSWORD = $PGPassword

Write-Output "Compilando proyecto..."
$mvnRes = & mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { Write-Error "Maven build falló."; exit 1 }

Write-Output "Ejecutando demo Postgres..."
# Invocar Maven pasando los argumentos de forma segura
$execArgs = @("-DskipTests", "-Dexec.mainClass=org.example.db.postgres.PostgresDemo", "exec:java")
& mvn @execArgs

Write-Output "Script finalizado."
exit 0

:SimulateOnly
Write-Output "Modo simulacion local: no se arranca Docker."
# Setear variables (opcionales)
$env:PGHOST = $PGHost
$env:PGPORT = $PGPort.ToString()
$env:PGDATABASE = $PGDatabase
$env:PGUSER = $PGUser
$env:PGPASSWORD = $PGPassword

Write-Output "Compilando proyecto..."
$mvnRes = & mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { Write-Error "Maven build falló."; exit 1 }

Write-Output "Ejecutando demo Postgres (modo simulacion)..."
$execArgs = @("-DskipTests", "-Dexec.mainClass=org.example.db.postgres.PostgresDemo", "exec:java")
& mvn @execArgs

Write-Output "Script finalizado (simulacion)."
exit 0
