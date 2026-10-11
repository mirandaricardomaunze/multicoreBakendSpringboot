# SPEC — Segurança de Faturação Única, Notas de Crédito/Débito e Reimpressão de Documentos Sensíveis

**Criado em:** 2026-10-11  
**Módulos:** `contracts`, `backend` (`mz.multicore.erp.modules.comercial`, `mz.multicore.erp.modules.printing`), `desktop` (`mz.multicore.erp.gui`, `mz.multicore.erp.gui.components`, `mz.multicore.erp.gui.commercial`)  
**Contexto:** Garantir integridade transacional contra dupla faturação de encomendas, alçadas e limites de notas de crédito/débito, visibilidade do estado de impressão de cada documento e segregação estrita de funções na emissão de segundas vias.

---

## 1. Diagnóstico e Objetivos de Segurança

1. **Risco de Dupla Faturação de Encomendas:**
   - Numa operação de múltiplos postos/caixas, requisições concorrentes ou duplo clique em "Faturar" podiam tentar faturar a mesma encomenda mais de uma vez.
   - É obrigatório que o carregamento da encomenda na transação use bloqueio pessimista (`PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`), verifique atomicamente se `invoiceId != null` ou `status == "BILLED"`, e recuse categoricamente a operação com mensagem explícita em PT-MZ.
   - O desktop deve impedir visualmente a seleção ou submissão de encomendas já faturadas.

2. **Integridade de Notas de Crédito e Notas de Débito:**
   - Notas de crédito e débito devem estar estritamente vinculadas a uma fatura comercial válida da empresa activa.
   - O valor acumulado das notas de crédito emitidas contra uma fatura não pode exceder o valor total da fatura.
   - A quantidade a devolver de cada artigo não pode exceder a quantidade vendida na fatura deduzida das devoluções anteriores.
   - Apenas utilizadores com alçada (`MANAGER` ou `ADMIN`) podem aprovar notas de crédito ou débito.
   - Devolução física de stock só se concretiza na aprovação da nota de crédito do tipo devolução (`RETURN`).
   - Notas de crédito ou débito já aprovadas são imutáveis e não podem ser canceladas nem rejeitadas.

3. **Visibilidade do Histórico de Impressão:**
   - Todos os documentos com impacto financeiro, stock ou vínculo fiscal (Faturas, Encomendas, Notas de Crédito, Notas de Débito, Guias de Remessa/Transporte, Recibos) devem ter o seu estado de impressão auditável e visível nas respetivas grelhas (`Não impressa`, `1 via (dd/MM/yyyy HH:mm)` ou `N vias`).
   - O painel QuickPeek e o rodapé devem expor de forma clara quantas vias já foram impressas.

4. **Segregação de Funções e Controlo de 2ª Via:**
   - A primeira via é emitida normalmente pelo operador responsável.
   - Qualquer via posterior é classificada como **REIMPRESSÃO** e é bloqueada no endpoint normal de impressão.
   - A reimpressão exige autorização presencial de **outro** utilizador pertencente à empresa com perfil `MANAGER` ou `ADMIN`, além da indicação obrigatória de motivo e terminal.
   - **Regra de ouro de segregação:** O operador com sessão aberta no sistema não pode autorizar a sua própria reimpressão, nem no cliente desktop nem no backend (`fail-closed`).
   - O documento reimpresso recebe marca de água/carimbo visível indelével `REIMPRESSÃO — n.ª VIA` em todas as páginas, e o evento é registado no log de auditoria forense.

---

## 2. Regras e Arquitetura

### R1. Faturação Única e Bloqueio Pessimista (`ComercialService.billOrder`)
- Ao faturar uma encomenda, o backend executa:
  ```java
  Order order = orderRepository.findForUpdate(orderId, companyId)
          .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada."));
  ```
- Validação atómica prévia:
  ```java
  if (order.getInvoiceId() != null || "BILLED".equalsIgnoreCase(order.getStatus()) || "INVOICED".equalsIgnoreCase(order.getStatus())) {
      throw new BusinessRuleException("A encomenda " + order.getOrderNumber()
              + " já foi faturada anteriormente"
              + (order.getInvoiceId() != null ? " (Fatura ID: " + order.getInvoiceId() + ")" : "")
              + ". Não é permitido faturar a mesma encomenda novamente.");
  }
  ```
- Ao persistir a fatura, a encomenda é atualizada no mesmo commit: `order.setInvoiceId(invoice.getId())`, `order.setStatus("BILLED")`.

### R2. Integridade e Limites de Notas de Crédito e Débito (`CreditNoteService`, `DebitNoteService`)
- A nota de crédito exige fatura existente pertencente à empresa activa do contexto.
- Linhas de nota de crédito devem referenciar linhas originais da fatura (`invoiceLineId`).
- A soma das devoluções já realizadas para a linha da fatura mais a nova quantidade não pode ultrapassar a quantidade faturada.
- O total da nota de crédito não pode fazer o valor acumulado creditado superar o valor total da fatura.
- A aprovação exige perfil `MANAGER` ou `ADMIN` via `PermissionGuard.requireManagerOrAdmin`.
- Notas de crédito e débito aprovadas geram evento de domínio e auditoria e não admitem cancelamento posterior.

### R3. Rastreabilidade Universal de Impressão (`document_print_controls`)
- A tabela central `document_print_controls` mapeia `(company_id, document_type, document_id)`.
- As listagens no desktop (`ComercialPanel`, `CommercialNotesPanel`, `PosSalesHistoryPanel`) consultam o estado de impressão através de `PrintApiClient.getPrintStatuses(type, ids)` e mostram a contagem de vias na coluna `Impressões`.
- A primeira chamada a `firstPrint` incrementa a contagem para `1`, regista data, operador e terminal.
- Uma segunda chamada direta a `firstPrint` lança `BusinessRuleException: "Este documento já foi impresso. Use Reimprimir e obtenha autorização de outro gerente ou administrador."`.

### R4. Segregação de Funções na Reimpressão (`SensitivePrintCoordinator`, `SensitiveDocumentPrintService`)
- Reimpressão requer `POST /api/print/{type}/{documentId}/reprint` com:
  - `approverUsername`
  - `approverPassword`
  - `reason` (não vazio)
  - `terminalName`
- **Validação de Segregação (Backend & Desktop):**
  - Se `CurrentUserContext.getUsername().equalsIgnoreCase(approverUsername.trim())`, lança `BusinessRuleException: "A reimpressão exige autorização de outro utilizador (não pode autorizar a sua própria reimpressão)."`
- O autorizador deve ser autenticado contra `AppUserService` e ter `MANAGER` ou `ADMIN` na empresa activa.
- O PDF gerado recebe carimbo com o número da via através de `PdfReprintMarker.mark(...)`.
- O log de auditoria grava `SENSITIVE_DOCUMENT_REPRINT` com os nomes do operador, do autorizador e o motivo.

---

## 3. Critérios de Sucesso e Validação

1. Concorrência e bloqueio: tentar faturar a mesma encomenda mais de uma vez falha de forma determinística com `BusinessRuleException`.
2. O desktop informa clara e imediatamente caso uma encomenda já tenha sido faturada.
3. As grelhas de Notas de Crédito e Débito mostram a coluna `Impressões` e integram com o `SensitivePrintCoordinator`.
4. A tentativa de um operador autorizar a sua própria reimpressão é bloqueada tanto no formulário desktop quanto no backend.
5. Todos os testes unitários e de integração no harness passam com 100% de sucesso.
