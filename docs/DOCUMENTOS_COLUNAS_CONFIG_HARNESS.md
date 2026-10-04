# Harness — Colunas configuráveis dos documentos comerciais

> Cenários para [DOCUMENTOS_COLUNAS_CONFIG_SPEC.md](DOCUMENTOS_COLUNAS_CONFIG_SPEC.md).
> DC-01..DC-06 automáticos; DC-50..DC-53 manuais (UI + PDF).

**Última actualização:** 2026-09-25

## Automáticos

### `DocumentConfigServiceTest`
| ID    | Cenário | Esperado |
|-------|---------|----------|
| DC-01 | `getColumns` de empresa sem config guardada. | Devolve `all()` (11 colunas visíveis). |
| DC-02 | `save` com algumas colunas desligadas → `getColumns`. | Reflecte o guardado. |
| DC-03 | `save` a esconder **todas** as colunas. | `BusinessRuleException` (documento não pode ficar sem colunas). |
| DC-04 | `save` sem perfil MANAGER/ADMIN. | Bloqueado (`BusinessRuleException`). |
| DC-05 | `save`/`getColumns` de empresa diferente da activa. | Bloqueado (guarda multi-tenant). |

### `LineItemsTableRendererTest`
| ID    | Cenário | Esperado |
|-------|---------|----------|
| DC-06 | `build(rows, colsSemBarcodeSemValidade)`. | Tabela tem **9** colunas; sem "Cód." nem "Val.". `build(rows)` mantém 11. |
| DC-08 | Linha de 18 unidades, produto 12 embalagens × 6 unidades. | `Emb. = 3`, `Cx. = 0.25` e `% Cx. = 25%`. |
| DC-09 | Cabeçalho completo | Usa `Cód. · Ref. · Desc. · Val. · Qtd. · Emb. · Cx. · % Cx. · P. Unit. · IVA · Subt.`. |

## Manuais (UI + PDF)

| ID    | Passos | Esperado |
|-------|--------|----------|
| DC-50 | Config → "Colunas dos Documentos" → desmarcar Cód. Barras e Validade → Guardar. | Guarda sem erro. |
| DC-51 | Emitir/Imprimir uma **Fatura**. | PDF sem as colunas Cód. Barras e Validade; totais inalterados. |
| DC-52 | Imprimir **Encomenda**, **NC** e **Guia**. | Todas respeitam a mesma configuração. |
| DC-53 | Tentar desmarcar todas e Guardar. | Erro "o documento tem de ter pelo menos uma coluna". |
| DC-54 | Config tipo **Recibo POS** → desmarcar Qtd e Preço → vender no POS e imprimir recibo. | Recibo sem Qtd/Preço; Descrição e Total mantêm-se; totais no rodapé inalterados. |
| DC-55 | Config Recibo POS → marcar Referência → imprimir recibo de produto com referência. | Referência como sublinha sob o nome. |
| DC-56 | Config tipo **Comercial**: desmarcar Cód. Barras. Depois abrir tipo **Recibo POS**. | São **independentes** — a config do recibo não muda ao alterar a comercial (e vice-versa). |
| DC-57 | Config Recibo POS → escrever comentário "Trocas em 7 dias" no rodapé → Guardar → imprimir recibo. | O recibo mostra "Trocas em 7 dias" no fundo (em vez do texto padrão). |
| DC-58 | Config Comercial → activar Embalagens, Caixas e % da Caixa → imprimir documento com 18 unidades de um produto 12 × 6. | PDF mostra `3`, `0.25` e `25%`; totais e IVA permanecem iguais. |

### Automático adicional — `DocumentConfigServiceTest`
| ID    | Cenário | Esperado |
|-------|---------|----------|
| DC-07 | `save(company, POS_RECEIPT, dto)` não afecta `getColumns(company, COMMERCIAL)`. | Configs por tipo são independentes (repositório consultado por `company_id + document_type`). |

## Verificação

- `mvn clean compile` e `mvn clean test` verdes (inclui DC-01..DC-06). Flyway aplica `V22`.
- Revisão `multicore-solid-review` do diff sem apontamentos bloqueantes.
