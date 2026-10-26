# SPEC-RECURRING-CONTRACTS-PANEL-DECOMPOSITION-001: Decomposição Modular de Contratos Recorrentes & Avenças

## 1. Visão Geral e Objectivo
O painel `RecurringContractsPanel.java` (419 linhas) é responsável pela gestão de avenças periódicas, contratos recorrentes, apuramento de receita fixa e execução de ciclos de faturação em lote (*Batch Billing*).
O objetivo desta decomposição é desacoplar o diálogo modal de registo de contrato e as operações de ciclo de vida / faturação em lote para colaboradores especializados, eliminando o uso residual de `JOptionPane.showOptionDialog` e reduzindo o orquestrador para $\le 250$ linhas.

## 2. Invariantes Arquiteturais e Restrições Não Negociáveis
1. **Camadas Físicas**: Módulo `desktop` depende exclusivamente de `contracts` e consome serviços via HTTPS (`RecurringContractApiClient`). Proibido JPA, repositórios ou dependências circulares.
2. **Contratos Visuais e de Teste**:
   - Construtor público canónico: `public RecurringContractsPanel(RecurringContractApiClient contractApiClient)`.
   - Método de ciclo de vida do `MainFrame`: `public void onPanelSelected()`.
   - Layout:
     - Header com `InlineFeedbackPanel` e título via `UIHelper.createHeading("Contratos & Facturação Recorrente")`.
     - Grelha uniforme de 4 KPIs via `KpiCard.createGrid(4)`.
     - Card único `ModernPanel(16)` contendo a barra de filtros canónica e botões através de `UIHelper.tableCardTop`.
     - Tabela estilizada com 12 colunas operacionais e `UIHelper.ensureHeadersFit(table)`.
     - Renderizador semafórico de estado via `TableCellRenderers.status()`.
     - Ação de detalhes rápidos / duplo clique via client property `customDetailAction`.
   - Eliminação estrita de 100% de chamadas residuais a `JOptionPane`.
   - Conformidade com a moeda e formatação moçambicana (`pt-MZ`).

## 3. Arquitetura de Componentes e Decomposição
A gestão de contratos recorrentes é decomposta em:

```
mz.multicore.erp.gui
├── RecurringContractsPanel.java (Orquestrador UI: <= 250 linhas)
├── RecurringContractFormDialog.java (Diálogo modal corporativo de registo: <= 110 linhas)
└── RecurringContractsOperationsCoordinator.java (Coordenador de facturação em lote e transições de estado: <= 95 linhas)
```

### 3.1 `RecurringContractFormDialog.java`
- Diálogo modal moderno construído sobre `ModernFormDialog` com validação atómica (`setOnSave`).
- Formulário ergonómico com seleção de cliente, descrição do serviço, frequência de faturação (`BillingFrequency`), dia de cobrança (1 a 31), valor monetário positivo, data de início e término e opção de renovação automática.
- Tratamento de foco automático em campos inválidos sem fechar a janela.

### 3.2 `RecurringContractsOperationsCoordinator.java`
- Método `runBatchBilling(...)`: Confirmação com o utilizador em `ModernMessageDialog.confirm` e execução assíncrona do ciclo de faturação periódica via `SwingWorker` com relatório do total faturado e avisos.
- Método `changeContractStatus(...)`: Gestão corporativa do estado da avença (Activar, Suspender, Cancelar) substituindo completamente o `JOptionPane.showOptionDialog` legado por diálogo ou menu modal profissional.

## 4. Metas de Linhas de Código
| Ficheiro | Linhas Antes | Meta Após | Limite Máximo |
| :--- | :--- | :--- | :--- |
| `RecurringContractsPanel.java` | 419 | 220 | $\le 230$ |
| `RecurringContractFormDialog.java` | — | 129 | $\le 135$ |
| `RecurringContractsOperationsCoordinator.java` | — | 94 | $\le 95$ |
