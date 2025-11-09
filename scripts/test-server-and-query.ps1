# Script de prueba: compila, arranca Main en background y envía un POST a /query
# Resultado: imprime la respuesta JSON y muestra los últimos logs del servidor

Push-Location "c:\Users\Hecto\Desktop\Nueva carpeta\DAP--Gestor-Conexi-n-BBDD-Java"

Write-Output "Compilando proyecto..."
$mvn = mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { Write-Error "Maven build falló"; exit 1 }

$log = "$env:TEMP\dap_server.log"
if (Test-Path $log) { Remove-Item $log -Force }

# Arrancar la JVM en background y redirigir salida a fichero mediante cmd.exe (compatible con PowerShell 5.1)
$cmd = "java -cp target/classes org.example.Main > \"$log\" 2>&1"
Write-Output "Iniciando servidor (java) en background..."
Start-Process -FilePath cmd.exe -ArgumentList '/c', $cmd -WindowStyle Hidden | Out-Null
Start-Sleep -Seconds 2

Write-Output "Enviando petición POST a /query..."
$body = '{"db":"postgres","sql":"SELECT 1 AS prueba"}'
try {
    $res = Invoke-RestMethod -Uri 'http://127.0.0.1:8000/query' -Method Post -Body $body -ContentType 'application/json' -ErrorAction Stop
    $res | ConvertTo-Json -Depth 5
} catch {
    Write-Error "Error al invocar endpoint: $($_.Exception.Message)"
}

Write-Output "---- Últimos logs del servidor ----"
if (Test-Path $log) { Get-Content $log -Tail 200 } else { Write-Output "Log file no encontrado: $log" }

Pop-Location
