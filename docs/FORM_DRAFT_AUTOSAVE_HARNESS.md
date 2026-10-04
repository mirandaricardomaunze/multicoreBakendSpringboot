# Matriz de Testes & Harness: Auto-Salvamento de Rascunhos (HARNESS-DFRT-001)

Este documento especifica a suíte de validação automatizada para o subsistema de Auto-Salvamento & Recuperação de Rascunhos (`FormDraftAutoSaveHarnessTest`).

## Matriz de Validação

| ID | Cenário de Teste | Comportamento Esperado | Resultado Requerido |
|---|---|---|---|
| **DFRT-01** | Inicialização do `FormDraftManager` | Inicializa vazio ou carrega rascunhos em disco sem exceções | 🟢 PASS |
| **DFRT-02** | Gravação e Recuperação de Rascunho | Salva rascunho com payload e recupera fielmente todos os campos | 🟢 PASS |
| **DFRT-03** | Sobrescrita e Atualização Temporal | Gravar novamente a mesma `formKey` atualiza o payload e o timestamp | 🟢 PASS |
| **DFRT-04** | Purga após Submissão (`clearDraft`) | `clearDraft(key)` remove rascunho em memória e apaga o ficheiro do disco | 🟢 PASS |
| **DFRT-05** | Persistência em Pasta Isolada | Rascunhos gravados numa pasta temporária são persistidos e recarregáveis por nova instância | 🟢 PASS |
| **DFRT-06** | Formatação de Idade do Rascunho | `formattedTimeAgo()` formata "Agora mesmo", "há X min", "há X h" | 🟢 PASS |
| **DFRT-07** | Comportamento do Banner (`FormDraftBanner`) | Dispara callback `onRestore` com dados corretos e descarta rascunho ao clicar em descartar | 🟢 PASS |
| **DFRT-08** | Limite Estrito de Linhas de Código | Classes criadas e modificadas (`FormDraft`, `FormDraftManager`, `FormDraftBanner`, `UIHelper`) $\le 1000$ linhas | 🟢 PASS |
