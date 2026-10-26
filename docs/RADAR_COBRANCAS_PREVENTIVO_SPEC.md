# Especificação Canónica — Radar Inteligente de Cobranças & Lembrete Preventivo

## 1. Contexto e Justificação de Negócio

No modelo comercial B2B e distribuição em Moçambique, a cobrança puramente reativa (após o atraso de 30 a 90 dias) gera perdas severas de tesouraria e atrito na relação com os clientes.
O **Radar Inteligente de Cobranças** adiciona uma visão preditiva e preventiva à gestão de contas correntes, dividindo as obrigações a receber em horizontes temporais imediatos e futuros:
- **Vencimento Crítico:** Faturas vencidas há mais de 15 dias (risco elevado).
- **Vencimento Recente:** Faturas vencidas de 1 a 15 dias (lembrete de regularização imediata).
- **Vencimento Hoje:** Faturas cujo prazo expira na data corrente.
- **Prevenção a 7 Dias:** Faturas a vencer nos próximos 7 dias (lembrete amigável de tesouraria).
- **Prevenção a 15 Dias:** Faturas a vencer nos próximos 8 a 15 dias (planeamento de liquidação).
- **Prevenção a 30 Dias:** Faturas a vencer nos próximos 16 a 30 dias (visibilidade comercial).

Adicionalmente, o sistema disponibiliza a composição automática de **Lembretes Preventivos e Notificações de Cobrança** padronizados em português de Moçambique, prontos para envio ao cliente por Email, WhatsApp corporativo ou SMS, contendo número da fatura, valor em aberto, data de vencimento e coordenadas bancárias.

---

## 2. Contratos e DTOs (`contracts`)

1. `CollectionRadarBucket`:
   - `CRITICAL_OVERDUE` ("Vencidas > 15 dias", Vermelho Alerta)
   - `RECENT_OVERDUE` ("Vencidas 1-15 dias", Laranja Alerta)
   - `DUE_TODAY` ("Vencem Hoje", Amarelo Destaque)
   - `DUE_IN_7_DAYS` ("Vencem em 7 dias", Azul Preventivo)
   - `DUE_IN_15_DAYS` ("Vencem em 15 dias", Ciano)
   - `DUE_IN_30_DAYS` ("Vencem em 30 dias", Violeta)

2. `CollectionRadarItemDTO`:
   - `invoiceId` (Long)
   - `invoiceNumber` (String)
   - `clientId` (Long)
   - `clientName` (String)
   - `clientNuit` (String)
   - `clientPhone` (String)
   - `clientEmail` (String)
   - `issueDate` (LocalDate)
   - `dueDate` (LocalDate)
   - `totalAmount` (BigDecimal)
   - `paidAmount` (BigDecimal)
   - `remainingBalance` (BigDecimal)
   - `daysDiff` (int - negativo se vencido, positivo se a vencer)
   - `bucket` (CollectionRadarBucket)

3. `CollectionRadarSummaryDTO`:
   - `companyId` (Long)
   - `referenceDate` (LocalDate)
   - `criticalOverdueTotal` (BigDecimal), `criticalOverdueCount` (int)
   - `recentOverdueTotal` (BigDecimal), `recentOverdueCount` (int)
   - `dueTodayTotal` (BigDecimal), `dueTodayCount` (int)
   - `dueIn7DaysTotal` (BigDecimal), `dueIn7DaysCount` (int)
   - `dueIn15DaysTotal` (BigDecimal), `dueIn15DaysCount` (int)
   - `dueIn30DaysTotal` (BigDecimal), `dueIn30DaysCount` (int)
   - `totalReceivable` (BigDecimal), `totalInvoicesCount` (int)

4. `CollectionReminderTemplateDTO`:
   - `channel` (String: "WHATSAPP", "SMS", "EMAIL")
   - `subject` (String)
   - `message` (String)

---

## 3. Backend Headless (`backend`)

- `CollectionRadarService`:
  - `getRadarSummary(Long companyId, LocalDate referenceDate)`: agrega saldos em aberto e agrupa conforme a distância em dias do vencimento.
  - `getRadarItems(Long companyId, CollectionRadarBucket bucket, LocalDate referenceDate)`: lista itens detalhados com informações de contacto e ordenação de prioridade.
  - `generateReminderTemplate(Long invoiceId, String channel)`: gera mensagem personalizada em pt-MZ com dados do cliente e da fatura.
- `CollectionRadarController`:
  - `GET /api/comercial/radar/summary`
  - `GET /api/comercial/radar/items?bucket={bucket}`
  - `GET /api/comercial/radar/invoices/{id}/reminder?channel={channel}`

---

## 4. Desktop Client (`desktop`)

- `ComercialApiClient`:
  - `getRadarSummary(Long companyId)`
  - `getRadarItems(Long companyId, String bucket)`
  - `getReminderTemplate(Long invoiceId, String channel)`
- `CollectionRadarDialog` (ou painel integrado):
  - KPIs canónicos com `KpiCard.createGrid(...)` exibindo montantes e contagens de cada horizonte de cobrança.
  - Tabela com filtros por bucket, pesquisa de cliente e atalho de visualização rápida (`TableQuickPeekController`).
  - Botão de ação "Gerar Lembrete" que abre o modal de visualização e cópia de mensagem (WhatsApp/Email/SMS).

---

## 5. Harness e Verificação

- `CollectionRadarServiceTest`: valida agregação correta de montantes por horizonte temporal e geração de textos de lembrete em pt-MZ.
- `CollectionRadarUiHarnessTest`: valida integridade dos DTOs, cálculos de balanço e formatação de canais.
- `MultiModuleArchitectureHarnessTest`: assegura separação estrita de camadas.
- `DesktopThinContextTest`: confirma integridade da UI desktop no arranque.
