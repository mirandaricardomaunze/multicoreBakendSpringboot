# Harness — Paginação uniforme das tabelas

## Automatizado

Executar:

```powershell
mvn -q -Dtest=ClientTablePaginationTest,TableFilterTest,TablePagerTest test
mvn -q clean compile
mvn -q test
```

| Caso | Verificação | Resultado esperado |
|---|---|---|
| TP-01 | Listagem local com 60 linhas | 50 linhas na página 1 e 10 na página 2 |
| TP-02 | Aplicar filtro com 7 resultados estando na página 2 | regressa à página 1 e mostra 7 |
| TP-03 | Controlos nos extremos | anterior desactivado na primeira; seguinte na última |
| TP-04 | Pesquisa/estado/período | filtro é aplicado antes da paginação |
| TP-05 | Modelo cresce ou diminui | total e limite de página recalculados |
| TP-06 | Faturas, vendas POS e diário | usam `TablePager` e `PageResponse`, sem dupla barra |
| TP-07 | Carrinho e linhas de documento | não recebem paginação |
| TP-08 | Paginação seguida de botões de acção | 10 px de separação vertical; controlos com 8 px entre si |

## Manual no Windows

1. Abrir Clientes, Produtos/Stock, Compras, RH, CRM, Financeiro, Fiscal, Configuração e Plataforma.
2. Numa listagem com mais de 50 registos, navegar primeira/anterior/seguinte/última.
3. Trocar “Por página” entre 25, 50, 100 e 200.
4. Na página 2, pesquisar um texto que devolva poucos registos; confirmar retorno à página 1.
5. Confirmar que selecção, duplo clique, menu de contexto, exportação e botões de acção usam a
   linha correcta.
6. Confirmar que carrinho POS, linhas de fatura/encomenda e diálogos continuam sem paginação.
