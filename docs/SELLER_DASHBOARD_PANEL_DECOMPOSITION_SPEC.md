# SPEC-SELLER-DASHBOARD-PANEL-DECOMPOSITION-001: Decomposição Modular do Painel do Vendedor

## 1. Visão Geral & Contexto Operacional

O painel `SellerDashboardPanel.java` constitui a interface principal e dedicada para o perfil de utilizador Vendedor (`SELLER`) no Multicore ERP, fornecendo uma experiência limpa, focada e ágil na sessão de caixa, vendas POS, encomendas e cotações, sem expor métricas globais de gestão ou tesouraria corporativa.

Actualmente com 167 linhas, o painel centraliza a montagem dos cartões de KPI, card de ações rápidas com 6 atalhos operacionais e o ciclo assíncrono de leitura de sessão ativa (`POSApiClient`) e propostas abertas (`ComercialApiClient`).

Esta especificação define a extração da coordenação de apresentação e agregação de snapshot para um colaborador especializado.

---

## 2. Metas de Redução e Desacoplamento

| Componente | Linhas Atuais | Meta Máxima | Responsabilidade Canónica |
| :--- | :--- | :--- | :--- |
| `SellerDashboardPanel.java` | 167 | $\le 110$ | Orquestrador de visualização, layout de KPIs e botões de ação rápida |
| `SellerDashboardViewCoordinator.java` | 0 (Novo) | $\le 95$ | Agregação assíncrona de sessão/relatório Z, povoamento de KPIs e saudação do vendedor |

---

## 3. Contratos e Invariantes Preservados

1. **Construtores e Métodos Públicos:**
   - `public SellerDashboardPanel(ComercialApiClient comercialApiClient, POSApiClient posApiClient, Consumer<String> actionHandler)`.
   - `public void updateWelcomeMessage(String displayName)`.
   - `public void refreshData()`.
2. **KPIs Canónicos e Ações Rápidas (UXV-01 a UXV-05):**
   - 4 KPIs canónicos com altura de 96px (`KpiCard.STANDARD_CARD_HEIGHT`): `"VENDAS NA SESSÃO"`, `"ATENDIMENTOS"`, `"ENCOMENDAS PENDENTES"`, `"COTAÇÕES ABERTAS"`.
   - 6 ações rápidas: `"Nova venda POS"`, `"Nova encomenda"`, `"Nova cotação"`, `"Pesquisar clientes"`, `"Consultar produtos"`, `"Notificações"`.
   - Ausência absoluta de indicadores de gestão (fiscal, tesouraria, salário, aprovações).
3. **Feedback Profissional e Zero `JOptionPane`:**
   - Total ausência de `JOptionPane`.
