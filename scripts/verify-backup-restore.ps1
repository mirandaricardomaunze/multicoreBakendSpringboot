param(
    [Parameter(Mandatory = $true)][string]$PgBinDir
)

$ErrorActionPreference = 'Stop'
$workspace = Split-Path $PSScriptRoot -Parent
$runDir = Join-Path $workspace ('data/restore-validation/' + [guid]::NewGuid().ToString('N'))
$clusterDir = Join-Path $runDir 'cluster'
New-Item -ItemType Directory -Path $runDir | Out-Null
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$testPort = $listener.LocalEndpoint.Port
$listener.Stop()
$started = $false
$envNames = @('MULTICORE_RESTORE_TEST_PORT', 'MULTICORE_RESTORE_TEST_DIR', 'MULTICORE_RESTORE_TEST_PG_BIN')
$previous = @{}
foreach ($name in $envNames) { $previous[$name] = [Environment]::GetEnvironmentVariable($name) }
Push-Location $workspace
try {
    & (Join-Path $PgBinDir 'initdb.exe') -D $clusterDir -U restore_test --auth=trust --encoding=UTF8 --locale=C *> (Join-Path $runDir 'initdb.log')
    if ($LASTEXITCODE -ne 0) { throw "Falha ao criar a instancia temporaria. Consulte $runDir/initdb.log" }
    & (Join-Path $PgBinDir 'pg_ctl.exe') -D $clusterDir -l (Join-Path $runDir 'postgres.log') -o "-h 127.0.0.1 -p $testPort" -w start
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao iniciar a instancia temporaria.' }
    $started = $true
    $env:MULTICORE_RESTORE_TEST_PORT = "$testPort"
    $env:MULTICORE_RESTORE_TEST_DIR = $runDir
    $env:MULTICORE_RESTORE_TEST_PG_BIN = $PgBinDir
    & mvn -pl backend -am '-Dtest=DatabaseBackupRoundTripTest,DatabaseBackupServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
    if ($LASTEXITCODE -ne 0) { throw 'A verificacao de recuperacao falhou.' }
    Write-Host "Evidencia e backup preservados em $runDir"
} finally {
    if ($started) {
        & (Join-Path $PgBinDir 'pg_ctl.exe') -D $clusterDir -m fast -w stop
        if ($LASTEXITCODE -ne 0) { Write-Warning "Nao foi possivel parar a instancia temporaria em $clusterDir" }
    }
    foreach ($name in $envNames) { [Environment]::SetEnvironmentVariable($name, $previous[$name]) }
    Pop-Location
}
