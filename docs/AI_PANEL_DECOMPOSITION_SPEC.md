# Especificação — Decomposição Canónica do Assistente de Gestão & Inteligência
**Código:** `SPEC-AI-PANEL-DECOMPOSITION-001`  
**Módulo:** `desktop` (`mz.multicore.erp.gui.intelligence` / `mz.multicore.erp.gui`)  
**Data:** 2026-10-22  

---

## 1. Contexto e Diagnóstico

O painel `AiManagementAssistantPanel.java` acumulou mais de 600 linhas de código no desktop, acumulando 4 responsabilidades de negócio não correlacionadas no mesmo ficheiro:
1. **Consulta & Análise:** Ciclo de perguntas e respostas à IA, atalhos de sugestões rápidas e tabela de evidências quantitativas/contabilísticas.
2. **Rascunhos Operacionais:** Conversão de texto em linguagem natural para parâmetros de cotações/encomendas e redirecionamento de navegação para outros módulos do ERP.
3. **Elaboração de Documentos:** Seleção de modelos documentais (Relatório, Memorando, Proposta, Carta, Acta, Plano de Acção), envio assíncrono e visualização/impressão PDF.
4. **Arquivo & Histórico de Documentos:** Gestão de estados (Submeter, Aprovar, Rejeitar, Arquivar), versionamento, descarregamento de PDFs históricos e modal complexo de comparação diff textual lado a lado.

Essa acumulação viola o princípio da responsabilidade única (**SRP**) e o teto de complexidade do sistema, além de dificultar testes isolados de interface.

---

## 2. Metas de Linhas e Arquitetura Canónica

| Componente | Ficheiro | Responsabilidade | Teto de Linhas |
| :--- | :--- | :--- | :--- |
| **Orquestrador de Abas** | `AiManagementAssistantPanel.java` | Cabeçalho executivo e `JTabbedPane` com `UIHelper.styleTabbedPaneMulticore` | <= 120 linhas |
| **Aba 1: Consulta & Evidências** | `AiAnalysisView.java` | Q&A executivo, chips de sugestões e tabela ampla de evidências com exportador Excel/CSV | <= 180 linhas |
| **Aba 2: Rascunhos Operacionais** | `AiOperationalDraftsView.java` | Extração de rascunhos operacionais e navegação para módulos de destino | <= 130 linhas |
| **Aba 3: Elaboração Documental** | `AiDocumentCreationView.java` | Formulário de documentos formais com preview PDF e salvamento | <= 140 linhas |
| **Aba 4: Histórico & Versões** | `AiDocumentHistoryView.java` | Listagem com paginação, filtros, ações de workflow e acionamento de comparação | <= 190 linhas |
| **Modal de Comparação** | `AiDocumentComparisonDialog.java` | Diálogo modal ergonómico de comparação lado a lado entre versões | <= 80 linhas |

---

## 3. Regras Arquiteturais Não-Negociáveis

1. **Retrocompatibilidade de Construtores:**
   `AiManagementAssistantPanel` deve manter estritamente os dois construtores existentes:
   * `public AiManagementAssistantPanel(AiAssistantApiClient apiClient)`
   * `public AiManagementAssistantPanel(AiAssistantApiClient apiClient, Consumer<String> navigationHandler)`
2. **Contenção e Design System:**
   * Todas as tabelas continuam contidas dentro de `ModernPanel(16)` via `tableCardTop`.
   * A tabela de evidências mantém `ClientTablePagination.DISABLED = Boolean.TRUE` para preservar altura vertical total.
   * Modais respeitam `UIHelper.applyStandardModalBounds` / `ModernFormDialog`.
   * Proibido o uso de emojis em abas, botões ou cabeçalhos.
3. **Fronteiras Thin Client:**
   * Todos os sub-painéis comunicam exclusivamente via `AiAssistantApiClient` e DTOs imutáveis de `contracts`.
   * Zero JPA, zero Spring, zero Repository no desktop.

---

## 4. Critérios de Aceitação

- [x] `AiManagementAssistantPanel` com menos de 150 linhas atuando puramente como compositor das abas.
- [x] Cada aba decomposta num ficheiro individual com responsabilidade única.
- [x] Diálogo modal de comparação de versões desacoplado.
- [x] `mvn compile` sem avisos ou falhas.
- [x] Testes `AiPanelDecompositionHarnessTest`, `TableCardContainmentAuditTest` e `DesktopThinContextTest` 100% verdes.
