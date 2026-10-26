# Especificação de Decomposição - QuotationService

## 1. Contexto e Motivação
O `QuotationService` (552 linhas) é o serviço autoritativo de propostas comerciais e cotações ao cliente no Multicore ERP (docs/COTACAO_SPEC.md). Ele concentra três responsabilidades de negócio fundamentais:
1. **Ciclo de Vida da Proposta:** Emissão em rascunho (`create`), edição com bloqueio optimista de versão (`update`), envio ao cliente (`send`), aceitação (`accept`), rejeição com motivo auditado (`reject`), cancelamento (`cancel`) e extensão de validade por gerente (`extendValidity`).
2. **Conversão Documental Fiel:** Conversão para Encomenda de Cliente (`convert`) e Factura Comercial directa (`convertToInvoice`), assegurando a regra inviolável de que o preço cotado é rigorosamente o preço honrado (sem reapreçamento pelo catálogo) e que cotações caducadas são bloqueadas.
3. **Consultas Analíticas e Projeção:** Listagem por empresa, filtragem em tempo real de propostas em aberto e não-expiradas, e projecção detalhada com resolução de unidades de negócio (`toDTO`).

## 2. Nova Arquitectura Modular
O `QuotationService` passa a actuar como orquestrador enxuto ($\le 160$ linhas) delegando para 3 serviços especializados no pacote `mz.multicore.erp.modules.comercial.service`:

1. `QuotationLifecycleService` ($\le 295$ linhas):
   - Gestão das fases de negociação da proposta (`create`, `update`, `send`, `accept`, `reject`, `cancel`, `extendValidity`).
   - Validações comerciais e apreçamento inicial com `LineCalculator`.
2. `QuotationConversionService` ($\le 150$ linhas):
   - Transformação documental de cotações em encomendas (`OrderDTO`) ou facturas (`InvoiceDTO`).
   - Carimbo de decisão e garantia de integridade contratual (preços acordados e prazos).
3. `QuotationQueryService` ($\le 135$ linhas):
   - Consultas de cotações por empresa (`findByCompany`, `findOpenByCompany`, `findById`).
   - Mapeamento analítico para `QuotationDTO` e `QuotationLineDTO`, com cálculo derivado de caducidade e unidade de negócio.

## 3. Compatibilidade e Regras
- Preservar o construtor sobrecarregado com 9 parâmetros em `QuotationService` para compatibilidade total com os testes unitários (`QuotationServiceTest`, `QuotationConversionHarnessTest`).
- Nenhuma dependência com `InventoryService` ou `FinanceService` na cotação.
- Respeitar as directrizes de auditoria e internacionalização pt-MZ.
