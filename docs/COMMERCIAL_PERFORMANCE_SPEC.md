# Especificação — Centro de Desempenho Comercial (CDC)

**Versão:** 1.0  
**Data:** 2026-09-14  
**Módulo novo:** `performance`  
**Domínios dependentes:** `hr`, `reports`, `comercial`, `inventory`  
**Harness:** `CommercialPerformanceHarnessTest`

---

## 1. Contexto e o que já existe

| O que existe | Onde |
|---|---|
| `ReportService.salesByOperator()` | Vendas por operador no relatório diário |
| `PayrollBonus` + `PayrollBonusService` | Subsídios legais (13.º mês, férias) — **não** prémios comerciais |
| `Payslip` com `allowances` | Campo genérico — não discrimina o tipo de prémio |
| `PayrollDeductionKind` | Apenas ADIANTAMENTO / EMPRESTIMO / RECORRENTE |
| `AuditLogService` | Auditoria geral de ações |
| `PermissionGuard.requireManagerOrAdmin` | Guarda de permissão |
| `InvoiceLine.lineCost()` / `lineTotal` | Custo histórico e receita por linha |

**O que não existe:**  
Não há entidade de **Meta Comercial** (SalesGoal), nem motor de **avaliação automática** de cumprimento, nem prémio por performance ligado à folha salarial.

---

## 2. Modelo de domínio

### 2.1 `SalesGoal` — a meta

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | Long | PK |
| `companyId` | Long | Empresa dona da meta |
| `name` | String | Nome descritivo ("Meta Outubro 2026") |
| `period` | GoalPeriod (enum) | MONTHLY / QUARTERLY / SEMIANNUAL / ANNUAL |
| `periodStart` | LocalDate | Início do período |
| `periodEnd` | LocalDate | Fim do período (inclusive) |
| `scope` | GoalScope (enum) | COMPANY / WAREHOUSE / CATEGORY / PRODUCT / EMPLOYEE |
| `scopeRefId` | Long | ID do armazém/categoria/produto/colaborador (null = toda empresa) |
| `scopeLabel` | String | Label desnormalizado (ex.: "João Silva") para histórico |
| `targetRevenue` | BigDecimal | Receita alvo (MZN) — pode ser null se a meta é em margem |
| `targetMargin` | BigDecimal | Margem bruta alvo (MZN) — pode ser null se a meta é em receita |
| `targetMarginPct` | BigDecimal | Margem % alvo — opcional |
| `bonusType` | BonusType (enum) | FIXED / PERCENTAGE_OF_REVENUE / PERCENTAGE_OF_MARGIN |
| `bonusValue` | BigDecimal | Valor fixo (MZN) ou percentagem conforme `bonusType` |
| `bonusCap` | BigDecimal | Tecto máximo do prémio (opcional) |
| `status` | GoalStatus (enum) | ACTIVE / ACHIEVED / MISSED / CANCELLED |
| `autoApplyBonus` | boolean | Se verdadeiro, cria `SalesGoalBonus` automaticamente no fecho do período |
| `createdBy` | String | Username do criador (auditoria) |

### 2.2 `SalesGoalProgress` — snapshot do progresso

Calculado em tempo real (não persistido), devolvido pelo endpoint de dashboard:

| Campo | Detalhe |
|---|---|
| `goalId` | FK para `SalesGoal` |
| `currentRevenue` | Receita acumulada no período |
| `currentMargin` | Margem bruta acumulada |
| `progressPct` | `currentRevenue / targetRevenue × 100` |
| `daysElapsed` | Dias decorridos do período |
| `daysTotal` | Duração total do período em dias |
| `projectedRevenue` | Projeção linear ao ritmo actual |
| `isOnTrack` | `projectedRevenue >= targetRevenue` |
| `bonusEstimate` | Prémio estimado se a meta for atingida hoje |
| `alertLevel` | NONE / CAUTION / LATE / CRITICAL |

### 2.3 `SalesGoalBonus` — prémio apurado

Gerado quando a meta é atingida (manual ou automático):

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | Long | PK |
| `goal` | SalesGoal | FK |
| `employee` | Employee | FK — null quando o scope não é EMPLOYEE |
| `companyId` | Long | — |
| `calculatedAmount` | BigDecimal | Prémio calculado pela regra da meta |
| `approvedAmount` | BigDecimal | Valor aprovado (pode diferir, com justificação) |
| `justification` | String | Razão de ajuste manual |
| `status` | BonusStatus (enum) | PENDING / APPROVED / PAID / CANCELLED |
| `payslipId` | Long | FK para `Payslip` quando integrado na folha (nullable) |
| `createdBy` | String | Quem apurou |
| `approvedBy` | String | Quem aprovou |

---

## 3. Enums (todos em `contracts`)

```java
public enum GoalPeriod { MONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL }

public enum GoalScope  { COMPANY, WAREHOUSE, CATEGORY, PRODUCT, EMPLOYEE }

public enum BonusType  { FIXED, PERCENTAGE_OF_REVENUE, PERCENTAGE_OF_MARGIN }

public enum GoalStatus { ACTIVE, ACHIEVED, MISSED, CANCELLED }

public enum BonusStatus{ PENDING, APPROVED, PAID, CANCELLED }

public enum AlertLevel { NONE, CAUTION, LATE, CRITICAL }
```

---

## 4. Lógica de progresso e alertas

### 4.1 Progresso em tempo real

```
currentRevenue = sum(InvoiceLine.lineTotal) por scope/período
currentMargin  = sum(InvoiceLine.lineCost() subtracted from lineTotal)
progressPct    = currentRevenue / targetRevenue × 100
daysElapsed    = today − periodStart + 1
daysTotal      = periodEnd − periodStart + 1
expectedPct    = daysElapsed / daysTotal × 100   (ritmo esperado linear)
projectedRev   = currentRevenue / daysElapsed × daysTotal
isOnTrack      = projectedRev >= targetRevenue
```

### 4.2 Nível de alerta

| Condição | AlertLevel |
|---|---|
| `progressPct >= 100` ou `isOnTrack` | `NONE` |
| `progressPct >= expectedPct × 0.85` | `CAUTION` (amarelo) |
| `progressPct >= expectedPct × 0.70` | `LATE` (laranja) |
| `progressPct < expectedPct × 0.70` | `CRITICAL` (vermelho) |

### 4.3 Integração com `SmartAlertsDialog`

Metas em `LATE` ou `CRITICAL` são injectadas como alertas no `SmartAlertsDialog` existente,
com botão "Ver Metas" que navega para o painel CDC.

---

## 5. Motor de cálculo de prémio

```
FIXED                   → bonusValue
PERCENTAGE_OF_REVENUE   → currentRevenue × bonusValue / 100
PERCENTAGE_OF_MARGIN    → currentMargin × bonusValue / 100
```

Aplicar `bonusCap` se definido: `min(calculado, bonusCap)`.  
Para scope EMPLOYEE: prémio individual.  
Para scope COMPANY/WAREHOUSE: prémio distribuído igualmente pelos colaboradores activos do scope no período (ou conforme `distributionWeight` — ver §7).

---

## 6. Ranking de trabalhadores

Endpoint dedicado que devolve, por período:

| Campo | Detalhe |
|---|---|
| `rank` | Posição no ranking (1 = melhor) |
| `employeeId` | FK |
| `employeeName` | — |
| `revenue` | Receita gerada |
| `grossMargin` | Margem bruta |
| `avgTicket` | Receita / nº de faturas |
| `invoiceCount` | Nº faturas emitidas |
| `goalProgressPct` | % da sua meta (se existir) |

---

## 7. Integração com a folha salarial (HR)

Quando `SalesGoalBonus.status = APPROVED`:
- O `HRService.processMonthlyPayroll()` inclui o campo `salesBonus` no `Payslip`.
- `Payslip` recebe um novo campo `salesBonus` (BigDecimal).
- O `netPay` é recalculado: `netPay += salesBonus` (prémio não sujeito a IRPS automático neste módulo — a decisão fiscal é do administrador).
- `SalesGoalBonus.payslipId` é preenchido na transacção de pagamento.
- O `PayslipPrintService` mostra a linha "Prémio de Desempenho" no recibo.

> **Nota fiscal:** O prémio de desempenho em Moçambique é tratado como rendimento de trabalho e sujeito a IRPS. O cálculo fiscal fica fora do âmbito desta SPEC — o administrador decide incluir ou excluir do rendimento tributável via `PayrollDeduction`.

---

## 8. Auditoria

Todos os eventos críticos passam pelo `AuditLogService`:

| Evento | Código | Quando |
|---|---|---|
| Criação de meta | `GOAL_CREATED` | `createGoal()` |
| Alteração de meta | `GOAL_UPDATED` | `updateGoal()` |
| Cancelamento de meta | `GOAL_CANCELLED` | `cancelGoal()` |
| Prémio apurado | `BONUS_CALCULATED` | Automático ou manual |
| Prémio aprovado | `BONUS_APPROVED` | `approveBonus()` |
| Prémio ajustado | `BONUS_ADJUSTED` | `adjustBonus()` com `justification` |
| Prémio integrado na folha | `BONUS_PAID_IN_PAYSLIP` | `processMonthlyPayroll()` |

---

## 9. Contratos REST

Novo prefixo: `/api/performance`

| Endpoint | Método | Parâmetros | Resposta |
|---|---|---|---|
| `/api/performance/goals` | GET | `companyId, status?, period?` | `List<SalesGoalDTO>` |
| `/api/performance/goals` | POST | body `CreateSalesGoalRequest` | `SalesGoalDTO` |
| `/api/performance/goals/{id}` | PUT | body `UpdateSalesGoalRequest` | `SalesGoalDTO` |
| `/api/performance/goals/{id}` | DELETE | — | 204 |
| `/api/performance/goals/{id}/progress` | GET | — | `SalesGoalProgressDTO` |
| `/api/performance/goals/active-summary` | GET | `companyId` | `List<SalesGoalProgressDTO>` |
| `/api/performance/ranking` | GET | `companyId, from, to` | `List<EmployeeRankingDTO>` |
| `/api/performance/bonuses` | GET | `companyId, status?` | `List<SalesGoalBonusDTO>` |
| `/api/performance/bonuses/{id}/approve` | POST | body `ApproveBonusRequest` | `SalesGoalBonusDTO` |
| `/api/performance/bonuses/{id}/adjust` | POST | body `AdjustBonusRequest` | `SalesGoalBonusDTO` |
| `/api/performance/report` | GET | `companyId, from, to` | `PerformanceReportDTO` |
| `/api/print/performance-report` | POST | `companyId` + body `PerformanceReportDTO` | `application/pdf` |

---

## 10. Regras de negócio críticas (harness CDC-BIZ-01 a CDC-BIZ-12)

| ID | Regra |
|---|---|
| CDC-BIZ-01 | `targetRevenue` e `targetMargin` não podem ser ambos nulos — pelo menos um deve ser definido. |
| CDC-BIZ-02 | `periodEnd` deve ser posterior a `periodStart`. |
| CDC-BIZ-03 | Para scope `EMPLOYEE`, `scopeRefId` deve ser um `Employee.id` válido da mesma empresa. |
| CDC-BIZ-04 | Uma meta `ACHIEVED`, `MISSED` ou `CANCELLED` não pode ser editada — apenas visualizada. |
| CDC-BIZ-05 | O prémio calculado aplica `bonusCap` quando definido: `min(calculado, bonusCap)`. |
| CDC-BIZ-06 | Apenas `MANAGER` ou `ADMIN` pode criar, editar ou cancelar metas. |
| CDC-BIZ-07 | Apenas `ADMIN` pode aprovar ou ajustar prémios. |
| CDC-BIZ-08 | Um `SalesGoalBonus` já `PAID` não pode ser alterado — `BusinessRuleException`. |
| CDC-BIZ-09 | A integração na folha requer que o `PayrollPeriod` esteja `ABERTO` para o mês de pagamento. |
| CDC-BIZ-10 | O custo histórico `lineCost()` é sempre usado no cálculo de margem (não o `purchasePrice` actual). |
| CDC-BIZ-11 | Alteração de meta ou prémio gera entrada em `AuditLog` com valor anterior e novo. |
| CDC-BIZ-12 | O ranking usa apenas faturas `isRealisedSale() = true`. |

---

## 11. Estrutura de ficheiros

### contracts/
- `[NEW]` `performance/dto/SalesGoalDTO.java`
- `[NEW]` `performance/dto/CreateSalesGoalRequest.java`
- `[NEW]` `performance/dto/UpdateSalesGoalRequest.java`
- `[NEW]` `performance/dto/SalesGoalProgressDTO.java`
- `[NEW]` `performance/dto/EmployeeRankingDTO.java`
- `[NEW]` `performance/dto/SalesGoalBonusDTO.java`
- `[NEW]` `performance/dto/ApproveBonusRequest.java`
- `[NEW]` `performance/dto/AdjustBonusRequest.java`
- `[NEW]` `performance/dto/PerformanceReportDTO.java`
- `[NEW]` `performance/model/GoalPeriod.java` (enum)
- `[NEW]` `performance/model/GoalScope.java` (enum)
- `[NEW]` `performance/model/BonusType.java` (enum)
- `[NEW]` `performance/model/GoalStatus.java` (enum)
- `[NEW]` `performance/model/BonusStatus.java` (enum)
- `[NEW]` `performance/model/AlertLevel.java` (enum)

### backend/
- `[NEW]` `performance/model/SalesGoal.java`
- `[NEW]` `performance/model/SalesGoalBonus.java`
- `[NEW]` `performance/repository/SalesGoalRepository.java`
- `[NEW]` `performance/repository/SalesGoalBonusRepository.java`
- `[NEW]` `performance/service/SalesGoalService.java`
- `[NEW]` `performance/service/GoalProgressEngine.java`
- `[NEW]` `performance/service/BonusCalculatorEngine.java`
- `[NEW]` `performance/service/EmployeeRankingService.java`
- `[NEW]` `performance/service/PerformanceReportService.java`
- `[NEW]` `performance/controller/PerformanceController.java`
- `[MODIFY]` `hr/model/Payslip.java` — adicionar `salesBonus` (BigDecimal)
- `[MODIFY]` `hr/service/HRService.java` — incluir `salesBonus` no `netPay`
- `[NEW]` `printing/PerformanceReportPrintService.java`
- `[MODIFY]` `printing/PayslipPrintService.java` — nova linha "Prémio de Desempenho"

### desktop/
- `[NEW]` `desktop/client/PerformanceApiClient.java`
- `[NEW]` `gui/PerformancePanel.java` — painel principal CDC
- `[NEW]` `gui/performance/GoalsTab.java` — lista + criar/editar metas
- `[NEW]` `gui/performance/ProgressTab.java` — progresso em tempo real com barras
- `[NEW]` `gui/performance/RankingTab.java` — ranking de trabalhadores
- `[NEW]` `gui/performance/BonusTab.java` — prémios: aprovar, ajustar, integrar folha
- `[MODIFY]` `gui/MainFrame.java` — adicionar PerformancePanel à sidebar
- `[MODIFY]` `gui/components/SmartAlertsDialog.java` — injectar alertas de metas CDC

### testes/
- `[NEW]` `performance/service/CommercialPerformanceHarnessTest.java`
- `[NEW]` `performance/service/GoalProgressEngineTest.java`
- `[NEW]` `performance/service/BonusCalculatorEngineTest.java`

---

## 12. Critérios de aceitação

1. Admin cria meta "Outubro 2026 — João Silva — 500.000 MZN" → aparece no dashboard com barra de progresso.
2. João vende 400.000 → progresso = 80%, alert = CAUTION.
3. Meta atingida (100%) → estado muda para ACHIEVED, prémio calculado automaticamente.
4. Admin aprova prémio → `SalesGoalBonus.status = APPROVED`.
5. Ao processar folha de outubro → `Payslip.salesBonus` preenchido, `netPay` actualizado.
6. Recibo PDF de outubro inclui linha "Prémio de Desempenho: 5.000 MZN".
7. Alteração de meta gera entrada no log de auditoria com valor anterior e novo.
8. `EMPLOYEE` tenta criar meta → 403.
9. Prémio já `PAID` → `BusinessRuleException` ao tentar ajustar.
10. Ranking mostra top colaboradores por receita, margem e ticket médio no período.
