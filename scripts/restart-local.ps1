$ErrorActionPreference = 'Stop'
$workspacePath = Split-Path $PSScriptRoot -Parent
$statePath = Join-Path $workspacePath '.local/app-process.json'
$jarPath = Join-Path $workspacePath 'build/libs/ticketbox-0.0.1-SNAPSHOT.jar'
$javaDirectory = Get-ChildItem (Join-Path $workspacePath '.tools/java') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
$java = if ($javaDirectory) { Join-Path $javaDirectory.FullName 'bin/java.exe' } else { (Get-Command java -ErrorAction Stop).Source }
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'Build the frontend and Spring Boot JAR first.' }
$listener = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    if (-not (Test-Path -LiteralPath $statePath)) { throw 'Port 8080 is in use by an untracked process. Stop that app yourself before restarting.' }
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $running = Get-Process -Id $listener.OwningProcess
    $expectedStart = if ($state.startedAtUtc -is [datetime]) { $state.startedAtUtc.ToUniversalTime() } else { [DateTimeOffset]::Parse([string]$state.startedAtUtc).UtcDateTime }
    if ($running.Id -ne $state.processId -or $running.Path -ne $state.executable -or [Math]::Abs(($running.StartTime.ToUniversalTime() - $expectedStart).TotalSeconds) -gt 1) {
        throw 'Port 8080 process identity differs from the tracked Ticketbox instance. It was not stopped.'
    }
    Stop-Process -Id $running.Id
    Wait-Process -Id $running.Id -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Path (Split-Path $statePath -Parent) -Force | Out-Null
$agent = Start-Process -FilePath $java -ArgumentList '-jar','build/libs/ticketbox-0.0.1-SNAPSHOT.jar','--spring.profiles.active=local' -WorkingDirectory $workspacePath -WindowStyle Hidden -RedirectStandardOutput (Join-Path $workspacePath '.local/app.stdout.log') -RedirectStandardError (Join-Path $workspacePath '.local/app.stderr.log') -PassThru
@{ processId = $agent.Id; executable = $java; startedAtUtc = $agent.StartTime.ToUniversalTime().ToString('o') } | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding utf8
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    if ($agent.HasExited) { throw 'Ticketbox exited during startup. Check private settings locally; do not share secrets or raw configuration logs.' }
    try { if ((Invoke-RestMethod 'http://127.0.0.1:8080/api/actuator/health' -TimeoutSec 2).status -eq 'UP') { Write-Output 'Ticketbox is running at http://localhost:8080/api/'; return } } catch { }
    Start-Sleep -Seconds 1
}
throw 'Ticketbox has not reported ready. Check it locally before retrying.'
