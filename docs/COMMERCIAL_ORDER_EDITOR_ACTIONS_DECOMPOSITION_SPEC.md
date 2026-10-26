# Especificação Técnica: Decomposição de CommercialOrderEditorActions

- **Document ID**: SPEC-COMMERCIAL-ORDER-EDITOR-ACTIONS-DECOMPOSITION-001
- **Fase**: 81
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Alvo**: `desktop/src/main/java/mz/multicore/erp/gui/CommercialOrderEditorActions.java` (459 linhas)

---

## 1. Contexto e Diagnóstico

O ficheiro `CommercialOrderEditorActions.java` concentra todas as interações e manipulações do editor de encomendas do módulo Comercial. Com **459 linhas**, acumulava três responsabilidades distintas:
1. **Políticas de Crédito e Condições de Pagamento**:
   - Validação de clientes com limite zero (`creditLimit().signum() == 0`).
   - Consulta assíncrona de elegibilidade e restrições de crédito (`getClientCreditEligibility`).
   - Determinação e inserção dinâmica de opções de prazos de pagamento (`ensurePaymentTermsOption`) e controle de avisos de risco.
2. **Ciclo de Vida da Encomenda**:
   - Abertura de encomendas existentes (`openSelected`, `load`).
   - Validação de estados editáveis (`PENDING_APPROVAL`, `PENDING`, `AWAITING_SEPARATION`).
   - Seleção de armazéns de origem e destino (`selectWarehouse`).
3. **Leitura Óptica e Pesquisa de Artigos**:
   - Leitura rápida por código de barras / SKU com incremento inteligente de quantidade (`handleBarcodeScanned`).
   - Aplicação em lote de produtos selecionados no diálogo de pesquisa (`applyProductsFromLookup`).
4. **Manipulação de Linhas na Grelha PHC**:
   - Sincronização bidirecional entre célula e rascunho (`syncLineFromGrid`).
   - Adição de linhas em branco e remoção de linhas (`addBlankLine`, `removeSelectedLine`).
   - Recálculo de totais com IVA e métricas de logística multinível (`packagesPerBox`, `unitsPerPackage`, `multiply(unitsPerBox)`).

---

## 2. Arquitetura Proposta e Divisão de Responsabilidades

```
mz.multicore.erp.gui/
├── CommercialOrderEditorActions.java                  (Orquestrador canónico da grelha: <= 180 linhas)
│   ├── syncLineFromGrid (compatível com PhcDocumentEditorHomologationHarnessTest)
│   ├── addBlankLine, removeSelectedLine, refreshTable, stopCellEditing, previewPrice
│   └── Ponto de entrada estático com delegações canónicas
├── CommercialOrderCreditPolicy.java                   (Políticas e elegibilidade de crédito: <= 120 linhas)
│   ├── handleClientChanged(ComercialPanel owner)
│   ├── ensurePaymentTermsOption(JComboBox<String> combo, String option)
│   └── clientIndex(ComercialPanel owner, Long clientId)
├── CommercialOrderLifecycleCoordinator.java           (Ciclo de vida e abertura de encomendas: <= 110 linhas)
│   ├── openSelected(ComercialPanel owner)
│   ├── load(ComercialPanel owner, OrderDTO order)
│   ├── selectWarehouse(ComercialPanel owner, JComboBox<String> combo, Long warehouseId)
│   └── isEditableStatus(String status)
└── CommercialOrderBarcodeAndLookupHandler.java        (Scanner e pesquisa em lote: <= 110 linhas)
    ├── handleBarcodeScanned(ComercialPanel owner, String code)
    └── applyProductsFromLookup(ComercialPanel owner, List<ProductDTO> selected)
```

---

## 3. Metas Físicas de Linhas

| Classe | Linhas Antes | Meta Teto |
|---|---|---|
| `CommercialOrderEditorActions.java` | 459 linhas | $\le 210$ linhas |
| `CommercialOrderCreditPolicy.java` | Nova classe | $\le 120$ linhas |
| `CommercialOrderLifecycleCoordinator.java` | Nova classe | $\le 75$ linhas |
| `CommercialOrderBarcodeAndLookupHandler.java` | Nova classe | $\le 110$ linhas |

---

## 4. Requisitos Não Negociáveis

1. **Compatibilidade 100% com Testes Existentes**:
   - Manter todas as assinaturas estáticas existentes em `CommercialOrderEditorActions`:
     `addBlankLine`, `handleBarcodeScanned`, `syncLineFromGrid`, `openSelected`, `load`, `handleClientChanged`, `ensurePaymentTermsOption`, `removeSelectedLine`, `refreshTable`, `stopCellEditing`, `previewPrice`, `applyProductsFromLookup`.
   - Preservar strings canónicas validadas por `PhcDocumentEditorHomologationHarnessTest` e `PhcInlineDocumentGridHarnessTest` (`packagesPerBox()`, `unitsPerPackage()`, `multiply(unitsPerBox)`).
2. **Zero Diálogos Legados `JOptionPane`**.
3. **Comparação de Valores Decimais e Zero**:
   - Uso estrito de `client.creditLimit().signum() == 0` ou `.compareTo(BigDecimal.ZERO) == 0`.
4. **Mensagens em Português de Moçambique**.
