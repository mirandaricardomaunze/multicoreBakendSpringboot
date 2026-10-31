# Harness de Decomposição - SystemMonitoringService (Fase 119)

## 1. Objectivo

Validar estrutural e funcionalmente a decomposição de `SystemMonitoringService.java`, garantindo conformidade com os limites físicos de linhas, ausência de dependências de interface gráfica e delegação canónica para colaboradores especializados.

## 2. Casos de Teste

| ID | Descrição | Critério de Aceitação |
|---|---|---|
| **SMSD-01** | Limites físicos estritos de linhas | `SystemMonitoringService` $\le 120$, `SystemTelemetryCollector` $\le 210$, `SystemDiagnosticsReportBuilder` $\le 120$. |
| **SMSD-02** | Zero diálogos legados `JOptionPane` | Nenhuma chamada a `JOptionPane` nas classes do módulo de monitorização. |
| **SMSD-03** | Delegação canónica | `SystemMonitoringService` delega telemetria para `telemetryCollector` e relatórios para `reportBuilder`. |
| **SMSD-04** | Preservação de contratos | Preservação dos métodos `getSystemHealth()`, `getDiagnosticsExport()` e `exportDiagnostics()`. |
