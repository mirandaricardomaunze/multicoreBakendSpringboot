# Multicore - installs a verified update after the ERP closes (docs/ACTUALIZACAO_UM_CLIQUE_SPEC.md 4.4).
# Copied by the ERP to %LOCALAPPDATA%\MulticoreERP\updates and started hidden. ASCII only (PowerShell 5.1).
param(
    [Parameter(Mandatory = $true)][string]$Installer,
    [Parameter(Mandatory = $true)][string]$Sha256,
    [Parameter(Mandatory = $true)][string]$AppExe,
    [Parameter(Mandatory = $true)][string]$Log,
    [switch]$Relaunch
)

function Write-UpdateLog([string]$message) {
    $line = (Get-Date -Format "yyyy-MM-dd HH:mm:ss") + "  " + $message
    try { Add-Content -LiteralPath $Log -Value $line -Encoding UTF8 } catch { }
}

Write-UpdateLog "A aguardar que o Multicore feche ($AppExe)."
while (Get-Process -ErrorAction SilentlyContinue | Where-Object { $_.Path -and ($_.Path -ieq $AppExe) }) {
    Start-Sleep -Seconds 2
}

# The file sat on disk since the ERP verified it: check it again right before running it.
if (-not (Test-Path -LiteralPath $Installer)) {
    Write-UpdateLog "Instalador em falta: $Installer. Nada instalado."
    exit 2
}
# .NET directly, not Get-FileHash: that cmdlet lives in a module that fails to load when PSModulePath is
# inherited from PowerShell 7, and an empty hash must never be mistaken for a check.
$actual = ""
try {
    $stream = [System.IO.File]::OpenRead($Installer)
    try {
        $sha = [System.Security.Cryptography.SHA256]::Create()
        $actual = ([System.BitConverter]::ToString($sha.ComputeHash($stream)) -replace "-", "").ToLowerInvariant()
    } finally { $stream.Dispose() }
} catch {
    Write-UpdateLog "Nao foi possivel ler o instalador: $($_.Exception.Message). Nada instalado."
    exit 4
}
if ($actual -ne $Sha256.ToLowerInvariant()) {
    Write-UpdateLog "SHA-256 diferente do verificado ($actual). Instalador apagado, nada instalado."
    Remove-Item -LiteralPath $Installer -Force -ErrorAction SilentlyContinue
    exit 3
}

Write-UpdateLog "A instalar $Installer."
$process = Start-Process -FilePath $Installer -ArgumentList "/quiet" -Wait -PassThru
$code = $process.ExitCode
Write-UpdateLog "Instalador terminou com o codigo $code."
if ($code -eq 0) {
    Remove-Item -LiteralPath $Installer -Force -ErrorAction SilentlyContinue
    if ($Relaunch) {
        Write-UpdateLog "A reabrir o Multicore."
        Start-Process -FilePath $AppExe
    }
}
exit $code
