# Especificação de Decomposição Modular: Activos Fixos & Património (AssetsPanel)

**Identificador:** `SPEC-ASSETS-PANEL-DECOMPOSITION-001`  
**Domínio:** Ativos Fixos / Imobilizado Corpóreo & Incorpóreo / Património  
**Alvo:** `mz.multicore.erp.gui.AssetsPanel.java` (454 linhas)  
**Meta de Redução:** $\le 250$ linhas no orquestrador principal ($\approx 48\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `AssetsPanel.java` é responsável pela gestão do imobilizado da empresa, apuramento das quotas mensais de depreciação conforme o Código do IRPC de Moçambique, acompanhamento do Valor Líquido Contabilístico (VLC) e emissão do Mapa de Amortizações e Reintegrações (Modelo 22 Anexo D).

Actualmente, o painel centralizava num único ficheiro monolítico:
1. **Formulário Modal de Registo de Activo:** Criação com validação rigorosa de campos (código, nome, categoria com taxa automática do IRPC, datas, custos, valor residual e responsável) em `ModernFormDialog`.
2. **Processamento & Histórico de Quotas:** Diálogo para cálculo mensal de quotas por competência e diálogo com tabela cronológica de histórico de amortizações.
3. **Orquestração da Grelha & KPIs:** Tabela de 11 colunas estilizada com `TableFilter.bar`, renderizadores monetários e de estado, e grelha canónica de KPIs (`KpiCard.createGrid(4)`).

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **`GuiActionAlertGuidanceHarnessTest` (`ALERT-05`):**
   - Campos com visibilidade de pacote:
     - `final JTable table;`
     - `final InlineFeedbackPanel feedback;`
   - Método com visibilidade de pacote:
     - `void showAssetDetails()`
   - Alerta de ausência de selecção:
     - Título: `"Seleccione um activo"`
     - Tipo: `FeedbackType.WARNING`
     - Mensagem contendo: `"Seleccione um activo na tabela"`
2. **Layout Invariante:**
   - Card único `ModernPanel(16)` contendo filtros e tabela via `UIHelper.tableCardTop`.
   - Grid de KPIs via `KpiCard.createGrid(4)` no topo.
   - Construtores preservados: `AssetsPanel(AssetApiClient)` e `AssetsPanel(AssetApiClient, PrintApiClient)`.
3. **Eliminação de Diálogos Legados:**
   - Zero chamadas a `JOptionPane`.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui/
├── AssetsPanel.java                     (Orquestrador principal: <= 250 linhas)
│   ├── table, feedback, showAssetDetails() [visibilidade de pacote preservada]
│   ├── updateTableAndKpis()
│   └── loadAssets()
├── AssetFormDialog.java                 (Colaborador modal: <= 125 linhas)
│   └── show(Component parent, AssetApiClient client, Runnable onCreated)
└── AssetDepreciationCoordinator.java    (Coordenador de amortizações: <= 90 linhas)
    ├── openDepreciationDialog(...)
    └── showDepreciationHistoryDialog(...)
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `AssetsPanel.java` | $\le 250$ linhas | Layout, tabela, filtros, KPIs e orquestração |
| **Modal de Cadastro** | `AssetFormDialog.java` | $\le 125$ linhas | Diálogo modal com validação semântica de novo activo |
| **Coordenador de Amortização** | `AssetDepreciationCoordinator.java` | $\le 100$ linhas | Apuramento de quotas e histórico de VLC |
