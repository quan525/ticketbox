# Run this yourself in your local terminal. AI assistants must not run it or read the resulting secret file.
param(
    [Parameter(Mandatory = $true)][ValidateSet('TEST', 'LIVE')][string]$Mode,
    [ValidateSet('API_KEY', 'HMAC')][string]$Auth = 'API_KEY',
    [ValidatePattern('^[A-Za-z0-9_-]{2,50}$')][string]$Bank = 'VPBank',
    [string]$Gateway = 'VPBank',
    [Parameter(Mandatory = $true)][ValidatePattern('^[A-Za-z0-9]{1,40}$')][string]$AccountNumber,
    [ValidatePattern('^[A-Za-z0-9]{0,40}$')][string]$SubAccount = '',
    [Parameter(Mandatory = $true)][string]$AccountName,
    [switch]$Force,
    [switch]$NoRestart
)
$ErrorActionPreference = 'Stop'
$workspacePath = Split-Path $PSScriptRoot -Parent
$secretFolder = Join-Path $workspacePath 'secrets'
$settingsPath = Join-Path $secretFolder 'sepay.properties'
if ((Test-Path -LiteralPath $settingsPath) -and -not $Force) { throw 'Private SePay settings already exist. Use -Force only if you want to replace them. Existing values will not be read.' }
if ([string]::IsNullOrWhiteSpace($Gateway) -or [string]::IsNullOrWhiteSpace($AccountName)) { throw 'Gateway and account holder are required.' }
$secure = Read-Host 'Enter the SePay WEBHOOK secret/key (not your ngrok token; input hidden)' -AsSecureString
$secret = (New-Object System.Net.NetworkCredential('', $secure)).Password
if ($secret.Length -lt 16) { $secret = $null; $secure.Dispose(); throw 'Use the same webhook secret as SePay, with at least 16 characters.' }
function ConvertTo-PropertyValue([string]$value) {
    $builder = New-Object System.Text.StringBuilder
    foreach ($character in $value.ToCharArray()) {
        $code = [int]$character
        if ($code -le 32 -or $code -gt 126 -or $character -eq [char]92 -or $character -eq '=' -or $character -eq ':' -or $character -eq '#' -or $character -eq '!') {
            [void]$builder.Append(('\u{0:x4}' -f $code))
        } else { [void]$builder.Append($character) }
    }
    $builder.ToString()
}
try {
    New-Item -ItemType Directory -Path $secretFolder -Force | Out-Null
    $acl = New-Object System.Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true, $false)
    $identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().User
    foreach ($sid in @($identity, (New-Object System.Security.Principal.SecurityIdentifier('S-1-5-18')))) {
        $rule = New-Object System.Security.AccessControl.FileSystemAccessRule($sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
        $acl.AddAccessRule($rule)
    }
    Set-Acl -LiteralPath $secretFolder -AclObject $acl
    $content = @(
        'payments.sepay.enabled=true',
        ('payments.sepay.mode=' + $Mode),
        ('payments.sepay.auth=' + $Auth),
        ('payments.sepay.webhook-secret=' + (ConvertTo-PropertyValue $secret)),
        ('payments.sepay.bank=' + (ConvertTo-PropertyValue $Bank)),
        ('payments.sepay.gateway=' + (ConvertTo-PropertyValue $Gateway)),
        ('payments.sepay.account-number=' + (ConvertTo-PropertyValue $AccountNumber)),
        ('payments.sepay.sub-account=' + (ConvertTo-PropertyValue $SubAccount)),
        ('payments.sepay.account-name=' + (ConvertTo-PropertyValue $AccountName)),
        'payments.mock.enabled=false'
    )
    [System.IO.File]::WriteAllLines($settingsPath, $content, (New-Object System.Text.UTF8Encoding($false)))
    Write-Output ('Saved private SePay settings in ' + $Mode + ' mode. Secret values were not printed.')
} finally {
    $secret = $null; $content = $null; $secure.Dispose()
}
if (-not $NoRestart) { & (Join-Path $PSScriptRoot 'restart-local.ps1') }
Write-Output 'Match the webhook authentication type and secret in the SePay dashboard, then replay the existing failed event.'
Write-Output 'TBTEST references are retained for review only. Expired seats are never rebooked automatically.'
