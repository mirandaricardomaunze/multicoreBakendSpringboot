param(
    [string]$Version = "1.0.0",
    [string]$OutputDirectory = "dist"
)

$ErrorActionPreference = "Stop"
$workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$output = Join-Path $workspace $OutputDirectory
$desktopTarget = Join-Path $workspace "desktop\target"
$license = Join-Path $workspace "installer\LICENSE.txt"
$portableWix = Join-Path $workspace "tools\wix"

if ((Test-Path (Join-Path $portableWix "candle.exe")) -and
    (Test-Path (Join-Path $portableWix "light.exe"))) {
    $env:PATH = "$portableWix;$env:PATH"
}

Push-Location $workspace
try {
    mvn -pl desktop -am -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "A compilação do desktop falhou." }

    $desktopJar = Get-ChildItem -LiteralPath $desktopTarget -File -Filter "multicore-desktop-*.jar" |
        Where-Object { $_.Name -notlike "*.original" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $desktopJar) { throw "O JAR compilado do desktop não foi encontrado." }

    if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
        throw "WiX 3 não encontrado. Instale-o ou extraia os binários em tools\wix."
    }
    $icon = Join-Path $workspace "installer\app-icon.ico"
    New-Item -ItemType Directory -Force -Path $output | Out-Null
    jpackage `
        --type exe `
        --name "Multicore" `
        --app-version $Version `
        --vendor "Multicore" `
        --description "Sistema integrado de gestão Multicore" `
        --icon $icon `
        --input $desktopTarget `
        --main-jar $desktopJar.Name `
        --main-class "org.springframework.boot.loader.launch.JarLauncher" `
        --license-file $license `
        --dest $output `
        --win-per-user-install `
        --win-menu `
        --win-shortcut `
        --win-dir-chooser
    if ($LASTEXITCODE -ne 0) { throw "A criação do instalador falhou." }
} finally {
    Pop-Location
}
