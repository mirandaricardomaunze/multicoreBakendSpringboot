# HARNESS — Lote de UX das tabelas

Complementa [UI_TABELAS_UX_SPEC.md](UI_TABELAS_UX_SPEC.md). Automáticos sobre a lógica pura
(JUnit, molde `TableFilterTest`); manuais na UI.

## Automáticos

| ID    | Componente | Cenário | Esperado |
|-------|------------|---------|----------|
| UX-01 | TableNavigator | `overflowed(min,max,extent)` com conteúdo que transborda | `true` |
| UX-02 | TableNavigator | `overflowed` com conteúdo que cabe (extent ≥ max-min) | `false` |
| UX-03 | TableEmptyState | `resolveText` sem client-property | `"Sem registos."` |
| UX-04 | TableEmptyState | `resolveText` com `emptyText="Sem encomendas."` | `"Sem encomendas."` |
| UX-05 | TableContextMenu | `rowToText({"A", 2, null})` | `"A\t2\t"` (nulos → vazio, separado por tab) |
| UX-06 | TableContextMenu | `cellToText(null)` / valor | `""` / texto do valor |

## Manuais (UI)

| ID    | Cenário | Evidência |
|-------|---------|-----------|
| UX-50 | Abrir tabela longa | Barra lateral aparece à direita (fora da tabela) |
| UX-51 | Abrir/filtrar até a tabela caber toda | Barra **desaparece** (auto-hide) |
| UX-52 | Focar tabela e premir Home/End/PgUp/PgDn | Salta topo/fundo/página |
| UX-53 | Tabela sem registos | Mostra "Sem registos." (ou texto próprio) centrado |
| UX-54 | Botão direito numa linha | Menu: Copiar linha/célula, Ir topo/fundo; a linha fica seleccionada |
| UX-55 | Atualizar a aba Guias de Remessa | Cursor de espera durante a chamada; tabela preenche ao chegar |

## Definition of done

- `mvn -o clean compile` passa.
- `mvn -o test` passa (novos testes UX-01..06).
- Sem emojis (só `UIHelper.icon`).
- `tasks/current.md` actualizado.
