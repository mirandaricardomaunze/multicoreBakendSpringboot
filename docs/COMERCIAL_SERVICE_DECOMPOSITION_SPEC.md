# Especificação de Decomposição — ComercialService (Fase 194)

## Objetivo
Decompor o monólito `ComercialService` (`2.155` linhas) em 4 colaboradores coesos com responsabilidade única, reduzindo a fachada `ComercialService` para coordenação fina e mantendo os pontos de bloqueio pessimista e paginação verificados pelos harnesses de concorrência e escalabilidade (`InvoicePaymentConcurrencyHarnessTest` e `OrderPaginationHarnessTest`).

## Colaboradores Extraídos (`mz.multicore.erp.modules.comercial.service`)
1. `ComercialClientAndCreditService` — Gestão de clientes (CRUD, validação fiscal NUIT/NIF, auditoria de limite de crédito), elegibilidade de crédito, mora e cálculo de vencimento a partir das condições de pagamento.
2. `ComercialProductCatalogService` — Catálogo de artigos, categorias, taxas de IVA activas, composição multinível de embalagens (`Caixa → Embalagem → Unidade`), pesos logísticos, catálogo paginado POS e imagens de artigos.
3. `ComercialInvoiceLifecycleService` — Emissão de faturas comerciais (directa, a partir de cotação, ou com aprovação de desconto especial >10%), anulação com reversão de stock e recibos, listagens/paginação de faturas e resumo analítico de vendas POS.
4. `ComercialOrderLifecycleService` — Ciclo de vida de encomendas de clientes e reposições internas (criação, edição com revalidação de reserva, aprovação/rejeição, conversão em fatura `billOrder`, cancelamento e controlo de impressão).

## Compatibilidade
- Construtores legados de 19 e 21 argumentos preservados em `ComercialService`.
- Métodos estáticos e públicos preservados sem quebra de contrato.
