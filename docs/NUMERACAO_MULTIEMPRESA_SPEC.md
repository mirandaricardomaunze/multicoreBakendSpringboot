# Numeração de documentos única POR EMPRESA (correcção multi-tenant)

## Problema

A numeração de documentos é **gerada por empresa** (ver `DocumentNumberService` + tabela
`document_sequences` com chave `(company_id, series, doc_year)`, migração `V30`). Porém, a coluna do
**número** em cada tabela de documento tinha uma restrição `UNIQUE` **global** — só sobre a coluna do
número, sem a empresa.

Consequência: duas empresas distintas (dois NUITs) que cheguem ao **mesmo número** (ex.: ambas
`FT-2026/5`) colidem na chave única; a segunda gravação falha com `ConstraintViolationException`
(HTTP 500). **Falha mesmo sem concorrência** — basta a 2.ª empresa alcançar um número que a 1.ª já
emitiu. É um bug de correcção multi-tenant, não de concorrência.

Reproduzido ao vivo (PostgreSQL real): empresa MZ com contador FT=4 → próximo `FT-2026/5`; empresa PT
já tinha `FT-2026/5` → 500. Descoberto ao validar "vários postos ao mesmo tempo".

## Correcção

Trocar `UNIQUE(numero)` por `UNIQUE(company_id, numero)` nas **8 tabelas de documento que têm
`company_id`** — alinhado com o que `employees` / `product_batches` / `clients` já faziam, e com a
realidade fiscal (cada NUIT tem a sua própria série). O número **string mantém-se** (`FT-2026/N`); o
documento impresso identifica a empresa pelo cabeçalho/NUIT.

- **Entidades** (`@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", <numero>}))`,
  remoção do `unique = true` da coluna): `Invoice`, `Order`, `CreditNote`, `DebitNote`, `Receipt`,
  `Purchase`, `PurchaseOrder`, `StockTransfer`.
- **Migração** `V31__per_company_document_numbers.sql`: larga a `UNIQUE(numero)` global e adiciona
  `uk_<tabela>_company_number (company_id, numero)`. Seguro sobre dados existentes (a global garantia
  que não há repetidos, logo também não há dentro de nenhuma empresa).

**`payslips` (feito na V32):** a tabela não tinha `company_id`, pelo que a mesma correcção exigiu
primeiro adicionar a coluna (= empresa do colaborador, backfill de `employees`) e a FK, antes de trocar
`UNIQUE(payslip_number)` por `UNIQUE(company_id, payslip_number)`. `Payslip` ganhou o campo `company`
(definido em `HRService.createPayslip` a partir do `employee`). Regressão:
`PayslipNumberUniquenessPerCompanyTest`.

## Rede de segurança de concorrência (relacionada)

`ConcurrencyRetry` (`architecture/concurrency`): reexecuta uma escrita quando falha por
`ConcurrencyFailureException` (lock optimista `@Version` ou pessimista não adquirido), 3 tentativas com
backoff curto, **cada tentativa numa transação nova** (o `Supplier` invoca o método `@Transactional`
pelo proxy do bean). Ligado em `POSController.checkout` e `ComercialController.createInvoice`. Não
substitui os locks (numeração pessimista gapless, `@Version`) — é a rede para o caso raro de dois
postos escreverem a MESMA linha no mesmo instante por um caminho que não serializa.

## Notas de desenho

- Hibernate `validate` (perfil prod) **não** valida restrições únicas — a mudança da anotação não
  quebra o arranque; a migração é a fonte da verdade no PostgreSQL.
- Testes correm em H2 com Flyway desligado (schema gerado das entidades); a `@UniqueConstraint`
  composta passa a valer também aí. A migração só corre em PostgreSQL (prod).
