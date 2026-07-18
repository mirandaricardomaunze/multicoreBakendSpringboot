# Harness — Inventário físico (contagem cega)

> Cenários para [INVENTARIO_FISICO_SPEC.md](INVENTARIO_FISICO_SPEC.md). PDF + UI são **manuais**; a
> orquestração das sessões é **automática** (`InventoryCountServiceTest`). A regra do ajuste
> (MANAGER/ADMIN + auditoria) reusa `adjustStock`, já coberto por testes.

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
| IF-58 | "Inventário Físico" → **Nova Contagem** → escolher armazém → criar. | Cria uma sessão **DRAFT** com uma linha por artigo do armazém e abre-a para contar (SKU · Artigo · Contagem, **sem** quantidade do sistema). |
| IF-59 | Numa sessão DRAFT, preencher contagens → **Guardar Rascunho**; fechar e reabrir (**Abrir**). | As contagens guardadas persistem — a sessão é retomável mais tarde (não se perdem ao fechar). |
| IF-60 | Numa sessão DRAFT com contagens → **Aplicar Ajustes**. | Só as linhas contadas geram ajuste; a sessão passa a **Aplicada** (só-leitura); resumo com diferenças (sistema → contado, ±); stock acertado + Movimentos auditados. |
| IF-61 | Contar um artigo com valor **igual** ao sistema (ex.: 10 = 10) **e** outro com diferença → Aplicar. | **Não** falha nem faz rollback: a linha igual é no-op (sem movimento), a diferente ajusta; a sessão fecha na mesma. |
| IF-62 | **Cancelar Sessão** numa DRAFT; depois tentar aplicar/guardar/cancelar uma sessão já aplicada/cancelada. | Cancelada fica **Cancelada** (stock intacto); operações sobre não-DRAFT recusadas ("já foi aplicada ou cancelada"). |
| IF-63 | **Abrir** uma sessão já **Aplicada**. | Vista só-leitura com colunas Contagem · Sistema · **Diferença** (histórico da reconciliação). |
| IF-64 | REST: `POST /api/inventory/counts` → `PUT /{id}/counts` → `POST /{id}/apply` (ou `/cancel`); `GET /api/inventory/counts?companyId=`. | Mesma orquestração da UI via HTTP; `@Valid` + regras (MANAGER/ADMIN, tenant) aplicadas; violações → resposta de erro global. |

## Verificação

- `mvn -o compile` limpo; arranque do desktop sem erros de bean (`InventoryCountService` injectado em StockPanel ← MainFrame).
- **Automático:** `InventoryCountServiceTest` cobre a orquestração das sessões (criar DRAFT com linha por artigo, aplicar só as linhas contadas, **no-op da linha igual ao sistema**, guardas de estado, cancelar); um `@SpringBootTest` de contexto arranca com o `InventoryCountController` registado e a migração `V28` aplicada.
- Verificação ao vivo: geração do PDF da folha de contagem (conteúdo cego) + gestor de sessões (criar / guardar / retomar / aplicar / cancelar) funcional.
