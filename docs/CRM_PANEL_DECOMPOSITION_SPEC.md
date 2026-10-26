# Especificação de Decomposição Modular: CRM & Assistência ao Cliente (CRMPanel)

**Identificador:** `SPEC-CRM-PANEL-DECOMPOSITION-001`  
**Domínio:** Comercial / CRM / Gestão de Pedidos de Assistência e Folhas de Obra  
**Alvo:** `mz.multicore.erp.gui.CRMPanel.java` (299 linhas)  
**Meta de Redução:** $\le 205$ linhas no orquestrador principal ($\approx 32\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `CRMPanel.java` providencia a gestão unificada de CRM & Assistência da empresa através de duas abas operacionais ergonómicas:
1. **Pedidos de Assistência:** Tabela de tickets de clientes com status, técnico atribuído, prioridade, filtros por estado/prioridade/data e inspeção rápida (`TableQuickPeekController`).
2. **Folhas de Obra:** Tabela de ordens de serviço executadas pelos técnicos, contendo horas, tarifa horária, totalização monetária e menu semântico de acções `ActionMenuButton("Ações da Folha")`.

Actualmente, o painel orquestrador acumulava:
1. **Mapeamento de Modelos & Renderização:** Conversão manual de DTOs (`SupportTicketDTO` e `WorkSheetDTO`) para linhas de tabela com formatação de datas e valores monetários.
2. **Inspeção Rápida (Quick Peek):** Mapeamento detalhado de campos, status e botão de abertura contextual.
3. **Filtragem de Regras de Negócio:** Extração de pedidos em aberto (`openTickets()`).
4. **Construção de Abas & Cards:** Duas abas com `ModernPanel(16)`, barras de filtros `TableFilter.bar` e cabeçalhos ergonómicos com `UIHelper.tableCardTop`.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **`AdminApprovalsUnifiedHarnessTest` (`AA-02`):**
   - Conter `UIHelper.ensureHeadersFit(ticketsTable)`.
   - Conter `TableQuickPeekController.install(ticketsTable`.
2. **`HrCrmUiErgonomicsHarnessTest`:**
   - Conter textualmente:
     ```java
     UIHelper.tableCardTop("Folhas de Obra Registadas", wsBar,
                     moreBtn, billBtn, newWsBtn)
     ```
   - Conter "Ações da Folha".
   - Conter "Imprimir PDF", "Corrigir", "Anular", "Tarifa/hora".
3. **`TableActionPlacementHarnessTest`:**
   - Conter `UIHelper.tableCardTop(`.
4. **`DesktopInitializationTest`:**
   - Preservar o construtor público `CRMPanel(CRMApiClient, ComercialApiClient)`.
5. **Eliminação de Diálogos Legados:**
   - Zero chamadas a `JOptionPane`.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui/
├── CRMPanel.java                     (Orquestrador principal: <= 205 linhas)
│   ├── buildPanel() / abas ("Pedidos de Assistência" e "Folhas de Obra")
│   ├── refreshData(), applyData(), onPanelSelected()
│   └── ticketsTable, worksheetsTable, openTickets()
├── CrmViewCoordinator.java           (Mapeamento de dados e Quick Peek: <= 110 linhas)
│   ├── populateTicketPeek(...)
│   ├── populateTicketsModel(...)
│   ├── populateWorksheetsModel(...)
│   └── filterOpenTickets(...)
├── CrmTicketActions.java             (Acções sobre pedidos: existente)
└── CrmWorkSheetActions.java          (Acções sobre folhas de obra: existente)
```

---

## 4. Métricas e Limites Estritos

| Componente | Ficheiro | Teto de Linhas | Responsabilidade |
| :--- | :--- | :--- | :--- |
| **Painel Orquestrador** | `CRMPanel.java` | $\le 205$ linhas | Layout das abas, tabelas, filtros e orquestração de dados |
| **Coordenador de Visualização** | `CrmViewCoordinator.java` | $\le 110$ linhas | Povoamento de modelos, filtros de tickets e Quick Peek |
