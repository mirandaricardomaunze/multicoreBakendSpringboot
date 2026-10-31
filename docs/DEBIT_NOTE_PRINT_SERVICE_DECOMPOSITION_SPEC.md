# Especificação de Decomposição: DebitNotePrintService (Fase 185)

## Objetivo
Decompor `DebitNotePrintService.java` (172 linhas) numa fachada enxuta (`<= 55` linhas) que carrega a Nota de Débito e delega a composição das 4 vias fiscais A4 para `DebitNoteDocumentRenderer`.

## Arquitetura Alvo
1. `DebitNotePrintService.java` (`<= 55` linhas) — Fachada `@Service` preservando `DEBIT_NOTE_COPIES` e o construtor legado.
2. `DebitNoteDocumentRenderer.java` (`<= 145` linhas) — Renderizador OpenPDF A4 com as 4 vias fiscais, contexto do cliente/fatura, linhas, totais e assinaturas.
