# Especificação de Decomposição: InventoryPhysicalCountingPrintService (Fase 191)

## Objetivo
Decompor `InventoryPhysicalCountingPrintService.java` (164 linhas) numa fachada enxuta (`<= 50` linhas) que carrega a empresa e delega a composição do Dossiê de Inventário Físico OpenPDF para `InventoryPhysicalCountingDocumentRenderer`.

## Arquitetura Alvo
1. `InventoryPhysicalCountingPrintService.java` (`<= 50` linhas) — Fachada `@Service` preservando o construtor legado.
2. `InventoryPhysicalCountingDocumentRenderer.java` (`<= 145` linhas) — Renderizador OpenPDF A4 com cabeçalho, metadados, cartões de resumo financeiro, tabela de itens com suporte a contagem cega e assinaturas.
