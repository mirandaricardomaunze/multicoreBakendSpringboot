# Harness do Editor Unificado de Encomendas

**ID:** HARNESS-ORDER-EDITOR-001  
**SPEC:** `docs/ORDER_EDITOR_SPEC.md`

## Cobertura automatizada

`OrderEditorHarnessTest` protege:

1. editor único alojado por `CardLayout`, sem diálogo de edição;
2. entrada `Editar encomenda` na lista;
3. tabela de itens com `Editar item`, `Actualizar item` e `Remover item`;
4. carregamento de `OrderDTO` no mesmo editor usado na criação;
5. `PUT /api/comercial/orders/{id}` e uso de `UpdateOrderRequest` com versão;
6. aviso de alterações por gravar no `DocumentEditorHost`;
7. número/tipo imutáveis e estados editáveis declarados no backend.

`ComercialServiceTest` cobre a regra executável:

- actualização de encomenda pendente e recálculo dos totais;
- rejeição de estado fechado;
- rejeição de versão desactualizada;
- nova aprovação para encomenda formal alterada;
- preservação/revalidação da reserva antes da separação.

## Execução

```powershell
mvn -Dtest=OrderEditorHarnessTest,ComercialServiceTest,MultiModuleArchitectureHarnessTest,DesktopThinContextTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

O gate final permanece `mvn clean compile`.
