<#
.SYNOPSIS
  Ralph loop: runs the AI repeatedly over tasks/goals, one acceptance criterion per iteration,
  with an independent gate after each iteration. Canonical rules: docs/RALPH_LOOP_SPEC.md.

.DESCRIPTION
  Default mode works in an isolated git worktree (..\<repo>-ralph, branch ralph/goals) so it never
  touches files of other sessions in the main checkout. Gate-green states are committed there.
  Nothing is ever pushed. Stop gracefully by creating .ralph\STOP in the main repository.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\ralph\ralph.ps1
  powershell -ExecutionPolicy Bypass -File scripts\ralph\ralph.ps1 -Goal GOAL-001 -MaxIterations 10
  powershell -ExecutionPolicy Bypass -File scripts\ralph\ralph.ps1 -DryRun
#>
[CmdletBinding()]
param(
    [string]$Goal,
    [int]$MaxIterations = 20,
    [int]$MaxGateFailures = 3,
    [switch]$NoWorktree,
    [switch]$SkipPermissions,
    [switch]$Offline,
    [string]$Model,
    [string]$ClaudeExe,
    [switch]$DryRun
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"

$repo = (Resolve-Path "$PSScriptRoot\..\..").Path
$branch = 'ralph/goals'
$claude = Find-ClaudeExe $ClaudeExe
$coAuthor = 'Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>'

# Tools the unattended agent may use without a prompt. No push, no reset, no rm.
$allowedTools = @(
    'Read', 'Edit', 'Write', 'Glob', 'Grep', 'TodoWrite',
    'Bash(mvn:*)', 'Bash(git status:*)', 'Bash(git diff:*)', 'Bash(git log:*)', 'Bash(git show:*)',
    'Bash(ls:*)', 'Bash(cat:*)', 'Bash(head:*)', 'Bash(tail:*)', 'Bash(grep:*)', 'Bash(find:*)',
    'Bash(wc:*)', 'Bash(sed -n:*)', 'Bash(mkdir:*)', 'Bash(cd:*)'
)

# ---------- workspace ----------
function Initialize-Worktree {
    $work = Join-Path (Split-Path $repo -Parent) ((Split-Path $repo -Leaf) + '-ralph')
    if (-not (Test-Path $work)) {
        git -C $repo show-ref --verify --quiet "refs/heads/$branch"
        if ($LASTEXITCODE -eq 0) { git -C $repo worktree add $work $branch | Out-Host }
        else { git -C $repo worktree add -b $branch $work HEAD | Out-Host }
        if ($LASTEXITCODE -ne 0) { throw "git worktree add failed for $work" }
    }
    # .claude/ is gitignored: carry skills and project settings into the worktree.
    $srcClaude = Join-Path $repo '.claude'
    if (Test-Path $srcClaude) {
        $dst = Join-Path $work '.claude'
        New-Item -ItemType Directory -Force -Path $dst | Out-Null
        Copy-Item (Join-Path $srcClaude 'skills') $dst -Recurse -Force -ErrorAction SilentlyContinue
        Copy-Item (Join-Path $srcClaude 'settings.json') $dst -Force -ErrorAction SilentlyContinue
    }
    $synced = New-Object System.Collections.Generic.List[string]
    # Goals/specs/baselines created in the main checkout: copy only when missing, never overwrite
    # progress the loop already made in the worktree (e.g. baseline lines it has already paid off).
    $copyIfMissing = @('backend\src\test\resources\architecture\cross-module-repository-baseline.txt')
    foreach ($g in Get-RalphGoals $repo) {
        $rel = $g.Path.Substring($repo.Length + 1)
        $copyIfMissing += @($rel, $g.Spec) + @((Get-Content $g.Path -Encoding UTF8 | Where-Object { $_ -match '^harness:\s*(\S+)' } | ForEach-Object { $Matches[1] }))
    }
    foreach ($f in $copyIfMissing) {
        if (-not $f) { continue }
        $src = Join-Path $repo $f; $dst = Join-Path $work $f
        if ((Test-Path $src) -and -not (Test-Path $dst)) {
            New-Item -ItemType Directory -Force -Path (Split-Path $dst -Parent) | Out-Null
            Copy-Item $src $dst
            $synced.Add($f)
            Write-Host "[ralph] copied into worktree: $f"
        }
    }
    # The harness judges the loop, so the main checkout is its source of truth: always refresh it.
    $harness = @(
        'docs\RALPH_LOOP_SPEC.md', 'docs\CLEAN_ARCHITECTURE_RULES_SPEC.md',
        'tasks\goals\README.md', 'tasks\goals\_TEMPLATE.md', 'CLAUDE.md',
        'scripts\ralph\common.ps1', 'scripts\ralph\gate.ps1', 'scripts\ralph\diff-guard.ps1', 'scripts\ralph\PROMPT.md',
        'backend\src\test\java\mz\multicore\erp\architecture\RalphGoalsHarnessTest.java',
        'backend\src\test\java\mz\multicore\erp\architecture\CleanArchitectureHarnessTest.java',
        'backend\src\test\java\mz\multicore\erp\architecture\ModuleDecouplingHarnessTest.java',
        'backend\src\test\java\mz\multicore\erp\architecture\CrossModuleRepositoryScanner.java'
    )
    foreach ($f in $harness) {
        $src = Join-Path $repo $f; $dst = Join-Path $work $f
        if (Test-Path $src) {
            New-Item -ItemType Directory -Force -Path (Split-Path $dst -Parent) | Out-Null
            Copy-Item $src $dst -Force
            $synced.Add($f)
        }
    }
    # Commit ONLY the synced files, so pending (not yet gated) iteration work is never swept into
    # the bootstrap commit, and the diff guard only judges what the iterations add.
    $paths = @($synced | ForEach-Object { $_ -replace '\\', '/' } | Select-Object -Unique)
    $changed = @(git -C $work status --porcelain -- $paths)
    if ($changed.Count -gt 0) {
        git -C $work add -- $paths
        git -C $work commit -q -m "chore(ralph): sincroniza goals, specs e harness do checkout principal`n`n$coAuthor" -- $paths
        Write-Host "[ralph] bootstrap commit: $($changed.Count) harness/goal file(s) from the main checkout"
    }
    return $work
}

$work = if ($NoWorktree) { $repo } else { Initialize-Worktree }
$state = Join-Path $work '.ralph'
New-Item -ItemType Directory -Force -Path (Join-Path $state 'logs') | Out-Null
$stopFiles = @((Join-Path $repo '.ralph\STOP'), (Join-Path $state 'STOP'))
$progressLog = Join-Path $state 'progress.log'
$gateFile = Join-Path $state 'last-gate.txt'
$template = Get-Content (Join-Path $PSScriptRoot 'PROMPT.md') -Raw -Encoding UTF8

Write-Host "[ralph] claude : $claude"
Write-Host "[ralph] work   : $work $(if (-not $NoWorktree) { "(branch $branch)" })"
if ($NoWorktree) { Write-Warning 'NoWorktree: no automatic commits (shared checkout). Review and commit by hand.' }

function Write-Progress-Line([string]$line) {
    $stamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'
    Add-Content -LiteralPath $progressLog -Value "$stamp  $line" -Encoding UTF8
    Write-Host "[ralph] $line"
}

function Get-GateTail {
    if (-not (Test-Path $gateFile)) { return 'Sem gate anterior.' }
    $lines = Get-Content $gateFile -Encoding UTF8
    $interesting = $lines | Where-Object { $_ -match 'ERROR|FAIL|Tests run:.*Fail|expected|cannot find symbol|Caused by' } | Select-Object -First 40
    (@($lines | Select-Object -First 2) + '...' + $interesting + '...' + ($lines | Select-Object -Last 25)) -join "`n"
}

# ---------- loop ----------
$gateFailures = 0
$lastGate = 'NONE'
for ($i = 1; $i -le $MaxIterations; $i++) {
    if ($stopFiles | Where-Object { Test-Path $_ }) { Write-Progress-Line 'STOP file found, stopping.'; break }

    $g = Select-NextGoal -Root $work -GoalId $Goal
    if (-not $g) { Write-Progress-Line 'No executable goals (TODO/IN_PROGRESS). Done.'; break }
    if ($g.Status -notin @('TODO', 'IN_PROGRESS')) { Write-Progress-Line "$($g.Id) is $($g.Status). Stopping."; break }

    $relGoal = $g.Path.Substring($work.Length + 1) -replace '\\', '/'
    $gateStatus = switch ($lastGate) { 'PASS' { 'PASSOU' } 'FAIL' { 'FALHOU - corrigir primeiro' } default { 'nenhum nesta sessao' } }
    $prompt = $template.Replace('{{ITERATION}}', "$i").Replace('{{GOAL_ID}}', $g.Id).Replace('{{GOAL_FILE}}', $relGoal).
        Replace('{{GATE_STATUS}}', $gateStatus).Replace('{{GATE_TAIL}}', $(if ($lastGate -eq 'FAIL') { Get-GateTail } else { '-' }))
    $promptFile = Join-Path $state 'iteration-prompt.md'
    Set-Content -LiteralPath $promptFile -Value $prompt -Encoding UTF8

    $cliArgs = @('-p', 'Executa a iteracao Ralph descrita em .ralph/iteration-prompt.md. Le esse ficheiro primeiro e segue-o a risca.', '--output-format', 'text')
    if ($Model) { $cliArgs += @('--model', $Model) }
    if ($SkipPermissions) { $cliArgs += '--dangerously-skip-permissions' }
    else { $cliArgs += @('--permission-mode', 'acceptEdits', '--allowedTools') + $allowedTools }

    Write-Progress-Line "iter $i :: $($g.Id) [$($g.Closed)/$($g.Closed + $g.Open)] $($g.Title)"
    if ($DryRun) { Write-Host "[ralph] DRY RUN: would call claude with prompt $promptFile"; break }

    $started = Get-Date
    Push-Location $work
    try { $out = & $claude @cliArgs | Out-String } finally { Pop-Location }
    $mins = [math]::Round(((Get-Date) - $started).TotalMinutes, 1)
    $logFile = Join-Path $state ("logs\iter-{0:D3}-{1}.md" -f $i, $g.Id)
    Set-Content -LiteralPath $logFile -Value $out -Encoding UTF8

    $status = if ($out -match 'RALPH_STATUS:\s*(CONTINUE|GOAL_DONE|NEEDS_HUMAN)') { $Matches[1] } else { 'UNKNOWN' }
    $summary = if ($out -match 'RALPH_SUMMARY:\s*(.+)') { $Matches[1].Trim() } else { '(sem resumo)' }

    $gateArgs = @{ GoalFile = $g.Path; WorkDir = $work; OutFile = $gateFile; Offline = $Offline; Full = ($status -eq 'GOAL_DONE'); IncludeUntracked = (-not $NoWorktree) }
    & "$PSScriptRoot\gate.ps1" @gateArgs
    $lastGate = if ($LASTEXITCODE -eq 0) { 'PASS' } else { 'FAIL' }

    $commit = '-'
    if ($lastGate -eq 'PASS') {
        $gateFailures = 0
        if (-not $NoWorktree) {
            git -C $work add -A
            git -C $work diff --cached --quiet
            if ($LASTEXITCODE -ne 0) {
                $msgFile = Join-Path $state 'commit-msg.txt'
                Set-Content -LiteralPath $msgFile -Encoding UTF8 -Value "ralph($($g.Id)): $summary`n`nIteracao $i do Ralph loop. Gate: PASS.`n`n$coAuthor"
                git -C $work commit -q -F $msgFile
                $commit = (git -C $work rev-parse --short HEAD).Trim()
            }
        }
    } else {
        $gateFailures++
    }
    Write-Progress-Line "iter $i :: $($g.Id) agent=$status gate=$lastGate commit=$commit ${mins}min :: $summary"

    if ($gateFailures -ge $MaxGateFailures) { Write-Progress-Line "Gate failed $gateFailures times in a row. Stopping for human review."; break }
    if ($status -eq 'NEEDS_HUMAN' -and $Goal) { Write-Progress-Line "$($g.Id) needs a human decision. Stopping."; break }
    if ($status -eq 'GOAL_DONE' -and $lastGate -eq 'PASS' -and $Goal) { Write-Progress-Line "$($g.Id) DONE."; break }
}
Write-Host "[ralph] finished. Log: $progressLog"
