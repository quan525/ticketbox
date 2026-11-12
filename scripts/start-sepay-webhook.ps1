param(
    [ValidateRange(1, 65535)][int]$Port = 8080,
    [ValidateRange(5, 60)][int]$WaitSeconds = 30
)
$ErrorActionPreference = 'Stop'
$workspacePath = Split-Path $PSScriptRoot -Parent
$localNgrok = Join-Path $workspacePath '.tools/ngrok/ngrok.exe'
$policyPath = Join-Path $PSScriptRoot 'ngrok-sepay-policy.yml'
$statePath = Join-Path $workspacePath '.local/ngrok-sepay.json'
$client = Get-Command ngrok -ErrorAction SilentlyContinue
$executable = if ($client) { $client.Source } elseif (Test-Path -LiteralPath $localNgrok) { $localNgrok } else { throw 'Install ngrok from https://ngrok.com/download/windows, then authenticate it locally. Do not paste the authtoken into chat.' }
try {
    $health = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/api/actuator/health" -TimeoutSec 4
    if ($health.status -ne 'UP') { throw 'Not ready' }
} catch { throw 'Start Ticketbox first, then run this script again.' }
if (Get-NetTCPConnection -LocalPort 4040 -State Listen -ErrorAction SilentlyContinue) {
    throw 'Port 4040 is already in use. Stop the existing ngrok agent before starting this dedicated webhook tunnel.'
}
if (-not (Test-Path -LiteralPath $policyPath)) { throw 'Webhook traffic policy is missing; refusing to expose the app without it.' }
New-Item -ItemType Directory -Path (Split-Path $statePath -Parent) -Force | Out-Null
$start = New-Object System.Diagnostics.ProcessStartInfo
$start.FileName = $executable
$start.WorkingDirectory = $workspacePath
$start.Arguments = 'http http://127.0.0.1:' + $Port + ' --name=ticketbox-sepay --inspect=false --log=false --traffic-policy-file="' + $policyPath + '"'
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
$agent = New-Object System.Diagnostics.Process
$agent.StartInfo = $start
if (-not $agent.Start()) { throw 'ngrok could not start.' }
# Drain diagnostics without persisting or displaying output that may contain credentials.
$outputTask = $agent.StandardOutput.ReadToEndAsync()
$errorTask = $agent.StandardError.ReadToEndAsync()
try {
    for ($attempt = 0; $attempt -lt $WaitSeconds; $attempt++) {
        if ($agent.HasExited) {
            $diagnostic = $errorTask.GetAwaiter().GetResult() + $outputTask.GetAwaiter().GetResult()
            $codes = [regex]::Matches($diagnostic, 'ERR_NGROK_[0-9]+') | ForEach-Object { $_.Value } | Select-Object -Unique
            $diagnostic = $null
            $safeCode = if ($codes) { ' (' + ($codes -join ', ') + ')' } else { '' }
            throw ('ngrok did not connect' + $safeCode + '. Configure your ngrok account/authtoken locally and check network access or plan restrictions. Raw diagnostics were not printed or saved.')
        }
        try {
            $endpoints = Invoke-RestMethod -Uri 'http://127.0.0.1:4040/api/tunnels' -TimeoutSec 2
            $tunnel = $endpoints.tunnels | Where-Object { $_.name -eq 'ticketbox-sepay' -and $_.public_url -like 'https://*' } | Select-Object -First 1
            if ($tunnel) {
                $webhook = $tunnel.public_url.TrimEnd('/') + '/api/payments/sepay/webhook'
                $state = [ordered]@{ processId = $agent.Id; executable = $executable; startedAtUtc = $agent.StartTime.ToUniversalTime().ToString('o'); webhookUrl = $webhook }
                $state | ConvertTo-Json | Set-Content -LiteralPath $statePath -Encoding utf8
                Write-Output ('SePay webhook URL: ' + $webhook)
                Write-Output 'Only POST to this webhook path is forwarded. Request-body inspection is disabled.'
                Write-Output 'Choose incoming money + JSON in SePay and retain HMAC/API-key authentication. Local QR preview alone does not enable webhook processing.'
                Write-Output 'Stop with: ./scripts/stop-sepay-webhook.ps1'
                return
            }
        } catch { }
        Start-Sleep -Seconds 1
    }
    throw 'ngrok did not publish the dedicated webhook endpoint within the startup window.'
} catch {
    if (-not $agent.HasExited) { $agent.Kill(); $agent.WaitForExit() }
    throw
}
