# Especificação de Decomposição Modular: Gestão e Decisão de Cotações (QuotationsPanel)

**Identificador:** `SPEC-QUOTATIONS-PANEL-DECOMPOSITION-002`  
**Domínio:** Comercial / Vendas / Orçamentação / Cotações ao Cliente  
**Alvo:** `mz.multicore.erp.gui.commercial.QuotationsPanel.java` (350 linhas)  
**Meta de Redução:** $\le 210$ linhas no orquestrador principal ($\approx 40\%$ de redução)  
**Módulos Envolvidos:** `desktop` (exclusivo, sem impacto em `contracts` ou `backend`)  

---

## 1. Contexto e Diagnóstico

O painel `QuotationsPanel.java` gerencia o ciclo completo de orçamentação e propostas comerciais no Multicore ERP: emissão, edição, extensão de validade, aceitação/recusa, geração de Ordens de Fabrico e conversão direta em Encomendas ou Faturas.

Anteriormente, o painel utilizava `QuotationOperationsCoordinator.java` para o desacoplamento das ações remotas. Contudo, acumulava ainda 350 linhas devido a:
1. **Transformação e Povoamento de Dados:** Cálculo dinâmico de rótulos de validade e caducidade (`validityLabel`), formatação de unidades de negócio e preenchimento tabular com datas moçambicanas.
2. **Povoamento e Configuração de Quick Peek:** Inspeção lateral silenciosa via tecla Espaço com dados cadastrais e de validade.
3. **Estrutura de Menus e Ações Operacionais:** Grupos de ações do ciclo de decisão e conversão.

---

## 2. Invariantes de Negócio e Contratos de Teste Obrigatórios

A refatoração deve manter estritamente:
1. **Contratos Canónicos da UI Multicore & Homologação Comercial:**
   - `UIHelper.tableCardTop("Cotações", ...)` no topo do `ModernPanel(16)`.
   - Preservar `TableQuickPeekController.install(table, card, ...)`.
   - Calibração de larguras de coluna via `UIHelper.ensureHeadersFit(table)`.
   - Manutenção de `PrintPreviewDialog.show(` referenciado para impressão oficial de propostas.
   - Preservar ações "Converter em Factura" com ícone `fas-file-invoice-dollar` e "Converter em Encomenda", com método handler `convertToInvoice()`.
   - Construtores sobrecarregados de 5 e 6 argumentos:
     - `QuotationsPanel(ComercialApiClient, Supplier<List<ClientDTO>>, Supplier<List<ProductDTO>>, Supplier<List<WarehouseDTO>>, Runnable)`
     - `QuotationsPanel(..., ProductionApiClient)`
2. **Eliminação de Diálogos Legados:**
   - Zero chamadas a `JOptionPane`.
3. **Ausência de Emojis Unicode:**
   - Zero caracteres Unicode especiais/emojis no código-fonte.

---

## 3. Arquitetura da Solução e Decomposição em Colaboradores

```
mz.multicore.erp.gui.commercial/
├── QuotationsPanel.java                      (Orquestrador principal: <= 210 linhas)
│   ├── openEditor(), openSelectedEditor(), saveEditor()
│   ├── send(), accept(), reject(), cancel(), extendValidity(), convert(), convertToInvoice(), print()
├── QuotationOperationsCoordinator.java      (Operações assíncronas e conversões: <= 230 linhas)
│   ├── send, accept, reject, cancel, extendValidity, generateProductionOrder, convert, convertToInvoice, print
├── QuotationEditorForm.java                  (Formulário e grelha de edição de linhas)
└── QuotationViewCoordinator.java             (Apresentação, formatação, validade e Quick Peek: <= 110 linhas)
    ├── applyQuotations(model, businessUnitCombo, quotations)
    ├── validityLabel(quotation)
    ├── populateQuickPeek(peek, model, row, onOpenFull)
    └── blank(value)
```
