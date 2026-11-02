# Documentos com várias páginas — Especificação e harness

**Última actualização:** 2026-11-01

## Objectivo

A revisão com `scripts/empresa-exemplo.py` mostrou que a fatura só estava certa quando cabia numa
página por via. Com muitas linhas, o documento fiscal identificava mal a via e perdia referências.

## Decisões

- O rodapé identifica a via do documento, não o número da página do ficheiro. Cada via conta as suas
  páginas: "Original · Página 2".
- Da segunda página em diante, cada via mostra no topo o número do documento e a via.
- A tabela de linhas repete os títulos em cada página, em todos os documentos que a usam.
- O rodapé fica fora da área do conteúdo: a margem inferior subiu de 28 para 36 pontos.
- Os lotes `AUTO-` são gerados pelo sistema e não aparecem nos documentos do cliente. Os lotes reais
  continuam a aparecer.
- No recibo térmico, a coluna do total alarga para os valores não partirem a meio.

## Cenários

| ID | Cenário | Resultado |
|----|---------|-----------|
| DP-01 | Página 2 do original | Diz "Original · Página 2", nunca "Duplicado" |
| DP-02 | Primeira página do duplicado | Diz "Duplicado · Página 1" |
| DP-03 | Página de continuação | Tem o número do documento e os títulos das colunas |
| DP-04 | Fatura numa página por via | Mantém quatro páginas e quatro vias |
| DP-05 | Lote gerado pelo sistema | Não aparece |
| DP-06 | Lote real | Aparece |
| DP-07 | Total de 66.869.318,95 no recibo térmico | Fica numa só linha |

Teste: `DocumentPaginationHarnessTest`.

## Limites

- A identificação no topo existe na fatura A4. Os outros documentos ganham os títulos repetidos.
- Lotes `AUTO-` deixam de aparecer mesmo que tenham validade associada.
