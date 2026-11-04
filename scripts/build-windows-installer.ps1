param(
    # Vazio = a versão do pom.xml, a mesma que o programa envia ao servidor (X-Client-Version).
    [string]$Version = "",
    [string]$OutputDirectory = "dist"
)

$ErrorActionPreference = "Stop"
$workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$pomVersion = [regex]::Match((Get-Content -Raw (Join-Path $workspace "pom.xml")),
    '<artifactId>multicore-parent</artifactId><version>([^<]+)</version>').Groups[1].Value
if (-not $pomVersion) { throw "Versão do pom.xml não encontrada." }
if (-not $Version) { $Version = $pomVersion }
if ($Version -ne $pomVersion) {
    throw "A versão $Version difere do pom.xml ($pomVersion): o servidor veria outra versão. Use mvn versions:set."
}
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
    # Nome fixo para publicar em <servidor>/downloads/ (scripts/publicar-instalador.sh).
    $built = Join-Path $output "Multicore-$Version.exe"
    Copy-Item -LiteralPath $built -Destination (Join-Path $output "Multicore-instalador.exe") -Force
    Write-Host "Instalador $Version pronto: $output\Multicore-instalador.exe"
} finally {
    Pop-Location
}
