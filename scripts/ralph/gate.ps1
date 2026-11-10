<#
.SYNOPSIS
  Ralph gate (backpressure): verifies a goal independently of the AI. See docs/RALPH_LOOP_SPEC.md section 6.
.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\ralph\gate.ps1 -GoalFile tasks\goals\GOAL-001-x.md
  powershell -ExecutionPolicy Bypass -File scripts\ralph\gate.ps1 -GoalFile tasks\goals\GOAL-001-x.md -Full
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$GoalFile,
    [string]$WorkDir,
    [string]$OutFile,
    [switch]$Full,
    [switch]$Offline,
    [switch]$IncludeUntracked
)
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. "$here\common.ps1"
if (-not $WorkDir) { $WorkDir = (Resolve-Path "$here\..\..").Path }

if (-not [IO.Path]::IsPathRooted($GoalFile)) { $GoalFile = Join-Path $WorkDir $GoalFile }
$goal = Read-RalphGoal $GoalFile
$offlineFlag = if ($Offline) { '-o ' } else { '' }

# Mandatory quality profile on every iteration (docs/CLEAN_ARCHITECTURE_RULES_SPEC.md section 3).
$qualityTests = @(
    # loop and delivery
    'RalphGoalsHarnessTest', 'DeliveryGraphHarnessTest',
    # layers, modularity, coupling, SRP
    'MultiModuleArchitectureHarnessTest', 'EnterpriseModulesArchitectureHarnessTest', 'CleanArchitectureHarnessTest',
    # security and persistence boundaries
    'ApiRolePolicyEndpointCoverageHarnessTest', 'MigrationPostgresDialectHarnessTest',
    # UI uniformity: colours, icons, KPIs, contrast, panel pattern, thin desktop context
    'UiPrecisionHarnessTest', 'FinalUiUniformityHarnessTest', 'EnterpriseModulesUiUniformityHarnessTest',
    'IconSystemHarnessTest', 'KpiCardUniformityHarnessTest', 'ButtonIconContrastHarnessTest', 'DesktopThinContextTest'
)

if ($Full) {
    $cmdLine = "mvn ${offlineFlag}-q test"
} else {
    $tests = $qualityTests + $goal.Verify | Select-Object -Unique
    $cmdLine = "mvn ${offlineFlag}-q test `"-Dtest=$($tests -join ',')`" `"-Dsurefire.failIfNoSpecifiedTests=false`""
}

$started = Get-Date
Write-Host "[gate] $($goal.Id) :: diff-guard"
$guardText = & powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$here\diff-guard.ps1" -WorkDir $WorkDir -IncludeUntracked:$IncludeUntracked | Out-String
$guardExit = $LASTEXITCODE
Write-Host $guardText.Trim()

Write-Host "[gate] $($goal.Id) :: $cmdLine"
$result = Invoke-Native -CommandLine $cmdLine -WorkDir $WorkDir
$seconds = [int]((Get-Date) - $started).TotalSeconds
$exitCode = if ($result.ExitCode -ne 0) { $result.ExitCode } elseif ($guardExit -ne 0) { 1 } else { 0 }
$verdict = if ($exitCode -eq 0) { 'PASS' } else { 'FAIL' }

$report = "GATE $verdict ($($goal.Id), ${seconds}s, maven exit $($result.ExitCode), diff-guard exit $guardExit)`n`n$guardText`n$cmdLine`n`n$($result.Output)"
if ($OutFile) {
    New-Item -ItemType Directory -Force -Path (Split-Path $OutFile -Parent) | Out-Null
    Set-Content -LiteralPath $OutFile -Value $report -Encoding UTF8
}
Write-Host "[gate] $verdict in ${seconds}s"
exit $exitCode
