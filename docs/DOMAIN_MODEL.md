# Modelo de Dominio - Multicore ERP

Este documento define ownership dos modulos. Quando houver duvida sobre onde colocar uma regra, este ficheiro prevalece sobre conveniencia momentanea.

## Principio central

Cada modulo e dono das suas entidades, regras e invariantes. Outros modulos devem depender de Services publicos ou DTOs, nao de Repositories internos.

## Modulos

| Modulo | Responsabilidade | Entidades/Conceitos |
|--------|------------------|---------------------|
| `company` | Empresa activa, dados legais, configuracoes por tenant | Empresa, tenant, contexto fiscal |
| `users` | Utilizadores, roles e acesso por empresa | `AppUser`, roles, acesso multi-empresa |
| `approvals` | Pedidos de aprovacao para operacoes sensiveis | Aprovacoes, estados, aprovador |
| `audit` | Registo de eventos importantes | Logs de login, anulacoes, aprovacoes |
| `comercial` | Clientes, produtos, encomendas, facturas, notas | Cliente, produto, factura, encomenda, NC, ND |
| `inventory` | Stock fisico, armazens, lotes, validades, transferencias | Stock, lote, movimento, transferencia |
| `pos` | Sessoes de caixa e vendas rapidas | Sessao POS, movimentos de caixa, checkout |
| `purchases` | Compras, recepcao e entrada de stock | Compra, linha de compra, fornecedor quando existir |
| `financeira` | Tesouraria, contas e movimentos financeiros | Conta de tesouraria, transaccao |
| `fiscal` | Impostos, taxas e resumos fiscais | IVA, retencao, IRPS quando aplicavel |
| `hr` | Funcionarios, salarios, ausencias, ferias, despesas | Funcionario, folha salarial, IRPS |
| `crm` | Clientes em acompanhamento, tickets e actividades | Ticket, worksheet, interaccao |
| `printing` | Geracao de PDFs e documentos imprimiveis | Factura PDF, recibo, ordem, payslip |
| `reports` | Relatorios agregados e dashboards | Consultas de leitura e agregacoes |
| `numbering` | Numeracao sequencial de documentos | Serie, proximo numero, escopo por empresa |
| `backup` | Backup e exportacao operacional | Copias, restauracao controlada |

## Dependencias permitidas

Fluxo padrao dentro de um modulo:

```text
controller -> service -> repository -> model
```

Dependencias entre modulos devem passar por Services:

```text
POSService -> ComercialService
ComercialService -> InventoryService
ComercialService -> FinanceService
PurchaseService -> InventoryService
HRService -> PayrollTaxService
PrintingService -> DTOs/Services de leitura
```

Evitar:

- `Service A` chamar `Repository B`.
- UI Swing conhecer Repository.
- Repository devolver DTO de outro modulo.
- Entidade de um modulo ser alterada directamente por outro modulo.

## Ownership de regras criticas

| Regra | Dono |
|-------|------|
| Preco, desconto, cliente, documento comercial | `comercial` |
| Existencia de stock, lote, validade, FEFO, transferencia | `inventory` |
| Abertura/fecho de caixa, entrada/saida de dinheiro POS | `pos` |
| Impostos, taxas e resumos fiscais | `fiscal` |
| Lancamento financeiro e tesouraria | `financeira` |
| Salario, IRPS, ausencias e ferias | `hr` |
| Permissao e acesso por empresa | `users` + `approvals` |
| Auditoria de operacoes sensiveis | `audit` |
| Numeracao oficial de documentos | `numbering` |

## Invariantes globais

- Toda operacao sensivel deve estar no tenant/empresa actual.
- Documentos comerciais oficiais nao devem ser apagados; devem ser anulados ou compensados.
- Movimentos de stock devem ser rastreaveis ate ao documento que os gerou.
- Movimentos financeiros devem ter origem clara: POS, factura, recibo, ajuste ou tesouraria.
- Numeracao oficial nao pode reutilizar numeros.
- Regra fiscal deve ser explicita, testavel e documentada quando nao for obvia.
