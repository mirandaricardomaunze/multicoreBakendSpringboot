# Ralph Loop — Execução Autónoma por Goals

Status: **canonical**
Effective date: 2026-11-09
Harness: `RalphGoalsHarnessTest` · scripts em [`scripts/ralph/`](../scripts/ralph/)

## 1. Propósito

Permitir que a IA trabalhe de forma contínua e sem supervisão sobre uma fila de **goals**, sem
abandonar o desenvolvimento orientado a especificações (SDD) nem a engenharia de harness do projecto.

O loop é deliberadamente simples ("Ralph"): o mesmo prompt corre repetidamente, cada iteração começa
**com o contexto limpo** e a memória entre iterações vive só em ficheiros versionados — goal, SPEC,
HARNESS, código e `git log`.

## 2. Camadas

| Camada | Fonte de verdade | Papel |
|---|---|---|
| Goal | `tasks/goals/GOAL-NNN-*.md` | O quê e quando está feito (critérios com ID) |
| Spec | `docs/<TEMA>_SPEC.md` | Regras de negócio aprovadas pelo responsável |
| Harness | `docs/<TEMA>_HARNESS.md` + testes | ID → teste que prova a regra |
| Gate | `scripts/ralph/gate.ps1` | Verificação independente da IA (backpressure) |
| Loop | `scripts/ralph/ralph.ps1` | Orquestra iterações, commits e paragens |
| Grafo | `tasks/graph.yaml` | Opcional: o goal pode fechar um nó (`graphNode`) |

## 3. Modelo do goal

Frontmatter YAML obrigatório:

- `id`: `GOAL-NNN`, igual ao prefixo do nome do ficheiro;
- `title`: resultado em PT-MZ;
- `status`: `TODO`, `IN_PROGRESS`, `BLOCKED` ou `DONE`;
- `priority`: `CRITICAL`, `HIGH`, `MEDIUM` ou `LOW`;
- `spec`: caminho para a SPEC (tem de existir);
- `harness`: caminho para o HARNESS (pode ser criado pela 1.ª iteração);
- `verify`: lista de classes de teste que provam o goal;
- `blockedBy`: obrigatório e não vazio só quando `BLOCKED`;
- `graphNode`: opcional, tem de existir em `tasks/graph.yaml`.

Corpo: secção `## Critérios de aceitação` com checkboxes `- [ ] ID — garantia — \`Teste\``.

## 4. Regras (validadas por `RalphGoalsHarnessTest`)

- RL-01: frontmatter válido, `id` único e igual ao prefixo do ficheiro.
- RL-02: `status` e `priority` dentro dos valores permitidos.
- RL-03: a `spec` existe no repositório.
- RL-04: `BLOCKED` declara `blockedBy`; os outros estados não.
- RL-05: há pelo menos um critério de aceitação com ID.
- RL-06: `DONE` tem todos os critérios marcados e todas as classes de `verify` existem.
- RL-07: no máximo um goal `IN_PROGRESS`.
- RL-08: `graphNode`, quando presente, existe no grafo de entrega.
- RL-09: todo o ID de critério do goal aparece na tabela do HARNESS (aceita linhas agrupadas como
  `SA-UI-05/06`). Assim nenhum critério existe sem regra rastreável até a um teste.
- RL-10: o HARNESS refere o nome do ficheiro da SPEC do goal.
- Um goal `TODO` pode ainda não ter o ficheiro de HARNESS (a 1.ª iteração escreve-o); a partir de
  `IN_PROGRESS` o HARNESS é obrigatório.

## 5. Uma iteração

1. Se o último gate falhou, a iteração só corrige essa falha.
2. Se a regra do próximo critério não está na SPEC, a iteração escreve a SPEC/HARNESS e termina
   (spec primeiro, código depois).
3. Caso contrário, pega **um** critério por marcar: teste primeiro (vermelho), implementação nas
   camadas correctas, teste verde.
4. Marca o critério, actualiza o HARNESS e acrescenta uma linha ao registo de progresso do goal.
5. Termina com `RALPH_SUMMARY:` e `RALPH_STATUS: CONTINUE | GOAL_DONE | NEEDS_HUMAN`.

## 6. Gate e paragens

- Depois de cada iteração o loop corre o gate **por conta própria**: compilação do reactor,
  `RalphGoalsHarnessTest`, `DeliveryGraphHarnessTest`, `MultiModuleArchitectureHarnessTest` e os
  testes de `verify`. Quando a IA declara `GOAL_DONE`, o gate corre a suite completa.
- Só estados com gate verde são commitados (na worktree isolada `ralph/goals`). Nunca há `push`.
- O loop pára quando: não há goals executáveis; atinge `-MaxIterations`; o gate falha
  `-MaxGateFailures` vezes seguidas; existe o ficheiro `.ralph/STOP`.
- `NEEDS_HUMAN`: dúvida de regra de negócio, fiscal ou legal. O goal passa a `BLOCKED` com a pergunta
  escrita em `## Perguntas em aberto`; o loop segue para o próximo goal.

## 7. Proibições na iteração

- Inventar regras fiscais, legais ou de negócio que a SPEC não diga.
- Apagar, desactivar (`@Disabled`) ou enfraquecer testes ou harness existentes para ficar verde.
- Alterar outro goal, fazer `git push`, `--no-verify` ou mexer em POMs sem o critério o exigir.
- Divergir de `AGENTS.md`, `ARCHITECTURE.md` ou `CONVENTIONS.md`.
