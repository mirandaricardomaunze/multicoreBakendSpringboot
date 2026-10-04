# HARNESS: «Quick Peek» Silencioso em Tabelas Multicore ERP

## 1. Objectivo do Harness
Garantir a verificação automatizada contínua das garantias de produto, ergonomia silenciosa e integridade operacional do `TableQuickPeekController` e `QuickPeekPanel`.

---

## 2. Casos de Teste do Harness

| ID | Cenário | Comportamento Esperado |
|---|---|---|
| **PEEK-01** | Inicialização silenciosa | Ao instalar o controlador na tabela, o painel lateral é criado com `isVisible() == false` e largura 0, sem ruído visual. |
| **PEEK-02** | Acionamento por Tecla Espaço (`SPACE`) | Pressionar a barra de espaço com uma linha selecionada abre o Quick Peek exibindo título, badge de estado e campos chave. |
| **PEEK-03** | Fecho por Tecla `ESC` e `SPACE` | Pressionar `ESC` ou `SPACE` novamente fecha o painel e devolve o foco à tabela. |
| **PEEK-04** | Atualização Reativa por Navegação (`UP` / `DOWN`) | Com o painel aberto, alterar a linha selecionada na tabela atualiza instantaneamente o conteúdo do painel com os dados da nova linha. |
| **PEEK-05** | Fallback Gracioso sem Seleção | Pressionar `SPACE` com a tabela vazia ou sem linha selecionada não dispara exceções; se existirem linhas, seleciona a primeira e abre. |
| **PEEK-06** | Ação Rápida de Abertura Completa | O botão "Ver Completo" no rodapé do painel dispara o callback registrado (ex.: abertura do diálogo completo). |

---

## 3. Implementação dos Testes
Classe canónica: `mz.multicore.erp.gui.components.TableQuickPeekHarnessTest`.
