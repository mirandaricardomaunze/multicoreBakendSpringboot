# Harness — Bloqueio de stock (contagem cega)

> Cenários para [BLOQUEIO_STOCK_SPEC.md](BLOQUEIO_STOCK_SPEC.md). Todos **manuais** (permissão +
> visibilidade UI). A regra "só ADMIN altera" reusa `PermissionGuard.requireAdmin` (já com testes de
> guarda).

## Manuais

| ID    | Passos | Esperado |
|-------|--------|----------|
| BL-50 | Entrar como **ADMIN**, abrir Stock. | Botão "Trancar Stock" (cadeado) visível no topo. |
| BL-51 | Clicar "Trancar Stock". | Banner amarelo "Stock trancado — … Como administrador, vê tudo."; botão passa a "Destrancar Stock"; **admin continua a ver as quantidades**. Auditoria regista `STOCK_LOCK`. |
| BL-52 | Ainda como ADMIN, ver Níveis / Alertas / Lotes. | Quantidades **reais** visíveis (o mascaramento é só para não-admins). |
| BL-53 | Terminar sessão e entrar como **EMPLOYEE/MANAGER**; abrir Stock. | Níveis (Qtd Unidades/Caixas/Estado), Alertas (Stock/Qtd) e Lotes (Quantidade) mostram **`•••`**; resumo de alertas "Quantidades ocultas…". **Sem** botão de trancar. |
| BL-54 | Como não-admin, confirmar o resto. | SKU/nome/código de barras, **preço**, validade/dias e Movimentos continuam **visíveis**. |
| BL-55 | Voltar como ADMIN, clicar "Destrancar Stock". | Banner desaparece; auditoria `STOCK_LOCK` (destrancado). Não-admins voltam a ver quantidades. |
| BL-56 | Tentar `setStockCountLocked` como não-admin (via API/serviço). | Recusado (permissão ADMIN). |
| BL-57 | Persistência: trancar, fechar e reabrir a app. | Continua trancado (estado na BD). |

## Verificação

- `mvn -o compile` limpo; migração `V27` aplica (Flyway) e Hibernate valida a coluna.
- Verificação ao vivo do lado ADMIN (toggle + banner + persistência + auditoria).
