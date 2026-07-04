# Harness — Colunas configuráveis dos documentos comerciais

> Cenários para [DOCUMENTOS_COLUNAS_CONFIG_SPEC.md](DOCUMENTOS_COLUNAS_CONFIG_SPEC.md).
> DC-01..DC-06 automáticos; DC-50..DC-53 manuais (UI + PDF).

**Última actualização:** 2026-07-04

## Automáticos

### `DocumentConfigServiceTest`
| ID    | Cenário | Esperado |
|-------|---------|----------|
| DC-01 | `getColumns` de empresa sem config guardada. | Devolve `all()` (8 colunas visíveis). |
| DC-02 | `save` com algumas colunas desligadas → `getColumns`. | Reflecte o guardado. |
| DC-03 | `save` a esconder **todas** as colunas. | `BusinessRuleException` (documento não pode ficar sem colunas). |
| DC-04 | `save` sem perfil MANAGER/ADMIN. | Bloqueado (`BusinessRuleException`). |
| DC-05 | `save`/`getColumns` de empresa diferente da activa. | Bloqueado (guarda multi-tenant). |

### `LineItemsTableRendererTest`
| ID    | Cenário | Esperado |
|-------|---------|----------|
| DC-06 | `build(rows, colsSemBarcodeSemValidade)`. | Tabela tem **6** colunas; sem "Cód. Barras" nem "Validade". `build(rows)` mantém 8. |

## Manuais (UI + PDF)

| ID    | Passos | Esperado |
|-------|--------|----------|
| DC-50 | Config → "Colunas dos Documentos" → desmarcar Cód. Barras e Validade → Guardar. | Guarda sem erro. |
| DC-51 | Emitir/Imprimir uma **Fatura**. | PDF sem as colunas Cód. Barras e Validade; totais inalterados. |
| DC-52 | Imprimir **Encomenda**, **NC** e **Guia**. | Todas respeitam a mesma configuração. |
| DC-53 | Tentar desmarcar todas e Guardar. | Erro "o documento tem de ter pelo menos uma coluna". |

## Verificação

- `mvn clean compile` e `mvn clean test` verdes (inclui DC-01..DC-06). Flyway aplica `V22`.
- Revisão `phc-solid-review` do diff sem apontamentos bloqueantes.
