# Harness — Numeração de documentos única por empresa

Ver [NUMERACAO_MULTIEMPRESA_SPEC.md](NUMERACAO_MULTIEMPRESA_SPEC.md).

Verificação **manual ao vivo** (backend `prod` contra PostgreSQL real + 2 sessões HTTP em paralelo).
Guião reproduzível: `scratchpad/concurrency-demo.ps1` (login duplo → resolve armazém/produto/cliente por
HTTP → dispara N faturas em paralelo por 2 "postos" → verifica números + stock).

## Automáticos
Não há teste automático dedicado: a colisão é uma regra **da BD** (restrição única) e o
`ComercialServiceTest` usa repositórios **mock** (não a faz cumprir). A regressão é coberta pela
`@UniqueConstraint(company_id, numero)` nas entidades (H2 nos testes) + verificação ao vivo abaixo.

| ID | Cenário | Esperado |
|----|---------|----------|
| NM-01 | `mvn -o compile` após alterar as 8 entidades | BUILD SUCCESS |
| NM-02 | `ComercialServiceTest` + `POSServiceTest` + `PurchaseOrderServiceTest` | 64, 0 falhas |
| NM-03 | `InvoiceNumberUniquenessPerCompanyTest` (@DataJpaTest, comportamento em H2) | 2, 0 falhas |
| NM-04 | `DocumentNumberPerCompanyConstraintTest` — invariante por reflexão nos **9** tipos de documento: `@Table` declara `UNIQUE(company_id, número)` e a coluna **não** tem `unique=true` | 9, 0 falhas; falha se qualquer entidade for revertida (flip-proof feito no `Order`) |

## Manuais (ao vivo)

| ID | Passos | Esperado | Resultado |
|----|--------|----------|-----------|
| NM-50 | Antes do fix: 2 empresas, a 2.ª a numerar até um número que a 1.ª já tem | HTTP 500 `invoices_invoice_number_key` | ✅ reproduzido (MZ `FT-2026/5` vs PT `FT-2026/5`) |
| NM-51 | Aplicar `V31` (reiniciar backend) | Flyway V31 `success=t`; `invoices` passa a `UNIQUE (company_id, invoice_number)` | ✅ |
| NM-52 | Depois do fix: empresa MZ cria `FT-2026/5..12`, 2 postos em paralelo | 8/8 OK, gapless, sem colidir com a PT | ✅ `FT-2026/5..12`, stock −8 |
| NM-53 | Confirmar coexistência na BD | `FT-2026/5` existe para company_id 1 **e** 2 | ✅ 2 linhas |
| NM-54 | Mesma empresa (PT), 2 postos, 8 faturas em paralelo | consecutivas, distintas, sem buracos; stock exacto | ✅ `FT-2026/6..13` e `/14..21`, stock −8 |
| NM-55 | `createInvoice` com `ConcurrencyRetry` ligado (happy path) | 200, número emitido | ✅ |

## Follow-up
- `payslips` (sem `company_id`) continua com `UNIQUE(payslip_number)` global — só colide se a numeração
  de recibos de salário for por empresa e duas empresas alcançarem o mesmo número. Precisa de
  `company_id` na tabela antes de aplicar o mesmo padrão.
