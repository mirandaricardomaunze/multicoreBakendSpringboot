# Especificação Técnica: Atalhos Favoritos Personalizáveis na Barra Lateral (SPEC-SFAV-001)

## 1. Visão Geral & Objetivos
Esta especificação define o sistema de **Atalhos Favoritos Personalizáveis** na barra lateral executiva (`CollapsibleSidebar`). O objetivo é permitir que cada operador ou gestor fixe os seus módulos mais utilizados na secção de acesso rápido **"⭐ FAVORITOS"** no topo do menu lateral, acelerando a navegação diária e personalizando a experiência de trabalho.

## 2. Regras de Negócio & UX

### 2.1. Secção de Favoritos na Barra Lateral
1. A secção **"⭐ FAVORITOS"** é exibida no topo do corpo da `CollapsibleSidebar`, imediatamente acima da secção **"OPERAÇÕES"**.
2. Quando não existirem favoritos salvos pelo utilizador, a secção assume um conjunto recomendado por omissão:
   - `Painel Inicial`
   - `POS — Caixa`
   - `Stock & Armazéns`
3. Qualquer item marcado como favorito aparece duplicado no topo sob a secção **"⭐ FAVORITOS"**, mantendo a sua funcionalidade e ícone semântico originais.

### 2.2. Gestão de Favoritos via UI (Menu de Contexto)
1. **Clique com o Botão Direito (`MouseEvent.BUTTON3`)**:
   - Ao clicar com o botão direito em qualquer item de navegação (`SidebarNavItem`), é exibido um menu de contexto (`JPopupMenu`).
   - Se o item não for favorito: mostra a opção **"⭐️ Fixar nos Favoritos"**.
   - Se o item já for favorito: mostra a opção **"❌ Remover dos Favoritos"**.
2. **Atualização Instantânea**: A alteração tem efeito imediato na barra lateral sem necessidade de reiniciar a aplicação ou recarregar a janela.
3. **Indicador Visual**: Os itens marcados como favoritos exibem uma estrela dourada translúcida `⭐` ou indicador de favorito quando a barra lateral está expandida.

### 2.3. Persistência de Preferências
1. As preferências de favoritos são salvas por utilizador em formato JSON no ficheiro local:
   `${user.home}/.multicore/user_favorites.json`
2. O salvamento é atómico e tolerante a falhas (se o ficheiro estiver corrompido ou inacessível, o gestor recorre aos favoritos padrão sem interromper a execução).

## 3. Arquitetura de Componentes (`desktop`)

### 3.1. `SidebarFavoritesManager.java` (`mz.multicore.erp.gui.components`)
- Singleton thread-safe responsável por carregar, guardar e gerir a lista de rótulos favoritos.
- Métodos públicos:
  - `isFavorite(String label)`: verifica se o rótulo é favorito.
  - `toggleFavorite(String label)`: alterna o estado de favorito.
  - `getFavorites()`: devolve a lista imutável de favoritos ativos.
  - `addChangeListener(Runnable listener)`: subscreve notificações de alteração para atualização de UI.

### 3.2. `CollapsibleSidebar.java` & `SidebarNavItem.java`
- `CollapsibleSidebar` subscreve o `SidebarFavoritesManager` e reconstrói dinamicamente os itens da secção **"⭐ FAVORITOS"**.
- `SidebarNavItem` intercepta cliques de botão direito para exibir o menu de contexto de favoritos.

## 4. Critérios de Aceitação & Conformidade
- Todos os testes automatizados da suíte `SidebarFavoritesHarnessTest` devem passar a 100%.
- A classe `CollapsibleSidebar` e `SidebarNavItem` devem manter-se estritamente abaixo do limite de 1.000 linhas.
