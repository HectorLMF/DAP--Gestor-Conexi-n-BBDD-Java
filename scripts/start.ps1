<#
Unified start script for the project.

Usage examples (PowerShell):
  # Show help
  .\scripts\start.ps1 -Action help

  # Start Docker Postgres and the app (foreground)
  .\scripts\start.ps1 -Action up

  # Start Docker Postgres and the app (background, logs in %TEMP%\dap_server.log)
  .\scripts\start.ps1 -Action up -Foreground:$false

  # Start only the app (simulate/no docker)
  .\scripts\start.ps1 -Action simulate

  # Stop and remove the Postgres container
  .\scripts\start.ps1 -Action down

  # Start in debug mode (passes Debug flag to JVM via MAVEN_OPTS)
  .\scripts\start.ps1 -Action up -Debug

This script unifies previous scripts and aims to be simple to operate.
#>

param(
    [ValidateSet('help','up','down','simulate','run')]
    [string]$Action = 'help',
    [string]$ContainerName = 'dap-postgres',
    [int]$PGPort = 5432,
    [string]$PGUser = 'postgres',
    [string]$PGPassword = 'postgres',
    [string]$PGDatabase = 'postgres',
    [switch]$Foreground = $true,
    [switch]$Debug = $false
)

function Write-Usage {
    Write-Output ''
    Write-Output 'Usage: start.ps1 -Action <help|up|down|simulate|run> [options]'
    Write-Output ''
    Write-Output 'Actions:'
    Write-Output '  help      Show this help message'
    Write-Output '  up        Start Postgres (docker) and the application (or only the app if docker not available)'
    Write-Output '  down      Stop and remove the Postgres docker container (if exists)'
    Write-Output '  simulate  Skip docker, set env vars and run the application'
    Write-Output '  run       Run only the application (assumes Postgres already available)'
    Write-Output ''
    Write-Output 'Options:'
    Write-Output '  -ContainerName <name>   Docker container name (default: dap-postgres)'
    Write-Output '  -PGPort <port>          Host port to map to Postgres 5432 (default: 5432)'
    Write-Output '  -Foreground             Run the app in foreground (default). Omit to run background.'
    Write-Output '  -Debug                  Enable debug mode (sets MAVEN_OPTS to enable remote debugging)
'
}

function Check-Docker {
    try { docker --version > $null 2>&1; return $true } catch { return $false }
}

function Start-Postgres {
    param($ContainerName,$PGUser,$PGPassword,$PGDatabase,$PGPort)
    if (-not (Check-Docker)) {
        Write-Warning "Docker no disponible en este equipo. Use -Action simulate para saltar Docker."
        return $false
    }

    # Remove existing container with same name
    $existing = docker ps -a --filter "name=$ContainerName" -q
    if ($existing) {
        Write-Output "Parando y eliminando contenedor previo $ContainerName..."
        docker rm -f $ContainerName | Out-Null
    }

    Write-Output "Iniciando contenedor Postgres ($ContainerName) en el puerto $PGPort..."
    $dockerArgs = @( 'run', '--name', $ContainerName, '-e', "POSTGRES_PASSWORD=$PGPassword", '-e', "POSTGRES_USER=$PGUser", '-e', "POSTGRES_DB=$PGDatabase", '-e', 'POSTGRES_HOST_AUTH_METHOD=trust', '-p', "$($PGPort):5432", '-d', 'postgres:16' )
    $run = & docker @dockerArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Error al lanzar el contenedor: $run"
        return $false
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
        Write-Error "Timeout esperando a Postgres. Compruebe docker logs: docker logs $ContainerName"
        return $false
    }

    Write-Output "Postgres listo."
    return $true
}

function Stop-Postgres {
    param($ContainerName)
    if (-not (Check-Docker)) { Write-Warning "Docker no disponible."; return }
    $existing = docker ps -a --filter "name=$ContainerName" -q
    if ($existing) {
        Write-Output "Deteniendo y eliminando $ContainerName..."
        docker rm -f $ContainerName | Out-Null
        Write-Output "Contenedor eliminado."
    } else {
        Write-Output "No existe contenedor llamado $ContainerName"
    }
}

function Set-EnvVars {
    param($PGHost,$PGPort,$PGDatabase,$PGUser,$PGPassword)
    $env:PGHOST = $PGHost
    $env:PGPORT = $PGPort.ToString()
    $env:PGDATABASE = $PGDatabase
    $env:PGUSER = $PGUser
    $env:PGPASSWORD = $PGPassword
    Write-Output "Variables de entorno PG*: PGHOST=$env:PGHOST PGPORT=$env:PGPORT PGDATABASE=$env:PGDATABASE"
}

function Build-App {
    Write-Output "Compilando proyecto..."
    & mvn -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Maven build falló" }
}

function Run-App {
    param($Foreground,$Debug)
    $log = "$env:TEMP\dap_server.log"
    if ($Debug) {
        Write-Output "Modo DEBUG activado: exponiendo puerto 5005 para depuración remota (attach)"
        $env:MAVEN_OPTS = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    }

    if ($Foreground) {
        Write-Output "Ejecutando aplicación en primer plano (mostrar logs en consola)..."
        & mvn -DskipTests -Dexec.mainClass=org.example.Main exec:java
    } else {
        Write-Output "Ejecutando aplicación en background; logs en: $log"
        if (Test-Path $log) { Remove-Item $log -Force }
        $arg = "/c mvn -DskipTests -Dexec.mainClass=org.example.Main exec:java > `"$log`" 2>&1"
        Start-Process -FilePath cmd.exe -ArgumentList $arg -WindowStyle Hidden | Out-Null
        Write-Output "Servidor arrancado en background. Espera unos segundos y comprueba: Get-Content $log -Tail 200"
    }
}

# Main control flow
try {
    switch ($Action) {
        'help' { Write-Usage; break }
        'down' { Stop-Postgres -ContainerName $ContainerName; break }
        'simulate' {
            Write-Output "Modo simulación: no se intentará arrancar Docker."
            Set-EnvVars -PGHost 'localhost' -PGPort $PGPort -PGDatabase $PGDatabase -PGUser $PGUser -PGPassword $PGPassword
            Build-App
            Run-App -Foreground:$Foreground -Debug:$Debug
            break
        }
        'run' {
            Set-EnvVars -PGHost 'localhost' -PGPort $PGPort -PGDatabase $PGDatabase -PGUser $PGUser -PGPassword $PGPassword
            Build-App
            Run-App -Foreground:$Foreground -Debug:$Debug
            break
        }
        'up' {
            $ok = Start-Postgres -ContainerName $ContainerName -PGUser $PGUser -PGPassword $PGPassword -PGDatabase $PGDatabase -PGPort $PGPort
            if (-not $ok) {
                Write-Warning "No se pudo iniciar Postgres. Para simular, usa -Action simulate"
                exit 1
            }
            Set-EnvVars -PGHost 'localhost' -PGPort $PGPort -PGDatabase $PGDatabase -PGUser $PGUser -PGPassword $PGPassword
            Build-App
            Run-App -Foreground:$Foreground -Debug:$Debug
            break
        }
        default { Write-Usage }
    }
} catch {
    Write-Error "Error: $($_.Exception.Message)"
    exit 1
}

exit 0
