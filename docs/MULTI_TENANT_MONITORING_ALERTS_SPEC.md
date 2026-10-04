# SPEC-MTMA-001: Módulo de Observabilidade Multi-Tenant & Central de Alarmes

## 1. Contexto & Objectivo
O Multicore ERP opera em regime multi-empresa (multi-tenant) sobre persistência relacional. Para garantir prontidão operacional de nível NOC (Network Operations Center), esta especificação define:
1. **Saúde Multi-Tenant em Tempo Real:** Agregação contínua do estado operacional de cada empresa/inquilino (utilizadores ativos, estado de backups automáticos e anomalias forenses pendentes).
2. **Sistema de Alarmes Imediatos:**
   - **Alarme Sonoro no Desktop:** Síntese de tom duplo senoidal PCM em memória (900 Hz + pausa + 1200 Hz), com salvaguarda estrita para ambientes de teste headless (`GraphicsEnvironment.isHeadless()`).
   - **Alerta por E-mail ao Administrador:** Disparo automático de e-mail transacional de incidente técnico com detalhes da falha, métricas e recomendações de acção imediata, com proteção anti-spam via *cooldown* inteligente de 15 minutos por chave de incidente (`subsystem:tenant`).
3. **Injeção no Sino de Notificações (`NotificationFeed`):** Incidentes de severidade crítica (`CRITICAL`) injetados com prioridade máxima para administradores.
4. **Painel de Controlo & Testes:** Diálogo `SystemMonitoringDialog` com abas para diagnóstico de hardware/JVM, saúde multi-tenant e central de alarmes com testes interativos.

---

## 2. Fronteiras Arquiteturais & Contratos

```
+-------------------------------------------------------------+
|                     multicore-contracts                     |
|  - TenantHealthDTO (companyId, companyName, status, ...)     |
|  - SystemAlertIncidentDTO (incidentId, severity, message...) |
|  - SystemAlertTestResultDTO (channel, success, message...)   |
+-------------------------------------------------------------+
              ^                                 ^
              |                                 |
+-----------------------------+   +-----------------------------+
|      multicore-backend      |   |      multicore-desktop      |
|  - TenantMonitoringService  |   |  - SystemMonitoringApiClient|
|  - SystemIncidentManager    |   |  - SoundAlertManager        |
|  - SystemAlertEmailService  |   |  - SystemMonitoringDialog   |
|  - SystemMonitoringController   |  - NotificationFeed (Alerts)|
+-----------------------------+   +-----------------------------+
```

### Regras de Isolamento:
- `contracts` não referencia Spring, JPA ou Swing.
- `desktop` não referencia repositórios, entidades ou base de dados; comunica estritamente via HTTPS usando DTOs canónicos.
- `backend` é headless e não referencia classes do pacote `gui`.
- Linhas de código em componentes visuais mantidas estritamente $\le 1000$ linhas.

---

## 3. Matriz de Estados de Saúde Operacional

| Estado | Critério de Classificação | Ação do Sistema |
|---|---|---|
| `HEALTHY` | Todos os subsistemas operacionais, backup em dia, 0 anomalias críticas. | Indicador verde (`● SAUDÁVEL`). |
| `WARNING` | Empresa desativada ou com anomalias suspeitas leves (>0). | Indicador amarelo (`▲ ATENÇÃO`). |
| `CRITICAL` | Falha de persistência, backup com falha ou anomalias forenses graves (>0). | Indicador vermelho (`✖ CRÍTICO`), emissão de tom sonoro e despacho de e-mail com cooldown. |

---

## 4. Endpoints REST Canónicos

**Autorização:** `ADMIN` recebe apenas a saúde da empresa activa; `SUPERADMIN` recebe a visão
global e pode consultar incidentes e enviar e-mail de teste. Ver
[SECURITY_ATTACK_SURFACE_REMEDIATION_SPEC.md](SECURITY_ATTACK_SURFACE_REMEDIATION_SPEC.md).

- `GET /api/monitoring/tenants-health` — Retorna `List<TenantHealthDTO>`.
- `GET /api/monitoring/incidents` — Retorna `List<SystemAlertIncidentDTO>` (histórico dos últimos 50 incidentes em memória).
- `POST /api/monitoring/test-email-alert?recipientEmail=...` — Dispara teste de envio de e-mail e retorna `SystemAlertTestResultDTO`.
