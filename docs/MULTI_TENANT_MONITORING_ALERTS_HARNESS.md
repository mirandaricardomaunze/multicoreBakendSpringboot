# HARNESS-MTMA-001: Matriz de Testes & Validação de Observabilidade Multi-Tenant & Alarmes

Esta matriz define a cobertura automatizada de testes para o subsistema de observabilidade multi-tenant e central de alarmes.

---

## 1. Casos de Teste Automatizados

| ID | Componente | Descrição | Critério de Sucesso |
|---|---|---|---|
| **MTMA-01** | `SystemIncidentManager` | Registo de incidentes em memória com deduplicação e limite estrito de 50 itens. | Não ultrapassa 50 registos; mantém ordem cronológica inversa (mais recentes primeiro). |
| **MTMA-02** | `SystemIncidentManager` | Resolução de incidentes por identificador (`resolveIncident`). | Campo `resolved` passa a `true` sem corromper a coleção. |
| **MTMA-03** | `SystemAlertEmailService` | Cooldown inteligente de 15 minutos contra avalanche de alertas (anti-spam). | Primeiro alerta retorna `true`; segundo alerta idêntico em < 15 min retorna `false` (suprimido); alerta para subsistema distinto retorna `true`. |
| **MTMA-04** | `SystemAlertEmailService` | Validação de formato de e-mail e modo de teste de envio. | Rejeita endereços inválidos com mensagem explicativa; aceita endereços válidos em modo seguro. |
| **MTMA-05** | `TenantMonitoringService` | Consolidação do estado multi-tenant a partir de bases, utilizadores e auditorias. | Retorna lista de `TenantHealthDTO` com status válido (`HEALTHY`, `WARNING`, `CRITICAL`). |
| **MTMA-06** | `SystemMonitoringController` | Endpoints REST protegidos por autenticação e RBAC. | `/api/monitoring/tenants-health`, `/api/monitoring/incidents` e `/api/monitoring/test-email-alert` respondem `200 OK`. |
| **MTMA-07** | `SoundAlertManager` | Síntese PCM de onda senoidal de pulso duplo (900 Hz / 1200 Hz). | Gera bytes de áudio válidos; buffer > 100 bytes e amplitudes limitadas ao volume ativo. |
| **MTMA-08** | `SoundAlertManager` | Resiliência em ambientes de teste headless (`GraphicsEnvironment.isHeadless()`). | Execuções assíncronas não lançam exceções na JVM. |

---

## 2. Testes de Regressão & Arquitetura

- `MultiModuleArchitectureHarnessTest`: 6/6 testes verdes garantindo isolamento total do reactor Maven.
- `UiPanelDecompositionTest`: 1/1 teste verde garantindo que nenhum painel visual ultrapassa 1000 linhas (`SystemMonitoringDialog.java` mantido em 560 linhas).
