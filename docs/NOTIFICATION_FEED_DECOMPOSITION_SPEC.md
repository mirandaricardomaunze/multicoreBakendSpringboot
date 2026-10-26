# Especificação Arquitetural: Decomposição de NotificationFeed

- **Document ID**: SPEC-NOTIFICATION-FEED-DECOMPOSITION-001
- **Fase**: 78
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Alvo**: `desktop/src/main/java/mz/multicore/erp/gui/NotificationFeed.java` (537 linhas)

---

## 1. Contexto e Justificação

O `NotificationFeed.java` é o agregador central de alertas operacionais proativos do ERP Multicore, alimentando o ícone do sino (Bell) no cabeçalho e a página corporativa de notificações (`NotificationsPanel`). Ele acumula inteligência de alertas de múltiplos domínios de negócio: Aprovações, Inventário, Lotes, Validades, Quebras, Trânsito, Assinatura da Empresa, Contratos de Trabalho, Retenções Fiscais/Salariais, Documentos de Colaboradores, Saúde Ocupacional, Metas Comerciais, Risco de Crédito e Qualidade/EPI.

### Dívidas Técnicas Identificadas
1. **Monolito de Agregação (537 linhas)**: Acumulação excessiva de lógica de mapeamento e parsing de alertas de diferentes subsistemas num único ficheiro.
2. **Alta densidade de regras de domínio**: As regras de inventário/logística e de recursos humanos/saúde/qualidade obscurecem a lógica de agregação do feed.

---

## 2. Desenho Arquitetural da Decomposição

A decomposição mantém o `NotificationFeed` como orquestrador central, preservando todos os construtores e o record público `NotificationItem`, desacoplando os provedores de domínio:

```
+---------------------------------------------------------------------------------+
| NotificationFeed (Orquestrador Central <= 200 linhas)                           |
| - Construtores sobrecarregados retrocompatíveis                                 |
| - Record público canónico NotificationItem                                      |
| - Agregação e ordenação por prioridade no método load(companyId)                |
| - Alertas de Aprovações, Assinatura, Sistema, Metas Comerciais e Risco Crédito  |
+---------------------------------------------------------------------------------+
                 |                                               |
                 v                                               v
+-----------------------------------+           +---------------------------------+
| NotificationInventoryAlertProvider|           | NotificationHRAlertProvider     |
| (<= 150 linhas)                   |           | (<= 175 linhas)                 |
| - addLowStock                     |           | - addContracts                  |
| - addExpiries                     |           | - addPayrollLiabilities         |
| - addStockWasteAlerts             |           | - addEmployeeDocuments          |
| - addInTransitOverdue             |           | - addOccupationalHealth         |
|                                   |           | - addQualityAlerts              |
+-----------------------------------+           +---------------------------------+
```

---

## 3. Limites Físicos Estritos

| Classe | Teto de Linhas | Responsabilidade |
|---|---|---|
| `NotificationFeed.java` | $\le 200$ | Orquestrador do feed, record `NotificationItem` e alertas financeiros/sistema. |
| `NotificationInventoryAlertProvider.java` | $\le 150$ | Alertas de stock baixo, validade de lotes, quebras e trânsito em atraso. |
| `NotificationHRAlertProvider.java` | $\le 175$ | Alertas de contratos, retenções, documentos, saúde ocupacional e EPIs. |

---

## 4. Requisitos de Qualidade e Preservação de Contratos

1. **Retrocompatibilidade Rigorosa**:
   - Construtores com 4, 5, 7, 8, 9 e 10 argumentos rigorosamente preservados para não quebrar `RadarAlertasProactivoHarnessTest`, `StockInTransitOverdueAlertHarnessTest`, `QualityModuleUiHarnessTest` e `NotificationFeedTest`.
   - Record `NotificationItem(String type, String title, String detail, String when, String moduleCard, int priority)` intacto.
2. **Zero `JOptionPane`**: Proibido qualquer diálogo modal em classes de backend/feed.
3. **Tratamento de Exceções Gracioso**: Falhas em clientes secundários (Quality, StockTransfer, etc.) degradam suavemente sem bloquear os alertas essenciais.
