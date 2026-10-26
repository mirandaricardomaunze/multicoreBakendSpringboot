# Especificação de Decomposição: Diálogo de Fecho Cego de Caixa POS (PosBlindCloseDialog.java)

- **Document ID**: SPEC-POS-BLIND-CLOSE-DIALOG-001
- **Fase**: 73
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Módulo**: `desktop`
- **Domínio**: Ponto de Venda (`mz.multicore.erp.gui.pos`)

---

## 1. Contexto e Diagnóstico

O ficheiro `desktop/src/main/java/mz/multicore/erp/gui/pos/PosBlindCloseDialog.java` acumulava **690 linhas de código**, concentrando num único componente UI:
1. Painel de contagem física discriminada de notas (1000, 500, 200, 100, 50, 20 MT) e moedas (10, 5, 2, 1, 0.50 MT), com botões rápidos de incremento (+1, +5) e serialização JSON de denominações (`buildDenominationsJson`);
2. Modo alternativo de introdução direta de valor global e recálculo reativo;
3. Painel de resultado e reconciliação pós-fecho (esperado pelo sistema, contado pelo operador, quebra/sobra auditada com cores semânticas);
4. Orquestração do fecho de turno com a API REST (`posApiClient.closeSession`), consulta do relatório Z (`posApiClient.getZReport`) e impressão oficial via `SensitivePrintCoordinator`.

---

## 2. Decisão Arquitetural e Decomposição

Para modularizar o diálogo, garantir testabilidade isolada e manter a classe principal estritamente abaixo do teto de linhas, a decomposição é estruturada em:

1. **`PosCashBreakdownPanel.java`** ($\le 270$ linhas):
   - Gere o selector de modo (contagem discriminada por cédulas/moedas vs introdução direta);
   - Constrói o cartão de denominações com botões de incremento rápido (+1, +5) e cálculo de subtotais em tempo real;
   - Constrói o cartão de resumo do total físico contado na gaveta (total geral, total notas, total moedas);
   - Serialização JSON de denominações (`buildDenominationsJson`) e cálculo do montante contado (`calculateCountedAmount`).

2. **`PosBlindCloseResultPanel.java`** ($\le 140$ linhas):
   - Constrói a superfície de resultado pós-fecho (`ModernPanel(14)`);
   - Apresenta e formata com cores semânticas o montante esperado, contado, divergência (quebra/sobra/bate certo) e notas auditadas (`showResult`).

3. **`PosBlindCloseDialog.java`** ($\le 290$ linhas, reduzido de 690 linhas):
   - Mantém rigorosamente os construtores sobrecarregados públicos:
     - `PosBlindCloseDialog(Window, POSApiClient, TillSessionDTO, Runnable)`
     - `PosBlindCloseDialog(Window, POSApiClient, TillSessionDTO, Runnable, PrintApiClient)`
   - Monta cabeçalho informativo com avatar/ícone e identificação de operador/sessão/saldo de abertura;
   - Orquestra os componentes `PosCashBreakdownPanel` e `PosBlindCloseResultPanel`;
   - Executa o fecho da sessão via `posApiClient.closeSession`, notifica via `ToastManager` e ativa impressão oficial do Relatório Z via `SensitivePrintCoordinator`.

---

## 3. Contratos Preservados

- `getTitle()` retorna `"Fecho Cego de Caixa — Sessão #" + session.id()`;
- Construtores públicos inalterados para manter compatibilidade com `PosCashSessionActions.java`, `PosBlindCloseDialogHarnessTest` e `PosZReportUiHarnessTest`;
- Zero diálogos legados `JOptionPane`;
- Impressão segue estritamente o `PrintPreviewDialog` / `SensitivePrintCoordinator`, sem tocar `PdfFileSaver` directamente (`PrintModalHarnessTest`).
