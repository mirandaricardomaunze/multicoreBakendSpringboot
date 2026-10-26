# Especificação — Decomposição Canónica do Painel de Ponto de Venda (POS)
**Código:** `SPEC-POS-PANEL-DECOMPOSITION-001`  
**Módulo:** `desktop` (`mz.multicore.erp.gui` / `mz.multicore.erp.gui.pos`)  
**Data:** 2026-10-23  

---

## 1. Contexto e Diagnóstico

O painel `POSPanel.java` atingiu **998 linhas**, situando-se a apenas 2 linhas do limite máximo estrito do sistema (`MAX_PANEL_LINES = 1000`) e configurando acoplamento excessivo de responsabilidades:
1. **Montagem de Interface Monolítica (385 linhas):** Construção em bloco do cabeçalho de turnos/balança, barra de seletores de cliente/armazém/conta, catálogo com paginação e grelha do carrinho com resumo de totais.
2. **Orquestração de Checkout (106 linhas):** Validação de pré-voo de crédito, recolha de cliente avulso com nome obrigatório, chamada de diálogo de pagamento, envio com chave de idempotência e chave de contingência offline.
3. **Gestão do Carrinho e Cálculos de IVA (98 linhas):** Totalização de carrinho, formatação de subtotais e taxas efetivas de IVA, edição modal de quantidades e alternância de estado vazio (*empty state*).

---

## 2. Metas de Linhas e Arquitetura Canónica

| Componente | Ficheiro | Responsabilidade | Teto de Linhas |
| :--- | :--- | :--- | :--- |
| **Orquestrador POS** | `POSPanel.java` | Coordenação central, exposição de campos para retrocompatibilidade com atalhos de teclado e testes | <= 400 linhas |
| **Orquestrador de Checkout** | `PosCheckoutCoordinator.java` | Pré-voo de crédito, prompt de cliente avulso, recolha de pagamento, envio assíncrono e contingência | <= 200 linhas |
| **Controlador de Carrinho** | `PosCartController.java` | Recálculo de totais, impostos, quantidades, atalhos de remoção e alternância de empty state | <= 180 linhas |
| **Construtor do Espaço de Trabalho** | `PosWorkspaceBuilder.java` | Construção declarativa da barra superior, barra de seletores, divisão em split pane e grelha do carrinho | <= 260 linhas |

---

## 3. Regras Arquiteturais Não-Negociáveis

1. **Retrocompatibilidade de Construtores e Métodos Públicos:**
   - Todos os construtores sobrecarregados de `POSPanel` devem manter-se exatamente iguais:
     - `POSPanel(POSApiClient, ComercialApiClient, InventoryApiClient, FinanceApiClient, PromotionApiClient, ScaleBarcodeParser)`
     - `POSPanel(..., PrintApiClient)`
     - `POSPanel(..., PosContingencyManager, PosContingencySyncService)`
     - `POSPanel(..., PosContingencyManager, PosContingencySyncService, PrintApiClient)`
   - Métodos acedidos por atalhos de teclado e testes (`runCheckout()`, `clearCart()`, `editSelectedCartQuantity()`, `removeFromCart()`, `showReturnDialog()`, `isHistoryView()`, `effectiveTaxRate()`) devem permanecer inalterados.
2. **Campos Package-Private Preservados:**
   - Todos os elementos Swing testados por `PosErgonomicsHarnessTest` e `PosHeaderLayoutHarnessTest` (`cartTable`, `cartModel`, `totalLabel`, `subtotalValueLabel`, `ivaValueLabel`, `clientCombo`, `warehouseCombo`, `accountCombo`, `clientSearchField`, `barcodeField`, `productGrid`, `sessionBanner`, `statusLabel`, `creditCheck`, `checkoutBtn`, etc.) continuam como campos em `POSPanel`.
3. **Validação de Cliente Avulso:**
   - A exigência de nome obrigatório via `UIHelper.promptRequiredText("Identificação do Cliente", ...)` e mensagem `"É obrigatório indicar o nome do cliente para efetuar a venda no POS."` deve ser estritamente preservada (conforme `PosCustomerRequirementHarnessTest`).

---

## 4. Critérios de Aceitação

- [x] `POSPanel.java` reduzido para menos de 450 linhas (meta ideal <= 400 linhas).
- [x] Suíte de testes POS (`PosErgonomicsHarnessTest`, `PosHeaderLayoutHarnessTest`, `PosProfessionalErgonomicsHarnessTest`, `PosCustomerRequirementHarnessTest`, `PosKeyboardShortcutsHarnessTest`, `POSKeyboardShortcutTest`) 100% verde.
- [x] Teste de inicialização de UI (`DesktopThinContextTest`) e auditorias (`TableCardContainmentAuditTest`, `MultiModuleArchitectureHarnessTest`) 100% verdes.
- [x] Novo harness `PosPanelDecompositionHarnessTest` validando a decomposição física e limites de linhas.
