# Harness — Encomendas: duas vias declaradas

**Spec:** [ENCOMENDA_DUAS_VIAS_SPEC.md](ENCOMENDA_DUAS_VIAS_SPEC.md)

Cada caso diz **o que falha se a regra não existir**. Casos marcados 🔴 devem ser confirmados a
falhar contra o código anterior — é o que distingue um teste que prova de um teste que acompanha.

---

## 1. O tipo como facto do documento (ED-01..08)

| ID | Cenário | Esperado |
|----|---------|----------|
| ED-01 | `OrderKind.FORMAL_ORDER.requiresApproval()` | `true` |
| ED-02 | `OrderKind.PICKING_REQUEST.requiresApproval()` | `false` |
| ED-03 | `PICKING_REQUEST.isThermal()` / `FORMAL_ORDER.isThermal()` | `true` / `false` |
| ED-04 | Rótulos PT-MZ de ambos | "Pedido de separação" / "Encomenda (A4)" — sem códigos internos |
| ED-05 | `createOrder` sem tipo indicado | nasce `FORMAL_ORDER` (R1) |
| ED-06 | `createOrder` de uma `FORMAL_ORDER` | estado `PENDING_APPROVAL` **e** pedido de aprovação submetido (R2) |
| ED-07 🔴 | `createOrder` de um `PICKING_REQUEST` | **nenhum** pedido de aprovação é submetido (R3) |
| ED-08 | `CreateOrderRequest` com o construtor antigo (4 args) | compila e resulta em `FORMAL_ORDER` |

## 2. Circuito de separação fechado à via A4 (ED-09..14)

| ID | Cenário | Esperado |
|----|---------|----------|
| ED-09 🔴 | `printForPicking` sobre uma `FORMAL_ORDER` | recusa; mensagem nomeia a encomenda e diz que é do tipo A4 (R4) |
| ED-10 🔴 | `completeSeparation` sobre uma `FORMAL_ORDER` | recusa pela mesma razão, antes de olhar para o estado |
| ED-11 | `reprint` sobre uma `FORMAL_ORDER` | recusa |
| ED-12 | Mensagem de recusa da via errada | sem `FORMAL_ORDER`/`PICKING_REQUEST` no texto |
| ED-13 | `submit` (expedição) com `kind=FORMAL_ORDER` no pedido HTTP | servidor força `PICKING_REQUEST` (§6) |
| ED-14 | `submit` já não cancela aprovações | `cancelPendingForDocument` não é chamado no caminho normal |

## 3. Impressão decidida pelo tipo (ED-15..18)

| ID | Cenário | Esperado |
|----|---------|----------|
| ED-15 🔴 | Encomenda num estado desconhecido (`kind=PICKING_REQUEST`) | continua a sair talão térmico, não A4 (R5) |
| ED-16 | `FORMAL_ORDER` em qualquer estado | sai sempre A4 |
| ED-17 | Selector do desktop com `kind` conhecido | não consulta o estado para escolher o formato |
| ED-18 | `OrderDTO` transporta `kind` e `kindLabel` | ambos preenchidos |

## 4. Desenho partilhado por construção (ED-19..24)

| ID | Cenário | Esperado |
|----|---------|----------|
| ED-19 🔴 | Talão de separação de empresa com NUIT/morada/telefone | os três aparecem no PDF (hoje não aparecem) |
| ED-20 🔴 | Talão de separação de empresa com logótipo | imagem embutida no PDF |
| ED-21 | Talão de separação de empresa sem logótipo / logótipo ilegível | sai na mesma, sem excepção |
| ED-22 | Recibo do POS depois da extracção | `ReceiptPrintServiceTest` continua verde, sem alterar asserções |
| ED-23 | Auditoria estática: `buildClientBlock` | existe **num só** ficheiro em `modules/printing` (R8) |
| ED-24 | Auditoria estática: cabeçalho térmico | `ReceiptPrintService` e `OrderPickingPrintService` não desenham o cabeçalho por conta própria (R7) |

## 5. Migração (ED-25..27)

| ID | Cenário | Esperado |
|----|---------|----------|
| ED-25 | Encomenda antiga com `idempotency_key` | fica `PICKING_REQUEST` |
| ED-26 | Encomenda antiga em `BILLED`/`PENDING`/`CANCELLED` sem chave | fica `FORMAL_ORDER` |
| ED-27 | Encomenda antiga em `SEPARATED` sem chave | fica `PICKING_REQUEST` (rede de segurança) |

---

## 6. Manuais, com o sistema de pé (ED-50..58)

| ID | Passo | Esperado |
|----|-------|----------|
| ED-50 | Criar encomenda escolhendo **Encomenda (A4)** | aparece em "por aprovar"; coluna Tipo mostra "Encomenda (A4)" |
| ED-51 | Imprimir essa encomenda | PDF A4 lado a lado com uma fatura: cabeçalho, bloco do cliente e totais indistinguíveis |
| ED-52 | Tentar marcá-la como separada | recusa a explicar que é do tipo A4 |
| ED-53 | Aprovar e facturar | percurso completo até à fatura |
| ED-54 | Criar escolhendo **Pedido de separação** | entra directo em "aguarda separação", sem passar por aprovações |
| ED-55 | Imprimir o pedido | talão de 80 mm **com** logótipo, NUIT, morada, telefone e linhas pontilhadas |
| ED-56 | Comparar esse talão com um recibo do POS | mesmo cabeçalho e mesmo estilo |
| ED-57 | Marcar como separado e facturar | percurso completo |
| ED-58 | Encomenda criada antes desta versão | continua a imprimir e a comportar-se como sempre |
