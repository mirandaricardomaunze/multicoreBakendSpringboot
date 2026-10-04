# Especificação Técnica: Histórico de Itens Recentes & Quick-Recall (SPEC-RHIS-001)

## 1. Visão Geral & Objetivos
Esta especificação define o subsistema de **Histórico de Itens Recentes & Quick-Recall** (`RecentItemsHistoryManager` e `RecentItemsDialog`).
O objetivo é capacitar operadores, vendedores e gestores em Moçambique a alternar instantaneamente entre os registos operacionais mais recentes (clientes, produtos, faturas, cotações, compras e módulos) com o atalho universal `Ctrl+H` ou pelo botão dedicado na barra superior, evitando pesquisas repetidas e acelerando o fluxo diário de atendimento e conferência.

## 2. Regras de Negócio & Ergonomia de Navegação

### 2.1. Modelo Canónico `RecentItem`
Cada entrada de histórico recente é imutável e contém:
1. `id`: Chave de deduplicação (ex.: `"CLI:14"`, `"PROD:20"`, `"FAT:104"`, `"VIEW:stock"`).
2. `category`: Categoria funcional legível (ex.: `"Cliente"`, `"Produto"`, `"Fatura"`, `"Cotação"`, `"Módulo"`).
3. `title`: Título principal em destaque (ex.: `"Supermercado Recheio, Lda"`, `"Cimento Moçambique 50kg"`).
4. `subtitle`: Informação contextual complementar (ex.: `"NUIT: 400123456 | Maputo"`, `"Preço: 450,00 MT"`).
5. `targetView`: Código canónico de navegação do painel correspondente (ex.: `"clientes"`, `"stock"`, `"comercial"`, `"pos"`).
6. `recordId`: Identificador numérico do registo na base de dados (opcional, pode ser `null` para módulos).
7. `iconCode`: Código do ícone vetorial FontAwesome (ex.: `"fas-user-tie"`, `"fas-box"`, `"fas-file-invoice"`).
8. `timestampMillis`: Carimbo de data/hora em milissegundos para cálculo de tempo relativo decorrido.

### 2.2. Lógica LRU (Least Recently Used) & Deduplicação
1. **Capacidade Máxima**: O gestor armazena até 20 registos recentes.
2. **Promoção ao Topo (MRU)**: Se um item com o mesmo `id` já existir na lista, é removido da sua posição anterior e reinserido no topo (índice 0) com o timestamp atualizado.
3. **Evicção Automática**: Quando a lista atinge 20 itens e um novo item é inserido, o registo mais antigo (fim da lista) é descartado.

### 2.3. Persistência Atómica Local
1. As entradas são persistidas localmente em formato JSON no ficheiro:
   `${user.home}/.multicore/recent_items.json`
2. O carregamento é tolerante a falhas: se o ficheiro estiver ausente ou corrompido, a lista é reiniciada sem exceções.
3. Operação `clear()` limpa o histórico em memória e remove o ficheiro ou limpa o conteúdo em disco.

### 2.4. Diálogo de Acesso Rápido (`RecentItemsDialog`)
1. **Acionamento**:
   - Atalho global de teclado: `Ctrl+H`.
   - Botão de histórico na barra superior (`fas-history`).
   - Ação na Command Palette (`Ctrl+K` $\rightarrow$ `"Histórico de Itens Recentes"`).
2. **Interface & Acessibilidade**:
   - Barra de filtro instantâneo por texto no topo com foco automático.
   - Lista rica com ícone temático, crachá colorido por categoria, títulos e tempo relativo ("Agora mesmo", "há 2 min", "há 1 h").
   - Tecla `Enter` ou duplo clique navega diretamente para o registo/módulo pretendido.
   - Tecla `Esc` fecha o diálogo.
   - Botão `[ Limpar Histórico ]` com confirmação.

## 3. Critérios de Aceitação & Conformidade
- Todos os testes da suíte `RecentItemsHistoryHarnessTest` passam a 100%.
- O `MainFrame.java` e o `RecentItemsDialog.java` mantêm-se estritamente $\le 1000$ linhas.
