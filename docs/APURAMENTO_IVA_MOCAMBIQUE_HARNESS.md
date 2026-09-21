# Matriz de Testes & Harness de Conformidade: Apuramento de IVA de Moçambique
**Código:** HARNESS-AIVA-001  
**Referência:** `docs/APURAMENTO_IVA_MOCAMBIQUE_SPEC.md` (SPEC-AIVA-001)  
**Data:** 2026-09-17  

---

## 1. Objectivo
Garantir o cálculo exato e semântico do IVA apurado periodicamente de acordo com o Regulamento do IVA de Moçambique, cobrindo IVA liquidado, dedutível, compensação de crédito anterior e geração do Modelo A em PDF.

---

## 2. Critérios de Avaliação Automatizada

| ID | Área | Descrição do Teste | Critério de Aceitação |
|---|---|---|---|
| **AIVA-01** | Cálculo com Saldo a Pagar | $IVA_L > (IVA_D + C_{ant})$ | `payableAmount = IVA_L - IVA_D - C_ant`, `creditToCarry = 0`, `status = A_PAGAR` |
| **AIVA-02** | Cálculo com Crédito a Transportar | $(IVA_D + C_{ant}) > IVA_L$ | `payableAmount = 0`, `creditToCarry = IVA_D + C_ant - IVA_L`, `status = CREDITO_A_TRANSPORTAR` |
| **AIVA-03** | Isenção Artigo 9º | Vendas isentas de IVA | Base isenta agregada sem gerar imposto liquidado |
| **AIVA-04** | Renderização Modelo A | Geração do PDF Oficial A4 | PDF contém menções à Autoridade Tributária, Modelo A, NUIT e tabelas segregadas |
| **AIVA-05** | UI Desktop | `FiscalPanel` renderiza com 4 KPI Cards padronizados | Usa `KpiCard.createMetricCard` e atualiza saldo dinamicamente |
| **AIVA-06** | Limite de Linhas | `FiscalPanel.java` $\le 1000$ linhas | `UiPanelDecompositionTest` passa a 100% |

---

## 3. Testes Automatizados
- Backend: `backend/src/test/java/mz/multicore/erp/modules/fiscal/service/VatSettlementHarnessTest.java`
- Desktop: `UiPanelDecompositionTest.java`, `FinalUiUniformityHarnessTest.java`
