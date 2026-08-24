# Spec — RH completo: contrato, ponto, cessação e o dinheiro que fica por entregar

> O que falta ao módulo `mz.multicore.erp.modules.hr` para deixar de ser **folha de salários** e passar
> a ser **gestão de pessoal**. Complementa (não substitui) [HR_PAYROLL_SPEC.md](HR_PAYROLL_SPEC.md),
> que cobre o motor fiscal e o recibo, e [RH_VISAO_GERAL_SPEC.md](RH_VISAO_GERAL_SPEC.md), que cobre
> a apresentação. Progresso medido em [RH_COMPLETO_HARNESS.md](RH_COMPLETO_HARNESS.md).

**Data:** 2026-08-22 · **Estado a 2026-08-24: implementado por inteiro (B1..B8).**

> O que se segue continua escrito no presente do diagnóstico original — é aí que está o valor do
> documento, porque descreve **porque é que cada furo doía**. O que mudou está medido no
> [harness](RH_COMPLETO_HARNESS.md), migrações **V48–V56**, e resumido em §8 no fim.

---

## 1. Problema

O RH sabe **pagar**. Não sabe **empregar**.

O que existe hoje é o fim da cadeia — o recibo, o IRPS, o INSS, o 13.º, as férias e a saída de
tesouraria. O que não existe é tudo o que devia estar **antes** e **depois** desse recibo:

- O que foi **combinado** com o colaborador (contrato) não é um documento — são dois campos soltos
  na ficha, `hireDate` e `contractEndDate`
  ([Employee.java:59-63](../src/main/java/mz/multicore/erp/modules/hr/model/Employee.java#L59-L63)).
- O que o colaborador **efectivamente fez** no mês (ponto) não existe de todo. As faltas são
  digitadas à mão por quem se lembrar, e as horas extra são um número que alguém escreve no pedido
  do recibo ([CreatePayslipRequest.java](../src/main/java/mz/multicore/erp/modules/hr/dto/CreatePayslipRequest.java)) —
  sem marcação, sem cálculo, sem prova.
- O que acontece quando o colaborador **sai** é uma mudança de texto de `ACTIVE` para `TERMINATED`
  ([HRService.java:156](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L156)).
  Não há acerto final, não há proporcionais, não há documento de cessação.
- O dinheiro **retido e não entregue** (IRPS do trabalhador, INSS de ambas as partes) é calculado,
  impresso no mapa fiscal e depois **desaparece**. Nenhuma linha de código volta a tocar em
  `employerInss` depois do PDF: não há obrigação registada, não há saída de tesouraria, não há aviso
  de prazo. Só o **líquido** sai da tesouraria
  ([HRService.java:259-261](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L259-L261)).

Consequência prática numa loja: a empresa consegue processar a folha de Agosto e **não consegue
responder** a "este contrato expira quando?", "quantas horas extra é que o Sr. Tembe fez?", "quanto
é que devo ao INSS este mês?" ou "o que tenho de pagar ao colaborador que se despediu?".

### O padrão já conhecido neste projecto

Três dos furos abaixo são **exactamente a mesma forma** de bugs que este sistema já apanhou e fechou
no comercial. Vale a pena dizê-lo, porque é o argumento mais forte para os fechar:

| Bug já fechado no comercial | O mesmo bug, aberto no RH |
|---|---|
| Margem lida com o preço de compra **actual** → `InvoiceLine.unitCost` fotografado (V37) | `Employee.baseSalary` é mutável e não tem histórico. Um aumento em Junho reescreve o passado para todo o lado que não seja o recibo já emitido |
| Vencimento **derivado** em vez de gravado → `Invoice.dueDate` gravada no documento (V35) | O direito a férias é a constante `DEFAULT_ANNUAL_VACATION_DAYS = 22` compilada no serviço ([HRService.java:49](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L49)) — não é da empresa, não é do contrato, não é da lei |
| A mesma regra em duas portas (IVA, saldo em dívida) | "Quantos dias de falta contam" vive no `HRService` e "quantas horas trabalhou" não vive em lado nenhum |

---

## 2. Objectivo

Fechar o ciclo de vida do colaborador — **admissão → contrato → assiduidade → alterações → cessação** —
e fechar o ciclo do dinheiro da folha — **custo → retenção → entrega ao Estado → contabilidade** —
sem quebrar as regras de tenant, papel e camadas do projecto.

---

## 3. Blocos de trabalho

Ordenados por **dano se ficar como está**, não por facilidade.

### B1 — Contrato de trabalho como documento *(pedido pelo utilizador)*

**Hoje:** dois campos na ficha. Nenhum tipo, nenhum período experimental, nenhuma renovação,
nenhum PDF, nenhum aviso de fim.

**A regra que carrega o bloco: o contrato manda na folha.** Hoje
`processMonthlyPayroll` filtra **só** por `"ACTIVE".equals(status)`
([HRService.java:232-235](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L232-L235)) —
um colaborador cujo contrato terminou a 31 de Julho continua a receber recibo em Agosto, em silêncio,
com saída de tesouraria e tudo. Quem não fechar o contrato paga a quem já não trabalha lá.

Requisitos:

- Nova entidade `EmploymentContract` (um colaborador, N contratos ao longo do tempo; **um só vigente**
  numa data). Campos: tipo, início, fim, período experimental (fim), salário acordado, horário
  semanal, local de trabalho, motivo (obrigatório no contrato a termo, exigência da lei laboral),
  número do contrato (série própria, gapless, por empresa — molde do `DocumentSeries.PAYSLIP`).
- Tipos: `SEM_TERMO` · `TERMO_CERTO` · `TERMO_INCERTO` · `TEMPORARIO` · `ESTAGIO`.
- Estados: `RASCUNHO → VIGENTE → CESSADO`; `EXPIRADO` é **derivado** da data contra "hoje", nunca
  gravado — lição da caducidade da cotação (`Quotation.isExpired`), que evita o agendador nocturno
  e as linhas desactualizadas.
- **Renovação** cria um contrato novo ligado ao anterior (`renewedFromId`), não edita o antigo. O
  histórico do que foi acordado é imutável.
- Ficha do colaborador passa a **derivar** do contrato vigente: salário base, função e horário.
  `Employee.baseSalary` deixa de ser editável directamente (ver B4).
- A folha mensal **ignora** quem não tem contrato vigente no período e diz porquê no resultado.
- PDF do contrato, com dados da empresa, do colaborador, cláusulas e assinaturas — reutilizando os
  blocos partilhados de `modules/printing` (`CommercialTermsRenderer`, `SignatureBlockRenderer`).
- Alertas: contrato a expirar em ≤30 dias e período experimental a terminar em ≤7 dias entram no
  `NotificationFeed` já existente.

### B2 — Ponto e assiduidade *(pedido pelo utilizador)*

**Hoje:** zero. Explicitamente excluído em [HR_PAYROLL_SPEC.md §Não-objectivos](HR_PAYROLL_SPEC.md).
Esta spec **revoga** essa exclusão para a parte de registo e cálculo (a integração com relógio
biométrico continua fora).

**A regra que carrega o bloco: as horas extra do recibo têm de ter origem.** Hoje
`CreatePayslipRequest.overtime` é um `BigDecimal` que quem processa a folha escreve à mão. Ninguém
sabe de onde veio, ninguém o pode contestar, e o mesmo valor pode ser pago duas vezes sem que nada
o note.

Requisitos:

- `TimeEntry` (marcação): colaborador, data, entrada, saída, pausa, origem (`MANUAL` · `IMPORTADO` ·
  `TERMINAL`), quem registou, observação. Único por colaborador+data+turno.
- `WorkSchedule` (horário): horas/dia por dia da semana, tolerância de atraso, feriados.
  Por empresa, atribuído no contrato.
- **Folha de ponto mensal** por colaborador: dias previstos, dias trabalhados, horas normais,
  **horas extra por escalão** (diurnas, nocturnas, dia de descanso/feriado), atrasos, faltas
  detectadas.
- A folha de ponto do mês **fecha** (`FECHADA`) antes de a folha salarial correr; depois de fechada
  não se altera sem reabertura auditada.
- O recibo passa a **ler** horas extra e faltas da folha de ponto fechada. O campo manual
  mantém-se só como excepção, exige justificação e fica auditado — a porta que hoje é a regra
  passa a ser a excepção declarada (mesmo padrão do campo `taxRate` do `CreateInvoiceLineRequest`).
- **Faltas geradas pelo ponto**, não digitadas: um dia previsto sem marcação nasce como falta
  `POR_JUSTIFICAR`, e quem justifica muda o tipo com motivo e documento.
- Multiplicadores de hora extra configuráveis por empresa (a lei laboral moçambicana tem
  acréscimos distintos por tipo de hora — **os valores a usar têm de ser confirmados com o
  contabilista da empresa**, ver §6).

### B3 — Cessação e acerto final

**Hoje:** `changeEmployeeStatus(id, "TERMINATED")` — uma `String`. Nada mais acontece.

**O que se perde:** ao sair, o colaborador tem direito a proporcionais que o sistema **já sabe
calcular mas nunca calcula neste contexto** — 13.º proporcional (`PayrollBonusService.thirteenthMonth`
existe), férias vencidas e não gozadas (o saldo existe em `sumReservedDays`), aviso prévio e, quando
aplicável, compensação por cessação. Hoje isso é feito à mão, em papel, ou não é feito.

Requisitos:

- `Termination`: contrato, data, motivo (`INICIATIVA_TRABALHADOR` · `INICIATIVA_EMPREGADOR` ·
  `MUTUO_ACORDO` · `FIM_DO_TERMO` · `JUSTA_CAUSA`), aviso prévio cumprido (s/n), observações.
- **Acerto final** como documento próprio, com linhas: salário do mês até à data de saída, 13.º
  proporcional, férias não gozadas, subsídios em dívida, aviso prévio não cumprido (a descontar),
  compensação, adiantamentos/empréstimos por liquidar (B6) — total líquido a pagar.
- Só `MANAGER/ADMIN`; auditado; pagamento gera saída de tesouraria pela mesma porta
  `financeService.registerAutoPayout` que o recibo usa.
- A cessação **fecha o contrato**, muda o colaborador para `TERMINATED` e **impede** novos recibos
  e novos pedidos de férias — hoje impede só parcialmente, por `findActiveEmployee`.
- Certificado de trabalho imprimível.

### B4 — Histórico salarial e alterações contratuais

**Hoje:** `updateEmployee` sobrepõe `baseSalary`
([HRService.java:143](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L143)).
O valor anterior não fica em lado nenhum, nem quem o mudou, nem porquê, nem a partir de que data.
É o bug da margem histórica (V37), transposto para os salários.

Requisitos:

- `SalaryChange`: colaborador, salário anterior, novo, **data de efeito**, motivo
  (`AUMENTO` · `PROMOCAO` · `REVISAO_ANUAL` · `ACORDO` · `CORRECCAO`), quem aprovou, auditado.
- O salário do recibo é o **vigente à data do período do recibo**, não o de hoje. Reprocessar Março
  em Setembro tem de dar o valor de Março.
- Alteração de função/departamento com data de efeito, na mesma tabela.
- Ecrã "evolução salarial" do colaborador (o `SimpleBarChart` já existe e é reutilizável).

### B5 — Retenções por entregar e contabilização da folha

**Hoje:** o líquido sai da tesouraria; o IRPS retido, o INSS do trabalhador e o INSS patronal são
calculados, somados no mapa fiscal ([PayrollFiscalMapPrintService.java:114](../src/main/java/mz/multicore/erp/modules/printing/PayrollFiscalMapPrintService.java#L114))
e **nunca mais aparecem**. O dinheiro fica na conta da empresa sem estar marcado como dívida ao
Estado, e ninguém é avisado do prazo de entrega.

E, como já está declarado em [CONTABILIDADE_SPEC.md §7](CONTABILIDADE_SPEC.md), **os salários não
fazem lançamento contabilístico automático**. O maior custo fixo de uma empresa de retalho não chega
ao razão nem ao balancete.

Requisitos:

- `PayrollLiability` por empresa/período/tipo (`IRPS` · `INSS_TRABALHADOR` · `INSS_PATRONAL`):
  valor apurado, prazo legal de entrega, estado (`POR_ENTREGAR` · `ENTREGUE`), data e referência
  do pagamento.
- Marcar como entregue gera **saída de tesouraria** e fica auditado — espelho exacto do
  `markPayslipPaid`.
- Notificação de retenção por entregar com prazo a aproximar-se, no `NotificationFeed`.
- **Lançamento contabilístico automático da folha por evento** (`PayslipPaidEvent`), no molde do
  `SaleRegisteredEvent`/`PaymentReceivedEvent` que a contabilidade já consome — para o RH **não
  passar a conhecer a contabilidade**. Débito de gastos com pessoal e INSS patronal; crédito de
  remunerações a pagar, retenções a entregar e caixa/bancos.
- KPI de **custo total do trabalhador** (base + subsídios + patronal), que hoje não existe em lado
  nenhum: o INSS patronal é custo da empresa e não aparece em nenhum relatório de resultados.

### B6 — Descontos recorrentes, adiantamentos e empréstimos

**Hoje:** um único campo `otherDeductions`, um número solto por recibo, sem dizer o que é.

Numa loja em Moçambique isto é o dia-a-dia: adiantamento a meio do mês, desconto por quebra de
caixa, sindicato, seguro, empréstimo em prestações.

Requisitos:

- `PayrollDeduction` recorrente: tipo, valor ou percentagem, início, fim ou nº de prestações, activo.
- `SalaryAdvance` (adiantamento): valor, data, **saída de tesouraria imediata**, e desconto
  automático no recibo do período — hoje um adiantamento sai da caixa sem nunca voltar.
- `EmployeeLoan` com plano de prestações e **saldo em dívida** — o mesmo conceito de
  `Invoice.outstandingAmount()` que já existe do lado do cliente, aplicado ao colaborador.
- O recibo passa a discriminar os descontos por linha, no PDF. Um número anónimo num recibo é
  a origem clássica da reclamação do trabalhador.
- Um saldo por liquidar **entra no acerto final** (B3).

### B7 — Estrutura, permissões e self-service

**Hoje**, três buracos verificáveis no código:

1. `recordAbsence` ([HRService.java:363](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L363)),
   `deleteAbsence` ([:381](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L381)),
   `submitVacation` ([:413](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L413))
   e `submitExpense` ([:90](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L90))
   **não chamam `ensureHrManager()` nem auditam**, e recebem o `employeeId` no corpo do pedido.
   Qualquer utilizador autenticado da empresa lança uma falta, **apaga** uma falta, pede férias ou
   submete uma despesa **em nome de outro colaborador** — a despesa vai à Engine de Aprovações com
   o nome do colega. É a mesma classe de furo que o `pay-invoice` sem `PermissionGuard` (fechado a
   09/08) e o `decisionBy` vindo do corpo do pedido (fechado a 22/06).
2. **Não há ligação `Employee ↔ User`.** Sem ela, o perfil "Colaborador" descrito em
   [HR_PAYROLL_SPEC.md §Perfis alvo](HR_PAYROLL_SPEC.md) é ficção: ninguém consegue ver o seu
   próprio recibo nem pedir as suas próprias férias.
3. `department` e `role` são texto livre. Não há chefia, pelo que uma aprovação não pode ser
   encaminhada para o responsável do pedinte — vai toda para `MANAGER/ADMIN`.

Requisitos: ligar `Employee` a `User`; `submitVacation`/`submitExpense` sem `employeeId` resolvem o
colaborador **pelo utilizador autenticado** (e só um gestor pode indicar outro); `recordAbsence`/
`deleteAbsence` exigem gestor e ficam auditadas; `Department` como entidade com responsável;
`Employee.managerId` para linha hierárquica.

### B8 — Correcções ao que já existe

Furos pequenos, todos confirmados no código, que não justificam bloco próprio mas justificam commit:

| # | O quê | Onde | Porque importa |
|---|---|---|---|
| 8.1 | **Férias contadas em dias de calendário**, mas a spec promete "22 **dias úteis**" | [HRService.java:419](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L419) usa `ChronoUnit.DAYS`, fins-de-semana incluídos | Quem pede 22 dias seguidos gasta o ano inteiro; quem parte em bocados sai a ganhar. Duas contas diferentes para o mesmo direito |
| 8.2 | **Direito anual fixo em 22** e compilado | [HRService.java:49](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L49) | A lei laboral moçambicana faz o direito **crescer com a antiguidade** nos primeiros anos. Um número fixo está errado para alguém — ver §6 |
| 8.3 | `contractEndDate` **não trava a folha** | [HRService.java:232-235](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L232-L235) | Paga-se a quem já saiu (fecha com B1) |
| 8.4 | Recibo **não tem `APPROVED`** | [Payslip.java:89](../src/main/java/mz/multicore/erp/modules/hr/model/Payslip.java#L89): `DRAFT, PAID, CANCELLED` | A spec §3 promete `DRAFT → APPROVED → PAID`. Quem processa paga sem segunda vista |
| 8.5 | Faltas `SICK`/`MATERNITY` **pagas por omissão** | [HRService.java:305-326](../src/main/java/mz/multicore/erp/modules/hr/service/HRService.java#L305-L326) desconta só `UNJUSTIFIED` | Pode até estar certo, mas está por **acidente** — o tipo de falta não tem regra declarada de remuneração |
| 8.6 | Sem **fecho de período** na folha | `processMonthlyPayroll` corre para qualquer mês, sempre | Um mês já pago e contabilizado continua a aceitar recibos novos |
| 8.7 | Sem **ficheiro de pagamento bancário** | — | Numa folha de 30 pessoas, paga-se uma a uma |
| 8.8 | Sem **documentos do colaborador** (BI/DIRE, contrato assinado, certificados) com alerta de validade | — | O DIRE de um trabalhador estrangeiro caducar sem aviso é multa |

---

## 4. Não-objectivos

- Integração com **hardware biométrico**. O `TimeEntry` fica com origem `TERMINAL` preparada, mas
  esta iteração só regista manual/importado.
- **Submissão electrónica** aos portais do INSS/AT. Gera-se o mapa e a obrigação; a entrega é manual.
- Avaliação de desempenho, recrutamento e formação.
- Sistema de turnos rotativos complexos (escala é horário fixo por dia da semana).

---

## 5. Ordem sugerida e dependências

```
B7.1 (guardas nas faltas/férias/despesas)   ← independente, ~meio dia, fecha um furo real
       │
B1 (contrato) ──┬──► B8.3 (folha respeita o contrato)
                ├──► B2 (ponto: o horário vem do contrato)
                └──► B3 (cessação: cessa um contrato, não um "estado")
                             │
B4 (histórico salarial) ─────┤   B6 (adiantamentos) ──► B3 (entram no acerto)
                             │
B5 (retenções + contabilidade) ← independente de B1..B4, e é onde está o dinheiro
```

Recomendação: **B7.1 → B1 → B5 → B2 → B4 → B3 → B6**. B5 sobe na ordem por ser dinheiro do Estado
com prazo legal; B2 é o maior em esforço e ganha por vir depois de o contrato existir.

~~**Bloqueador de migrações:** duas migrações com a versão 46.~~ **Resolvido a 2026-08-22** (a do
CRM passou a `V47`).

**Numeração efectiva** (a proposta original mudou porque a ordem de execução mudou — B2 e B4 vieram
antes de B5, e o ponto precisou de duas migrações):

| Migração | Bloco |
|---|---|
| V48 | ligação `Employee ↔ AppUser` (B7.2) |
| V49 | contratos de trabalho (B1) |
| V50 | ponto e assiduidade (B2.1) |
| V51 | acréscimos de hora extra (B2.2) |
| V52 | histórico salarial (B4) |
| V53 | retenções por entregar **+ valores legais configuráveis** (B5 e §6) |
| V54 | descontos, adiantamentos e empréstimos (B6) |
| V55 | cessações e acertos finais (B3) |
| V56 | correcções: fecho do mês, documentos do colaborador, conta bancária (B8) |

Todas **aplicadas e validadas contra PostgreSQL real a 2026-08-24** (schema v57, com
`ddl-auto=validate` a confirmar cada mapeamento).

---

## 6. Declarações honestas (o que esta spec **não** afirma)

Regra do projecto: o que não foi verificado fica escrito como não verificado.

- **Os valores legais moçambicanos não estão confirmados nesta spec.** Direito a férias por
  antiguidade, acréscimos de hora extra, prazos de entrega de INSS e IRPS, aviso prévio e
  compensação por cessação vêm da Lei do Trabalho e legislação fiscal, que mudam. Esta spec pede que
  sejam **configuráveis por empresa** e que o valor inicial seja confirmado com o contabilista do
  cliente — exactamente como já se fez com os escalões IRPS (`PayrollTaxConfig`, com vigência por
  data). **Não é a IA que decide o número.**
- O ponto **não substitui prova legal** de assiduidade sem registo assinado ou terminal certificado.
- ~~Tudo o que está em §3 é **proposta**; nenhuma linha foi escrita.~~ **Implementado a 2026-08-24**
  (ver §8). O que está em §3 marcado com ficheiro e linha era **facto verificado no código** a
  2026-08-22 — as referências de linha já não correspondem, e isso é deliberado: são a fotografia do
  diagnóstico, não um índice do código actual.
- **Os valores legais continuam por confirmar.** O que mudou foi o sítio onde vivem: passaram de
  constantes compiladas para `HrPolicyConfig` e `OvertimeRateConfig`, por empresa, com vigência e
  base legal. Sem configuração, o sistema **recusa-se a valorizar horas extra** e o acerto final
  **diz por escrito** quando usa o valor histórico de 22 dias de férias. Continua a não ser a IA a
  decidir o número — só deixou de o esconder.

---

## 7. Critério de "pronto"

Cada bloco fecha quando os cenários `RHC-xx` do bloco estiverem verdes no
[RH_COMPLETO_HARNESS.md](RH_COMPLETO_HARNESS.md), incluindo os cenários que se exige **confirmar a
falhar** contra o código actual, e com `mvn test` verde.

---

## 8. O que ficou feito, e o que não *(2026-08-24)*

**Feito:** B1..B8, 169 testes próprios de RH, suite completa em **860 verdes**, migrações V48–V56
aplicadas contra PostgreSQL real. Os sete 🔴 da auditoria de 22/08 estão fechados.

O ciclo de vida fecha os dois lados que a §2 pedia:

- **colaborador** — admissão → contrato → ponto → alterações salariais → cessação com acerto final;
- **dinheiro** — custo → retenção registada como dívida ao Estado → entrega com saída de tesouraria
  → lançamento contabilístico por evento.

**Por fazer, e declarado:**

1. **Validação ao vivo pela UI.** O backend está testado e as migrações correram; os separadores
   novos e os diálogos nunca foram abertos numa janela. RHC-90..94 continuam por fazer.
2. **Adiantamentos, empréstimos e acertos finais movem tesouraria mas não lançam na contabilidade.**
   A folha e as retenções lançam. Estes ficam de fora porque um adiantamento é um crédito ao
   trabalhador e o abate no acerto liquida-o — mapear isso a contas é uma decisão de plano que
   pertence ao contabilista. Declarado aqui em vez de adivinhado, no molde do §7 da
   [CONTABILIDADE_SPEC](CONTABILIDADE_SPEC.md), que é como esta lacuna foi encontrada.
3. **Os valores legais e os acréscimos de hora extra** continuam por confirmar (§6).
4. **Turnos rotativos** continuam fora (§4): o anti-duplicação do ponto é por colaborador+data, e
   quando existirem turnos o índice único da V50 muda.
