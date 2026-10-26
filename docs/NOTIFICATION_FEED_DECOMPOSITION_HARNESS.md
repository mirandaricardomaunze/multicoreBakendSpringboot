# Plano de Testes de Harness: Decomposição de NotificationFeed

- **Document ID**: HARNESS-NOTIFICATION-FEED-001
- **Fase**: 78
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Classe de Teste**: `mz.multicore.erp.gui.NotificationFeedDecompositionHarnessTest`

---

## 1. Casos de Teste Automatizados

| ID | Descrição do Teste | Critério de Sucesso |
|---|---|---|
| **NFD-01** | Limites físicos estritos de linhas | `NotificationFeed.java` $\le 200$ linhas; `NotificationInventoryAlertProvider.java` $\le 150$ linhas; `NotificationHRAlertProvider.java` $\le 175$ linhas. |
| **NFD-02** | Zero diálogos legados `JOptionPane` | Nenhuma das classes de alertas contém chamadas a `JOptionPane.`. |
| **NFD-03** | Preservação de construtores e record | Construtores sobrecarregados de `NotificationFeed` e record público `NotificationItem` intactos. |
| **NFD-04** | Delegação canónica | `NotificationFeed.load(...)` delega para os provedores especializados desacoplados. |

---

## 2. Instruções de Execução

```powershell
mvn test -pl desktop "-Dtest=NotificationFeedDecompositionHarnessTest,NotificationFeedTest,RadarAlertasProactivoHarnessTest,StockInTransitOverdueAlertHarnessTest,QualityModuleUiHarnessTest,DesktopThinContextTest"
```
