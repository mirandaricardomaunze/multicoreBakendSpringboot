# Matriz de Testes & Harness: Histórico de Itens Recentes (HARNESS-RHIS-001)

Este documento especifica a suíte de validação automatizada para o Histórico de Itens Recentes (`RecentItemsHistoryHarnessTest`).

## Matriz de Validação

| ID | Cenário de Teste | Comportamento Esperado | Resultado Requerido |
|---|---|---|---|
| **RHIS-01** | Inicialização do `RecentItemsHistoryManager` | Inicializa vazio ou carrega entradas persistidas sem falhas | 🟢 PASS |
| **RHIS-02** | Inserção e Promoção MRU | Novo item é inserido na posição 0; itens existentes com mesmo ID são promovidos ao topo com timestamp renovado | 🟢 PASS |
| **RHIS-03** | Limite de Capacidade LRU | Ao atingir a capacidade máxima (20 itens), o 21º item descarta o mais antigo da cauda | 🟢 PASS |
| **RHIS-04** | Deduplicação e Imutabilidade | Itens com mesma chave identificadora não duplicam na lista | 🟢 PASS |
| **RHIS-05** | Formatação de Tempo Relativo | `formattedTimeAgo()` calcula corretamente "Agora mesmo", "há X min", "há X h" | 🟢 PASS |
| **RHIS-06** | Persistência e Recuperação Local | Itens guardados em `recent_items.json` são recarregados fidedignamente | 🟢 PASS |
| **RHIS-07** | Operação de Limpeza (`clear()`) | `clear()` esvazia a lista e sincroniza a persistência | 🟢 PASS |
| **RHIS-08** | Limite de Linhas de Código | Classes criadas e modificadas (`RecentItem`, `RecentItemsHistoryManager`, `RecentItemsDialog`, `MainFrame`) $\le 1000$ linhas | 🟢 PASS |
