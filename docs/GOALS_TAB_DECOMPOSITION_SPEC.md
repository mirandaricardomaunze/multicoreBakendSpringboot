# Especificação Técnica — Decomposição Modular de GoalsTab (Fase 66)

## 1. Contexto e Diagnóstico

A aba `GoalsTab.java` (478 linhas) é parte fundamental do Centro de Desempenho Comercial (CDC), reunindo o painel executivo de metas de vendas. Suas responsabilidades acumuladas incluíam:
1. **Apresentação e Listagem Tabular de Metas:** Montagem da tabela com 10 colunas, filtros de estado e período, botões de ação ("Recarregar", "Cancelar Meta", "Nova Meta"), Quick Peek lateral analítico e feedback de erro/sucesso.
2. **Ciclo de Vida Assíncrono:** Carregamento de metas via `PerformanceApiClient.getGoals` e cancelamento de meta selecionada via `cancelSelectedGoal()`.
3. **Diálogo Modal Monolítico de Criação de Metas:** A rotina privada `openCreateGoalDialog()` continha 220 linhas de layout visual com `GridBagLayout`, carregamento de unidades de negócio, regras reativas de exibição para vendedor/supervisor/negócio conforme o escopo selecionado (`GoalScope`), regras condicionais de bonificação com teto máximo e percentual/fixo, validação de campos obrigatórios e submissão assíncrona.

## 2. Objectivos da Decomposição

1. **Extração de `GoalCreateDialog.java`:**
   - Componente especializado e autocontido no pacote `mz.multicore.erp.gui.performance`.
   - Responsável por renderizar a janela modal `Nova Meta Comercial` com limites canónicos (`UIHelper.applyStandardModalBounds(dialog, 540, 600)`).
   - Gerir o formulário de metas:
     - Nome da meta, periodicidade e intervalo de datas via `DateField`.
     - Seletor reativo de escopo (`GoalScope`): alternância automática de visibilidade para vendedor, supervisor e unidade de negócio.
     - Carregamento dinâmico de unidades de negócio ativas via `PerformanceApiClient.getActiveBusinessUnits`.
     - Alvos financeiros de receita e margem bruta com `MoneyField`.
     - Configuração opcional de bónus/comissão (`BonusType`, valor, teto máximo e geração automática).
     - Validação semântica e feedback com `ToastManager` e `ModernMessageDialog`.
   - Limite físico: $\le 210$ linhas.

2. **Refatoração e Compactação de `GoalsTab.java`:**
   - Reduzir de 478 linhas para $\le 165$ linhas.
   - Preservar integralmente o construtor público `GoalsTab(PerformanceApiClient, DesktopSession, Runnable)`.
   - Preservar o record retrocompatível `GoalsTab.BusinessUnitOption`.
   - Delegar a abertura do modal para `GoalCreateDialog.show(window, apiClient, session, onSaved)`.
   - Zero diálogos legados `JOptionPane`.

3. **Invariantes e Contratos Preservados:**
   - Construtor e ciclo de vida de `GoalsTab`.
   - Comportamento de recarregamento (`reload()`) e notificação `onGoalChanged`.
   - Renderização no Quick Peek e cabeçalhos fit via `UIHelper.ensureHeadersFit(table)`.
