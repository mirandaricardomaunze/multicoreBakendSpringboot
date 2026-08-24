# Harness — RH completo (contrato, ponto, cessação, retenções)

Mede o progresso contra [RH_COMPLETO_SPEC.md](RH_COMPLETO_SPEC.md). Não repete o que já é medido em
[HR_PAYROLL_HARNESS.md](HR_PAYROLL_HARNESS.md) (RH-01..25) — aqui só entra o que **falta**.

Cada cenário corre com **empresa activa, utilizador identificado e dados controlados**.
Legenda: ✅ feito · 🟡 parcial · ❌ em falta · 🔴 **defeito confirmado no código actual**.

**Data inicial:** 2026-08-22.

> **Estado a 2026-08-24: todos os blocos fechados (B1..B8).** Os sete 🔴 que a auditoria de 22/08
> levantou estão fechados, cada um com teste que carrega a regra. As tabelas abaixo preservam o
> diagnóstico original e a evolução incremental. Suite completa: **860 testes, 0 falhas, 0 erros,
> 0 ignorados**, e as migrações **já correram contra PostgreSQL real** (v57, com
> `ddl-auto=validate`). O que fica por fazer está em "Declarações honestas", no fim: validação ao
> vivo pela UI (RHC-90..94) e decisões que não são da IA — não código.

---

## Dados base

Reutilizar os de [HR_PAYROLL_HARNESS.md](HR_PAYROLL_HARNESS.md) e acrescentar:

- `João Mucavel` — contrato **sem termo** desde 2024-01-15, salário 25.000,00, 2 dependentes.
- `Ana Sitoe` — contrato **a termo certo** de 2026-03-01 a **2026-07-31** (já expirado hoje).
- `Carlos Tembe` — `TERMINATED`, sem acerto final feito.
- `Rita Nhaca` — contrato com **período experimental** a terminar dentro de 5 dias.
- Utilizador `colaborador` (EMPLOYEE) ligado a `João Mucavel`.

---

## B7.1 — Guardas em falta *(**fechado em 2026-08-22**)*

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-01 | EMPLOYEE chama `POST /api/hr/absences` com o `employeeId` **de outro colaborador** | Recusado por falta de perfil | ✅ `ensureHrManager` em `recordAbsence`; `HRServiceTest.recordAbsence_employeeRole_isBlocked` (**confirmado a falhar** contra o código antigo) |
| RHC-02 | EMPLOYEE chama `DELETE /api/hr/absences/{id}` de uma falta que não é sua | Recusado | ✅ guarda + teste com a falta existente e stubbed (sem a guarda, eliminava) |
| RHC-03 | EMPLOYEE submete férias em nome de outro colaborador | Recusado; o colaborador vem do utilizador autenticado | ✅ **B7.2** — `ensureCanActFor`: gestor age por qualquer um, os restantes só por si. Conta sem colaborador associado é recusada com o que fazer a seguir ("Peça ao RH…") |
| RHC-04 | EMPLOYEE submete **despesa** em nome de um colega | Recusado; a Engine de Aprovações recebe o pedinte real | ✅ **B7.2** — recusado; submeter em nome próprio continua a funcionar e fica auditado (`EXPENSE_SUBMIT` nomeia o colaborador **e** quem submeteu) |
| RHC-05 | Lançar e eliminar falta como gestor | Executa e **fica auditado** (`ABSENCE_CREATE`/`ABSENCE_DELETE`) | ✅ dois testes, ambos confirmados a falhar contra o código antigo (fecha **RH-21** do outro harness) |
| RHC-06 | Colaborador consulta **o seu** recibo | Vê só os seus; os dos colegas dão 403 | ✅ **B7.2** — `getAllPayslips` filtra para quem não é gestor **e** `loadPayslipForPrint` aplica a mesma regra (filtrar a lista e deixar imprimir por id era meia porta) |

## B7.2 — Ligação `Employee ↔ AppUser` *(**fechado em 2026-08-22**, migração V48)*

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-07 | Associar conta inexistente ao colaborador | Recusado nomeando o utilizador | ✅ |
| RHC-08 | Associar conta **sem acesso à empresa activa** | Recusado | ✅ teste |
| RHC-09 | Associar conta **já ligada a outro colaborador** da empresa | Recusado (espelha o índice único da V48) | ✅ teste |
| RHC-10a | Campo em branco no cadastro | Desliga a ligação; o colaborador deixa de fazer self-service | ✅ |

**Regra central:** *um gestor age por qualquer colaborador; toda a gente age por si própria e por
mais ninguém.* Até à V48, "o próprio" não era identificável, pelo que agir por outro era
indistinguível de agir por si — e a única defesa possível (exigir MANAGER em tudo) matava o
self-service.

## B1 — Contrato

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-10 | Criar contrato sem termo, número gapless por empresa | Série própria (`CTR`), auditado | ✅ nasce **`RASCUNHO`**, não `VIGENTE` — ver nota abaixo |
| RHC-11 | Criar 2.º contrato vigente sobreposto ao primeiro | Bloqueio em PT nomeando o contrato em vigor | ✅ teste (`ensureNoOverlap`, verificado **na activação**) |
| RHC-12 | Contrato **a termo** sem motivo | Bloqueio (motivo obrigatório no contrato a termo) | ✅ teste |
| RHC-13 | Contrato com fim anterior ao início | Bloqueio | ✅ (+ termo sem fim, e sem-termo *com* fim) |
| RHC-14 | Ler contrato da `Ana Sitoe` depois de 2026-07-31 | `expirado=true` **derivado**, sem nada gravado (molde `Quotation.isExpired`) | ✅ teste: coluna continua `VIGENTE`, DTO sai `expired=true` |
| RHC-15 | **Processar folha de Agosto com a `Ana Sitoe` de contrato expirado** | **Não gera recibo** e diz porquê | ✅ **fechado** — `processMonthlyPayroll` devolve `PayrollRunDTO(gerados, saltados)` e a caixa do painel nomeia quem ficou de fora |
| RHC-16 | Renovar contrato a termo | Nasce contrato novo ligado ao anterior; o anterior **não muda** | ✅ 3 testes; o anterior fecha na **véspera** do novo (nunca dois vigentes no mesmo dia) |
| RHC-17 | Notificações com contrato a expirar ≤30 dias e experimental ≤7 dias | Ambos no `NotificationFeed` | ✅ **B1.2** — `/api/hr/contracts/alerts` devolve as duas listas numa só ida (o sino já faz 4 chamadas por refresh); 2 testes no `NotificationFeedTest` |
| RHC-18 | Imprimir contrato | PDF com empresa, colaborador, cláusulas e assinaturas | ✅ **B1.2** — `EmploymentContractPrintService`, 5 testes que lêem o **texto do PDF** (inclui: o papel mostra o salário do contrato, não o da ficha) |
| RHC-19 | Salário do recibo vem do **contrato vigente**, não da ficha | Coincidem; alterar a ficha à mão não é possível | ✅ **fechado pelo B4** — a activação regista uma alteração salarial (motivo `CONTRATO`) e `updateEmployee` **recusa** um salário divergente, dizendo onde se faz |

**Divergência assumida no RHC-10.** A linha dizia "nasce `VIGENTE`", mas isso contradiz a máquina de
estados da própria spec (`RASCUNHO → VIGENTE → CESSADO`) e não deixa onde pôr a verificação de
sobreposição: se o contrato já nasce a vigorar, ou se valida a sobreposição na criação — e aí não há
como preparar um contrato com antecedência — ou não se valida de todo. Por isso o contrato nasce
**`RASCUNHO`** e é a **activação** que verifica a invariante e escreve o salário na ficha. Um
rascunho não manda em nada: a folha só olha para vigentes.

**B1.2 fechado (2026-08-23).** PDF, alertas e separador de contratos no desktop.

- O PDF **não usa** o `LineItemsTableRenderer`/`TotalsBlockRenderer`: um contrato não tem linhas nem
  totais, tem cláusulas. Compõe o `CompanyHeaderRenderer` e o `SignatureBlockRenderer`. A spec
  mencionava também o `CommercialTermsRenderer` — **não se aplica**: esse renderiza condições de
  pagamento e entrega, que um contrato de trabalho não tem.
- As cláusulas são **geradas do que está gravado**, não texto fixo, e numeram-se sozinhas conforme
  se aplicam (sem termo não tem cláusula de justificação do termo).
- O separador nasceu em `HRContractsPanel`, molde do `HRExpensesPanel`. O `HRPanel` ficou em
  **998/1000** — **a próxima adição ao RH tem de extrair um separador primeiro**, não cabe mais nada.

## B2 — Ponto e assiduidade

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-20 | Registar marcação entrada/saída/pausa | Gravada com origem e autor | ✅ **B2.1** — `TimeEntry` com `source` (MANUAL/IMPORTADO/TERMINAL) e `recorded_by`; auditado |
| RHC-21 | Duas marcações para o mesmo colaborador/dia/turno | Bloqueio anti-duplicação | ✅ **B2.1** por colaborador+data (índice único da V50). **Sem modelo de turnos** nesta iteração — quando entrarem, o índice muda |
| RHC-22 | Saída antes da entrada | Bloqueio | ✅ **divergência assumida**: saída "anterior" é tratada como **turno que atravessa a meia-noite** (22:00→06:00 = 8h). Bloquear perdia o turno da noite inteiro. Bloqueado é o caso real: pausa ≥ turno, e entrada = saída |
| RHC-23 | Folha de ponto mensal | Previstos, trabalhados, normais, extra por escalão, atrasos, faltas | ✅ **B2.1** — `getMonthlySheet`; totais **nunca gravados**, apuram-se sempre das marcações |
| RHC-24 | Dia previsto **sem marcação** | Nasce falta `POR_JUSTIFICAR` automaticamente | ✅ **B2.2** — gerada no **fecho** do mês, idempotente, nunca em dia de descanso. Nasce `PENDING_JUSTIFICATION` e **não desconta** até alguém decidir (3 testes) |
| RHC-25 | Justificar essa falta com motivo e documento | Muda de tipo, auditado; deixa de descontar se for remunerada | ✅ **B2.2** — `justifyAbsence`, motivo obrigatório, `ABSENCE_JUSTIFY` auditado (2 testes) |
| RHC-26 | **Recibo lê horas extra da folha de ponto fechada** | Valor do recibo = valor apurado no ponto | ✅ **B2.2 fechado** — com o ponto fechado o valor vem apurado das marcações. **Sem ponto fechado o comportamento é o de sempre**, para não obrigar uma loja que não usa o módulo a usá-lo |
| RHC-27 | Processar folha com o ponto do mês **por fechar** | Bloqueio nomeando o mês | ✅ **B2.2** — **refinamento assumido:** só bloqueia se houver ponto marcado nesse mês; sem marcações não há fecho que faça sentido exigir (2 testes) |
| RHC-28 | Alterar marcação em mês já fechado | Bloqueio; reabertura só de gestor e auditada | ✅ **B2.1** — 4 testes (marcar, eliminar, reabrir sem motivo, reabrir auditado) |
| RHC-29 | Hora extra em dia de descanso vs. dia normal | Multiplicadores distintos, **configuráveis** (§6 da spec) | ✅ **B2.2** — `OvertimeRateConfig` por empresa, com vigência e **base legal registada** (molde do `PayrollTaxConfig`). **Sem valores por omissão:** sem configuração o recibo recusa-se a valorizar e diz que os valores têm de vir do contabilista |
| RHC-30 | Valor manual de hora extra por excepção | Exige justificação e fica auditado | ✅ **B2.2** — divergir do apurado exige justificação e grava `PAYSLIP_OVERTIME_OVERRIDE`. A porta que era a regra passou a ser a excepção declarada (2 testes) |

**B2.1 fechado (2026-08-23), migração V50.** A fundação: `WorkSchedule`, `TimeEntry`, `TimeSheet`.

- **Nada de multiplicadores inventados.** A spec (§6) diz que os acréscimos legais têm de ser
  confirmados com o contabilista da empresa. O B2.1 **conta e classifica** as horas; o B2.2
  atribui-lhes valor. Escrever uma percentagem à sorte era o pior resultado possível: pareceria
  certo e pagaria mal.
- **A janela nocturna é dado, não constante** (`work_schedules.night_start/night_end`), pela mesma
  razão. E a hora de entrada prevista existe porque sem ela o atraso é incalculável.
- **Escalões separados, nunca somados:** extra em dia normal (diurna), extra em janela nocturna, e
  horas em dia de descanso — que contam **todas** como extraordinárias, nenhuma como normal.
- Os totais **nunca são gravados**: apuram-se das marcações. Gravá-los criaria uma segunda verdade
  que se desactualiza à primeira correcção — a mesma lição da caducidade do contrato.
- **Desktop:** separador "Ponto" (`HRTimeSheetPanel`) com apuramento, registo de marcação e
  fechar/reabrir mês. Para caber, o separador de Férias foi extraído para `HRVacationsPanel` — o
  `HRPanel` desceu de **998 para 865** linhas.

**B2.2 fechado (2026-08-23), migração V51.** O recibo passou a ler do ponto.

- **Os multiplicadores continuam por decidir — de propósito.** `OvertimeRateConfig` é configurável
  por empresa, com vigência e `legal_basis`, **sem valores por omissão**. Sem configuração em vigor
  o sistema **recusa-se a valorizar horas extra** e diz que os valores têm de ser confirmados com o
  contabilista. É a única coisa deste bloco que não é minha para decidir, e falha em voz alta em
  vez de adivinhar.
- **A hora normal sai do salário a dividir pelas horas previstas *nesse mês*** — um mês de 22 dias
  úteis e outro de 20 não valem a hora ao mesmo preço.
- **Duas portas ficaram abertas de propósito**, ambas testadas: sem ponto fechado o recibo mantém o
  valor manual (uma loja que não usa o módulo continua a emitir recibos), e o valor manual
  divergente continua possível — mas só com justificação e auditoria.

## B3 — Cessação e acerto final

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-35 | Cessar contrato com motivo e data | Contrato `CESSADO`, colaborador `TERMINATED`, auditado | ✅ **B3** — `Termination` com motivo tipado, série própria `AF`, `TERMINATION_CREATE` auditado. Cessar duas vezes é recusado |
| RHC-36 | Acerto final de quem sai a meio do ano | Linhas: salário do mês, 13.º proporcional, férias não gozadas, saldos | ✅ **B3** — 4 testes. Salário proporcional aos dias, 13.º aos meses, férias por gozar ao dia, aviso prévio **só quando era o trabalhador a devê-lo** |
| RHC-37 | Acerto de quem tem **empréstimo por liquidar** (B6) | Saldo em dívida abatido no acerto | ✅ **B3** — o saldo do B6 entra como desconto; **o líquido pode ficar negativo** e isso aparece em vez de virar zero |
| RHC-38 | Pagar o acerto | Saída de tesouraria pela mesma porta do recibo; auditado | ✅ **B3** — `registerAutoPayout`, `TERMINATION_PAID`, pagar duas vezes recusado. Um acerto negativo **não se paga** |
| RHC-39 | Emitir recibo a colaborador cessado | Bloqueio | ✅ **B3** — cessar põe o colaborador `TERMINATED`, e `findActiveEmployee` já bloqueava recibos e férias. O que faltava era a cessação chegar lá |
| RHC-40 | Certificado de trabalho | PDF | ✅ **B3** — `TerminationPrintService.renderCertificate`. Deliberadamente **seco**: função, datas e motivo. Um certificado que opina sobre o desempenho deixa de ser documento |

**B3 fechado (2026-08-24), migração V55.** A saída passou a ser um documento.

- **O que substitui é uma String:** `changeEmployeeStatus(id, "TERMINATED")` e mais nada. O 13.º
  proporcional e o saldo de férias — que o sistema **já sabia calcular** — nunca eram calculados
  nesta situação. A conta era feita à mão, em papel, ou não era feita.
- **O que não sabe calcular, diz.** O direito a férias por antiguidade e o aviso prévio são
  configuráveis (§6); sem configuração essas linhas não entram e o acerto sai com um **aviso em
  PT-MZ** a dizer o que falta. Um acerto que esconde o que não sabe calcular é pior do que um
  acerto incompleto que o declara.
- **As linhas do acerto são gravadas**, ao contrário dos totais do ponto, e de propósito: um acerto
  é um **documento**. O que foi pago naquele dia não pode mudar porque o direito a férias da empresa
  mudou no ano seguinte — mesma razão pela qual a `InvoiceLine` fotografa o custo (V37).
- **Cessar mostra a conta primeiro** (`/preview`): é irreversível, e obrigar a cessar para ver os
  números seria pedir para descobrir o erro tarde de mais.

## B4 — Histórico salarial

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-45 | Registar aumento com data de efeito e motivo | Gravado; salário anterior **preservado**; auditado | ✅ **B4** — `SalaryChange` com anterior, novo, data de efeito, motivo e autor; `SALARY_CHANGE` auditado. A ficha **recusa** alterar o salário e diz onde se faz |
| RHC-46 | **Emitir recibo de Março depois de um aumento em Junho** | Usa o salário **de Março** | ✅ **B4 fechado** — `salaryForPeriod` usa o vigente no **último dia do período**. Sem histórico (colaboradores anteriores ao bloco) mantém o valor da ficha |
| RHC-47 | Evolução salarial do colaborador | Série por data, sem buracos | ✅ **fechado a 2026-08-24** — ecrã em `HREmployeeActions.openSalaryHistory` (gráfico `SimpleBarChart` + tabela + registar alteração). **Diálogo por colaborador, não separador**: a série pertence a uma pessoa, e a barra do RH já não tinha folga |

**B4 fechado (2026-08-23), migração V52.** O salário passou a ser uma série datada.

- **O defeito sério não era o histórico perdido** — era o **recibo de Março passar a pagar ao valor
  de Setembro** quando alguém o reprocessasse, sem nada parecer errado porque o número era normal.
  É o mesmo defeito que a V37 corrigiu na margem histórica do comercial.
- **A ficha deixou de ser a porta.** `updateEmployee` recusa um salário divergente e diz onde se
  faz. `employees.base_salary` continua a existir mas passa a ser **reflexo** da série, não a origem.
- **Uma alteração com data futura não mexe na ficha** até lá chegar: é um compromisso, não um facto.
- **A activação de contrato regista uma alteração** (motivo `CONTRATO`) em vez de escrever na ficha
  — senão a série ficava com buracos exactamente nos momentos que mais interessam.

## B5 — Retenções por entregar e contabilidade

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-50 | Pagar a folha do mês | Nascem obrigações `IRPS`, `INSS_TRABALHADOR`, `INSS_PATRONAL` `POR_ENTREGAR` | ✅ **B5 fechado** — `accrueForPeriod` no `markPayslipPaid`. **Reapura em vez de somar**, pelo que pagar o mesmo recibo duas vezes não duplica a dívida ao Estado |
| RHC-51 | Marcar retenção como entregue | Saída de tesouraria + auditoria; não permite entregar duas vezes | ✅ **B5** — espelho exacto do `markPayslipPaid`: `registerAutoPayout`, `PAYROLL_LIABILITY_DELIVERED`, entrega dupla recusada nomeando a data |
| RHC-52 | Prazo de entrega a aproximar-se | Notificação no `NotificationFeed` | ✅ **B5** — atrasadas, a ≤7 dias, **e as sem prazo configurado**. A terceira é a que interessa: uma obrigação sem data nunca chega a estar atrasada, logo nunca apareceria (2 testes no `NotificationFeedTest`) |
| RHC-53 | **Lançamento contabilístico automático da folha** | Razão e balancete com gastos com pessoal, patronal e retenções | ✅ **B5** — `onPayslipPaid` e `onPayrollLiabilityDelivered`. Fecha a lacuna declarada na [CONTABILIDADE_SPEC §7](CONTABILIDADE_SPEC.md). 4 contas novas no PGC-NIRF (2421, 2451, 2602, 6302) |
| RHC-54 | O RH **não** importa a contabilidade | Auditoria estática: zero imports de `modules.accounting` no `modules.hr`; ligação por evento | ✅ **B5** — `HrDoesNotKnowAccountingTest`, nos dois sentidos: o RH não importa contabilidade **e** o `AutomaticPostingService` não importa o modelo do RH |
| RHC-55 | KPI de custo total do trabalhador | Base + subsídios + patronal, visível | ✅ **B5** — `monthlyCost` + barra no separador Retenções. O patronal era impresso no mapa fiscal e não aparecia em relatório de custo nenhum |

**B5 fechado (2026-08-24), migração V53.** O dinheiro do Estado passou a existir como dívida.

- **O buraco:** o IRPS retido e o INSS das duas partes eram calculados, somados no mapa fiscal e
  **nunca mais tocados**. Só o líquido saía da tesouraria, pelo que esse dinheiro ficava na conta da
  empresa **indistinguível de dinheiro próprio**. Quem o gasta não descobre o buraco no mês em que o
  gasta: descobre no dia da entrega.
- **A obrigação nasce sem prazo quando o prazo não está configurado**, e isso é deliberado. Não
  saber o prazo nunca foi razão para perder o rasto do dinheiro — e o sino tem uma linha própria
  para essas, senão eram as únicas a desaparecer.
- **Uma obrigação já entregue não se mexe:** um recibo novo num período já declarado ao Estado é
  recusado nomeando o período. Alterar em silêncio um valor já declarado é pior do que não deixar
  pagar.
- **O lançamento fecha sem ajudas:** débito de custos com pessoal (ilíquido − faltas) e de encargos
  patronais; crédito de IRPS retido, INSS a entregar, outros descontos e caixa. As duas quotas do
  INSS entregam-se juntas, e por isso partilham conta.
- **Plano de contas incompleto não bloqueia o salário.** Uma empresa que semeou o plano antes destas
  contas existirem tem um plano incompleto, não inexistente: o lançamento é saltado, fica
  `PAYROLL_POSTING_SKIPPED` na auditoria a dizer o que fazer, e o `seedDefaultChart` passou a
  **preencher lacunas** em vez de ser tudo-ou-nada.

## §6 — Valores legais configuráveis *(fechado com o B5, migração V53)*

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-56 | Configurar prazos, férias por antiguidade e aviso prévio | Por empresa, com vigência e base legal | ✅ `HrPolicyConfig` — **uma tabela para os três assuntos**, porque a pergunta é sempre a mesma: que valores legais usa esta empresa, desde quando, com que fundamento |
| RHC-57 | Perguntar um valor que não está configurado | Devolve vazio; quem chama **declara** o que faz com a ausência | ✅ `HrPolicyService` responde sempre em `Optional`. É isso que impede um valor por omissão de se instalar por acidente — foi assim que o `22` sobreviveu compilado |

## B6 — Descontos, adiantamentos e empréstimos

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-60 | Adiantamento a meio do mês | Saída de tesouraria imediata **e** desconto no recibo do período | ✅ **B6 fechado** — sai da tesouraria na criação e volta pelo recibo do período (2 testes: a saída e o regresso) |
| RHC-61 | Empréstimo em N prestações | Saldo em dívida decresce a cada recibo; nunca abaixo de zero | ✅ **B6** — 3 testes: leva uma prestação, a última leva só o que falta, e um saldado não volta a descontar. **Anular o recibo põe as prestações de volta em dívida** |
| RHC-62 | Desconto recorrente (sindicato, seguro) | Aplicado enquanto activo; pára na data de fim | ✅ **B6** — a vigência está na consulta, não num `if`: um desconto fora de prazo nem chega ao apuramento. Não sai dinheiro da caixa a criá-lo |
| RHC-63 | PDF do recibo com descontos **discriminados** | Uma linha por desconto, não um total anónimo | ✅ **B6** — `PayslipPrintService` imprime uma linha por compromisso; o que não estiver comprometido sai em "Outros Descontos" à parte, para a discriminação não mentir por omissão |

**B6 fechado (2026-08-24), migração V54.** O desconto do recibo passou a ter nome.

- **Uma tabela para os três casos**, e não três: um empréstimo é um adiantamento em N prestações, e
  um desconto recorrente é um empréstimo sem capital. O que muda é se o dinheiro saiu da caixa antes
  e quantas vezes se desconta.
- **O saldo em dívida nunca é gravado** — apura-se das linhas que os recibos levaram. Um saldo em
  coluna própria seria uma segunda verdade, e desactualizava-se à primeira anulação de recibo.
- **Nunca se desconta mais do que sobra.** Se o salário não chegar, leva o que há e o resto continua
  em dívida: não é erro, é a única coisa honesta a fazer com um líquido que não chega. E a ordem é a
  mais antiga primeiro, para o líquido ser previsível em vez de depender da base de dados.
- **Um defeito de Java apanhado pelo teste:** `kind == ADIANTAMENTO ? 1 : request.installments()`
  desembrulhava o `Integer` (o ternário passa a ser de tipo `int`), e um recorrente sem prestações
  rebentava com `NullPointerException`. Corrigido com `Integer.valueOf(1)`.

## B8 — Correcções ao existente

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| RHC-70 | Pedir férias de 6.ª a 2.ª (4 dias de calendário, 2 úteis) | Debita **dias úteis**, como a spec promete | ✅ **B8** — `TimeSheetService.workingDaysBetween`, que usa o `WorkSchedule` da empresa. A resposta vive **no mesmo sítio** que o ponto usa para classificar dia de descanso: ter as duas contas separadas era a causa do defeito |
| RHC-71 | Direito anual por antiguidade e por empresa | Configurável com vigência, molde do `PayrollTaxConfig` | ✅ **B8** — vem do `HrPolicyConfig` por antiguidade; sem configuração usa o valor histórico de 22 e a mensagem **diz o número que usou** (2 testes) |
| RHC-72 | Recibo `DRAFT → APPROVED → PAID` | Pagar um recibo por aprovar é recusado | ✅ **B8** — `approvePayslip` + `PAYSLIP_APPROVE` auditado; pagar sem aprovar é recusado com o que fazer. Botão **Aprovar** explícito no painel, como as outras acções críticas |
| RHC-73 | Falta `SICK`/`MATERNITY` | Remuneração **declarada** por tipo de falta, não por omissão | ✅ **B8** — `AbsencePayRule` com a lista explícita do que desconta. A regra estava enterrada num literal da consulta (`= 'UNJUSTIFIED'`): estava certa **por acidente**, e um tipo novo passava a ser pago sem ninguém decidir |
| RHC-74 | Fechar o período da folha e tentar emitir recibo nesse mês | Bloqueio nomeando o período | ✅ **B8** — `PayrollPeriod`. **Coisa diferente do fecho do ponto**: o ponto fecha antes de a folha correr, a folha fecha depois de estar paga. Reabrir exige motivo e fica auditado (6 testes) |
| RHC-75 | Exportar ficheiro de pagamento bancário da folha | Ficheiro com os líquidos por colaborador | ✅ **B8** — CSV dos recibos **aprovados** (um pago já saiu da conta; um rascunho ainda muda). Quem não tem conta bancária vem em lista à parte, nunca em silêncio (6 testes) |
| RHC-76 | Documento do colaborador (BI/DIRE) a caducar | Alerta antes da data | ✅ **B8** — `EmployeeDocument` + sino a 45 dias, **e os já caducados continuam na lista**: sair da janela não pode ser a forma de o alerta desaparecer (5 testes) |

**B8 fechado (2026-08-24), migração V56.** Os furos pequenos que custavam dinheiro todos os meses.

- **Férias em dias úteis** (8.1): quem pedia 22 dias seguidos gastava o ano inteiro e quem as partia
  em bocados saía a ganhar — duas contas diferentes para o mesmo direito.
- **O `22` deixou de ser a única resposta possível** (8.2): passou a ser o último recurso, e a
  mensagem diz qual o número que usou. A lei laboral moçambicana faz o direito crescer com a
  antiguidade, pelo que um número fixo está errado para alguém.
- **`APPROVED` entrou** (8.4): a `HR_PAYROLL_SPEC §3` prometia-o e o recibo nunca o teve — quem
  processava a folha pagava-a sozinho, sem segunda vista sobre os números.
- **A remuneração por tipo de falta passou a ser declarada** (8.5). Provavelmente já estava certa;
  estava certa **por acidente**.
- **O mês da folha fecha** (8.6), o **ficheiro de pagamento** existe (8.7) — com duas colunas novas
  na ficha, sem as quais o ficheiro seria inútil — e os **documentos do colaborador** têm validade
  e alerta (8.8).

---

## Cenários manuais (com o backend de pé)

| ID | Cenário |
|----|---------|
| RHC-90 | Percurso completo: admitir → contrato → ponto do mês → folha → pagar → entregar retenções |
| RHC-91 | Cessar a `Ana Sitoe` no fim do termo e conferir o acerto final contra cálculo em papel |
| RHC-92 | Login do `colaborador`: vê o **seu** recibo e pede as **suas** férias; não vê os colegas |
| RHC-93 | Comparar o PDF do contrato e o do acerto com os documentos comerciais (mesmo cabeçalho, mesmas assinaturas) |
| RHC-94 | Abas novas do `HRPanel` a 1382×736, tema claro e escuro, sem cortes |

---

## Estado dos testes automatizados

Actual de RH: `HRServiceTest` (**46**) + `TimeSheetServiceTest` (**17**) +
`EmploymentContractServiceTest` (**15**) + `TerminationServiceTest` (**14**) +
`PayrollDeductionServiceTest` (**14**) + `PayrollLiabilityServiceTest` (**11**) +
`SalaryHistoryServiceTest` (**9**) + `OvertimeValuationServiceTest` (**7**) +
`PayrollPeriodServiceTest` (**6**) + `BankPaymentFileServiceTest` (**6**) +
`PayrollBonusServiceTest` (6) + `EmployeeDocumentServiceTest` (**5**) +
`EmploymentContractPrintServiceTest` (5) + `HRApiIntegrationTest` (4) +
`HrDoesNotKnowAccountingTest` (**2**) + `PayrollTaxServiceTest` (2) = **169**.
Suite completa: **860 verdes**. Alvo por bloco:

- [x] `HRServiceTest` +6 (B7.1, 2026-08-22) — RHC-01/02/03 (guardas), RHC-05 (auditoria de criar e
      eliminar falta) e RHC-04 (rasto da despesa). **Os 6 confirmados a falhar** contra o
      `HRService` de HEAD: 13/13 verdes com o código novo, **6 falhas** com o antigo.
- [x] `HRServiceTest` +8 (B7.2, 2026-08-22) — self-service em nome próprio (férias e despesa),
      recusa em nome de colega (ambos), conta sem colaborador associado, recibos próprios na
      listagem **e** na impressão, e as duas validações da associação. **21/21 verdes**; contra uma
      variante sem `ensureCanActFor` e sem o filtro dos recibos, **5 falham** — as 5 que carregam a
      regra (as outras 3 afirmam permissão, não recusa, pelo que passam nas duas versões).
- [x] `EmploymentContractServiceTest` +15 e `HRServiceTest` +2 (B1.1, 2026-08-23) — sobreposição na
      activação, termo sem motivo, termo sem fim, sem-termo com fim, termo incerto sem fim (o único
      a termo que pode nascer sem data), expiração derivada, salário acordado escrito na ficha,
      renovação (3), cessação (2), guarda de perfil e o último dia de contrato a contar.
      **RHC-15 fechado:** a folha salta quem não tem contrato vigente **e diz quem**.
- [x] `TimeSheetServiceTest` +14 (B2.1, 2026-08-23) — marcações (origem, autor, turno da noite,
      pausa impossível, guarda de perfil), duplicação, apuramento por escalão, dia de descanso,
      dias previstos/em falta, tolerância de atraso, colaborador sem contrato, e fecho/reabertura
      (4 casos), e a geração de faltas no fecho (3).
- [x] `OvertimeValuationServiceTest` +7 e `HRServiceTest` +7 (B2.2, 2026-08-23) — recusa sem
      configuração, escalões com multiplicadores próprios, vigência e desactivação, recibo a ler do
      ponto fechado, excepção manual com e sem justificação, bloqueio da folha salarial e
      justificação de falta.
- [x] `TerminationServiceTest` **14** (B3, 2026-08-24) — proporcionais (salário do mês, 13.º,
      férias por gozar), aviso prévio só quando é devido pelo trabalhador, saldo do B6 abatido,
      acerto negativo visível e não pago, cessação dupla recusada, tesouraria e auditoria, guarda de
      perfil, saída anterior à admissão, e os dois avisos de valor legal por configurar.
- [x] `SalaryHistoryServiceTest` +9 e `HRServiceTest` +3 (B4, 2026-08-23) — anterior preservado,
      salário vigente à data (RHC-46), sem histórico cai na ficha, data futura não mexe na ficha,
      salário igual não é alteração, duas na mesma data bloqueadas, guarda de perfil, promoção com
      função, e a ficha a recusar alterar o salário.
- [x] `PayrollLiabilityServiceTest` **11** e `HRServiceTest` +2 (B5, 2026-08-24) — as três
      obrigações nascem do pagamento, só contam recibos pagos, reapuramento idempotente, obrigação
      sem prazo, obrigação datada, entrega com tesouraria/auditoria/evento, entrega dupla recusada,
      guarda de perfil, período já entregue recusa recibo novo, alertas incluem as sem prazo, e o
      custo total com patronal.
- [x] `AutomaticPostingServiceTest` +4 (B5, 2026-08-24) — o lançamento da folha **fecha sem
      ajudas**, as faltas reduzem o custo em vez de serem proveito, a entrega liquida a dívida ao
      Estado, e um plano sem as contas de pessoal não lança **mas deixa rasto**.
- [x] `HrDoesNotKnowAccountingTest` **2** (B5, 2026-08-24) — RHC-54 nos dois sentidos.
- [x] `PayrollDeductionServiceTest` **14** e `HRServiceTest` +2 (B6, 2026-08-24) — adiantamento sai
      e volta, prestações, última prestação não passa do saldo, saldado não desconta, vigência,
      tecto pelo líquido disponível, ordem previsível, anulação devolve à dívida, validações,
      recorrente não sai da caixa, guarda de perfil e linhas discriminadas.
- [x] `PayrollPeriodServiceTest` **6**, `BankPaymentFileServiceTest` **6**,
      `EmployeeDocumentServiceTest` **5** e `HRServiceTest` +6 (B8, 2026-08-24) — dias úteis
      (RHC-70), direito configurável e a mensagem com o número usado (RHC-71), aprovar e recusar
      pagamento sem aprovação (RHC-72), tipos de falta declarados (RHC-73), mês fechado (RHC-74),
      ficheiro bancário (RHC-75) e documentos a caducar (RHC-76).

Meta original: ≈60 testes próprios de RH. **Atingida e ultrapassada: 169.**
`mvn -o test` → **860 testes, 0 falhas, 0 erros, 0 ignorados** (2026-08-24).

---

## Verificação

```
mvn -o clean compile   → BUILD SUCCESS
mvn -o test            → 860 testes, 0 falhas, 0 erros, 0 ignorados (2026-08-24)
```

**Migrações validadas contra PostgreSQL real (2026-08-24).** Cluster descartável na porta 55433
(receita de 21/08 em [tasks/current.md](../tasks/current.md)), sem tocar no servidor do utilizador:

- **56 migrações aplicadas, schema em v57**, e a aplicação arrancou com `ddl-auto=validate` no
  perfil `prod` — ou seja, **cada mapeamento de entidade bate com o schema que o Flyway construiu**.
  É isso que fecha a dívida das V48–V52 que estavam por correr, mais as V53–V57 deste bloco.
- Confirmado no schema real: as **8 tabelas novas** (`hr_policy_configs`, `payroll_liabilities`,
  `payroll_deductions`, `payslip_deduction_lines`, `terminations`,
  `termination_settlement_lines`, `payroll_periods`, `employee_documents`), as colunas
  `employees.bank_name` e `bank_account` **nullable**, e os 4 índices únicos que carregam as
  invariantes (`uk_payroll_liabilities_period_type`, `uk_payslip_deduction_lines`,
  `uk_terminations_employee`, `uk_payroll_periods`).
- Cluster destruído no fim.

---

## Declarações honestas (o que este harness **não** afirma)

- **Nada disto foi validado ao vivo pela UI.** O backend está testado e as migrações correram contra
  PostgreSQL; os separadores novos (Descontos, Retenções, Cessações) e os diálogos de evolução
  salarial, documentos, acréscimos e justificação de faltas **nunca foram abertos numa janela**.
  RHC-90..94 continuam por fazer.
- **Adiantamentos, empréstimos e acertos finais movem tesouraria mas não fazem lançamento
  contabilístico.** A folha e as retenções fazem (RHC-53). Estes ficam de fora porque um adiantamento
  é um crédito ao trabalhador e o abate no acerto liquida-o: mapear isso a contas exige uma decisão
  de plano que é do contabilista, não da IA. **Está declarado aqui em vez de adivinhado.**
- **Os multiplicadores de hora extra e os valores legais continuam por confirmar** com o
  contabilista da empresa. O sistema recusa-se a valorizar horas extra sem eles, e o acerto final
  diz por escrito quando usa o valor histórico de 22 dias de férias.
- **`AbsencePayRule` é uma decisão registada, não uma verificação legal.** A lista do que desconta
  passou a estar explícita; se a lei disser outra coisa, muda-se num sítio — que é o ponto.

**Antes de qualquer migração nova:** resolver as **duas migrações com versão 46**
(`V46__crm_ticket_lifecycle.sql` e `V46__internal_replenishment.sql`) — o Flyway recusa arrancar com
versões duplicadas. Confirmar com a receita do cluster PostgreSQL descartável descrita em
[tasks/current.md](../tasks/current.md) (2026-08-21), já que a cadeia não corre em H2.
