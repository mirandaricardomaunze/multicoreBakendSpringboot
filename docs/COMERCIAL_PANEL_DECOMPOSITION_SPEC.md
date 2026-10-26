# Especificação Canónica — Decomposição Modular do Painel Comercial (ComercialPanel)
**Código:** `SPEC-COMERCIAL-PANEL-DECOMPOSITION-001`  
**Data:** 2026-10-23  
**Status:** APROVADO  
**Alvo:** `desktop/src/main/java/mz/multicore/erp/gui/ComercialPanel.java` (975 linhas $\rightarrow \le 420$ linhas)

---

## 1. Diagnóstico e Objectivos

### 1.1 Diagnóstico
O ficheiro `ComercialPanel.java` possui atualmente 975 linhas (o maior painel operacional do ERP). Acumula:
1. Orquestração das 10 abas comerciais (`Cotações`, `Faturação`, `Pedidos & Separação`, `Recibos`, `Guias`, `Notas de Crédito`, `Notas de Débito`, `Contas Correntes`, `Promoções`, `Movimentos`).
2. Ciclo de carregamento assíncrono de metadados (`clients`, `products`, `warehouses`, `businessUnits`).
3. Operações de faturação (paginação de faturas, verificação remota de vias impressas, liquidação e anulação).
4. Operações de encomendas e separação (paginação, conversão em guia/transferência/faturação, estado do editor e cache).
5. Coordenação de impressões e exportações sensíveis (térmica 80mm vs A4, guias, folhas de separação).

### 1.2 Invariantes e Restrições Não-Negociáveis
1. **Contrato de Abas e Largura:** As 10 abas corporativas devem ser preservadas com os mesmos títulos e ícones validados por `TabStripFitsTest` ("Cotações", "Faturação", "Pedidos & Separação", etc.).
2. **Âncoras de Código Fonte nos Testes Homologados:**
   - `DocumentConversionWorkflowHarnessTest`: `convertSelectedOrderToGuide`, `convertSelectedOrderToTransfer`, `billSelectedOrder`.
   - `PhcDocumentEditorHomologationHarnessTest`: `stopInvoiceCellEditing()`.
   - `OrderEditorHarnessTest`: `openOrderEditor()`, `openSelectedOrderEditor()`, `CommercialOrderEditorActions.openSelected(this)`, `addDraftOrderLine()`, `removeSelectedDraftOrderLine()`.
   - `SellerExperienceHarnessTest`: `boolean sellerView` e `if ("SELLER".equalsIgnoreCase(CurrentUserContext.getRole())) return;`.
   - `ProfessionalFeedbackHarnessTest`: `optionPaneCalls(gui.resolve("ComercialPanel.java"))` estritamente zero.
3. **Limite Físico de Linhas:**
   - `ComercialPanel.java` $\le 450$ linhas (meta $\le 420$ linhas).
   - Componentes extraídos $\le 250$ linhas cada.

---

## 2. Arquitetura da Solução

### 2.1 Componentes a Extrair

| Componente | Responsabilidade | Linhas Estimadas |
|---|---|---|
| `CommercialInvoiceOperations.java` | Paginação de faturas, status de impressão sensível, cancelamento, liquidação e diálogo de formato de impressão (A4 vs Térmico) | $\sim 170$ |
| `CommercialOrderOperations.java` | Paginação de encomendas, conversões de workflow (guia, transferência, faturação), formatação de entrega e cache de encomendas | $\sim 190$ |
| `CommercialMetadataCoordinator.java` | Carregamento assíncrono e povoamento de combos de clientes, produtos, armazéns e unidades de negócio | $\sim 90$ |

### 2.2 Orquestrador Limpo (`ComercialPanel.java`)
Mantém o `JTabbedPane` com os 10 separadores corporativos, cartões de topo, os delegados públicos e os manipuladores de eventos sem lógica de infraestrutura acumulada.
