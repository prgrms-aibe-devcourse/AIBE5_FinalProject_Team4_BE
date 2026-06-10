# RECO-004 clothes 후보 풀 덤프
# 사용:
#   데이터만: .\src\main\resources\db\share\export-clothes-pool.ps1 -DataOnly
#   구조+데이터: .\src\main\resources\db\share\export-clothes-pool.ps1

param(
    [switch]$DataOnly
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)))
$OutDir = Join-Path $Root "build\db-share"
$OutFile = if ($DataOnly) {
    Join-Path $OutDir "clothes-pool-data-only.sql"
} else {
    Join-Path $OutDir "clothes-pool-full.sql"
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$DbHost = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "127.0.0.1" }
$DbPort = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3307" }
$DbName = if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "closetnangamdb" }
$DbUser = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$DbPass = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "local1234" }

$dumpArgs = @(
    "-h", $DbHost,
    "-P", $DbPort,
    "-u", $DbUser,
    "-p$DbPass",
    "--complete-insert",
    "--single-transaction",
    "--no-tablespaces"
)
if ($DataOnly) {
    $dumpArgs += "--no-create-info"
}

Write-Host "Exporting to $OutFile ... ($(if ($DataOnly) { 'data only' } else { 'schema + data' }))"

& mysqldump @dumpArgs $DbName clothes clothing_colors clothing_styles `
    | Set-Content -Path $OutFile -Encoding utf8

$sizeMb = [math]::Round((Get-Item $OutFile).Length / 1MB, 2)
Write-Host "Done. Size: ${sizeMb} MB"
