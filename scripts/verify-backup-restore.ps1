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
$socketOption = if ($IsLinux) { ' -k /tmp' } else { '' }
$started = $false
$envNames = @('MULTICORE_RESTORE_TEST_PORT', 'MULTICORE_RESTORE_TEST_DIR', 'MULTICORE_RESTORE_TEST_PG_BIN')
$previous = @{}
foreach ($name in $envNames) { $previous[$name] = [Environment]::GetEnvironmentVariable($name) }
Push-Location $workspace
try {
    & (Join-Path $PgBinDir 'initdb') -D $clusterDir -U restore_test --auth=trust --encoding=UTF8 --locale=C *> (Join-Path $runDir 'initdb.log')
    if ($LASTEXITCODE -ne 0) { throw "Falha ao criar a instancia temporaria. Consulte $runDir/initdb.log" }
    # O postgres fica a correr com os handles de saida herdados do pg_ctl. Se fossem os do chamador
    # (ex.: script.ps1 *> ficheiro), o PowerShell esperaria por eles para sempre e os testes nunca
    # arrancavam. Por isso o pg_ctl escreve em ficheiros proprios e espera-se so pelo pg_ctl.
    $startArgs = "-D `"$clusterDir`" -l `"$(Join-Path $runDir 'postgres.log')`" -o `"-h 127.0.0.1 -p $testPort$socketOption`" -w start"
    $pgCtlStart = Start-Process -FilePath (Join-Path $PgBinDir 'pg_ctl') -ArgumentList $startArgs -NoNewWindow -PassThru `
        -RedirectStandardOutput (Join-Path $runDir 'pg_ctl-start.log') -RedirectStandardError (Join-Path $runDir 'pg_ctl-start.err')
    $null = $pgCtlStart.Handle  # sem isto o ExitCode pode vir vazio no Windows PowerShell
    $pgCtlStart.WaitForExit()
    if ($pgCtlStart.ExitCode -ne 0) { throw "Falha ao iniciar a instancia temporaria. Consulte $runDir/pg_ctl-start.err" }
    $started = $true
    $env:MULTICORE_RESTORE_TEST_PORT = "$testPort"
    $env:MULTICORE_RESTORE_TEST_DIR = $runDir
    $env:MULTICORE_RESTORE_TEST_PG_BIN = $PgBinDir
    & mvn -pl backend -am '-Dtest=DatabaseBackupRoundTripTest,DatabaseBackupServiceTest,PostgresSchemaParityHarnessTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
    if ($LASTEXITCODE -ne 0) { throw 'A verificacao de recuperacao falhou.' }
    Write-Host "Evidencia e backup preservados em $runDir"
} finally {
    if ($started) {
        & (Join-Path $PgBinDir 'pg_ctl') -D $clusterDir -m fast -w stop
        if ($LASTEXITCODE -ne 0) { Write-Warning "Nao foi possivel parar a instancia temporaria em $clusterDir" }
    }
    foreach ($name in $envNames) { [Environment]::SetEnvironmentVariable($name, $previous[$name]) }
    Pop-Location
}
