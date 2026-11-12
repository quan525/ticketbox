$ErrorActionPreference = 'Stop'
$statePath = Join-Path (Split-Path $PSScriptRoot -Parent) '.local/ngrok-sepay.json'
if (-not (Test-Path -LiteralPath $statePath)) { Write-Output 'No tracked SePay tunnel.'; return }
$state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
$agent = Get-Process -Id $state.processId -ErrorAction SilentlyContinue
if ($agent) {
    $expectedStart = if ($state.startedAtUtc -is [datetime]) { $state.startedAtUtc.ToUniversalTime() } else { [DateTimeOffset]::Parse([string]$state.startedAtUtc).UtcDateTime }
    $actualStart = $agent.StartTime.ToUniversalTime()
    if ($agent.ProcessName -ne 'ngrok' -or $agent.Path -ne $state.executable -or [Math]::Abs(($actualStart - $expectedStart).TotalSeconds) -gt 1) {
        throw 'Tracked process identity changed. No process was stopped.'
    }
    Stop-Process -Id $agent.Id
}
Remove-Item -LiteralPath $statePath
Write-Output 'Stopped the tracked SePay webhook tunnel.'
