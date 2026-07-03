# Harness — Reposição automática de stock

> Cenários para [REPOSICAO_AUTOMATICA_SPEC.md](REPOSICAO_AUTOMATICA_SPEC.md).
> RA-01..RA-03 automáticos (`ReorderServiceTest`); RA-50..RA-52 manuais (UI).

**Última actualização:** 2026-07-03

## Automáticos — `ReorderServiceTest`

| ID    | Cenário | Esperado |
|-------|---------|----------|
| RA-01 | Açúcar (24 und/caixa, mínimo 100, stock 30); Arroz (mínimo 50, stock 60). | Só sugere Açúcar; falta 70 → **3 caixas / 72 unidades** (arredonda para cima). |
| RA-02 | Feijão (10 und/caixa, mínimo 40) **sem qualquer linha de stock**. | Conta como 0 → sugere **4 caixas / 40 unidades**. |
| RA-03 | Produto sem mínimo (min=0) e produto não-stockável. | Ambos **ignorados**. |

## Manuais (UI)

| ID    | Passos | Esperado |
|-------|--------|----------|
| RA-50 | Compras → aba **"Reposição"**. | Lista os produtos abaixo do mínimo, mais urgentes no topo; rodapé com contagem. |
| RA-51 | Baixar o stock de um produto abaixo do mínimo (vender/ajustar) e "Atualizar". | Produto passa a aparecer com a quantidade sugerida em caixas e unidades. |
| RA-52 | Botão **"Criar Encomenda"**. | Salta para a aba "Encomendas a Fornecedor" para encomendar. |

## Verificação

- `mvn clean test` → verde (inclui `ReorderServiceTest`, 3).
