# Selecção de linha nas acções de tabela — Especificação e harness

**Última actualização:** 2026-11-06

## Problema

`JTable.getSelectedRow()` devolve a posição **na vista**. Os dados vivem no modelo, ou numa lista
carregada pela mesma ordem. Com filtro, ordenação ou paginação, os dois índices separam-se. A
paginação (`ClientTablePagination`) instala sempre um ordenador, por isso o erro aparece da 2.ª página
em diante, mesmo sem filtro.

O efeito é silencioso e grave: o operador selecciona a Ana e a acção (desactivar utilizador,
suspender contrato, cancelar contagem, reimprimir Relatório Z) cai sobre outro registo.

## Decisão

- Uma acção lê a linha com `TableFilter.selectedModelRow(table)`, que converte para o modelo.
- O painel de espreitar já recebe a linha do modelo e não a volta a converter (FN-10, em
  `FORMATO_NUMERICO_SPEC.md`).
- Leituras que ficam na vista, como `table.getValueAt(...)`, pintura e navegação por teclado, usam o
  índice da vista e estão correctas.

## Corrigido em 2026-11-06

Utilizadores, contratos recorrentes, contagens de inventário (abrir e cancelar), radar de quebras,
prémios, metas, histórico de sessões POS, vendas de contingência, backups (dois ecrãs), risco de
crédito, auditoria forense, categorias e remoção de linhas em transferências e notas de débito.

## Harness

| Id | Cenário | Espera-se | Teste |
|----|---------|-----------|-------|
| SL-01 | Filtro activo, 1.ª linha visível seleccionada | Linha do modelo correcta | `SelectedRowModelIndexHarnessTest` |
| SL-02 | 2.ª página, 1.ª linha seleccionada | Linha 50 do modelo | `SelectedRowModelIndexHarnessTest` |
| SL-03 | `int x = t.getSelectedRow()` seguido de `model.getValueAt(x`, `removeRow(x)` ou `lista.get(x)` sem conversão | Reprova com ficheiro e linha | `SelectedRowModelIndexHarnessTest` |

## Limites

- SL-03 olha para as 12 linhas a seguir à leitura. Um índice passado a outro método escapa-lhe.
