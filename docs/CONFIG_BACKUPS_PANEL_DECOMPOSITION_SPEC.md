# SPEC-CONFIG-BACKUPS-PANEL-DECOMPOSITION-001: Decomposição Modular da Gestão de Backups

## 1. Visão Geral & Contexto Operacional

A interface `ConfigBackupsPanel.java` (no módulo desktop) é responsável pela gestão de cópias de segurança (backups lógicos em `.json`, backups físicos da base de dados PostgreSQL e rotinas automáticas de retenção), consola operacional de log e arquivo de snapshots da empresa.

Actualmente com 212 linhas de código, a classe acumula a construção do layout de dois cartões em grelha (`consoleCard` e `archiveCard`), os handlers de execução de cópias lógicas e físicas com elevação de privilégios (`SignedInUser.isAdmin()`), verificação de integridade de snapshot e sincronização com auditoria.

Esta especificação estabelece a extração das operações de backup para um colaborador especializado:
- `ConfigBackupOperations.java`: Executor e orquestrador das operações de backup (lógico, físico, automático, verificação e povoamento de ficheiros).
- `ConfigBackupsPanel.java`: Painel orquestrador conciso focado na montagem da consola visual e tabela de arquivo com Quick Peek.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `ConfigBackupsPanel.java` | 212 | $\le 115$ | Orquestrador de layout, consola de terminal e tabela de arquivo de snapshots |
| `ConfigBackupOperations.java` | 0 (Novo) | $\le 125$ | Operações assíncronas de backup (lógico/físico/auto), verificação e estado |

---

## 3. Contratos e Invariantes Preservados

1. **Construtores e Métodos:**
   - Package-private `ConfigBackupsPanel(ConfigPanel owner)`.
   - `public JPanel buildPanel()`.
   - `public void refresh()`.
   - Package-private `void refreshAutoBackupStatus()`.
   - Package-private `void loadBackupFilesList()`.
2. **Layout Canónico e UX:**
   - Painel em grelha 1x2 com `consoleCard` à esquerda e `archiveCard` à direita (`ModernPanel(16)`).
   - Menu de ações de criação via `ActionMenuButton` com 3 ações (`"Backup lógico (.json)"`, `"Backup físico (BD)"`, `"Backup automático agora"`).
   - Tabela de arquivo de ficheiros com `UIHelper.ensureHeadersFit(backupFilesTable)` e `TableQuickPeekController.install(backupFilesTable, archiveCard, ...)`.
3. **Segurança e Zero `JOptionPane`:**
   - Validação estrita de perfil administrador (`SignedInUser.isAdmin()`) antes de qualquer operação sensível.
   - Total ausência de `JOptionPane`, utilizando `ToastManager` e diálogos canónicos.
