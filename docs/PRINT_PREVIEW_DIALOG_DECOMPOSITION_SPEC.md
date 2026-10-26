# SPEC-PRINT-PREVIEW-DIALOG-DECOMPOSITION-001 — Decomposição Modular do Modal de Pré-visualização de Impressão

## 1. Contexto e Diagnóstico

O componente canónico [PrintPreviewDialog.java](file:///c:/Users/miran/Desktop/manager/desktop/src/main/java/mz/multicore/erp/gui/components/PrintPreviewDialog.java) centraliza o fluxo de impressão prévia de documentos em PDF antes da saída física para o papel. Com **773 linhas**, a classe acumulava:
1. Renderização gráfica e transformações afins da folha A4 com proporção, sombras e desenho em canvas customizado (`PaperCanvas`).
2. Configuração e layout da coluna lateral de opções (`OptionsColumn`), validação de intervalos de páginas, combos de impressora, orientação e ajuste.
3. Coordenador assíncrono de renderização de páginas com controle de tickets e thread daemon descartável.
4. Orquestração de ações de exportação em ficheiro, envio para a fila do spooler (`PdfPrinter.print`) e atalhos de teclado.

Essa sobrecarga violava os princípios de responsabilidade única (SRP) e dificultava testes visuais isolados.

## 2. Objectivos da Decomposição

1. **Reduzir o orquestrador `PrintPreviewDialog.java`** de 773 linhas para $\le 330$ linhas (redução de 58,8%).
2. **Extrair colaboradores especializados no pacote `mz.multicore.erp.gui.components`**:
   - `PrintPreviewPaperCanvas`: Componente `JPanel` responsável estritamente pelo desenho geométrico da folha no ecrã (`PaperLayout.paper`), cálculo de viewport e renderização rasterizada da página.
   - `PrintPreviewOptionsView`: Montador do painel lateral de opções com a coluna rolável responsiva (`OptionsColumn`), renderizadores de combo padronizados e sincronização de opções salvas (`PrintOptionsStore`).
3. **Preservar 100% de compatibilidade com os harnesses canónicos**:
   - Invocação estática `show(owner, pdf, baseName)` e `show(owner, pdf, baseName, documentLabel)`.
   - Método de teste `buildForHarness(pdf, baseName)`.
   - Conformidade com `PrintModalHarnessTest` (preservação estrita das palavras-chave auditadas: "Impressora", "Nº de cópias", "Páginas", "Orientação", "Ajuste", "Escala de cinzentos", "Imprimir", "Guardar PDF", "Cancelar", "PaperLayout.paper(", "PdfPrinter.print(").
   - Conformidade visual com `PrintPreviewDialogPaintTest` (renderização idêntica da folha A4 recortada em tema escuro e claro).
4. **Erradicação total de `JOptionPane`**: Mantém 0 ocorrências de `JOptionPane`.

## 3. Critérios de Aceitação

- Orquestrador `PrintPreviewDialog.java` $\le 225$ linhas.
- Colaboradores individuais $\le 220$ linhas.
- 100% dos testes aprovados: `PrintPreviewDialogDecompositionHarnessTest`, `PrintModalHarnessTest`, `PrintPreviewDialogPaintTest`, `DesktopThinContextTest`, `MultiModuleArchitectureHarnessTest`.
