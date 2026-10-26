# Harness de Testes - SystemMonitoringDialogDecomposition (Fase 88)

## 1. Objectivos do Teste
Validar que a decomposição do `SystemMonitoringDialog` respeita:
1. Limites físicos de linhas:
   - `SystemMonitoringDialog.java` $\le 210$ linhas;
   - `SystemMonitoringOverviewBuilder.java` $\le 215$ linhas;
   - `SystemMonitoringTabsBuilder.java` $\le 220$ linhas;
   - `SystemMonitoringExporter.java` $\le 120$ linhas.
2. Isolamento de responsabilidades e ausência de chamadas proibidas:
   - Zero ocorrências de `JOptionPane`;
   - Zero emojis Unicode no código;
   - Botão de fecho criado com `UIHelper.createDangerButton("Fechar")`.
3. Integridade e funcionamento do diálogo em modo headless:
   - Instanciação de `SystemMonitoringDialog` com cliente simulado;
   - Atualização de métricas e renderização sem quebra de hierarquia de componentes.
4. Coexistência harmónica com os testes globais:
   - `BrightButtonsHarnessTest`;
   - `DesktopThinContextTest`;
   - `MultiModuleArchitectureHarnessTest`.
