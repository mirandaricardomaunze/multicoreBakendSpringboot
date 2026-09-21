# HARNESS — Validação de Ergonomia e Design Profissional do POS

**Criado em:** 2026-09-15  
**Camada:** cliente desktop Swing  
**Classe de teste:** `mz.multicore.erp.gui.PosProfessionalErgonomicsHarnessTest`

---

## Critérios de Conformidade Automatizados

| ID | Regra | Teste Automatizado |
|---|---|---|
| **POS-PRO-01** | O estado do caixa é apresentado através de um banner/painel formatado (`sessionBanner`), eliminando etiquetas de texto soltas no topo. | `testSessionStateIsRenderedAsBanner` |
| **POS-PRO-02** | Os cartões de artigo utilizam ícones e cores contextuais de categoria quando não há imagem cadastrada. | `testCategoryContextualIconsAndColors` |
| **POS-PRO-03** | Os botões de quantidade `−` e `+` não duplicam texto e ícones (`--` ou `+ +`). | `testQuantityButtonsHaveCleanLabels` |
| **POS-PRO-04** | A opção de venda a crédito adota vocabulário institucional *"Venda a Crédito (Conta Corrente)"*. | `testCreditOptionUsesProfessionalWording` |
| **POS-PRO-05** | O display de Total a Pagar mantém hierarquia com tipografia destacada (`>= 22px`). | `testTotalDisplayTypography` |
| **POS-PRO-06** | `POSPanel.java` mantém rigorosamente menos de 1000 linhas de código. | `UiPanelDecompositionTest` |
| **POS-PRO-07** | A hierarquia semântica de botões canónicos (`PosButtonColourHierarchyTest`) permanece intacta. | `PosButtonColourHierarchyTest` |
