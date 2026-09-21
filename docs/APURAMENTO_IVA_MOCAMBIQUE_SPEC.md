# Especificação Canónica: Apuramento Periódico de IVA de Moçambique (Modelo A)
**Código:** SPEC-AIVA-001  
**Módulos:** `contracts`, `backend`, `desktop`  
**Legislação:** Regulamento do IVA de Moçambique (Decreto n.º 7/2008 de 16 de Abril, com as alterações do Pacote PAE - taxa de 16%)  
**Data:** 2026-09-17  
**Estado:** APROVADO / EM IMPLEMENTAÇÃO  

---

## 1. Enquadramento e Objectivo Fiscal
O Apuramento Periódico de IVA em Moçambique é a obrigação fiscal mensal através da qual os sujeitos passivos do regime geral apuram o imposto devido ao Estado ou o crédito de imposto a transportar para os períodos subsequentes.

A mecânica de apuramento obedece à fórmula legal:
$$IVA_{\text{líquido}} = IVA_{\text{liquidado}} - (IVA_{\text{dedutível}} + \text{Crédito Anterior})$$

- Se $IVA_{\text{líquido}} > 0$: **IVA a Pagar ao Estado** (Guia de Pagamento Modelo B).
- Se $IVA_{\text{líquido}} \le 0$: **Crédito de IVA a Reportar** para o período seguinte.

---

## 2. Contratos REST (`contracts`)
Extensão de `IvaSummaryDTO`:
- `BigDecimal previousCredit` (Crédito de IVA do período anterior deduzido no apuramento)
- `BigDecimal payableAmount` (Montante efetivo a pagar à AT, zero se for crédito)
- `BigDecimal creditToCarry` (Crédito a transportar para o período seguinte)
- `String fiscalStatus` (`"A_PAGAR"` ou `"CREDITO_A_TRANSPORTAR"`)

---

## 3. Motor Fiscal no Backend (`backend`)
- `FiscalSummaryService`:
  - Suporte a parâmetro opcional `previousCredit` (default: 0.00 MT).
  - Cálculo segregado de vendas tributadas à taxa normal (16%) e vendas isentas (Art. 9.º do RIVA).
  - Apuramento do saldo final com reporte automático de crédito anterior.
- `IvaDeclarationPrintService`:
  - Dossiê oficial em PDF A4:
    * Cabeçalho fiscal: *"REPÚBLICA DE MOÇAMBIQUE — AUTORIDADE TRIBUTÁRIA"* e *"DECLARAÇÃO PERIÓDICA DE APURAMENTO DE IVA (MODELO A)"*.
    * Quadro I: Identificação do Contribuinte (Denominação Social, NUIT, Endereço e Província).
    * Quadro II: IVA Liquidado sobre Operações Ativas (Base Tributável 16% + Base Isenta).
    * Quadro III: IVA Suportado Dedutível sobre Compras e Despesas elegíveis.
    * Quadro IV: Crédito do Mês Anterior e Apuramento do Saldo Fiscal Final.
    * Termo de Encerramento e Assinatura do Responsável Técnico.

---

## 4. Interface Desktop (`desktop`)
- `FiscalApiClient`: Inclusão de `previousCredit` na consulta e impressão.
- `FiscalPanel.java`:
  - Campo numérico para `Crédito Anterior (MT)`.
  - 4 KPI Cards padronizados com `KpiCard.createMetricCard`:
    1. *IVA Liquidado (Vendas)*
    2. *IVA Deduzido (Compras)*
    3. *Crédito do Mês Anterior*
    4. *Saldo Fiscal Final (A Pagar / Crédito a Transportar)*
  - Botão de impressão da Declaração Modelo A em PDF.
