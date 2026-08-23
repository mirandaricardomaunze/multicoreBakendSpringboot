# Harness — Encomenda profissional

**Spec:** [ENCOMENDA_PROFISSIONAL_SPEC.md](ENCOMENDA_PROFISSIONAL_SPEC.md)

Cada caso diz **o que falha se a regra não existir**. Casos 🔴 devem ser confirmados a falhar contra
o código anterior — é o que distingue um teste que prova de um teste que acompanha.

Automáticos: `OrderTest`, `OrderStatusLabelTest`, `QuotationServiceTest` (conversão),
`ComercialServiceTest` (placeOrder).

---

## 1. Data de entrega — regra de domínio (EP-01..06)

| ID | Cenário | Esperado |
|----|---------|----------|
| EP-01 | `assignExpectedDelivery(20/08, 7)` | `expectedDeliveryDate = 27/08` |
| EP-02 | `assignExpectedDelivery(20/08, null)` | fica **nula** — encomenda sem data prometida (P6) |
| EP-03 | `assignExpectedDelivery(20/08, 0)` ou negativo | recusa com mensagem PT-MZ |
| EP-04 | `assignExpectedDelivery(null, 7)` | recusa — sem data de confirmação não há conta a fazer |
| EP-05 | `isDeliveryOverdue` no próprio dia / no dia seguinte | `false` / `true` |
| EP-06 | `isDeliveryOverdue` de encomenda `BILLED`/`CANCELLED`/`GUIDED` | `false` — o que está fechado não está em atraso |

## 2. Origem e condições na conversão (EP-07..14)

| ID | Cenário | Esperado |
|----|---------|----------|
| EP-07 🔴 | Converter cotação `CT-x` | encomenda guarda `quotationId` **e** `quotationNumber` (P1) |
| EP-08 🔴 | Cotação com pagamento e prazo preenchidos | encomenda herda **os dois** (P2) |
| EP-09 🔴 | Cotação com `deliveryDays = 7`, convertida hoje | `expectedDeliveryDate = hoje + 7` (P3) |
| EP-10 | Cotação sem `deliveryDays` mas com texto de entrega | texto herdado, data **nula** |
| EP-11 | Cotação sem condições nenhumas | encomenda sem condições; nada rebenta |
| EP-12 | `Quotation.agreedTerms()` | devolve exactamente o que a cotação prometeu |
| EP-13 | Condições da encomenda depois de convertida | são **cópia** — mexer na cotação não as altera (P2) |
| EP-14 | Encomenda criada a partir do catálogo, sem condições no pedido | `OrderTerms.none()`; comportamento igual ao anterior (P6) |

## 3. Estado em PT-MZ (EP-15..19)

| ID | Cenário | Esperado |
|----|---------|----------|
| EP-15 🔴 | `OrderStatusLabel.of("PENDING_APPROVAL")` | "Pendente de aprovação" — nunca a constante |
| EP-16 | Todos os estados de `Order.status` documentados na entidade | têm rótulo próprio (nenhum cai no default) |
| EP-17 | `OrderStatusLabel.of(null)` / estado desconhecido | "—" / devolve o próprio valor, sem rebentar |
| EP-18 | Auditoria estática | a tradução dos estados **de encomenda** existe num só ficheiro (`OrderStatusLabel`); o `UIHelper.humanStatus` genérico é outra coisa e fica de fora (ver nota abaixo) |
| EP-19 | `CustomerOrderFulfillmentService` | delega em `OrderStatusLabel`, sem `switch` próprio |

## 4. PDF (EP-20..27)

| ID | Cenário | Esperado |
|----|---------|----------|
| EP-20 🔴 | PDF de encomenda vinda de cotação | contém "Origem: Cotação CT-…" |
| EP-21 🔴 | Idem | contém as condições e "Entrega prevista: dd/MM/aaaa" |
| EP-22 🔴 | Idem | contém "Estado: Pendente de aprovação" e **não** "PENDING_APPROVAL" (P4) |
| EP-23 | PDF de encomenda **sem** origem nem condições | sai sem esses blocos, sem linhas vazias nem excepção (P6) |
| EP-24 | PDF de encomenda | tem bloco de assinaturas (Pela empresa / Confirmação do cliente) |
| EP-25 | Encomenda A4 vs fatura | cabeçalho, bloco do cliente, tabela de linhas e totais continuam indistinguíveis (ED-51) |
| EP-26 | Auditoria estática: `signatureCell` | os **três documentos comerciais** (encomenda, cotação, guia) usam o `SignatureBlockRenderer` e nenhum tem cópia própria (ver nota abaixo) |
| EP-27 | Cotação e guia de remessa depois da extracção | continuam a sair com as mesmas assinaturas de antes |

> **Nota de rigor — o que estes dois casos NÃO afirmam.** Ao verificá-los ao vivo encontraram-se
> duplicações mais antigas e mais largas do que esta iteração, que ficam **por resolver e
> declaradas**, em vez de escondidas atrás de um caso de harness que passaria por ser estreito:
>
> 1. **`signatureCell` está copiado em mais 10 serviços de impressão** (`Payslip`, `StockTransfer`,
>    `CreditNote`, `DebitNote`, `GuideRemittance`, `InventoryCountSheet`, `InventoryReport`,
>    `IvaDeclaration`, `PayrollFiscalMap`, `POSZReport`) — verificados como **byte-a-byte iguais**
>    ao extraído. Migrá-los é mecânico e o `SignatureBlockRenderer` já os serve, mas são documentos
>    de salários, stock e fiscal fora do âmbito desta alteração.
> 2. **`UIHelper.humanStatus` traduz `PENDING_APPROVAL` como "Pendente"**, enquanto o
>    `OrderStatusLabel` diz "Pendente de aprovação". A mesma encomenda aparece com dois rótulos:
>    "Pendente" na tabela (renderer genérico, partilhado com faturas/notas/guias) e "Pendente de
>    aprovação" no PDF e nos detalhes. Não é contradição — é diferença de precisão — mas é a mesma
>    regra em duas portas, e o `humanStatus` não pode simplesmente passar a delegar porque serve
>    estados que o `OrderStatusLabel` não conhece (`PAID`, `APPROVED`, …).

## 5. Migração (EP-28..30)

| ID | Cenário | Esperado |
|----|---------|----------|
| EP-28 | `V45` sobre `customer_orders` com dados | cinco colunas novas, **todas nullable**; nenhuma linha existente alterada |
| EP-29 | Encomenda anterior a esta versão | origem/condições/data a nulo; imprime e comporta-se como sempre (P6) |
| EP-30 | Colunas da `V44`+`V45` vs mapeamento das entidades | batem exactamente, nos dois sentidos (é o que o `ddl-auto=validate` exige) |

---

## 6. Manuais, com o sistema de pé (EP-50..58)

| ID | Passo | Esperado |
|----|-------|----------|
| EP-50 | Cotar com pagamento "30 dias", entrega "7 dias úteis" e 7 dias de prazo | cotação emitida com as três coisas |
| EP-51 | Converter | encomenda com origem `CT-…`, as condições herdadas e entrega prevista = hoje + 7 |
| EP-52 | Imprimir a encomenda | origem, condições, entrega prevista, assinaturas e estado em português |
| EP-53 | Procurar códigos internos no PDF | nenhum (`PENDING_APPROVAL`, `FORMAL_ORDER`, …) |
| EP-54 | Comparar com o PDF de uma fatura | blocos partilhados indistinguíveis |
| EP-55 | Criar encomenda à mão, sem condições, e imprimir | documento limpo, sem blocos vazios |
| EP-56 | Tabela de encomendas | colunas **Origem** e **Entrega** preenchidas nas que vieram de cotação |
| EP-57 | Ver detalhes da encomenda | origem, condições e entrega prevista visíveis |
| EP-58 | Aprovar e facturar a encomenda vinda da cotação | percurso completo; a data prometida **não** se mexe com o atraso da aprovação |
