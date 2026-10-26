# Especificação Canónica — Decomposição Modular do Painel de Recursos Humanos (HRPanel)
**Código:** `SPEC-HR-PANEL-DECOMPOSITION-001`  
**Data:** 2026-10-23  
**Status:** APROVADO  
**Alvo:** `desktop/src/main/java/mz/multicore/erp/gui/HRPanel.java` (989 linhas $\rightarrow \le 420$ linhas)

---

## 1. Diagnóstico e Objectivos

### 1.1 Diagnóstico
O ficheiro `HRPanel.java` acumulou 989 linhas (apenas a 11 linhas do limite estrito de 1.000 linhas), acumulando responsabilidades dispersas:
1. Orquestração das 11 abas corporativas do módulo de RH.
2. Construção e cálculo de KPIs/gráficos da aba "Visão Geral" (`buildOverviewTab` e `refreshOverview`).
3. Construção modal e manipulação de fotografia/imagem de colaboradores (`openEmployeeDialog`, ~150 linhas).
4. Diálogo modal de emissão e cálculo de recibos de salário (`openCreatePayslipDialog`).
5. Diálogo modal de registo de faltas e justificações (`openCreateAbsenceDialog`).
6. Chamadas residuais a `JOptionPane` incompatíveis com as diretrizes de feedback moderno.

### 1.2 Invariantes e Restrições Não-Negociáveis
1. **Contrato de Abas:** O `JTabbedPane` com as 11 abas corporativas (`Visão Geral`, `Colaboradores`, `Contratos`, `Recibos de Salário`, `Faltas`, `Ponto`, `Férias`, `Descontos`, `Retenções`, `Cessações`, `Despesas`) deve ser rigorosamente mantido com a mesma nomenclatura e larguras validadas por `TabStripFitsTest`.
2. **Âncoras de Código Fonte nos Testes de Ergonomia:**
   - `HrCrmUiErgonomicsHarnessTest`: Preservar os cabeçalhos de card `UIHelper.tableCardTop` para "Quadro de Colaboradores", "Recibos de Salário" e "Registo de Faltas" com seus respetivos botões agrupados em `ActionMenuButton`.
   - `HRUnifiedHarnessTest`: Manter `UIHelper.ensureHeadersFit(employeesTable)` e `TableQuickPeekController.install(employeesTable`.
   - `HRSubsidiesUiHarnessTest`: Manter `"13.º Mês"` e `bonusActions::openThirteenthMonth`.
3. **Limite Físico de Linhas:**
   - `HRPanel.java` $\le 450$ linhas (meta $\le 420$ linhas).
   - Componentes extraídos $\le 250$ linhas cada.

---

## 2. Arquitetura da Solução

### 2.1 Componentes a Extrair

| Componente | Responsabilidade | Linhas Estimadas |
|---|---|---|
| `HROverviewTabPanel.java` | Construção de KPIs, cartões de métricas mensais e gráficos de barras da Visão Geral | $\sim 140$ |
| `HREmployeeFormDialog.java` | Diálogo modal com abas de dados pessoais, upload/escala de fotografia (Avatar), vínculo e pagamento | $\sim 150$ |
| `HRPayslipFormDialog.java` | Diálogo modal de cálculo, seleção de rubricas e criação de recibo de salário | $\sim 70$ |
| `HRAbsenceFormDialog.java` | Diálogo modal de ausências, atestados médicos e justificação | $\sim 50$ |

### 2.2 Orquestrador Limpo (`HRPanel.java`)
Coordena os dados via `HRApiClient`, gerencia as abas através de sub-painéis delegados e expõe as coleções de colaboradores, recibos e faltas de forma limpa.
