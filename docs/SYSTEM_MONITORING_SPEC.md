# SYSTEM MONITORING SPEC - Multicore ERP

Este documento estabelece as especificações normativas, regras de observabilidade, contratos e thresholds de diagnóstico para o subsistema de monitoramento em tempo real do Multicore ERP.

---

## 1. Objectivos e Arquitectura

O subsistema de monitoramento assegura a disponibilidade, integridade e performance do sistema através de:
1. **Telemetria de Runtime (JVM & Hardware)**: Memória heap, non-heap, threads e espaço em disco.
2. **Saúde de Persistência (Base de Dados)**: Latência de resposta, conexões activas/ociosas no pool HikariCP e integridade do motor de BD.
3. **Integridade de Negócio**: Auditoria de sequências documentais e estado do agendador de cópias de segurança (backups).
4. **Resiliência e Desacoplamento**: O monitoramento é não-intrusivo; uma falha na recolha de métricas de um subsistema não pode derrubar a API de saúde nem o ERP.

---

## 2. Classificação Geral de Saúde (`overallStatus`)

O estado geral do sistema é calculado dinamicamente de acordo com as seguintes regras de severidade:

| Estado | Critérios de Disparo | Ação Requerida |
| :--- | :--- | :--- |
| **`HEALTHY`** (🟢 Verde) | Todos os subsistemas operacionais; Heap JVM ≤ 75%; Disco ≤ 80%; Latência BD ≤ 100 ms. | Operação normal. Nenhuma intervenção necessária. |
| **`WARNING`** (🟡 Amarelo) | Heap JVM entre 75% e 90%; Disco entre 80% e 90%; Latência BD entre 100 ms e 500 ms; ou backup com atraso > 24h. | Alerta preventivo ao administrador. Recomenda-se limpeza de temporários ou verificação de memória. |
| **`CRITICAL`** (🔴 Vermelho) | Base de dados inacessível; Heap JVM > 90%; Espaço livre em disco < 1 GB ou > 90% ocupado; Falha crítica em subsistema vital. | Intervenção urgente requerida para evitar indisponibilidade de faturação ou perda de dados. |

---

## 3. Thresholds Oficiais e Métricas de Recursos

### 3.1. Memória JVM (Heap)
* **Aviso (`WARNING`)**: `heapUsedPercent >= 75.0%`
* **Crítico (`CRITICAL`)**: `heapUsedPercent >= 90.0%`

### 3.2. Espaço em Disco (Storage)
* O monitoramento deve verificar tanto o volume da aplicação como o directório de backups (`backups/`).
* **Aviso (`WARNING`)**: `usedDiskPercent >= 80.0%` ou `freeBytes < 2 GB`
* **Crítico (`CRITICAL`)**: `usedDiskPercent >= 90.0%` ou `freeBytes < 1 GB`

### 3.3. Base de Dados (HikariCP / Engine)
* **Latência de Ping**: Execução de query trivial (`SELECT 1`).
  * Normal: `< 50 ms`
  * Aviso: `100 ms - 500 ms`
  * Crítico: `> 500 ms` ou timeout
* **Esgotamento de Conexões**:
  * Aviso: Conexões activas `>= 80%` de `maxConnections`.
  * Crítico: Conexões activas `>= 95%` de `maxConnections`.

---

## 4. Subsistemas Auditados

O relatório de saúde audita obrigatoriamente os seguintes componentes:

1. **`Base de Dados Principal`**: Conectividade e integridade do pool de conexões.
2. **`Motor de Faturação & POS`**: Disponibilidade de séries, sequências de documentos e contingência.
3. **`Mecanismo de Cópias de Segurança`**: Execução do último backup e integridade do directório.
4. **`Armazenamento de Ficheiros & Imagens`**: Permissões de escrita e espaço disponível para thumbnails e PDFs.
5. **`Serviço de Licenciamento & Versão`**: Validade da licença e sincronismo de versão desktop/backend.

---

## 5. Contratos de API REST

Todos os endpoints de monitoramento residem sob o prefixo `/api/monitoring/` e requerem papel `ADMIN` ou `SUPERADMIN`:

`ADMIN` deve apresentar `X-Company-Id` de uma empresa activa a que pertence; os dados de empresas e
auditoria ficam limitados a essa empresa. `SUPERADMIN` acede sem empresa e pode consultar todos os
tenants. A lista global de incidentes e o envio de e-mail de teste exigem `SUPERADMIN`. Ver
[SECURITY_ATTACK_SURFACE_REMEDIATION_SPEC.md](SECURITY_ATTACK_SURFACE_REMEDIATION_SPEC.md).

### 5.1. `GET /api/monitoring/system-health`
* **Devolve**: [`SystemHealthDTO`](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/monitoring/dto/SystemHealthDTO.java)
* **Comportamento**: Snapshot completo instantâneo em formato JSON estruturado.

### 5.2. `GET /api/monitoring/diagnostics-export`
* **Devolve**: [`SystemDiagnosticsExportDTO`](file:///c:/Users/miran/Desktop/manager/contracts/src/main/java/mz/multicore/erp/modules/monitoring/dto/SystemDiagnosticsExportDTO.java)
* **Comportamento**: Pacote com métricas detalhadas, variáveis de ambiente seguras (sem senhas) e histórico recente de auditoria para exportação e envio ao suporte técnico.

---

## 6. Padrões de Interface Desktop (Swing)

1. **Assincronismo Obrigatório**: Chamadas de recolha de métricas devem ser executadas fora da Event Dispatch Thread (EDT) via `UIHelper.loadAsync` ou `SwingWorker`, mantendo a UI fluida.
2. **Semáforos e Cores Semânticas**:
   - `HEALTHY` -> `UIHelper.APPROVED_GREEN`
   - `WARNING` -> `UIHelper.PENDING_YELLOW`
   - `CRITICAL` -> `UIHelper.REJECTED_RED`
3. **Anti-Truncamento**: Valores numéricos e monetários devem ter folga geométrica de acordo com `CONVENTIONS.md`.
4. **Exportação Humana**: Possibilidade de copiar relatório de diagnóstico formatado em texto para a área de transferência com 1 clique.
