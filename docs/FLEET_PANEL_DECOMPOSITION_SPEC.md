# SPEC-FLEET-PANEL-DECOMPOSITION-001: Decomposição Modular do Painel de Gestão de Frotas & Viaturas

## 1. Contexto e Diagnóstico

O painel `FleetPanel.java` (510 linhas) é o centro de controlo de viaturas, odómetros e custos de combustível do Multicore ERP:
- **Inventário Operacional da Frota:** Matrículas, marcas/modelos, condutores atribuídos, seguro e inspecção periódica.
- **Odómetro & Rendimento (Km/L):** Apuramento contínuo de distância percorrida e consumo médio.
- **Abastecimento de Combustível:** Registo de abastecimentos em postos autorizados com cálculo automático de rendimento e odómetro.
- **Histórico & Rastreabilidade:** Modal de consulta cronológica de abastecimentos por viatura.
- **Manifestos de Carga:** Integração com rotas e manifestos de distribuição (`FleetManifestsViewDialog`).

### Problemas Arquiteturais Identificados no Monólito:
1. **Acumulação de Diálogos e Formulários Complexos:** O painel acumula a lógica do formulário de nova viatura (`openNewVehicleDialog` - 110 linhas), o diálogo de abastecimento (`openFuelDialog` - 110 linhas) e a tabela modal de histórico de combustível (`showFuelHistory` - 55 linhas).
2. **Alta Sensibilidade de Testes Automatizados:** O painel é auditado por testes estritos:
   - `GuiActionAlertGuidanceHarnessTest`: exige os campos package-private `table` e `feedback`, e o método `showFuelHistory()` alertando o operador via `InlineFeedbackPanel` quando nenhuma linha está selecionada.
   - `TableActionPlacementHarnessTest`: exige o padrão canónico de topo de card `UIHelper.tableCardTop`.

---

## 2. Decisão de Arquitetura e Estrutura Decomposta

No pacote `mz.multicore.erp.gui`:

1. **`FleetPanel.java` (Orquestrador Magro $\le 240$ linhas):**
   - Layout corporativo com cabeçalho, `InlineFeedbackPanel feedback`, grelha de 4 KPIs via `KpiCard.createGrid(4)` e card `ModernPanel(16)` contendo filtros (`TableFilter.bar`) e tabela `table`.
   - Preserva estritamente os campos package-private: `table` e `feedback`.
   - Preserva o método package-private `showFuelHistory()`, acionado por duplo clique (`customDetailAction`) e auditado por `GuiActionAlertGuidanceHarnessTest`.
   - Delega a criação de viaturas para `VehicleFormDialog`.
   - Delega o registo de combustível e exibição de histórico para `FuelOperationsCoordinator`.

2. **`VehicleFormDialog.java` ($\le 135$ linhas):**
   - Responsabilidade: Formulário modal ergonómico para registo de nova viatura da frota.
   - Validações atómicas de matrícula, marca, modelo, ano, combustível, odómetro e datas de seguro/inspecção via `ModernFormDialog`.

3. **`FuelOperationsCoordinator.java` ($\le 180$ linhas):**
   - Responsabilidade: Operações de abastecimento e consulta de histórico de combustível.
   - `openFuelDialog(...)`: Modal de registo de abastecimento, validação de odómetro não regressivo e cálculo de rendimento Km/L.
   - `showFuelHistoryDialog(...)`: Diálogo modal com tabela cronológica de abastecimentos (`listFuelLogs`), custos totais e consumo.

---

## 3. Preservação Estrita de Contratos e Restrições Não-Negociáveis

1. **Contratos Canónicos:**
   - Preservar `public FleetPanel(FleetApiClient fleetApiClient)`.
   - Preservar os campos package-private `table` e `feedback`.
   - Preservar o método package-private `showFuelHistory()`.
2. **Ergonomia e UI Corporativa:**
   - Proibido qualquer diálogo `JOptionPane`. Apenas `InlineFeedbackPanel` e `ModernFormDialog`.
   - Sem emojis Unicode em títulos, labels ou botões.
   - Tabela contida no card `ModernPanel(16)` com `UIHelper.tableCardTop`.

---

## 4. Métricas de Sucesso

| Componente | Linhas Anteriores | Linhas Alvo | Redução Esperada |
|---|---|---|---|
| `FleetPanel.java` | 510 | $\le 240$ | $\ge 53\%$ |
| `VehicleFormDialog.java` | 0 | $\le 135$ | Novo diálogo |
| `FuelOperationsCoordinator.java` | 0 | $\le 180$ | Novo colaborador |
| **Erros de Teste / Regressões** | 0 | 0 | 100% aprovado |
