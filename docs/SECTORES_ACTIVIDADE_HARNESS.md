# Harness — Sectores de Actividade

SPEC: `docs/SECTORES_ACTIVIDADE_SPEC.md` · detalhe: `UNIDADES_MEDIDA_SPEC.md`, `TABELAS_PRECO_SPEC.md`.

## F1 — Fundação (perfil, unidades, tabelas de preço)

| ID | Garantia | Teste |
|---|---|---|
| SA-01 | Sem perfil gravado vale Comércio geral; capacidades são a união dos perfis | `CompanyActivityRulesTest` |
| SA-02 | Toda a capacidade é usada por algum perfil; perfis inválidos ou vazios recusados | `CompanyActivityRulesTest` |
| SA-03 | Perfis gravam e relêem por HTTP; vendedor lê mas não altera | `SectorFoundationHttpIntegrationTest` |
| SA-04 | Factor fixo na mesma grandeza; base inteira recusa fracções | `UnitConversionRulesTest` |
| SA-05 | Catálogo do sistema com códigos únicos e referências por grandeza | `UnitConversionRulesTest` |
| SA-06 | Embalagem não é base; base bloqueia depois de haver unidades; 3 paletes = 120 un; areia em m³ | `SectorFoundationHttpIntegrationTest` |
| SA-07 | Escalões: o maior atingido; validade inclusiva; inactiva não vale | `PriceListRulesTest` |
| SA-08 | Fatura usa a tabela do cliente; sem tabela ou sem cliente, preço de sempre | `SectorFoundationHttpIntegrationTest` |
| SA-09 | Consumidor Final não recebe tabela; tabela com clientes não se elimina | `SectorFoundationHttpIntegrationTest` |
| SA-10 | Rotas novas classificadas na `ApiRolePolicy` | `ApiRolePolicyEndpointCoverageHarnessTest` |
| SA-11 | Migração V112 em sintaxe PostgreSQL e com paridade com as entidades | `MigrationPostgresDialectHarnessTest`, `PostgresSchemaParityHarnessTest` |
| SA-UI-01 | Comercial mantém 10 separadores; Tabelas de Preço dentro de Promoções; instalar 2 vezes não duplica | `SectorFoundationUiHarnessTest` |
| SA-UI-02 | Sem instalador, Comercial e Stock ficam como eram | `SectorFoundationUiHarnessTest` |
| SA-UI-03 | Configurações: Perfil de Actividade no fim, ordem das outras intacta | `SectorFoundationUiHarnessTest` |
| SA-UI-04 | Tabelas novas dentro de `ModernPanel` | `SectorFoundationUiHarnessTest` |
| SA-UI-05/06 | Desktop mostra o mesmo factor do servidor; números no formato central | `SectorFoundationUiHarnessTest` |
| SA-UI-07 | Ícones existem; sem emojis, `new Color(`, `JOptionPane` nem índice de vista | `SectorFoundationUiHarnessTest`, `UiPrecisionHarnessTest` |

## F2 — Material pago por levantar (SA-C07)

| ID | Garantia | Teste |
|---|---|---|
| LV-01 | Saldo = faturado − creditado − levantado, nunca negativo | `PickupBalanceRulesTest` |
| LV-02 | Recusa linha alheia, repetida, negativa, vazia ou acima do saldo (mensagem com números PT-MZ) | `PickupBalanceRulesTest` |
| LV-03 | Fatura de 200 sacos: levanta 20, recusa 181, conclui com 180; stock só mexe na fatura | `PickupHttpIntegrationTest` |
| LV-04 | Mesma `requestKey` devolve o mesmo talão; fatura não se regista duas vezes | `PickupHttpIntegrationTest` |
| LV-05 | Anular talão devolve a quantidade e reabre a conta; vendedor não anula | `PickupHttpIntegrationTest` |
| LV-06 | Material no estaleiro soma os saldos abertos; talão PDF gerado | `PickupHttpIntegrationTest` |
| LV-07 | Migração V120 em PostgreSQL e com paridade; série LV na numeração | `PostgresSchemaParityHarnessTest`, `DocumentSequenceBootstrapTest` |
| LV-UI-01/02 | Pedidos & Separação ganha a secção sem mudar a barra; sem cliente fica como era | `PickupUiHarnessTest` |
| LV-UI-03 | Tabelas dentro de cards | `PickupUiHarnessTest` |

## F2 — Stock a granel por medição (SA-C17)

| ID | Garantia | Teste |
|---|---|---|
| GR-01 | Volumes do monte (rectangular, prisma, cone); medidas em falta ou absurdas recusadas | `BulkMeasurementRulesTest` |
| GR-02 | Diferença desconta o material por levantar; percentagem e tolerância | `BulkMeasurementRulesTest` |
| GR-03 | Sem diferença não mexe; acima da tolerância espera e a aprovação acerta uma vez só; dentro da tolerância acerta logo | `BulkStockHttpIntegrationTest` |
| GR-04 | Material vendido por levantar entra na conta; monte abaixo do vendido é recusado | `BulkStockHttpIntegrationTest` |
| GR-05 | Volume em m³ convertido para toneladas pelas unidades do produto | `BulkStockHttpIntegrationTest` |
| GR-06 | Migração V121 em PostgreSQL e com paridade; rotas na `ApiRolePolicy` | `PostgresSchemaParityHarnessTest`, `ApiRolePolicyEndpointCoverageHarnessTest` |
| GR-UI-01 | Stock ganha um único separador "Granel" no fim, com tabelas em cards | `PickupUiHarnessTest` |

## F2 — Unidade na linha e frete

| ID | Garantia | Teste |
|---|---|---|
| UN-UI-01 | Fatura e encomenda ganham Ctrl+U uma só vez; sem linha seleccionada nada é escrito | `PickupUiHarnessTest` |
| FR-UI-01 | Fatura e encomenda ganham "Adicionar frete" uma só vez; códigos de zona estáveis | `PickupUiHarnessTest` |
| FR-01 | Zona de frete (serviço sem stock) fatura-se com IVA | `SectorFoundationHttpIntegrationTest` |
| FR-02 | A verificação de stock livre ignora artigos que não controlam stock (corrigido: recusava serviços) | `SectorFoundationHttpIntegrationTest` |

## Verificação manual pendente

- Rever os três ecrãs no desktop real, temas claro e escuro, com o procedimento habitual de arranque.
- Confirmar no posto que uma fatura a um empreiteiro com tabela sai com o preço da tabela.
