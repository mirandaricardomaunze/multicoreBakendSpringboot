# Matriz de Testes & Harness: Atalhos Favoritos na Barra Lateral (HARNESS-SFAV-001)

Este documento especifica a suíte de validação automatizada para os Atalhos Favoritos Personalizáveis da Barra Lateral (`SidebarFavoritesHarnessTest`).

## Matriz de Validação

| ID | Cenário de Teste | Comportamento Esperado | Resultado Requerido |
|---|---|---|---|
| **SFAV-01** | Inicialização do `SidebarFavoritesManager` sem preferências | Devolve favoritos padrão (`Painel Inicial`, `POS — Caixa`, `Stock & Armazéns`) | 🟢 PASS |
| **SFAV-02** | Adição de novo favorito | `toggleFavorite("Clientes")` adiciona "Clientes" e notifica ouvintes | 🟢 PASS |
| **SFAV-03** | Remoção de favorito existente | `toggleFavorite("POS — Caixa")` remove "POS — Caixa" | 🟢 PASS |
| **SFAV-04** | Persistência local em disco | Ficheiro `user_favorites.json` é gravado e recarregado corretamente | 🟢 PASS |
| **SFAV-05** | Integração na `CollapsibleSidebar` em Headless | Sidebar cria secção "FAVORITOS" e reconstrói itens sem exceções Swing | 🟢 PASS |
| **SFAV-06** | Clique com botão direito em `SidebarNavItem` | `SidebarNavItem` dispara evento de menu de contexto | 🟢 PASS |
| **SFAV-07** | Resiliência contra ficheiros JSON inválidos/corrompidos | Gestor ignora JSON corrompido e recupera com padrões seguros | 🟢 PASS |
| **SFAV-08** | Decomposição e Linhas de Código | Classes da barra lateral mantêm-se estritamente $\le 1000$ linhas | 🟢 PASS |
