# SPEC-FORENSIC-AUDIT-PANEL-DECOMPOSITION-001: Decomposição Modular da Central de Auditoria Forense

## 1. Visão Geral e Objectivo
O painel `ForensicAuditPanel.java` (438 linhas) é a central executiva de deteção de fraudes, anomalias fiscais e desvios operacionais da plataforma Multicore ERP.
O objetivo desta decomposição é desacoplar as responsabilidades secundárias (diálogo modal de detalhes de anomalia, coordenação de emissão de dossiê PDF / Quick Peek / parsing e renderizador de severidade de células) para colaboradores especializados de responsabilidade única, reduzindo o orquestrador para $\le 250$ linhas sem quebrar nenhum contrato de teste pré-existente.

## 2. Invariantes Arquiteturais e Restrições Não Negociáveis
1. **Camadas Físicas**: Módulo `desktop` depende exclusivamente de `contracts` e consome serviços via HTTPS (`ForensicAuditApiClient`). Proibido JPA, repositórios ou dependências circulares.
2. **Contratos Visuais e de Teste**:
   - Campos com visibilidade de pacote estritamente preservados para `ForensicAuditPanelHarnessTest`:
     - `kpiTotalRisk`, `kpiCritical`, `kpiSuspicious`, `kpiComplianceScore`
     - `startDateField`, `endDateField`, `severityCombo`, `categoryCombo`, `operatorField`
     - `table`, `tableModel`, `currentAnomalies`
     - `void renderSummary(ForensicAuditSummaryDTO summary)`
   - Nomes literais das 8 colunas da tabela:
     `Data/Hora`, `Severidade`, `Categoria`, `Referência`, `Operador`, `Impacto (MT)`, `Detalhes / Ocorrência`, `Recomendação Preventiva`
   - Invariantes exigidos por `AccountingFiscalUnifiedHarnessTest`:
     - Presença literal de `UIHelper.ensureHeadersFit(table)`
     - Presença literal de `TableQuickPeekController.install(table`
   - Invariante linguístico e de estilo (`ForensicAuditPanelHarnessTest.testCodeQualityAndStandards`):
     - Proibido `new Color(` em `ForensicAuditPanel.java` (usar tokens do `UIHelper`).
     - Proibida a palavra `atualizar` (case-insensitive); em Moçambique deve ser `Actualizar`.
   - Limite estrito de no máximo 1000 linhas (meta de decomposição: $\le 250$ linhas).
   - Zero chamadas a `JOptionPane`.

## 3. Arquitetura de Componentes e Decomposição
A central `ForensicAuditPanel.java` orquestra três novos colaboradores especializados:

```
mz.multicore.erp.gui
├── ForensicAuditPanel.java (Orquestrador UI: <= 250 linhas)
├── ForensicAnomalyDetailsDialog.java (Diálogo modal corporativo de dossiê da ocorrência: <= 75 linhas)
├── ForensicAuditOperationsCoordinator.java (Operações assíncronas de dossiê PDF, parsing e Quick Peek: <= 100 linhas)
└── ForensicCellRenderer.java (Renderizador de células com semáforo de severidade e alinhamentos: <= 45 linhas)
```

### 3.1 `ForensicAnomalyDetailsDialog.java`
- Diálogo modal moderno construído sobre `ModernPanel` e centrado na janela principal (`UIHelper.applyStandardModalBounds`).
- Apresenta cabeçalho com número do documento / referência, metadados formatados (Data/Hora, Severidade, Categoria, Operador, Impacto MZN, Motivo Original) e áreas de texto destacadas para "Detalhes Analíticos" e "Recomendação Preventiva".
- Botão "Fechar" via `UIHelper.createSecondaryButton("Fechar")`.

### 3.2 `ForensicAuditOperationsCoordinator.java`
- Método `emitForensicPdf(...)`: Executa `apiClient.getPdf` em `SwingWorker` em segundo plano com `busyBar` e exibe o PDF via `PrintPreviewDialog.show(parent, pdf, "dossie-auditoria-forense")`.
- Método `populatePeek(...)`: Configura o `QuickPeekPanel` com ícone de escudo, severidade em vermelho de auditoria e lista estruturada de metadados.
- Métodos utilitários de parsing: `parseDate(String text)`, `parseSeverity(int idx)` e `parseCategory(int idx)`.

### 3.3 `ForensicCellRenderer.java`
- Extende `DefaultTableCellRenderer` com alternância de linhas (`UIHelper.BG_CARD` / `UIHelper.BG_DARK`), formatação semafórica de severidade (CRITICAL em `REJECTED_RED`, SUSPICIOUS em `PENDING_YELLOW`, INFO em `TEXT_MUTED`) e alinhamentos adequados (centro para severidade, direita para impacto financeiro e esquerda para texto).

## 4. Metas de Linhas de Código
| Ficheiro | Linhas Antes | Meta Após | Limite Máximo |
| :--- | :--- | :--- | :--- |
| `ForensicAuditPanel.java` | 438 | 276 | $\le 280$ |
| `ForensicAnomalyDetailsDialog.java` | — | 62 | $\le 75$ |
| `ForensicAuditOperationsCoordinator.java` | — | 100 | $\le 115$ |
| `ForensicCellRenderer.java` | — | 42 | $\le 50$ |
