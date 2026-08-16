# Recebimentos e saldo em dívida — harness

Cenários de verificação da [RECEBIMENTOS_SALDO_SPEC.md](RECEBIMENTOS_SALDO_SPEC.md).
**RP-01..RP-23** automáticos; **RP-50..RP-56** manuais (com o backend de pé).

---

## Automáticos

Todos os cenários marcados ✅ foram **confirmados a falhar contra o código antigo** antes do
fix — a saída da execução está transcrita em §3.

### `ComercialServiceTest` — emissão de recibo

| ID | Cenário | Esperado | Falhava antes |
|---|---|---|---|
| RP-01 | Recibo de 100 numa fatura de 232 | `PARTIALLY_PAID`, `amountPaid = 100.00` | ✅ dava `PAID` |
| RP-02 | 2.º recibo de 132 completa o saldo | `PAID`, `amountPaid = 232.00` | ✅ recusava o 2.º recibo |
| RP-03 | Recibo de 500 numa fatura de 232 | recusa; sem movimento de tesouraria | ✅ aceitava |
| RP-04 | Recibo de valor zero | recusa; sem `Receipt` gravado | ✅ aceitava |
| RP-05 | Recibo do valor total | `PAID`; tesouraria recebe 232 | — (não-regressão) |
| RP-06 | Anular o 1.º de dois recibos (100 + 132) | `amountPaid = 132.00`, volta a `PARTIALLY_PAID` | ✅ voltava a `APPROVED` cego |

### `ReportServiceTest` — leitura

| ID | Cenário | Esperado | Falhava antes |
|---|---|---|---|
| RP-10 | Fatura de 1000 com 400 recebidos | dashboard por cobrar = **600** | ✅ dava 0 (ignorava `PARTIALLY_PAID`) |
| RP-11 | Fiado 500 + parcial (300/100) + paga + anulada | por cobrar = **700** | ✅ |
| RP-12 | Venda paga (100) + fiado (250) | vendas de hoje = **2 / 350** | ✅ contava só a paga |
| RP-13 | Por aprovar, anulada e rejeitada no mesmo dia | não contam como venda | — (guarda contra excesso) |
| RP-14 | Mesmo conjunto no dashboard e no relatório diário | **contagem e total iguais** | ✅ 1 vs 3 |

### `FinanceServiceTest` — tesouraria

| ID | Cenário | Esperado | Falhava antes |
|---|---|---|---|
| RP-20 | `payInvoice` com perfil EMPLOYEE | recusa; fatura intacta; sem transacção | ✅ deixava passar |
| RP-21 | `payInvoice` com perfil MANAGER | `PAID`; entra 1000 | ✅ (contexto do guard) |
| RP-22 | `payInvoice` de fatura com 400 de 1000 já pagos | entra **600**, não 1000 | ✅ recusava `PARTIALLY_PAID` |
| RP-23 | `payInvoice` de fatura já paga | recusa | — (não-regressão) |

### Não-regressão do POS

`POSServiceTest` (19) verde depois de `deriveStatus` ter sido substituído por
`Invoice.deriveStatusFromPayments()` — a extracção da regra não mudou o comportamento do POS,
que já era o correcto.

---

## Manuais — ✅ **VALIDADOS AO VIVO** (2026-08-09)

Backend `mvn spring-boot:run` (perfil default, H2 em memória, dados de demonstração), percurso
HTTP completo com `ana`/ADMIN e `maria`/EMPLOYEE. Fatura **FT-2026/1** de **950,00** (10 × 95,00
de artigo isento — confirma de passagem que a taxa é a do artigo e não a do pedido, que insistia
em 16%). Conta de tesouraria a abrir em **18.464,50**.

| ID | Passos | Esperado | Resultado ao vivo |
|---|---|---|---|
| RP-50 | Emitir fatura de 950,00 | `APPROVED`, nada pago | `FT-2026/1` · `950.00` · `APPROVED` ✅ |
| RP-51 | Recibo parcial de `400` | `PARTIALLY_PAID`, 400 pagos | HTTP 200, `RC-2026/1`; fatura `400.00` · `PARTIALLY_PAID` ✅ |
| RP-53 | Recibo de `700` sobre saldo de `550` | recusa; fatura intacta | HTTP **400** — *"Valor do recibo (700) excede o saldo em dívida (550.00)."*; fatura continua `400.00` · `PARTIALLY_PAID` ✅ |
| RP-52 | Recibo dos `550` restantes | `PAID`; tesouraria +950 no total | `RC-2026/2`; fatura `950.00` · `PAID`; tesouraria `19.414,50` (= 18.464,50 + 400 + 550) ✅ |
| RP-54 | Anular o recibo de `400` | volta a `PARTIALLY_PAID` com 550; estorno de 400 | HTTP 204; fatura `550.00` · **`PARTIALLY_PAID`** (não `APPROVED`); tesouraria `19.014,50` ✅ |
| RP-55 | Dashboard vs Relatório Diário vs Contas Correntes | os três de acordo | dashboard `1 / 950.00` + por cobrar `400.00`; diário `1 / 950.00` + `400.00`; contas correntes `FT-2026/1` com `550.00` de `950.00` ✅ |
| RP-56 | EMPLOYEE tenta `POST /api/finance/pay-invoice` | recusa por permissão | HTTP 400 — *"Sem permissão para liquidar fatura. Esta operação requer perfil MANAGER ou ADMIN."* ✅ |
| RP-57 | ADMIN liquida a fatura com 550 de 950 já pagos | entra só o saldo (400) | tesouraria `19.014,50` → `19.414,50`; extracto: *"Recebimento Fatura FT-2026/1"* de **`400.00`**; fatura `PAID` ✅ |

### Falta validar na UI Swing

O percurso acima foi por HTTP (a UI é cliente-fino e chama exactamente estes endpoints). Por
confirmar no desktop: coluna **Em Dívida**, o aviso *"Continuam por receber N MT"* e o valor
sugerido no diálogo de liquidação.

---

## 2b. Bug encontrado **durante** esta validação (e corrigido)

RP-51 e RP-52 devolviam **HTTP 500** apesar de gravarem o recibo e actualizarem a fatura:

```
org.hibernate.LazyInitializationException: could not initialize proxy
  [com.phcpro.modules.comercial.model.Client#1] - no Session
  at ComercialService.toDTO(ComercialService.java:479)
```

**Causa:** o `ComercialController` chamava `comercialService.toDTO(...)` **fora** da transacção;
com `spring.jpa.open-in-view=false` o proxy lazy do cliente já não tinha sessão. Pré-existente e
independente dos fixes de saldo (o `toDTO` não foi tocado por eles) — mas **agravado** por eles:
antes, a fatura ficava logo `PAID` e uma repetição era recusada; depois, a fatura continua
cobrável, pelo que o utilizador que visse o erro e repetisse criaria um **segundo recibo** e
duplicaria a entrada de caixa.

**Correcção** (regras 3 e 4 do `CLAUDE.md` — *o Service não devolve `@Entity` para fora*,
*sempre DTO na fronteira*): `createReceipt` e `getReceiptsByCompany` passam a devolver
`ReceiptDTO`, convertido **dentro** da transacção; o controller deixou de mapear. `GET
/api/comercial/receipts` tinha o mesmo defeito latente e ficou fechado no mesmo passo.

---

## 3. Evidência — falha contra o código antigo

`mvn -o test -Dtest='ComercialServiceTest,ReportServiceTest,FinanceServiceTest'`, **antes** do fix:

```
Tests run: 43, Failures: 9, Errors: 3

ComercialServiceTest.createReceipt_pagamentoParcial...  expected: <PARTIALLY_PAID> but was: <PAID>
ComercialServiceTest.createReceipt_segundoRecibo...     BusinessRule: Apenas faturas no estado
                                                        APROVADA podem ter recibo. Estado atual: PAID
ComercialServiceTest.createReceipt_valorAcimaDoSaldo... Expected BusinessRuleException, nothing thrown
ComercialServiceTest.createReceipt_valorNaoPositivo...  Expected BusinessRuleException, nothing thrown
ComercialServiceTest.cancelReceipt_deReciboParcial...   BusinessRule: ... Estado atual: PAID
FinanceServiceTest.payInvoice_semPerfilAutorizado...    Expected BusinessRuleException, nothing thrown
FinanceServiceTest.payInvoice_faturaParcialmentePaga... BusinessRule: Apenas faturas no estado
                                                        APROVADA podem ser pagas. Estado atual: PARTIALLY_PAID
ReportServiceTest.dashboard_porCobrar_incluiParciais... por cobrar tem de ser o saldo em dívida
ReportServiceTest.dashboard_vendasDeHoje_incluiFiado... expected: <2> but was: <1>
ReportServiceTest.dashboard_eRelatorioDiario_mesmo...   expected: <1> but was: <3>
```

**Depois** do fix: `mvn -o clean test` → **371 testes, 0 falhas, 0 erros, 0 ignorados**
(eram 356 antes desta iteração).
