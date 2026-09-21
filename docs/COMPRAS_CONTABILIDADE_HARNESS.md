# Harness — compras e tesouraria na contabilidade

**Data:** 2026-09-04  
**Estado:** implementado

## Garantias

| Caso | Garantia |
|---|---|
| CT-30 | Compra debita Mercadorias e IVA dedutível e credita Fornecedores |
| CT-31 | Repetir o mesmo documento não duplica o lançamento |
| CT-32 | Pagamento no acto liquida Fornecedores contra a conta bancária seleccionada |
| CT-33 | Pagamento posterior liquida Fornecedores contra Caixa/Banco, sem repetir a compra |
| CT-34 | Nota de crédito estorna venda, IVA, cliente e custo da devolução |
| CT-35 | Nota de débito reconhece outros proveitos operacionais e IVA |
| ARQ | O enum e o DTO continuam em `contracts`; eventos e persistência permanecem no backend |

## Execução

```powershell
mvn -q "-Dtest=AutomaticPostingServiceTest,PurchaseServiceTest,MultiModuleArchitectureHarnessTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Resultado de referência: **34 testes, 0 falhas** (o total exacto pode crescer quando o harness
receber novos casos).
