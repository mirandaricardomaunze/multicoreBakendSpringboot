# SPEC-COTACAO-CONVERSAO-001 — Conversão Direta de Cotações e Pró-formas em Fatura ou Venda no POS

## 1. Contexto e Necessidade do Negócio

No ambiente comercial e de retalho/grossista do ERP Multicore, propostas comerciais e orçamentos aprovados pelo cliente (série `CT`) precisam de transitar de forma ágil para a liquidação final:
1. **Faturação Comercial Direta (B2B):** Quando um cliente formal aceita uma cotação/pró-forma, o departamento comercial deve poder convertê-la directamente numa Factura comercial (série `FT`), herdando preços cotados, quantidades, taxas de IVA e descontos acordados, sem necessidade de cerimónia adicional de encomenda interna caso o fluxo da empresa assim o exija.
2. **Atendimento ao Balcão no Ponto de Venda (POS):** Quando um cliente chega ao balcão com um orçamento/cotação (ou refere o número da cotação / seu nome), o operador de caixa deve poder importar a cotação aberta directamente para o carrinho do POS num único clique, carregando cliente, armazém, artigos com os respetivos preços unitários acordados e descontos da proposta.
3. **Integridade de Preços e Fecho de Ciclo:**
   - **O preço cotado é o preço honrado:** Os preços unitários acordados na cotação têm precedência sobre os preços de tabela do catálogo à data da conversão.
   - **Controlo de Validade:** Cotações expiradas não podem ser convertidas sem estender previamente a validade com autorização de gerência.
   - **Rastreabilidade e Estado Terminal:** Uma cotação convertida passa ao estado terminal `CONVERTED` (com registo do `invoiceId`, `invoiceNumber`, data e operador), não podendo ser convertida em duplicado.

---

## 2. Requisitos Funcionais

### RF-01: Conversão Direta de Cotação em Fatura Comercial (`POST /api/comercial/quotations/{id}/convert-to-invoice`)
- **Pré-condições:** Cotação deve pertencer à empresa ativa, estar em estado aberto (`DRAFT`, `SENT`, `ACCEPTED`) e com data de hoje $\le$ `validUntil`.
- **Efeito:**
  - Gera Factura comercial (série `FT`, canal `DIRECT`).
  - Preenche cliente, empresa, armazém, data de vencimento e linhas com quantidades e preços unitários cotados.
  - Verifica limite de crédito do cliente (`assertCreditAvailable`).
  - Baixa de stock no armazém da cotação (movimento físico `SALE`).
  - Se o desconto de linha exceder 10%, submete a aprovação de desconto; caso contrário, nasce no estado `APPROVED`.
  - Atualiza a cotação para o estado `CONVERTED`, registando `invoiceId`, `invoiceNumber` e data de decisão.
  - Registo em log de auditoria: `QUOTATION_CONVERT_INVOICE`.

### RF-02: Consulta de Cotações Abertas Vigentes (`GET /api/comercial/quotations/open?companyId={companyId}`)
- Devolve as cotações da empresa ativa nos estados `DRAFT`, `SENT`, `ACCEPTED` cuja validade ainda não tenha expirado.
- Utilizado pelo diálogo de importação do POS e seletores rápidos.

### RF-03: Importação de Cotação no Ponto de Venda (POS)
- No `POSPanel`, ação "Importar Cotação" abre o diálogo modal `PosImportQuotationDialog`.
- Lista cotações abertas com pesquisa incremental (número da cotação, cliente, NUIT).
- Ao confirmar a importação:
  - Seleciona automaticamente o cliente da cotação no cabeçalho do POS.
  - Seleciona o armazém associado da cotação.
  - Popula o carrinho com os produtos, quantidades cotadas, descontos acordados e **preços unitários cotados** (`customUnitPrice`).
  - Associa o identificador da cotação (`currentQuotationId`) à sessão de venda do carrinho.
  - Exibe um indicador/banner de cotação importada no carrinho com opção de remoção de vínculo.

### RF-04: Checkout no POS com Liquidação da Cotação
- No `POST /api/pos/checkout`, o DTO `POSCheckoutRequest` recebe o campo opcional `quotationId`.
- No `POSCheckoutLineRequest`, suporta campo opcional `unitPrice` para honrar o valor cotado na proposta.
- Quando `quotationId` é fornecido:
  - `POSService.checkout` valida a cotação e atualiza o seu estado para `CONVERTED`, gravando o número da fatura emitida pelo POS (`invoiceNumber`), `invoiceId`, data e operador.
  - Audita o evento: `QUOTATION_CONVERT_POS`.

---

## 3. Modelo de Dados e Migração Flyway

Tabela `quotations` enriquecida na migração `V75__quotation_invoice_conversion.sql`:
- `invoice_id`: `BIGINT REFERENCES invoices(id)`
- `invoice_number`: `VARCHAR(255)`
- Índice: `idx_quotations_company_invoice ON quotations (company_id, invoice_id)`

---

## 4. Fronteiras de Módulos (Maven Multi-Module)

- **`contracts`:**
  - `QuotationDTO`: adicionados `Long invoiceId` e `String invoiceNumber`, mantendo construtor retrocompatível.
  - `POSCheckoutLineRequest`: adicionado `BigDecimal unitPrice`, mantendo construtores retrocompatíveis.
  - `POSCheckoutRequest`: adicionado `Long quotationId`, mantendo construtores retrocompatíveis.
- **`backend`:**
  - `Quotation.java`, `QuotationService.java`, `QuotationController.java`, `ComercialService.java`, `POSService.java`.
- **`desktop`:**
  - `ComercialApiClient.java`, `QuotationsPanel.java`, `PosCartItem.java`, `PosImportQuotationDialog.java`, `POSPanel.java`.

---

## 5. Regras Não Negociáveis

1. Limite estrito de $< 1000$ linhas em ficheiros Swing (decomposição em `PosImportQuotationDialog`).
2. Proibição de emojis crus Unicode nos componentes Swing (usar FontAwesome vetorial via `UIHelper.icon`).
3. Valores monetários e quantidades sempre com `BigDecimal`.
4. Mensagens para o operador em português de Moçambique.
