# Harness — Grelhas documentais PHC

## Cobertura estrutural

- **PHC-01:** encomenda de cliente expõe células editáveis e sincroniza o DTO em cada alteração.
- **PHC-02:** cotação elimina o formulário de linha e usa pesquisa de produto na célula.
- **PHC-03:** encomenda a fornecedor edita produto, quantidades, preço, lote, validade e série.
- **PHC-04:** preparação de fatura usa grelha; a emissão continua a ser a fronteira de imutabilidade.
- **PHC-05:** notas de crédito/débito e recepção parcial mantêm a quantidade editável directamente.
- **PHC-06:** toolbars de linha ficam no topo do card e `Editar item` não integra o fluxo activo.

## Execução

```powershell
mvn -pl desktop -Dtest=PhcInlineDocumentGridHarnessTest,OrderEditorHarnessTest,EditableDocumentEditorsHarnessTest,StockTransferDraftUiHarnessTest,DesktopThinContextTest test
mvn -pl backend -Dtest=MultiModuleArchitectureHarnessTest test
```
