param([switch]$Test)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not $env:JAVA_HOME) {
    $localJava = Get-ChildItem '.tools/java' -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJava) { $env:JAVA_HOME = $localJava.FullName }
}
if ($env:JAVA_HOME) { $env:PATH = "$env:JAVA_HOME/bin;$env:PATH" }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Install Java 17 or set JAVA_HOME before running Ticketbox.' }
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.tools/gradle'
if ($Test) { & .\gradlew.bat test --console=plain; exit $LASTEXITCODE }
& .\gradlew.bat bootRun --args='--spring.profiles.active=local' --console=plain
