# Especificação Canónica — Alertas e Orientação de Acções na Interface (GUI)

## 1. Contexto e Problema
No ambiente corporativo do Multicore ERP (desktop Swing), operadores executam tarefas de alta frequência (lançamento de vendas, produção, reconciliação bancária, gestão de frotas e inventário).
Identificou-se que em determinados painéis, ao clicar em botões de acção sem possuir uma linha seleccionada na tabela ou sem cumprir os pré-requisitos funcionais da operação, o sistema executava um retorno silencioso (`if (row < 0) return;` ou `if (code.isEmpty()) return;`), sem exibir qualquer feedback visual.
Este silêncio gera incerteza operacional: o utilizador não compreende se o botão está inativo, se a aplicação travou ou qual o passo que deve cumprir para avançar.

## 2. Princípios Canónicos Não-Negociáveis
1. **Zero Retornos Silenciosos:** Nenhuma acção iniciada pelo utilizador através de botão, atalho de teclado ou menu contextual pode falhar ou retornar sem emitir um alerta visual explicativo.
2. **Orientação Activa (O Quê + Próximo Passo):** Toda a mensagem de aviso (`FeedbackType.WARNING`) ou informação (`FeedbackType.INFO`) deve declarar com clareza o elemento em falta e a acção esperada em português de Moçambique.
   - *Mau:* `"Erro: linha inválida."` ou `""` (silêncio).
   - *Bom:* `"Seleccione uma viatura na tabela para consultar o histórico de abastecimentos e rendimento."`
3. **Mecanismo Padronizado de Feedback:** Utilizar estritamente o `ToastManager.show(parent, FeedbackType.WARNING, mensagem)` ou o componente `FeedbackPanel` / `showNotice` do painel hospedeiro.

## 3. Matriz de Cobertura e Mensagens Canónicas

| Módulo | Componente / Método | Condição de Falha | Mensagem Canónica de Orientação |
| :--- | :--- | :--- | :--- |
| **Produção & Fábrico** | `ManufacturingPanel.showOrderActions()` | `ordersTable.getSelectedRow() < 0` | ⚠️ *"Seleccione uma Ordem de Produção na tabela para gerir o seu estado ou imprimir etiquetas."* |
| **Gestão de Frotas** | `FleetPanel.showFuelHistory()` | `table.getSelectedRow() < 0` | ⚠️ *"Seleccione uma viatura na tabela para consultar o histórico de abastecimentos e rendimento."* |
| **Ativos & Património** | `AssetsPanel.showAssetDetails()` | `table.getSelectedRow() < 0` | ⚠️ *"Seleccione um ativo na tabela para inspecionar o plano e quotas de amortização."* |
| **Reconciliação Bancária** | `BankReconciliationPanel.unmatchSelectedItem()` | `itemsTable.getSelectedRow() < 0` | ⚠️ *"Seleccione um movimento bancário reconciliado para anular a conciliação."* |
| **Reconciliação Bancária** | `BankReconciliationPanel.openManualMatchDialog()` | `itemsTable.getSelectedRow() < 0` | ⚠️ *"Seleccione um item do extracto pendente para associar a uma transacção de tesouraria."* |
| **Reconciliação Bancária** | `BankReconciliationPanel.createDirectCashMovement()` | `itemsTable.getSelectedRow() < 0` | ⚠️ *"Seleccione a linha do extracto para lançar a despesa ou receita directa de tesouraria."* |
| **Inventário Físico** | `PhysicalInventoryPanel.handleBarcodeScan()` | `code.isEmpty() \|\| currentSession == null` | ⚠️ *"Indique ou bipe o código do produto e confirme a quantidade antes de registar a contagem."* |
| **Notas Comerciais** | `CommercialNotesPanel` (modal débito/crédito) | `lineTable.getSelectedRow() < 0` | ⚠️ *"Seleccione na grelha a linha que pretende remover da nota."* |
| **Ponto de Venda (POS)** | `POSPanel.captureWeight()` / `scaleWidget` | `cartItems.isEmpty()` ou sem artigo pesável | ⚠️ *"Adicione ou seleccione um artigo de peso na carrinha para aplicar a pesagem da balança."* |
| **Ponto de Venda (POS)** | `POSPanel` (Atalho `F10` - Finalizar) | `cartItems.isEmpty()` | ⚠️ *"Adicione artigos ao carrinho antes de finalizar a venda (F10)."* |
| **Inspetor de Registos** | `RecordDetailsDialog.showDetails()` | `table.getSelectedRow() < 0` | ℹ️ *"Seleccione uma linha na tabela para inspecionar os detalhes completos."* |

## 4. Testes e Validação Obrigatória
Toda a alteração deve ser acompanhada pelo teste de harness `GuiActionAlertGuidanceHarnessTest` no módulo `desktop`, garantindo que:
1. Invocações sem selecção despoletam feedback registado no `ToastManager` ou no `FeedbackPanel` do componente.
2. A aplicação continua a compilar e passa no teste de contexto fino (`DesktopThinContextTest`).
