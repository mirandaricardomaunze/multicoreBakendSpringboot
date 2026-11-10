<#
.SYNOPSIS
  Ralph diff guard: checks only the lines ADDED by the current iteration against the mandatory
  rules in docs/CLEAN_ARCHITECTURE_RULES_SPEC.md (DG-01..DG-10). Legacy code is not judged here.
  ASCII-only on purpose (Windows PowerShell 5.1 reads BOM-less scripts as ANSI).
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\ralph\diff-guard.ps1 -WorkDir ..\manager-ralph -IncludeUntracked
#>
[CmdletBinding()]
param(
    [string]$WorkDir,
    [string]$Base = 'HEAD',
    [switch]$IncludeUntracked
)
$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $WorkDir) { $WorkDir = (Resolve-Path "$scriptDir\..\..").Path }

$findings = New-Object System.Collections.Generic.List[string]
function Add-Finding([string]$rule, [string]$file, [int]$line, [string]$text, [string]$why) {
    $snippet = $text.Trim()
    if ($snippet.Length -gt 110) { $snippet = $snippet.Substring(0, 110) + '...' }
    $findings.Add("[$rule] ${file}:$line  $why`n        > $snippet")
}

# Protected harness: the loop must never edit the rules that judge it.
$protected = @(
    'backend/src/test/resources/architecture/',
    'backend/src/test/java/mz/multicore/erp/architecture/CleanArchitectureHarnessTest.java',
    'backend/src/test/java/mz/multicore/erp/architecture/RalphGoalsHarnessTest.java',
    'scripts/ralph/',
    'docs/RALPH_LOOP_SPEC.md',
    'docs/CLEAN_ARCHITECTURE_RULES_SPEC.md'
)
# Only these desktop files may define raw colours (theme and palette owners).
$colourOwners = 'UIHelper\.java$|Theme[A-Za-z]*\.java$|Palette[A-Za-z]*\.java$|Colou?rs?\.java$|SimplePieChart\.java$'

# Emoji / pictograph ranges built from code points so this file stays ASCII:
# misc symbols + dingbats, misc symbols and arrows, arrows, geometric shapes, astral pictographs.
function Get-Range([int]$from, [int]$to) { return ([string][char]$from) + '-' + ([string][char]$to) }
$emoji = '[' + (Get-Range 0x2600 0x27BF) + (Get-Range 0x2B00 0x2BFF) + (Get-Range 0x2190 0x21FF) + (Get-Range 0x25A0 0x25FF) + ']' +
         '|[' + (Get-Range 0xD83C 0xD83E) + '][' + (Get-Range 0xDC00 0xDFFF) + ']'

Push-Location $WorkDir
try {
    if ($IncludeUntracked) { git add -N -A 2>$null | Out-Null }
    $diffFile = [IO.Path]::GetTempFileName()
    git -c core.quotepath=off diff $Base --unified=0 --no-color --no-ext-diff "--output=$diffFile" | Out-Null
    $status = git -c core.quotepath=off diff $Base --name-status --no-renames
} finally { Pop-Location }

# DG-08 deleted tests and protected files
foreach ($row in $status) {
    $parts = $row -split "`t"
    if ($parts.Count -lt 2) { continue }
    $code = $parts[0]; $path = $parts[1]
    if ($code -eq 'D' -and $path -match '/src/test/') { Add-Finding 'DG-08' $path 0 '(file deleted)' 'test apagado - proibido para ficar verde' }
    foreach ($p in $protected) {
        if ($path.StartsWith($p)) { Add-Finding 'DG-08' $path 0 '(file changed)' 'harness protegido: so o responsavel altera regras e baselines' }
    }
}

# Parse added lines
$added = New-Object System.Collections.Generic.List[object]
$file = $null; $lineNo = 0
foreach ($l in (Get-Content -LiteralPath $diffFile -Encoding UTF8)) {
    if ($l.StartsWith('+++ ')) { $file = if ($l -eq '+++ /dev/null') { $null } else { $l.Substring(6) }; continue }
    if ($l -match '^@@ -\d+(?:,\d+)? \+(\d+)') { $lineNo = [int]$Matches[1]; continue }
    if ($null -eq $file) { continue }
    if ($l.StartsWith('+') -and -not $l.StartsWith('+++')) {
        $added.Add([pscustomobject]@{ File = $file; Line = $lineNo; Text = $l.Substring(1) })
        $lineNo++
    }
}
Remove-Item $diffFile -ErrorAction SilentlyContinue

foreach ($a in $added) {
    $f = $a.File; $t = $a.Text
    if ($f -notlike '*.java') { continue }
    $isTest = $f -match '/src/test/'
    $isMain = $f -match '/src/main/'
    $isDesktop = $f -like 'desktop/src/main/*'
    $isBackendOrContracts = ($f -like 'backend/src/main/*') -or ($f -like 'contracts/src/main/*')
    $code = ($t -replace '//.*$', '')

    if ($isDesktop -and $code -match 'new\s+Color\s*\(' -and $f -notmatch $colourOwners) {
        Add-Finding 'DG-01' $f $a.Line $t 'cor fixa fora do tema: usar tokens do UIHelper / tema activo' }
    if ($isDesktop -and $t -match $emoji) {
        Add-Finding 'DG-02' $f $a.Line $t 'emoji/simbolo Unicode em Swing: usar UIHelper.icon(...)' }
    if ($isDesktop -and $code -match 'new\s+JComboBox\s*<') {
        Add-Finding 'DG-03' $f $a.Line $t 'combo cru: usar TableFilter.combo / UIHelper.createComboBox' }
    if ($isDesktop -and $code -match '\bJOptionPane\s*\.') {
        Add-Finding 'DG-03' $f $a.Line $t 'JOptionPane: usar os dialogos/feedback canonicos' }
    if ($isTest -and $code -match '@Disabled|assumeTrue\s*\(\s*false') {
        Add-Finding 'DG-04' $f $a.Line $t 'teste desactivado - proibido' }
    if ($isMain -and $code -match 'System\.(out|err)\.print|\.printStackTrace\s*\(') {
        Add-Finding 'DG-05' $f $a.Line $t 'usar logger (SLF4J), nunca System.out/printStackTrace' }
    if ($isMain -and $code -match 'BigDecimal\.ZERO\.equals\s*\(') {
        Add-Finding 'DG-06' $f $a.Line $t 'usar compareTo(BigDecimal.ZERO) == 0 ou signum()' }
    if ($isBackendOrContracts -and $code -match '\b(double|float|Double|Float)\s+\w+\s*[=;,)]') {
        Add-Finding 'DG-06' $f $a.Line $t 'dinheiro e quantidades em BigDecimal, nunca double/float' }
    if ($isMain -and $code -match '@Autowired|@Inject\b') {
        Add-Finding 'DG-07' $f $a.Line $t 'injeccao so por construtor, sem @Autowired' }
    if ($isBackendOrContracts -and $f -match '/controller/' -and $code -match '^\s*import .*\.repository\.') {
        Add-Finding 'DG-09' $f $a.Line $t 'Controller nao conhece Repository' }
}

# DG-10 DRY: the same block of 6+ meaningful added lines appearing twice.
$window = 6
$trivial = '^\s*$|^\s*[{}();]+\s*$|^\s*(import|package)\s|^\s*@\w+\s*$|^\s*(//|\*|/\*)|^\s*(return;|break;|else\s*\{?|try\s*\{|\}\s*catch.*)$|^\s*(public|private|protected)?\s*[\w<>\[\], ]+\s+\w+\s*\([^)]*\)\s*\{\s*$'
$seen = @{}
foreach ($grp in ($added | Where-Object { $_.File -like '*.java' -and $_.File -match '/src/main/' } | Group-Object File)) {
    $lines = @($grp.Group | Where-Object { $_.Text -notmatch $trivial })
    for ($i = 0; $i -le $lines.Count - $window; $i++) {
        $key = (($lines[$i..($i + $window - 1)] | ForEach-Object { ($_.Text.Trim() -replace '\s+', ' ') }) -join "`n")
        $loc = "$($grp.Name):$($lines[$i].Line)"
        if ($seen.ContainsKey($key) -and $seen[$key] -ne $loc) {
            Add-Finding 'DG-10' $grp.Name $lines[$i].Line $lines[$i].Text "bloco duplicado (tambem em $($seen[$key])): extrair para um metodo/classe partilhada"
        } elseif (-not $seen.ContainsKey($key)) { $seen[$key] = $loc }
    }
}

$unique = @($findings | Select-Object -Unique)
if ($unique.Count -eq 0) {
    Write-Output "DIFF-GUARD PASS ($($added.Count) linhas acrescentadas verificadas)"
    exit 0
}
Write-Output "DIFF-GUARD FAIL ($($unique.Count) violacoes nas linhas novas)"
$unique | ForEach-Object { Write-Output $_ }
exit 1
