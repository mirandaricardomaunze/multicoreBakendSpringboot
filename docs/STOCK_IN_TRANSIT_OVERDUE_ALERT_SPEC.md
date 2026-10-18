# Stock em Trânsito - Monitorização e Alertas Proactivos de Atraso

## 1. Problema
Após o despacho no armazém de origem, uma transferência permanece no estado `IN_TRANSIT` até que o
armazém de destino confirme a recepção física. Se a equipa do destino se esquecer de conferir ou se houver
atraso/extravio no transporte, a mercadoria pode ficar "esquecida" sem que a gestão perceba, a menos que
consulte manualmente a grelha de transferências.

## 2. Regras de Monitorização e Alertas Proactivos

- **R1 - Limiar de Atraso (Warning):**
  Toda a guia no estado `IN_TRANSIT` cujo tempo decorrido desde o despacho (`dispatchedAt`, ou `transferDate` como fallback) seja igual ou superior a **48 horas (2 dias)** gera um alerta com urgência **Atenção** (`priority = 2`) no `NotificationFeed` e no `SmartAlertsDialog` (Sino de Notificações).
- **R2 - Limiar Crítico (Critical):**
  Se a guia permanecer em trânsito por **5 dias ou mais (>= 120 horas)**, o alerta é promovido a **Crítico** (`priority = 3`).
- **R3 - Detalhes do Alerta:**
  - Categoria: `Stock em Trânsito`
  - Título: `Guia <Nº Guia> em trânsito há <X dias/horas>`
  - Detalhe: `<Armazém Origem> → <Armazém Destino> · Despachada há <tempo> por <Motorista> · Recepção pendente`
- **R4 - Ação e Navegação Direta:**
  Ao clicar na notificação (`moduleCard = "stock_transfers"`):
  - Rótulo de ação no `SmartAlertsDialog`: `Verificar Recepção`
  - O sistema navega para o módulo `Stock & Armazéns` e abre automaticamente o separador de Transferências entre Armazéns (`stockPanel.showWarehouseTransfers()`).
- **R5 - Resiliência e Isolamento:**
  A chamada ao `StockTransferApiClient.findInTransit(companyId, null)` é isolada por `try-catch`, degradando suavemente sem impedir o carregamento dos restantes alertas do sistema.
- **R6 - Retrocompatibilidade:**
  `NotificationFeed` mantém todos os construtores sobrecarregados anteriores para não quebrar testes unitários existentes.

## 3. Harness de Validação
`StockInTransitOverdueAlertHarnessTest`:
- Guia em trânsito recente (< 48h) não gera alerta.
- Guia em trânsito há 48h gera alerta de prioridade 2 (Warning) com `stock_transfers`.
- Guia em trânsito há 5 dias gera alerta de prioridade 3 (Critical).
- Navegação a partir de `stock_transfers` abre o separador de transferências.
