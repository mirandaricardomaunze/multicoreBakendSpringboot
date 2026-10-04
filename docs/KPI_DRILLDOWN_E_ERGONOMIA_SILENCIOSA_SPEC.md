# SPEC — KPI Drilldown Interativo e Ergonomia Invisível de Tabelas

**Criado em:** 2026-10-04  
**Camada:** Cliente desktop Swing (`mz.multicore.erp.gui.components`, `mz.multicore.erp.gui`)  
**Backend:** Inalterado (regras de apresentação e ergonomia no desktop).

---

## 1. Princípios e Diagnóstico

### 1.1 O Desafio do Ruído Visual
Em sistemas empresariais com alta densidade de informação, adicionar botões adicionais, popups intrusivos ou cores berrantes gera poluição cognitiva e fadiga visual no operador.
Uma interface de nível executivo e profissional alcança máxima usabilidade tornando **os elementos já existentes interactivos e contextuais**:
1. **Zero Ruído Visual:** Nenhuma barra ou botão extra é adicionado ao layout base.
2. **Descoberta Natural:** O cursor do rato (`HAND_CURSOR`) e um realce de borda ultra-sóbrio comunicam imediatamente que o elemento responde ao toque.
3. **Ergonomia Silenciosa:** Acções de consulta e detalhe são acionadas por gestos padrão da indústria (duplo-clique e clique direito).

---

## 2. KPI Cards Interativos (*Click-to-Filter Drilldown*)

### 2.1 Especificação em `KpiCard`
O componente `KpiCard` ganha métodos canónicos:
- `KpiCard.makeInteractive(ModernPanel card, String tooltip, Runnable onClickAction)`
- `KpiCard.createInteractiveCard(...)`
- `KpiCard.createInteractiveMetricCard(...)`

### 2.2 Comportamento e Contratos
1. **Cursor:** O cartão adopta `Cursor.HAND_CURSOR`.
2. **Acessibilidade e Foco:** O cartão torna-se focável (`setFocusable(true)`), permitindo acionamento via tecla `ENTER` ou `SPACE`.
3. **Realce Sóbrio:** No evento `mouseEntered`, a propriedade de borda (`card.border`) assume um tom ligeiramente mais brilhante (`border.brighter()`), retornando ao tom normal no `mouseExited`.
4. **Acção:** Ao clicar com o botão esquerdo, a acção do drilldown/filtro é executada de imediato.
5. **Tooltip Operacional:** Exibe dica clara em português de Moçambique (ex.: *"Clique para filtrar por este indicador"*).

---

## 3. Ergonomia Invisível de Tabelas

### 3.1 Duplo-Clique Canónico (`installRowDoubleClickHandler`)
Disponibilizado em `UIHelper.installRowDoubleClickHandler(JTable table, IntConsumer onRowDoubleClicked)`:
- Detecta duplo-clique com botão esquerdo (`clickCount == 2`).
- Converte o índice da linha de visualização para o índice do modelo (`convertRowIndexToModel`), prevenindo erros de ordenação/filtragem.
- Invoca o callback seguro apenas se uma linha válida foi atingida (`viewRow >= 0 && viewRow < rowCount`).

### 3.2 Menu de Contexto do Rato (`installRowContextMenu`)
Disponibilizado em `UIHelper.installRowContextMenu(JTable table, Function<Integer, JPopupMenu> popupProvider)`:
- Detecta o trigger de popup padrão do sistema operativo (`isPopupTrigger`).
- Selecciona automaticamente a linha sob o cursor do rato antes de abrir o menu.
- Apresenta menu pop-up limpo com ícones semânticos para acções rápidas (ex.: *"Ver Detalhes"*, *"Imprimir"*).

---

## 4. Matriz de Aplicação nos Módulos

| Componente | Gesto / Interacção | Acção Resultante |
|------------|--------------------|------------------|
| **Inventário Físico (KPI Sobras/Faltas)** | Clique no Card de KPI | Aplica filtro dinâmico de reconciliação na tabela de contagem |
| **Stock (KPI Rupturas/Disponíveis)** | Clique no Card de KPI | Filtra a lista de artigos por estado de stock |
| **Tabelas de Documentos / Listagens** | Duplo-clique na Linha | Abre visualização de detalhes ou modal executivo da entidade |
| **Tabelas de Documentos / Listagens** | Clique Direito na Linha | Apresenta menu de contexto rápido da linha seleccionada |
