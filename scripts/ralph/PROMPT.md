# Iteração Ralph {{ITERATION}} — {{GOAL_ID}}

Estás a correr **sem supervisão** dentro do Ralph Loop do Multicore ERP. Não há humano para responder.
Cada iteração começa do zero: o que não ficar escrito em ficheiros perde-se.

Regras do loop: `docs/RALPH_LOOP_SPEC.md`. Regras do projecto: `CLAUDE.md` e `AGENTS.md` (não negociáveis).

## Contexto desta iteração

- Goal: `{{GOAL_FILE}}`
- Último gate: **{{GATE_STATUS}}**

```text
{{GATE_TAIL}}
```

## O que fazer (por esta ordem, e só UMA coisa)

1. Lê o goal, a SPEC e o HARNESS que ele aponta. Lê só o código de que precisas — não varras `docs/`.
2. **Se o último gate FALHOU**: a iteração serve só para corrigir essa falha. Nada de critérios novos.
3. **Se a regra do próximo critério não está na SPEC** (ou a SPEC/HARNESS não existem): escreve-as,
   acrescenta a linha do critério ao HARNESS e termina. Spec primeiro, código depois.
4. **Senão**, pega o **primeiro critério por marcar** e faz a fatia vertical mínima:
   - teste primeiro (deve falhar), depois a implementação nas camadas certas
     (Controller → Service → Repository, DTO record na fronteira, `BusinessRuleException`, PT-MZ ao utilizador);
   - migração Flyway só se precisares, com o próximo número livre e sintaxe PostgreSQL;
   - corre o teste alvo: `mvn -q test "-Dtest=NomeDoTeste" "-Dsurefire.failIfNoSpecifiedTests=false"`
     e, se mexeste em UI, também `DesktopThinContextTest`.
5. No goal: marca o critério `[x]`, põe `status: IN_PROGRESS` (ou `DONE` quando todos estiverem marcados),
   acrescenta ao `verify` as classes de teste novas e escreve uma linha em `## Registo de progresso`
   (`- AAAA-MM-DD · ID · o que ficou feito`). Actualiza a tabela do HARNESS.
6. **Auto-revisão antes de terminar**: aplica a skill `multicore-solid-review` ao teu próprio diff
   (`git diff HEAD`) e corrige o que encontrar: SRP, DRY, acoplamento entre módulos (usar o Service do
   módulo dono, nunca o Repository dele), camadas e UI uniforme (tokens do tema, `UIHelper`, `ModernPanel`,
   `KpiCard`). O gate verifica mecanicamente as regras de `docs/CLEAN_ARCHITECTURE_RULES_SPEC.md` (CA-01..08,
   DG-01..10 e harness de UI); uma violação deixa o gate vermelho e a iteração seguinte terá de a corrigir.
7. Se o goal ficou `DONE` e tem `graphNode`, passa esse nó de `tasks/graph.yaml` a `VERIFIED`
   com as classes de teste em `verification`.

## Quando parar e pedir ajuda (NEEDS_HUMAN)

Se a decisão é de negócio, fiscal, legal ou de UX e a SPEC não a responde: **não inventes**.
Escreve a pergunta (com as opções e a tua recomendação) em `## Perguntas em aberto`, põe
`status: BLOCKED` e `blockedBy: ["pergunta curta"]`, e termina com `NEEDS_HUMAN`.

## Proibido

- Apagar, `@Disabled` ou enfraquecer testes/harness existentes para ficar verde.
- Mexer noutros goals, `git commit`, `git push`, `--no-verify` (o loop faz o commit depois do gate).
- Emojis em Swing, `@Autowired` em campo, devolver `@Entity` para fora do Service.
- Trabalhar em mais de um critério.

## Saída obrigatória

As duas últimas linhas da tua resposta têm de ser exactamente:

```text
RALPH_SUMMARY: <uma frase em PT-MZ sobre o que ficou feito>
RALPH_STATUS: CONTINUE | GOAL_DONE | NEEDS_HUMAN
```
