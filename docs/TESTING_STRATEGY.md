# Estratégia de Testes — Multicore ERP

Estado: **canónico**  
Última revisão: 2026-10-10

Este documento define como obter confiança proporcional ao risco sem transformar a suite num peso
inútil.

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

## Gates obrigatórios por tipo de alteração

| Alteração | Verificação mínima obrigatória |
|-----------|--------------------------------|
| POM, fronteira entre módulos, DTO ou classe movida | `MultiModuleArchitectureHarnessTest` |
| UI Swing ou construtor de painel | `DesktopThinContextTest` |
| Regra fiscal, stock, POS, caixa, salários ou permissões | Teste unitário da regra e teste de integração quando houver JPA, transacção ou segurança |
| Migration Flyway ou mapeamento JPA | Teste de integração com arranque do contexto e aplicação das migrations |
| Contrato HTTP | Teste do Controller/handler, incluindo status, payload de sucesso e erro |

Uma alteração não deve reduzir cobertura de uma regra crítica. Cobertura percentual é um indicador,
não substitui cenários de negócio explícitos, isolamento por empresa e verificação de efeitos.

## Concorrência, idempotência e compatibilidade

- Operações repetíveis por rede devem testar idempotência ou rejeição segura da segunda chamada.
- Escritas com versão optimista devem cobrir `409 Conflict` para versão desactualizada.
- Numeração, checkout, aprovação e movimentos de stock devem ter testes concorrentes quando a
  implementação puder receber pedidos simultâneos.
- Alterações em records de `contracts` devem testar desserialização e construtores retrocompatíveis
  usados pela versão desktop anterior.

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
