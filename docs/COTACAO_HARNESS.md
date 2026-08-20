# Harness — Cotação ao cliente

**Spec:** [COTACAO_SPEC.md](COTACAO_SPEC.md)

Cada caso diz **o que falha se a regra não existir**. Casos marcados 🔴 são os que provam uma regra
nova de dinheiro — se forem removidos do código, tem de haver uma falha visível, não um silêncio.

Automáticos: `QuotationTest` (domínio puro) e `QuotationServiceTest` (Mockito, sem Spring).

---

## 1. Validade — regra de domínio (CT-01..07)

| ID | Cenário | Esperado |
|----|---------|----------|
| CT-01 | `assignValidity(hoje, null)` | `validUntil = hoje + 30` (`QuotationValidity.DEFAULT_DAYS`) |
| CT-02 | `assignValidity(hoje, 7)` | `validUntil = hoje + 7` |
| CT-03 | `assignValidity(hoje, 0)` ou negativo | recusa com mensagem PT-MZ (uma proposta válida zero dias não é proposta) |
| CT-04 | `isExpired(validUntil)` | `false` — o último dia ainda é válido |
| CT-05 | `isExpired(validUntil + 1 dia)` | `true` |
| CT-06 | `daysUntilExpiry` antes / no dia / depois | positivo / zero / negativo |
| CT-07 | `extendValidity` para data anterior à actual | recusa (estender é para a frente) |

## 2. Criação (CT-08..12)

| ID | Cenário | Esperado |
|----|---------|----------|
| CT-08 | `create` com duas linhas | nasce `DRAFT`, número da série `CT`, totais = soma das linhas (`LineCalculator`) |
| CT-09 🔴 | `create` de artigo isento com o catálogo a dizer isento | IVA `0.00` — a taxa vem de `Product.effectiveTaxRate()`, não do pedido |
| CT-10 🔴 | `CreateQuotationLineRequest` | **não** tem campo de preço nem de taxa (auditoria estática: sem campo, não há porta a fechar) |
| CT-11 | `create` sem cliente registado (`clientId` nulo) | usa o cliente de balcão; `walkInName` fica como rótulo |
| CT-12 | `create` com quantidade que atinge a mínima de grosso | preço unitário = preço de grosso (`effectiveUnitPrice`) |

## 3. Máquina de estados (CT-13..20)

| ID | Cenário | Esperado |
|----|---------|----------|
| CT-13 | `send` de uma `DRAFT` | `SENT`, `sentAt` preenchido |
| CT-14 | `send` de uma já `SENT` | recusa |
| CT-15 | `accept` de uma `SENT` | `ACCEPTED`, `decidedAt`/`decidedBy` preenchidos |
| CT-16 | `reject` sem motivo | recusa — motivo obrigatório |
| CT-17 | `reject` com motivo | `REJECTED`, motivo gravado |
| CT-18 | `cancel` de uma `DRAFT`/`SENT` | `CANCELLED` |
| CT-19 | `cancel` de uma já `CONVERTED` | recusa (a encomenda já existe) |
| CT-20 | `accept` de uma `REJECTED` | recusa |

## 4. Conversão — o coração (CT-21..30)

| ID | Cenário | Esperado |
|----|---------|----------|
| CT-21 🔴 | Cotação a 80,00/un; catálogo entretanto a 120,00/un; converter | encomenda sai a **80,00** (R2) |
| CT-22 🔴 | Idem para a **taxa de IVA** e o **desconto** da linha | encomenda herda os da cotação, não os de hoje |
| CT-23 🔴 | Converter uma cotação **expirada** | recusa; mensagem nomeia a data de validade (R4) |
| CT-24 🔴 | Converter duas vezes a mesma cotação | a 2.ª recusa; existe **uma** encomenda (R5) |
| CT-25 | Converter uma `REJECTED` / `CANCELLED` | recusa |
| CT-26 | Converter uma `DRAFT` | aceita **e** carimba a aceitação (`decidedAt` preenchido) |
| CT-27 | Cotação convertida | fica `CONVERTED` com `orderId`/`orderNumber` da encomenda gerada |
| CT-28 | Encomenda gerada | é `FORMAL_ORDER`, nasce `PENDING_APPROVAL` e **é submetida** ao motor de aprovações (R6) |
| CT-29 🔴 | Conversão não move stock | `inventoryService.registerMovement` nunca chamado (R1) |
| CT-30 | Depois de estender a validade, converter | aceita |

## 5. Permissões e auditoria (CT-31..35)

| ID | Cenário | Esperado |
|----|---------|----------|
| CT-31 🔴 | `extendValidity` com perfil `CASHIER` | recusa; validade **inalterada** |
| CT-32 | `extendValidity` com `MANAGER`/`ADMIN` | aceita |
| CT-33 | Auditoria de `extendValidity` | o registo contém a validade **antiga e a nova** |
| CT-34 | `create`/`convert` com perfil sem privilégios | aceites (R1 / gate na aprovação da encomenda) |
| CT-35 | Qualquer acção sobre cotação de **outra empresa** | recusa (escopo por `company_id` no finder) |

## 6. Auditoria estática — uma regra, uma porta (CT-36..39)

| ID | Cenário | Esperado |
|----|---------|----------|
| CT-36 | `documentNumberService.next(DocumentSeries.ORDER)` | aparece **num só** ficheiro de `modules/comercial` (`ComercialService.placeOrder`) |
| CT-37 | `approvalService.submitRequest("ORDER", …)` | aparece **num só** sítio |
| CT-38 | `isExpired(` | definido **uma** vez, no modelo — nem painel nem controller comparam datas de validade |
| CT-39 | `QuotationService` | não importa `InventoryService` nem `FinanceService` (R1 por construção) |

## 7. Migração (CT-40..41)

| ID | Cenário | Esperado |
|----|---------|----------|
| CT-40 | `V44` em base já com dados | cria `quotations`/`quotation_lines`, `UNIQUE(company_id, quotation_number)`; nenhuma tabela existente alterada |
| CT-41 | Duas empresas a chegar ambas a `CT-2026/1` | coexistem (lição da V31) |

---

## 8. Manuais, com o sistema de pé (CT-50..62)

| ID | Passo | Esperado |
|----|-------|----------|
| CT-50 | Criar cotação com 2 artigos, validade 15 dias | aparece na lista como **Rascunho**, "válida até" a 15 dias |
| CT-51 | Imprimir | PDF A4 com cabeçalho igual ao da fatura, validade em destaque, condições e linha de assinatura do cliente |
| CT-52 | Comparar o PDF lado a lado com uma fatura | cabeçalho, bloco do cliente, tabela de linhas e totais indistinguíveis |
| CT-53 | Confirmar que o PDF **não** mostra o armazém | ausente (§10 da spec) |
| CT-54 | Marcar como enviada, depois aceite | estados e datas actualizam na tabela |
| CT-55 | Converter | encomenda criada, com o **mesmo total** da cotação; a cotação fica **Convertida** com o nº da encomenda |
| CT-56 | Tentar converter outra vez | recusa a dizer que já foi convertida e a nomear a encomenda |
| CT-57 | Alterar o preço do artigo no catálogo e converter outra cotação antiga | encomenda sai com o preço **cotado**, não o novo |
| CT-58 | Criar cotação com validade 1 dia, alterar a data do posto para +2 dias, converter | recusa a nomear a data de validade |
| CT-59 | Estender a validade com perfil de caixa | recusa por permissão |
| CT-60 | Estender com gerente e converter | percurso completo até à encomenda |
| CT-61 | Aprovar a encomenda gerada e facturar | percurso completo até à fatura, stock a sair só aí |
| CT-62 | Separador **Movimentos** | a cotação aparece com tipo "Cotação" e o seu total |
