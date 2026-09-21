# Especificação Canónica: Painel de Decisão Executiva Unificado (Dashboard 360°)
**Código:** SPEC-D360-001  
**Módulo Principal:** `desktop` (`mz.multicore.erp.gui.DashboardPanel`, `mz.multicore.erp.gui.components.StrategicPulseWidget`)  
**Data:** 2026-09-17  
**Estado:** APROVADO / EM IMPLEMENTAÇÃO  

---

## 1. Objectivo e Visão do Negócio
O **Painel de Decisão Executiva Unificado (Dashboard 360° — "Pulso Estratégico da Empresa")** agrega num único cockpit visual de alta densidade no topo do Dashboard os quatro pilares estratégicos de governação do Multicore ERP:
1. **Compliance & Fraude Forense:** Monitorização contínua de integridade operacional, rácio de conformidade (*Compliance Score*), total de anomalias críticas/suspeitas e exposição financeira.
2. **Liquidez Previsional (30 Dias):** Posição de tesouraria imediata versus saldo final projetado a 30 dias com alerta pró-ativo de défice (*Cash Shortage Alert*).
3. **Risco de Crédito em Mora (Aging & Cobrança):** Volume total de dívida vencida em risco, número de clientes com mora crítica (>60d) e provisão financeira recomendada.
4. **Metas Comerciais & Eficiência da Força de Vendas:** Concretização das metas de vendas do período activo, volume faturado face ao target e rácio de eficiência comercial.

Para além da leitura imediata dos 4 indicadores, o cockpit disponibiliza atalhos de **navegação executiva em 1 clique** para os respectivos módulos aprofundados (`ForensicAuditPanel`, `CashFlowForecastPanel`, `CreditRiskPanel` e `PerformancePanel`).

---

## 2. Decisões Arquiteturais e Restrições Não-Negociáveis
- **Desacoplamento e Limite de Linhas:** O `DashboardPanel.java` delega toda a composição visual e lógica assíncrona do cockpit no componente dedicado `StrategicPulseWidget.java`. O `DashboardPanel` mantém-se estritamente abaixo de 700 linhas (limite imposto: $\le 1000$ linhas).
- **Isolamento de Camadas (Reactor Maven):** O componente opera no módulo `desktop`, comunicando exclusivamente via clientes HTTP tipados (`ForensicAuditApiClient`, `CashFlowForecastApiClient`, `CreditRiskApiClient`, `PerformanceApiClient`) consumindo DTOs imutáveis de `contracts`. Sem JPA, sem Flyway, sem OpenPDF em compile scope.
- **Não-bloqueio da EDT (SwingWorker):** Todo o carregamento das 4 fontes de dados decorre em threads de segundo plano com tratamento de erros silencioso/resiliente (exibindo "—" ou estado neutro em caso de offline sem quebrar a interface).
- **Design System e Cores Semânticas:** Utilização estrita dos tokens de cor de `UIHelper` (`UIHelper.BG_DARK`, `UIHelper.PANEL_BG`, `UIHelper.TEXT_LIGHT`, `UIHelper.TEXT_MUTED`, `UIHelper.APPROVED_GREEN`, `UIHelper.REJECTED_RED`, `UIHelper.PENDING_YELLOW`, `UIHelper.ACCENT_BLUE`). Zero literais `new Color(...)` dispersos.

---

## 3. Composição Visual dos 4 Cartões do Pulso Estratégico

| Pilar | Título do Cartão | Métrica Principal | Métrica Secundária | Semáforo / Ação de 1-Clique |
|---|---|---|---|---|
| **1. Forense** | COMPLIANCE & FRAUDE | Score % (ex.: `98.5%`) | `X anomalias críticas` · `Y MT risco` | Verde se score $\ge 95\%$, Amarelo se $\ge 80\%$, Vermelho se $< 80\%$. Botão: `[ Dossiê Forense ]` |
| **2. Liquidez** | LIQUIDEZ PREVISIONAL | Saldo Projetado 30d | `Entradas / Saídas 30d` | Verde (Saudável) ou Vermelho (Défice Previsto). Botão: `[ Projeção ]` |
| **3. Crédito** | RISCO DE CRÉDITO | Total Vencido (MT) | `N clientes em risco crítico` | Amarelo se > 0, Verde se 0. Botão: `[ Matriz de Risco ]` |
| **4. Metas** | METAS COMERCIAIS | % Concretização (ex.: `84.2%`) | `Realizado vs Target (MT)` | Azul / Roxo corporativo. Botão: `[ Desempenho ]` |

---

## 4. Navegação Unificada em 1-Clique
O `StrategicPulseWidget` recebe um callback funcional `Consumer<String> navigationHandler`:
- `navigationHandler.accept("auditoria_forense")` $\rightarrow$ Abre `ForensicAuditPanel`.
- `navigationHandler.accept("previsao_tesouraria")` $\rightarrow$ Abre `FinanceiroPanel` com a aba de Projeção Previsional seleccionada.
- `navigationHandler.accept("risco_credito")` $\rightarrow$ Abre `ClientesPanel` com a aba de Risco de Crédito seleccionada.
- `navigationHandler.accept("desempenho")` $\rightarrow$ Abre `PerformancePanel`.
