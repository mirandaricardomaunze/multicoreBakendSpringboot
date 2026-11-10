# Goals — fila do Ralph Loop

Cada ficheiro `GOAL-NNN-<slug>.md` é um objectivo que a IA executa sozinha, um critério por iteração.
Regras completas em [docs/RALPH_LOOP_SPEC.md](../../docs/RALPH_LOOP_SPEC.md).

## Criar um goal

- No Claude Code: pedir "cria um goal para ..." (skill `multicore-goal`). A skill faz as perguntas de
  negócio **antes**, escreve a SPEC, o HARNESS e o goal.
- À mão: copiar [`_TEMPLATE.md`](_TEMPLATE.md) e validar com
  `mvn -q test -pl backend "-Dtest=RalphGoalsHarnessTest"`.

## Correr o loop

```powershell
# todos os goals executáveis, worktree isolada ..\manager-ralph (branch ralph/goals)
powershell -ExecutionPolicy Bypass -File scripts\ralph\ralph.ps1

# um goal só, no máximo 10 iterações
powershell -ExecutionPolicy Bypass -File scripts\ralph\ralph.ps1 -Goal GOAL-001 -MaxIterations 10

# só o gate (backpressure) sobre um goal
powershell -ExecutionPolicy Bypass -File scripts\ralph\gate.ps1 -GoalFile tasks\goals\GOAL-001-x.md
```

Parar com elegância: criar o ficheiro `.ralph\STOP` na raiz do repositório.
Acompanhar: `.ralph\progress.log` e `.ralph\logs\` dentro da worktree.

## Estados

`TODO` → `IN_PROGRESS` → `DONE`, ou `BLOCKED` quando há pergunta para o responsável.
Para desbloquear: responder em `## Perguntas em aberto`, actualizar a SPEC, voltar a `TODO` e limpar `blockedBy`.
