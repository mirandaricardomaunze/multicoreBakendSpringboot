# Harness — Inventário físico (contagem cega)

> Cenários para [INVENTARIO_FISICO_SPEC.md](INVENTARIO_FISICO_SPEC.md). Todos **manuais** (PDF + UI +
> orquestração de ajustes). A regra do ajuste (MANAGER/ADMIN + auditoria) reusa `adjustStock`, já
> coberto por testes.

## Manuais

| ID    | Passos | Esperado |
|-------|--------|----------|
| IF-50 | Stock → botão "Inventário Físico". | Abre diálogo com selector de armazém + tabela (SKU · Artigo · Contagem editável) + "Imprimir Folha de Contagem". |
| IF-51 | Escolher armazém → "Imprimir Folha de Contagem". | Gera/abre um PDF: cabeçalho da empresa, artigos (Ref/Cód/Nome), coluna **Contagem em branco**, **sem** quantidades do sistema; blocos "Contado por / Conferido por". |
| IF-52 | Trocar de armazém no combo. | A tabela recarrega com os artigos desse armazém. |
| IF-53 | Introduzir contagens em alguns artigos, deixar outros em branco, "Aplicar Ajustes". | Só os preenchidos são ajustados; resumo mostra **diferenças** (sistema → contado, ±). Os em branco ficam intactos. |
| IF-54 | Confirmar em Níveis de Stock que as quantidades ficaram iguais às contadas. | Stock igual ao contado; Movimentos regista os ajustes (auditados). |
| IF-55 | Contagem não numérica ou vazia. | Ignorada (não ajusta); sem erro. |
| IF-56 | Combinar com bloqueio: trancar stock (ADMIN) e imprimir a folha para o funcionário. | Folha sem quantidades + app sem quantidades para o não-admin = contagem verdadeiramente cega. |
| IF-57 | Aplicar como utilizador sem permissão de ajuste. | Ajustes recusados (guarda MANAGER/ADMIN); resumo indica o erro por artigo. |

## Verificação

- `mvn -o compile` limpo; arranque do desktop sem erros de bean (novo print service injectado).
- Verificação ao vivo: geração do PDF da folha de contagem (conteúdo cego) + diálogo funcional.
