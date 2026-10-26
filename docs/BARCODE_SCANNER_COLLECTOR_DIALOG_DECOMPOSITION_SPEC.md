# Especificação Técnica: Decomposição de BarcodeScannerCollectorDialog

- **Document ID**: SPEC-BARCODE-SCANNER-COLLECTOR-DIALOG-DECOMPOSITION-001
- **Fase**: 80
- **Data**: 2026-10-25
- **Estado**: Aprovado
- **Alvo**: `desktop/src/main/java/mz/multicore/erp/gui/components/BarcodeScannerCollectorDialog.java` (456 linhas)

---

## 1. Contexto e Diagnóstico

O diálogo `BarcodeScannerCollectorDialog` é a peça central da rastreabilidade óptica e de chão de fábrica do Multicore ERP, operando nos modos:
1. **Chão de Fábrica & Fabrico Industrial (`MANUFACTURING`)**: leitura do código do produto, verificação assíncrona da Ficha Técnica (BOM), parametrização da quantidade a fabricar e lançamento/início imediato de Ordem de Produção (OP).
2. **Armazém & Auditoria de Stocks (`INVENTORY`)**: conferência física e auditoria de existências com registo de contagem no histórico da sessão.

### Problemas Identificados no Monólito Actual:
1. **Acumulação de responsabilidades**:
   - Montagem de janelas e ciclo de vida do diálogo (`showDialog`, `buildScanBar`, `buildHistoryCard`).
   - Apresentação visual e gestão de estado do produto identificado e BOM (`lblProductName`, `lblBomStatus`, `btnAction`).
   - Operações assíncronas de rede (`processScan` com fallback para busca de SKU e `executeAction` com resolução de armazéns e criação de OP).
2. **Dimensionamento de janela modal**: uso de `dialog.setSize(840, 600)` sem aplicar `UIHelper.applyStandardModalBounds(dialog, 840, 600)`, violando a regra corporativa de limites contidos na área útil do ecrã do Windows.
3. **Complexidade do orquestrador**: 456 linhas com acoplamento interno entre componentes visuais e chamadas de API.

---

## 2. Arquitetura Proposta e Divisão de Responsabilidades

```
mz.multicore.erp.gui.components/
├── BarcodeScannerCollectorDialog.java           (Orquestrador magro: <= 180 linhas)
│   ├── Modo CollectorMode (MANUFACTURING / INVENTORY)
│   ├── Barra de leitura rápida (scanField + feedback)
│   ├── Histórico de leituras da sessão (tabela com hora, código, artigo, qtd)
│   └── Aplicação de UIHelper.applyStandardModalBounds
├── BarcodeScannerProductDetailsCard.java        (Componente visual do artigo: <= 150 linhas)
│   ├── Cartão com labels de nome, SKU, código de barras, preço e unidade
│   ├── Indicador de Ficha Técnica (BOM) e estado de verificação
│   ├── Seletor de quantidade (spinQuantity) e botão de ação estilizado
│   └── Exibição e limpeza de artigo identificado
└── BarcodeScannerExecutionEngine.java           (Motor de execução e pesquisa: <= 140 linhas)
    ├── Pesquisa assíncrona de artigo por barcode ou SKU (SwingWorker)
    ├── Verificação assíncrona de BOM ativa (ProductionApiClient)
    └── Lançamento de Ordem de Produção / registo de inventário
```

---

## 3. Metas Físicas de Linhas

| Classe | Linhas Antes | Meta Teto |
|---|---|---|
| `BarcodeScannerCollectorDialog.java` | 456 linhas | $\le 180$ linhas |
| `BarcodeScannerProductDetailsCard.java` | Nova classe | $\le 150$ linhas |
| `BarcodeScannerExecutionEngine.java` | Nova classe | $\le 140$ linhas |

---

## 4. Requisitos Não Negociáveis

1. **Contratos Públicos Preservados**:
   - Construtor canónico de 6 argumentos: `Window, CollectorMode, ComercialApiClient, ProductionApiClient, InventoryApiClient, Runnable`.
   - Enum `CollectorMode` (`MANUFACTURING`, `INVENTORY`).
   - Método `public void showDialog()`.
2. **Padrão Modal Corporativo**:
   - `UIHelper.applyStandardModalBounds(dialog, 840, 600)`.
3. **Zero Diálogos Legados `JOptionPane`**.
4. **Mensagens ao Utilizador em Português de Moçambique**.
5. **Compatibilidade Total com Testes**:
   - `BarcodeScannerCollectorDialogHarnessTest` (instanciação dos 2 modos).
   - `ManufacturingPanelDecompositionHarnessTest`, `StockWarehouseCoordinator` e `ProductionOrderOperationsCoordinator`.
   - `DesktopThinContextTest` e `MultiModuleArchitectureHarnessTest`.
