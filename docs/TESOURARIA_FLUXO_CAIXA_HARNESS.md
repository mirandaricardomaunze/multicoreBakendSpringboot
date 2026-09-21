# Harness de Conformidade: Tesouraria & Projeção de Fluxo de Caixa Previsional

- **Identificador:** `HARNESS-TFC-001`
- **Referência:** `SPEC-TFC-001`
- **Módulo:** `backend`, `desktop`
- **Data:** 2026-09-17

---

## 1. Critérios de Validação Automatizada

| ID | Critério | Verificação |
| :--- | :--- | :--- |
| **TFC-01** | **Cálculo da Posição Atual ($C_0$)** | A soma das contas de caixa (`CASH`) e banco (`BANK`) deve ser exata e refletir apenas a empresa do contexto. |
| **TFC-02** | **Alocação Temporal de Entradas** | Faturas vencidas devem cair no balde `OVERDUE`; faturas com vencimento nos intervalos 1-7d, 8-15d, 16-30d, 31-60d e >60d devem ser alocadas com precisão sem dupla contagem. |
| **TFC-03** | **Alocação Temporal de Saídas** | Compras a fornecedores com saldo pendente devem ser alocadas nos respectivos baldes de acordo com o prazo de vencimento. |
| **TFC-04** | **Evolução do Saldo Projetado ($C_t$)** | O saldo cumulativo deve satisfazer rigorosamente $C_t = C_{t-1} + (I_t - O_t)$ para todos os períodos. |
| **TFC-05** | **Deteção de Rutura de Caixa** | Se em qualquer intervalo $C_t < 0$, o alerta deve ser ativado com identificação do período e valor do défice. Se $C_t \ge 0$, o estado deve ser `HEALTHY`. |
| **TFC-06** | **Geração de PDF Executivo** | O endpoint `/api/finance/forecast/pdf` deve gerar um stream PDF válido (iniciando com `%PDF-1.`) com cabeçalho canónico, matriz temporal e tabela de maiores movimentos. |
| **TFC-07** | **Interface Gráfica e Teto de Linhas** | `CashFlowForecastPanel` deve renderizar os KPIs superiores, gráfico visual ou matriz de evolução, respeitando o teto de 1000 linhas e utilizando o design system (`UIHelper`). |

---

## 2. Implementação dos Testes
- Backend: `CashFlowForecastHarnessTest.java` (cobrindo TFC-01 a TFC-06).
- Desktop: `CashFlowForecastPanelHarnessTest.java` (cobrindo TFC-07).
