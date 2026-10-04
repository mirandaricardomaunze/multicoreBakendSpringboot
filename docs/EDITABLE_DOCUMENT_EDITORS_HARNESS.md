# Harness dos Editores de Documentos Pré-Emissão

**ID:** HARNESS-EDITABLE-DOCUMENT-EDITORS-001  
**SPEC:** `docs/EDITABLE_DOCUMENT_EDITORS_SPEC.md`

## Cobertura

`EditableDocumentEditorsHarnessTest` protege:

1. `CardLayout` e `DocumentEditorHost` nas cotações e encomendas a fornecedor;
2. ausência do modal de criação nesses dois fluxos;
3. acção `Editar / Consultar`, duplo clique e edição/remoção de linhas;
4. contratos `PUT` com versão optimista;
5. estados editáveis declarados no backend;
6. modo de consulta para documentos bloqueados;
7. auditoria `QUOTATION_UPDATE` e `PURCHASE_ORDER_UPDATE`.

Os testes de `QuotationService` e `PurchaseOrderService` cobrem actualização válida, versão
desactualizada e rejeição depois do envio/recepção.

`EditableDocumentEditorsHttpIntegrationTest` executa os mesmos fluxos pela fronteira HTTP usada
pelo desktop, numa base H2 isolada: cria e actualiza os dois documentos, prova a rejeição de uma
versão antiga, envia a cotação, recebe parcialmente a encomenda e confirma ambos os bloqueios.

## Execução

```powershell
mvn -Dtest=EditableDocumentEditorsHarnessTest,EditableDocumentEditorsHttpIntegrationTest,QuotationServiceTest,PurchaseOrderServiceTest,MultiModuleArchitectureHarnessTest,DesktopThinContextTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

O gate final permanece `mvn clean compile`.
