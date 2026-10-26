# Especificação Canónica: Decomposição de `ConfigPanel` (Fase 10)

**Identificador:** `SPEC-CONFIG-PANEL-DECOMPOSITION-001`  
**Módulo:** `desktop` (`mz.multicore.erp.gui`)  
**Data:** 2026-10-23  
**Status:** Em Implementação  

---

## 1. Contexto e Diagnóstico

O painel central de configurações (`ConfigPanel.java`) acumulou **866 linhas de código**, concentrando num único orquestrador:
1. Barra superior de aparência e diagnóstico (alternância de temas, densidade e saúde do sistema).
2. Tabela de logs de auditoria geral com `TableFilter` e `TableQuickPeekController`.
3. Gestão e consola de cópias de segurança (backups lógicos, backups físicos da base de dados e arquivo com verificação de integridade).
4. Configuração visual de colunas visíveis nos documentos e rodapé de recibos POS com predefinições comerciais e logísticas.
5. Painel de gestão e estado da assinatura corporativa (`MySubscriptionDTO`) com diálogo de renovação e alertas visuais de expiração.
6. Integração com painéis já desacoplados (`UserManagementPanel` e `ConfigSupportPanel`).

### Invariantes e Restrições
- **Preservação de Construtor e Contratos:** O construtor com 7 argumentos deve ser mantido rigorosamente compatível com a injeção em `DesktopClientFactory` e `MainFrame`.
- **Invariante `onPanelSelected()`:** A navegação preguiçosa (`onPanelSelected()`) deve continuar a atualizar todas as abas sem disparar `NullPointerException` (validado por `ConfigPanelSelectionHarnessTest`).
- **Eliminação de `JOptionPane`:** Zero chamadas a `JOptionPane` em qualquer um dos ficheiros desacoplados (validado por `ProfessionalFeedbackHarnessTest`).
- **Alinhamento de Acções em Tabelas:** Proibido empilhar botões de ação no rodapé das tabelas de arquivo (`TableActionPlacementHarnessTest`).
- **Tetos Físicos de Linhas:**
  - `ConfigPanel.java`: $\le 450$ linhas (meta: $\sim 250$ linhas).
  - `ConfigBackupsPanel.java`: $\le 220$ linhas.
  - `ConfigDocumentColumnsPanel.java`: $\le 200$ linhas.
  - `ConfigSubscriptionPanel.java`: $\le 150$ linhas.

---

## 2. Desenho da Decomposição Modular

### 2.1 Colaboradores Especializados

1. **`ConfigBackupsPanel.java` ($\le 220$ linhas):**
   - Responsabilidade: Construção da aba de cópias de segurança, consola de execução de backups (lógico, físico e automático), tabela de arquivos com `TableQuickPeekController` e diálogo de verificação de integridade.
   - Comunicação com `BackupApiClient` e registo de auditoria via `AuditApiClient`.

2. **`ConfigDocumentColumnsPanel.java` ($\le 200$ linhas):**
   - Responsabilidade: Construção da aba de colunas dos documentos, seletor de tipo de documento, aplicação de predefinições ("Comercial Executivo", "Logístico / Armazém", "Todas as Colunas"), grelha de checkboxes de visibilidade, comentário de rodapé do POS e gravação com barra de progresso.

3. **`ConfigSubscriptionPanel.java` ($\le 150$ linhas):**
   - Responsabilidade: Construção da aba de estado da assinatura, carregamento e renderização dinâmica de dados do plano (`MySubscriptionDTO`), alerta de dias restantes com cores semânticas e acionamento de `SubscriptionRenewalDialog`.

4. **`ConfigPanel.java` ($\le 450$ linhas):**
   - Responsabilidade: Orquestrador da barra de ferramentas superior (temas, densidade e saúde), aba de logs de auditoria geral e montagem das abas corporativas delegando aos painéis especializados.

---

## 3. Critérios de Sucesso e Validação

1. `ConfigPanel.java` $\le 450$ linhas.
2. `ConfigBackupsPanel.java` $\le 220$ linhas.
3. `ConfigDocumentColumnsPanel.java` $\le 200$ linhas.
4. `ConfigSubscriptionPanel.java` $\le 150$ linhas.
5. Zero chamadas a `JOptionPane`.
6. Testes aprovados: `ConfigPanelDecompositionHarnessTest`, `ConfigPanelSelectionHarnessTest`, `ProfessionalFeedbackHarnessTest`, `TableActionPlacementHarnessTest`, `DesktopThinContextTest`, `MultiModuleArchitectureHarnessTest`.
