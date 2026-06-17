# Estrategia de Testes - Multicore ERP

Este documento define como testar sem transformar a suite num peso inutil.

## Piramide

1. Testes unitarios de Service para regras de negocio.
2. Testes de integracao Spring apenas quando wiring, transaccao, JPA ou security importam.
3. Testes de UI Swing apenas para casos especificos e de alto risco, quando houver harness adequado.

## O que sempre merece teste

- Calculos fiscais, IRPS, IVA, retencoes.
- Totais de factura, desconto, arredondamento e notas.
- Consumo de stock e FEFO.
- Transferencias de stock e estados.
- Checkout POS e fecho de caixa.
- Permissoes e isolamento por empresa.
- Numeracao sequencial.
- Anulacoes e operacoes auditaveis.
- Payroll.

## Naming

Usar:

```java
@Test
void metodoSobTeste_cenario_resultadoEsperado() {
}
```

Exemplos:

```java
@Test void consumeFEFO_stockInsuficiente_lancaBusinessRuleException() {}
@Test void calculatePayroll_salarioComIrps_aplicaTabelaProgressiva() {}
@Test void closeSession_diferencaDeCaixa_registaMovimento() {}
```

## Testes unitarios

Preferir unitarios puros para Services:

- Mockar Repositories.
- Usar entidades pequenas construidas no teste.
- Verificar exceptions e efeitos.
- Nao subir Spring sem necessidade.

## Testes de integracao

Usar quando precisa validar:

- Mapeamento JPA.
- Queries customizadas.
- Transaccoes.
- Handlers REST.
- Security/interceptors.
- Migrations.

## Dados de teste

- Criar fixtures pequenas e explicitas.
- Evitar depender de `DataLoader` para regra especifica.
- Usar nomes que expliquem o caso.
- Valores monetarios devem usar `BigDecimal` com string: `new BigDecimal("123.45")`.

## Comandos

Build completo:

```powershell
mvn clean compile
```

Testes:

```powershell
mvn test
```

Teste especifico:

```powershell
mvn -Dtest=PayrollTaxServiceTest test
```

## Checklist antes de entregar

- [ ] Nova regra critica tem teste.
- [ ] Erro esperado usa `BusinessRuleException`.
- [ ] Teste cobre tenant/empresa quando aplicavel.
- [ ] Teste nao depende de ordem acidental.
- [ ] `mvn clean compile` passa quando possivel.
