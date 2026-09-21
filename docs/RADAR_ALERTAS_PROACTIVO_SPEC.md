# Especificação Técnica Canónica: Radar de Alertas Inteligentes & Risco Proactivo

**Identificador:** `SPEC-RAP-001`  
**Versão:** `1.0.0`  
**Data:** `2026-09-16`  
**Autor:** `Multicore ERP Architecture Team`  
**Status:** `Aprovado`  

---

## 1. Visão Geral & Enquadramento de Negócio

No ambiente empresarial moderno, a deteção tardia de riscos financeiros e operacionais acarreta custos de oportunidade elevados, perdas de inventário por falta de decisão e estrangulamento de liquidez por incumprimento de clientes.

O **Radar de Alertas Inteligentes & Risco Proactivo** unifica o sino de notificações superior (`NotificationFeed`), a página dedicada de notificações (`NotificationsPanel`) e o diálogo modal de intervenção rápida (`SmartAlertsDialog`), monitorizando continuamente a saúde do negócio sem exigir que os gestores abram múltiplos relatórios manuais.

---

## 2. Fontes e Vetores de Alerta

| Vetor de Risco | Origem / Serviço | Critério de Disparo | Prioridade | Ação Rápida (1-clique) | Módulo Destino |
|---|---|---|---|---|---|
| **Risco de Crédito Crítico** | `CreditRiskApiClient` | Clientes com nível `CRITICAL` (>60d mora) ou bloqueados por limite excedido | 3 (🔴 Crítico) | `"Cobrar / Ver Risco"` | `risco_credito` (`ClientesPanel` -> Aba Aging) |
| **Risco de Crédito Elevado** | `CreditRiskApiClient` | Clientes com nível `HIGH` (>30d mora) ou saldo em mora expressivo | 2 (🟡 Atenção) | `"Cobrar / Ver Risco"` | `risco_credito` (`ClientesPanel` -> Aba Aging) |
| **Quebras Pendentes de Validação** | `StockWasteApiClient` | Quebras de stock registadas com estado `PENDING_APPROVAL` | 2 (🟡 Atenção) | `"Aprovar Quebras"` | `stock_waste` (`StockPanel` -> Aba Quebras) |
| **Stock Crítico / Vencimentos** | `InventoryApiClient` | Lotes expirados ou produtos abaixo do stock mínimo | 3 / 2 | `"Resolver Imediatamente"` | `stock` (`StockPanel`) |
| **Metas em Atraso Crítico** | `PerformanceApiClient` | Metas comerciais com ritmo `< 60%` do ideal e tempo `> 50%` decorrido | 2 (🟡 Atenção) | `"Acompanhar Meta"` | `desempenho` (`PerformancePanel`) |
| **Aprovações Pendentes** | `ApprovalApiClient` | Ordens, despesas ou faturas aguardando alçada | 2 (🟡 Atenção) | `"Abrir Módulo"` | `approvals` (`ApprovalsPanel`) |

---

## 3. Comportamento e Resolução de Rotas

Quando o utilizador aciona a ação contextual no `SmartAlertsDialog` ou na lista de notificações:
1. O router interno do `MainFrame` intercepta os identificadores virtuais:
   - `risco_credito`: alterna o `CardLayout` para o cartão `"clientes"` e invoca `clientesPanel.selectCreditRiskTab()`.
   - `stock_waste`: alterna o `CardLayout` para o cartão `"stock"` e invoca `stockPanel.selectWasteTab()`.
2. A transição é imediata, sem bloquear a Event Dispatch Thread (EDT).
3. Caso os serviços de retaguarda estejam temporariamente inacessíveis, o feed degrada suavemente via `try-catch`, nunca impedindo a abertura da interface gráfica nem gerando popups bloqueantes de erro.

---

## 4. Requisitos de Estilo e Decomposição

1. Cores do Semáforo Canónico:
   - Crítico: `UIHelper.REJECTED_RED`
   - Atenção: `UIHelper.PENDING_YELLOW`
   - Informativo: `UIHelper.ACCENT_BLUE`
2. Altura dos botões de ação e controlos: uniformizada a `38px` (`UIHelper.FORM_CONTROL_HEIGHT`).
3. `MainFrame.java` deve preservar o seu teto máximo de 1000 linhas, mantendo métodos concisos e delegação clara.
