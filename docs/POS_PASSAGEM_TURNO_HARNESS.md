# HARNESS-POS-PASSAGEM-TURNO-001 — Matriz de Testes Automatizados de Passagem de Turno no POS

## 1. Objectivo

Garantir a verificação exaustiva e automatizada de todos os comportamentos de negócio, segurança de acesso, persistência e interface gráfica associados à funcionalidade de passagem de turno multi-operador e reconciliação parcial no POS.

---

## 2. Cobertura de Testes Automatizados

### 2.1 Backend Harness: `PosShiftHandoverHarnessTest`

| ID | Nome do Teste | Cenário de Teste | Asserções Principais |
|---|---|---|---|
| **SH-01** | `testSuccessfulShiftHandoverExactAmount` | Operador A abre sessão com 200 MT, efectua venda de 800 MT (esperado = 1000 MT), e transfere para operador B com 1000 MT contados e detalhe de denominações. | - `ShiftReconciliationDTO` retornado com saldo esperado 1000, contado 1000 e diferença 0.<br>- `till_sessions.status` permanece `OPEN`.<br>- `till_sessions.current_operator` torna-se `operadorB`.<br>- `getActiveSession("operadorB")` localiza a sessão.<br>- `getActiveSession("operadorA")` deixa de retornar a sessão como activa.<br>- `buildZReport` inclui a reconciliação e `currentOperator = "operadorB"`. |
| **SH-02** | `testShiftHandoverWithCashDifference` | Operador A passa a sessão com saldo contado inferior ao esperado (-20 MT de quebra) com autorização de perfil ADMIN/MANAGER. | - Diferença negativa registada correctamente (-20 MT).<br>- Reconciliação gravada com observações explicativas.<br>- Handover concluído com sucesso. |
| **SH-03** | `testShiftHandoverValidations` | Testa violações de integridade semântica: mesmo operador, operador de saída errado, operador de entrada em branco e tentativa em sessão já fechada. | - Lança `BusinessRuleException` com mensagens claras em todos os 4 casos de erro. |
| **SH-04** | `testMultipleConsecutiveHandovers` | Sessão diária com múltiplas passagens consecutivas (Operador 1 $\rightarrow$ Operador 2 $\rightarrow$ Operador 3). | - Histórico cronológico `getShiftReconciliations` contém os 2 handovers na ordem correcta.<br>- `currentOperator` final é `operador3`. |

### 2.2 Z-Report Harness: `PosZReportHarnessTest`

| ID | Nome do Teste | Cenário de Teste |
|---|---|---|
| **ZR-01** | `testZReportReconciliationExpectedCash` | Cálculo de esperado em sessão aberta com suprimentos, sangrias e devoluções. |
| **ZR-02** | `testZReportClosedSessionWithDifference` | Reconciliação final de caixa com divergência. |
| **ZR-03** | `testZReportBlindCloseWithDenominationsAndNotes` | Fecho cego com discriminação de notas/moedas e observações. |
| **ZR-04** | `testSessionsHistoryQuery` | Consulta de histórico consolidado de sessões. |

### 2.3 Desktop Context & Ergonomia UI

| Harness / Teste | Ficheiro | Validação |
|---|---|---|
| **DCT-01** | `DesktopThinContextTest` | Inicialização sem falhas de todos os painéis e controladores no `MainFrame` para perfis normais e super admin. |
| **MMA-01** | `MultiModuleArchitectureHarnessTest` | Estrita conformidade das fronteiras físicas entre `contracts`, `backend` e `desktop`. |
| **KEY-05** | `PosKeyboardShortcutsHarnessTest` | Verificação de limite estrito de < 1000 linhas em ficheiros Swing (`POSPanel.java` com 990 linhas). |
| **CONV-04** | `QuotationConversionUiHarnessTest` | Verificação de integridade estrutural e contagem de linhas em formulários e modais do POS. |

---

## 3. Comandos de Execução

```powershell
# Execução dos harnesses de turno e Z-report no backend:
mvn test -pl backend -Dtest="PosShiftHandoverHarnessTest,PosZReportHarnessTest"

# Execução do teste canónico de inicialização do Desktop:
mvn test -pl desktop -Dtest="DesktopThinContextTest"

# Execução de fronteiras físicas de arquitectura:
mvn test -pl backend -Dtest="MultiModuleArchitectureHarnessTest"
```
