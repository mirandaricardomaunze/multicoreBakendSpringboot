# HARNESS — Homologação dos editores documentais PHC

**ID:** HARNESS-PHC-DOCUMENT-HOMOLOGATION-001

## Cenários automatizados

- **HMG-01:** os cinco editores usam grelha directa e não contêm o fluxo antigo de editar item.
- **HMG-02:** cada gravação termina a célula activa e valida todas as linhas.
- **HMG-03:** embalagem, caixa e percentagem da caixa são derivadas da composição do produto.
- **HMG-04:** facturas emitidas não expõem `PUT`; correcções continuam por anulação/nota.
- **HMG-05:** criação, actualização, nova leitura e bloqueio de cotação, encomendas e transferência
  são exercitados pela fronteira HTTP usada pelo desktop.
- **HMG-06:** impressão inclui as colunas documentais configuráveis de embalagem.
- **HMG-07:** o contexto desktop e as fronteiras Maven continuam válidos.

## Execução

```powershell
mvn -pl desktop -Dtest=PhcDocumentEditorHomologationHarnessTest,PhcInlineDocumentGridHarnessTest,OrderEditorHarnessTest,EditableDocumentEditorsHarnessTest,StockTransferDraftUiHarnessTest,DesktopThinContextTest test
mvn -pl backend -Dtest=EditableDocumentEditorsHttpIntegrationTest,MoneyFlowHttpIntegrationTest,PackageQuantityHarnessTest,LineItemsTableRendererTest,LineRowMapperTest,StockTransferPrintServiceTest,MultiModuleArchitectureHarnessTest test
mvn clean compile
```

## Homologação visual

1. Abrir cada área de trabalho e confirmar que cabeçalho e grelha estão contidos nos respectivos
   cards, com acções no topo.
2. Confirmar que `Adicionar linha` inicia a pesquisa na célula de produto.
3. Confirmar alinhamento, legibilidade, scroll horizontal e estado somente leitura.
4. Não emitir documentos na base operacional durante a homologação visual.

