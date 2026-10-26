# HARNESS-PRINT-PREVIEW-DIALOG-001 — Matriz de Testes da Decomposição do PrintPreviewDialog

## 1. Escopo e Propósito

Este documento define os critérios de teste automatizado e auditoria estática para a decomposição do modal de impressão [PrintPreviewDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/PrintPreviewDialog.java) e seus colaboradores extraídos em `mz.multicore.erp.gui.components`.

## 2. Casos de Teste

1. **`testLineCountThresholds`**:
   - `PrintPreviewDialog.java` deve ter $\le 330$ linhas.
   - `PrintPreviewPaperCanvas.java` deve ter $\le 150$ linhas.
   - `PrintPreviewOptionsView.java` deve ter $\le 220$ linhas.

2. **`testZeroJOptionPaneUsage`**:
   - Garantir 0 ocorrências de `JOptionPane` em todos os novos ficheiros.

3. **`testCanonicalKeywordsPreserved`**:
   - Garantir que `PrintPreviewDialog.java` preserva as palavras-chave exigidas por `PrintModalHarnessTest`:
     - "Impressora", "Nº de cópias", "Páginas", "Orientação", "Ajuste", "Escala de cinzentos", "Imprimir", "Guardar PDF", "Cancelar"
     - "PaperLayout.paper("
     - "PdfPrinter.print("

4. **`testVisualIntegrity`**:
   - Execução integral de `PrintPreviewDialogPaintTest` (3/3) e `PrintModalHarnessTest` (21/21).
