# Matriz de Testes & Harness: Densidade de Interface & Escala (HARNESS-DENS-001)

Este documento especifica a suíte de validação automatizada para a Densidade de Interface e Escala de Tipografia (`UiDensityZoomHarnessTest`).

## Matriz de Validação

| ID | Cenário de Teste | Comportamento Esperado | Resultado Requerido |
|---|---|---|---|
| **DENS-01** | Integridade dos Níveis de Densidade | `UiDensity.COMPACT`, `STANDARD` e `COMFORTABLE` possuem alturas e escalas válidas | 🟢 PASS |
| **DENS-02** | Resolução por Identificadores | `UiDensity.byId` resolve `"compact"`, `"standard"`, `"comfortable"` e aceita variações com fallback | 🟢 PASS |
| **DENS-03** | Ciclo Circular de Densidade | `cycleDensity()` transiciona na ordem canónica `STANDARD -> COMPACT -> COMFORTABLE -> STANDARD` | 🟢 PASS |
| **DENS-04** | Atualização do `UIManager` | Alterar a densidade atualiza `Table.rowHeight` com a altura correspondente | 🟢 PASS |
| **DENS-05** | Aplicação em Tabelas (`styleTable`) | Tabelas estilizadas adotam a altura de linha da densidade ativa | 🟢 PASS |
| **DENS-06** | Persistência e Restauração | Preferência salva no `Preferences` é recarregada fielmente no arranque | 🟢 PASS |
| **DENS-07** | Ouvintes de Alteração | Listeners registados são notificados quando a densidade é alterada | 🟢 PASS |
| **DENS-08** | Limite Estrito de Linhas de Código | Classes criadas e modificadas (`UiDensity`, `UiDensityManager`, `ConfigPanel`, `MainFrame`) $\le 1000$ linhas | 🟢 PASS |
