# Shared helpers for the Ralph loop (docs/RALPH_LOOP_SPEC.md).
# Kept ASCII-only on purpose: Windows PowerShell 5.1 reads BOM-less scripts as ANSI.

$script:PriorityOrder = @{ 'CRITICAL' = 0; 'HIGH' = 1; 'MEDIUM' = 2; 'LOW' = 3 }

function Read-RalphGoal {
    param([Parameter(Mandatory)][string]$Path)
    $lines = Get-Content -LiteralPath $Path -Encoding UTF8
    $front = @{}
    if ($lines.Count -gt 0 -and $lines[0].Trim() -eq '---') {
        for ($i = 1; $i -lt $lines.Count; $i++) {
            if ($lines[$i].Trim() -eq '---') { break }
            if ($lines[$i] -match '^([A-Za-z]+):\s*(.*)$') { $front[$Matches[1]] = $Matches[2].Trim() }
        }
    }
    $verify = @()
    if ($front['verify'] -match '^\[(.*)\]$') {
        $verify = $Matches[1].Split(',') | ForEach-Object { $_.Trim().Trim('"', "'") } | Where-Object { $_ }
    }
    $open = @($lines | Where-Object { $_ -match '^\s*- \[ \] ' }).Count
    $closed = @($lines | Where-Object { $_ -match '^\s*- \[[xX]\] ' }).Count
    [pscustomobject]@{
        Path     = (Resolve-Path -LiteralPath $Path).Path
        Id       = $front['id']
        Title    = ($front['title'] -replace '^"|"$', '')
        Status   = $front['status']
        Priority = $front['priority']
        Spec     = $front['spec']
        Verify   = @($verify)
        Open     = $open
        Closed   = $closed
    }
}

function Get-RalphGoals {
    param([Parameter(Mandatory)][string]$Root)
    $dir = Join-Path $Root 'tasks\goals'
    if (-not (Test-Path $dir)) { return @() }
    Get-ChildItem -LiteralPath $dir -Filter 'GOAL-*.md' | ForEach-Object { Read-RalphGoal $_.FullName }
}

function Select-NextGoal {
    param([Parameter(Mandatory)][string]$Root, [string]$GoalId)
    $goals = @(Get-RalphGoals $Root)
    if ($GoalId) { return $goals | Where-Object { $_.Id -eq $GoalId } | Select-Object -First 1 }
    $active = $goals | Where-Object { $_.Status -eq 'IN_PROGRESS' } | Select-Object -First 1
    if ($active) { return $active }
    $goals | Where-Object { $_.Status -eq 'TODO' } |
        Sort-Object @{ e = { $p = $script:PriorityOrder[$_.Priority]; if ($null -eq $p) { 9 } else { $p } } }, Id |
        Select-Object -First 1
}

function Find-ClaudeExe {
    param([string]$Explicit)
    if ($Explicit) { return $Explicit }
    $cmd = Get-Command claude -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        (Join-Path $env:USERPROFILE '.local\bin\claude.exe'),
        (Join-Path $env:APPDATA 'npm\claude.cmd')
    ) | Where-Object { Test-Path $_ }
    if ($candidates) { return $candidates[0] }
    $ext = Get-ChildItem (Join-Path $env:USERPROFILE '.vscode\extensions') -Directory -Filter 'anthropic.claude-code-*' -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        ForEach-Object { Join-Path $_.FullName 'resources\native-binary\claude.exe' } |
        Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($ext) { return $ext }
    throw 'Claude CLI not found. Pass -ClaudeExe <path> or install it (npm i -g @anthropic-ai/claude-code).'
}

function Invoke-Native {
    # Runs a native command through cmd so stderr is captured as plain text on PowerShell 5.1.
    param([Parameter(Mandatory)][string]$CommandLine, [Parameter(Mandatory)][string]$WorkDir)
    Push-Location $WorkDir
    try {
        $text = cmd /c "$CommandLine 2>&1" | Out-String
        [pscustomobject]@{ ExitCode = $LASTEXITCODE; Output = $text }
    } finally { Pop-Location }
}
