# Harness — Validação de RH & Folha Salarial

Mede o progresso do módulo `mz.multicore.erp.modules.hr` contra [HR_PAYROLL_SPEC.md](HR_PAYROLL_SPEC.md).
Cada cenário corre com **empresa activa, utilizador identificado e dados controlados**.
Legenda de estado: ✅ feito · 🟡 parcial · ❌ em falta.

## Dados base

Criar ou confirmar:
- Empresa `Empresa Demo`.
- Utilizadores: `admin` (ADMIN), `rh` (MANAGER), `colaborador` (EMPLOYEE).
- Config fiscal vigente: escalões IRPS + taxas INSS para o ano corrente (seeder `PayrollTaxConfigSeeder`).
- Colaborador `João Mucavel`: ACTIVE, salário base, NUIT, nº INSS, 2 dependentes.
- Colaborador `Ana Sitoe`: ACTIVE, sem dependentes.
- Colaborador `Carlos Tembe`: TERMINATED (não deve entrar na folha).

## Matriz de cenários

| ID | Área | Cenário | Resultado esperado | Estado |
|----|------|---------|--------------------|--------|
| RH-01 | Colaborador | RH cria colaborador com nº/email únicos | Criado ACTIVE, auditado | ✅ (auditado) |
| RH-02 | Colaborador | Criar 2.º colaborador com mesmo nº/email | Bloqueio com mensagem PT | ✅ |
| RH-03 | Colaborador | EMPLOYEE tenta criar colaborador | 403 / `BusinessRuleException` | ✅ |
| RH-04 | Colaborador | Mudar estado para TERMINATED | Sai da folha mensal, auditado | ✅ (auditado) |
| RH-05 | Fiscal | Calcular IRPS com 0 vs. N dependentes | Dedução por dependente reflectida | 🟡 (1 teste só) |
| RH-06 | Fiscal | Rendimento em cada escalão IRPS | Taxa/parcela correctas por escalão | ❌ teste |
| RH-07 | Fiscal | Período sem config fiscal vigente | Falha explícita em PT | ❌ teste |
| RH-08 | Fiscal | INSS trabalhador e patronal | Ambos calculados pela taxa vigente | 🟡 |
| RH-09 | Recibo | Emitir recibo do colaborador no mês | Nº gapless, DRAFT, líquido correcto | ✅ (série REC gapless + auditado) |
| RH-10 | Recibo | Emitir 2.º recibo mesmo colaborador/mês | Bloqueio anti-duplicação | ✅ |
| RH-11 | Recibo | Marcar recibo como pago | PAID + **saída de tesouraria do líquido** | ✅ (payout + auditado) |
| RH-12 | Recibo | Cancelar recibo já pago | Bloqueio com mensagem PT | ✅ (auditado) |
| RH-13 | Recibo | Imprimir recibo (PDF) | PDF com cabeçalho + quebra fiscal | ✅ |
| RH-14 | Folha | Processar folha mensal | DRAFT só para ACTIVE sem recibo; idempotente | 🟡 (falta desconto faltas) |
| RH-15 | 13.º | Apurar 13.º mês / subsídio de Natal | Proporcional ao tempo de serviço | ✅ (cálculo + API + teste) |
| RH-16 | Férias | Subsídio de férias ao gozar férias | Valor calculado e pago | ✅ (calculado + pago + idempotente + auditado) |
| RH-17 | Férias | Pedido dentro do saldo anual | Aceite, saldo decrementa | ✅ (saldo reservado) |
| RH-18 | Férias | Pedido acima do saldo (>22 dias) | Bloqueio com mensagem PT | ✅ (bloqueado + teste) |
| RH-19 | Férias | Aprovar/rejeitar férias | Decisor do contexto, permissão, auditado | ✅ (contexto + MANAGER/ADMIN + auditado) |
| RH-20 | Faltas | Falta não remunerada num período | Desconta no recibo desse período | ✅ (UNJUSTIFIED desconta + teste) |
| RH-21 | Faltas | Lançar/eliminar falta | Exige permissão, auditado | 🟡 (sem permissão/auditoria) |
| RH-22 | Despesa | Despesa aprovada → reembolso | Saída de tesouraria automática | ✅ |
| RH-23 | Mapa fiscal | Gerar mapa INSS/IRPS do mês | Totais por colaborador, **via API + PDF** | ✅ (endpoint + PDF + botão) |
| RH-24 | Config | Editar escalão IRPS / taxa INSS | Persistido e aplicado, **via API/UI** | ✅ (endpoints GET/POST + UI) |
| RH-25 | Tenant | Utilizador de outra empresa lê RH | Acesso negado (403) | ✅ |

## Estado dos testes automatizados

Actual: `HRServiceTest` (4) + `PayrollTaxServiceTest` (2) = **6 testes**. Alvo abaixo.

- [ ] `PayrollTaxServiceTest`: um caso por escalão IRPS, dedução por dependente, INSS trab./patronal,
      arredondamento, ausência de config vigente. **(≈8)**
- [ ] `HRServiceTest` — recibos: emissão, anti-duplicação, marcar pago → tesouraria, cancelar pago
      bloqueado, processamento mensal idempotente, desconto de faltas. **(≈8)**
- [ ] `HRServiceTest` — colaborador: unicidade nº/email, fim de contrato < admissão, role gate. **(≈4)**
- [ ] `VacationServiceTest` (novo): saldo, bloqueio acima do saldo, decisor do contexto, auditoria. **(≈5)**
- [x] `PayrollBonusServiceTest` (novo, 6): 13.º mês proporcional ao tempo de serviço, exclusão de
      admitidos após o ano, subsídio de férias por valor/dia, bloqueio se férias não aprovadas,
      pagamento do 13.º idempotente (não duplica), subsídio já pago bloqueado.
- [x] `HRApiIntegrationTest` (novo, 4): mapa fiscal e config de impostos por API — 401 sem token,
      403 cross-tenant, 200 autenticado.

Meta: módulo RH com cobertura comparável ao retail (~30+ testes próprios), `mvn test` verde.
Estado actual: **118 testes** no total, RH com `HRServiceTest` (7) + `PayrollTaxServiceTest` (2)
+ `HRApiIntegrationTest` (4) + `PayrollBonusServiceTest` (6) = **19 testes próprios de RH**.

## Punch list priorizada (file:line)

1. ~~**Auditoria em todo o RH**~~ — **feito (2026-06-22)**: `AuditLogService` injectado em `HRService`,
   audita `EMPLOYEE_CREATE/UPDATE/STATUS`, `PAYSLIP_ISSUE/PAID/CANCEL`, `PAYROLL_PROCESS` e
   `VACATION_DECISION`. *(RH-01,04,09,11,12,19)*
2. ~~**Numeração gapless do recibo**~~ — **feito (2026-06-22)**: `generatePayslipNumber` (timestamp)
   substituído por `documentNumberService.next(DocumentSeries.PAYSLIP)` (série `REC`). *(RH-09)*
3. ~~**Líquido do recibo → tesouraria**~~ — **feito (2026-06-22)**: `markPayslipPaid` regista saída
   via `FinanceService.registerAutoPayout` (refactor DRY a partir de `registerAutoExpensePayout`). *(RH-11)*
4. ~~**Expor mapa fiscal + config de impostos**~~ — **feito (2026-06-22)**: `HRController` expõe
   `GET /api/hr/payroll/fiscal-summary/{year}/{month}`, `GET/POST /api/hr/payroll/tax-config`;
   novo `PayrollFiscalMapPrintService` (PDF do mapa INSS/IRPS) + botão "Imprimir Mapa Fiscal" na aba
   "IRPS & INSS Salarial" do `FiscalPanel`; coberto por `HRApiIntegrationTest`. *(RH-23,24)*
5. ~~**Férias: saldo + decisor seguro**~~ — **feito (2026-06-22)**: direito anual
   `DEFAULT_ANNUAL_VACATION_DAYS=22`, saldo = direito − reservados (`VacationRepository.sumReservedDays`),
   `submitVacation` bloqueia acima do saldo; `decideVacation(id, approve, reason)` passou a exigir
   `ensureHrManager`, resolver o decisor por `CurrentUserContext.getUsername()` e exigir motivo na
   rejeição. Controller e `HRPanel` actualizados. Testes: `HRServiceTest` (+2). *(RH-17,18,19)*
6. ~~**Faltas não remuneradas descontam no recibo**~~ — **feito (2026-06-22)**: novo campo
   `Payslip.absenceDeduction` (migration `V10`); `createPayslip` desconta faltas `UNJUSTIFIED` que se
   sobrepõem ao mês (valor/dia = salário base / 30), incluído no líquido, no `PayslipDTO` e na tabela
   de descontos do PDF. Testes: `HRServiceTest` (+1). *(RH-20)*
7. ~~**13.º mês e subsídio de férias**~~ — **feito (2026-06-22)**: novo `PayrollBonusService`
   (`thirteenthMonth(year)` proporcional; `vacationAllowance(vacationId)` por valor/dia) + endpoints
   `GET/POST /api/hr/payroll/thirteenth-month/{year}` e `/payroll/vacation-allowance/{vacationId}`.
   **Pagamento persistido e idempotente**: nova entidade `PayrollBonus` (migration `V11`, unique
   `employee+type+year+reference`), `payThirteenthMonth`/`payVacationAllowance` exigem MANAGER/ADMIN,
   registam saída de tesouraria (`registerAutoPayout`) e auditam (`BONUS_13TH_PAY`/`BONUS_VACATION_PAY`).
   `PayrollBonusServiceTest` (6). *(RH-15, RH-16)*
8. **Cobertura de testes** — subir de 6 para ~30+ conforme lista acima. *(todos)*

## Verificação

```
mvn clean compile   → deve manter BUILD SUCCESS
mvn test            → alvo: todos os testes RH verdes
```
