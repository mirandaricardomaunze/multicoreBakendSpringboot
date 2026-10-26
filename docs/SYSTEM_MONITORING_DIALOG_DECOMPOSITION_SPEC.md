# Especificação de Decomposição - SystemMonitoringDialog (Fase 88)

## 1. Contexto e Motivação
A classe `SystemMonitoringDialog.java` possuía 734 linhas, acumulando num único ficheiro:
- Construção de cabeçalho e telemetria de uptime;
- Cartões de métricas do sistema (JVM Heap, Espaço em Disco, HikariCP, Threads);
- Grelhas de dados com renderizadores de estado customizados (Subsistemas, Inquilinos Multi-Tenant, Incidentes de Alarme);
- Ações e preferências de alarmística de áudio e disparo de e-mails de teste;
- Gestão de exportação de ficheiros de diagnóstico (.txt) e cópia para área de transferência;
- Coordenação de chamadas assíncronas via `SwingWorker` e `SystemMonitoringApiClient`.

## 2. Princípios de Arquitetura e Limites Físicos
- **Modularização Estrita:**
  - `SystemMonitoringOverviewBuilder.java`: Gestão do cabeçalho de estado global, uptime e grelha de 4 cartões de métricas de recursos ($\le 215$ linhas).
  - `SystemMonitoringTabsBuilder.java`: Construção das abas de Subsistemas, Saúde Multi-Tenant e Central de Alarmes com renderizadores semânticos corporativos ($\le 210$ linhas).
  - `SystemMonitoringExporter.java`: Operações de exportação de ficheiro .txt, cópia para área de transferência e disparo de teste de e-mail ($\le 110$ linhas).
  - `SystemMonitoringDialog.java`: Orquestrador magro responsável pela janela modal, abas e coordenação assíncrona ($\le 200$ linhas).
- **Semântica Visual e Localização:**
  - Strings estritamente em português de Moçambique (pt-MZ).
  - Cores semânticas do `UIHelper` (`APPROVED_GREEN`, `PENDING_YELLOW`, `REJECTED_RED`, `ACCENT_CYAN`).
  - Proibição de emojis em elementos Swing (uso exclusivo de FontAwesome vetorial via `UIHelper.icon(...)`).
  - Proibição absoluta de `JOptionPane` (uso de `ToastManager` e `ModernMessageDialog.prompt`).
- **Compatibilidade:**
  - Preservação do construtor canónico `public SystemMonitoringDialog(Window parent, SystemMonitoringApiClient monitoringClient)`.
  - Preservação do método `show()`.
  - Conformidade com `BrightButtonsHarnessTest` (botão de fechar perigoso com `UIHelper.createDangerButton("Fechar")`).
