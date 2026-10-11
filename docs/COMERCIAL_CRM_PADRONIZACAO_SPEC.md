# Especificação Técnica de Padronização — Fase 4: Gestão Comercial, CRM & Histórico POS

## 1. Contexto e Diagnóstico

Na auditoria exaustiva do sistema, identificaram-se os últimos ecrãs com filtros empilhados (`GridBagLayout`) e sem calibração/Quick Peek nos módulos Comercial, CRM e Histórico POS.

### Painéis Diagnosticados
1. **Clientes** (`ClientesPanel.java`): Barra de pesquisa empilhada em 2 linhas com `GridBagLayout`; tabela sem calibração `ensureHeadersFit` e sem drawer Quick Peek.
2. **Promoções** (`PromotionsPanel.java`): Filtros de tipo e estado com rótulos acima dos combos em `GridBagLayout`; tabela sem `ensureHeadersFit` e sem Quick Peek.
3. **Contas Correntes de Clientes** (`OutstandingAccountsPanel.java`): Filtros empilhados com labels superiores; tabela sem `ensureHeadersFit` e sem Quick Peek.
4. **Notas Comerciais** (`CommercialNotesPanel.java`): Filtros de notas de crédito e débito com `GridBagLayout`; tabelas sem `ensureHeadersFit`.
5. **Movimentos Comerciais Unificados** (`CommercialMovementsPanel.java`): Filtros de documentos comerciais com `GridBagLayout`; tabela sem `ensureHeadersFit`.
6. **Histórico de Vendas POS** (`PosSalesHistoryPanel.java`): Filtro de estado, período e pesquisa em 2 linhas `GridBagLayout`; tabela sem `ensureHeadersFit`.

---

## 2. Requisitos Canónicos da Fase 4

1. **Eliminação de Filtros Verticais:** Substituição integral de `new JPanel(new GridBagLayout())` por `TableFilter.bar(...)` em linha horizontal compacta (ganho de 40px-60px verticais por ecrã).
2. **Calibração de Colunas:** Inclusão de `UIHelper.ensureHeadersFit(table)` em todos os 6 painéis.
3. **Instalação de Quick Peek:** Inclusão de `TableQuickPeekController` ativado por atalho de teclado `Espaço` e botão toggle no topo do card.
4. **Preservação de Integridade:** `DesktopThinContextTest` e `UiPanelDecompositionTest` 100% verdes.

---

## 3. Matriz de Ficheiros e Ações

| Painel | Localização | Ações Canónicas |
| :--- | :--- | :--- |
| `ClientesPanel` | `gui/ClientesPanel.java` | `TableFilter.bar`, `ensureHeadersFit`, Quick Peek (Ficha do Cliente). |
| `PromotionsPanel` | `gui/PromotionsPanel.java` | `TableFilter.bar`, `ensureHeadersFit`, Quick Peek (Detalhes da Promoção). |
| `OutstandingAccountsPanel` | `gui/commercial/OutstandingAccountsPanel.java` | `TableFilter.bar`, `ensureHeadersFit`, Quick Peek (Extrato do Cliente). |
| `CommercialNotesPanel` | `gui/commercial/CommercialNotesPanel.java` | `TableFilter.bar`, `ensureHeadersFit`. |
| `CommercialMovementsPanel` | `gui/commercial/CommercialMovementsPanel.java` | `TableFilter.bar`, `ensureHeadersFit`. |
| `PosSalesHistoryPanel` | `gui/PosSalesHistoryPanel.java` | `TableFilter.bar`, `ensureHeadersFit`, Quick Peek. |

---

## 4. Testes de Harness Automatizados

O teste `mz.multicore.erp.gui.CommercialCrmUnifiedHarnessTest` no módulo `desktop` assegura:
- `COMM-01`: `ClientesPanel` e `PromotionsPanel` seguem linha horizontal `TableFilter.bar`, sem `GridBagLayout` e com `ensureHeadersFit`.
- `COMM-02`: `OutstandingAccountsPanel`, `CommercialNotesPanel` e `CommercialMovementsPanel` em `gui/commercial` utilizam `TableFilter.bar` e `ensureHeadersFit`.
- `COMM-03`: `PosSalesHistoryPanel` utiliza `TableFilter.bar` e `ensureHeadersFit`.
