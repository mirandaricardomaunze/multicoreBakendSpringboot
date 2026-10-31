# Especificação de Decomposição: ReceiptPrintService (Fase 153)

## 1. Diagnóstico do Monólito
`ReceiptPrintService.java` possuía **231 linhas**, misturando:
- Carregamento de fatura (`InvoiceRepository`), validação de tenant, leitura de configuração de colunas (`DocumentConfigService`) e entradas de pagamento (`PaymentEntryRepository`).
- Renderização OpenPDF de recibo térmico 80mm (cabeçalho, linhas de artigos com sublinha de referência/IVA, totais, pagamentos/troco e rodapé).

## 2. Arquitectura Alvo
1. `ReceiptPrintService` (`<= 70` linhas): Fachada `@Service` que carrega a fatura, valida a empresa, consulta colunas e pagamentos e delega ao renderer, mantendo `vatRateLabel` e `lineDetailsLabel`.
2. `ReceiptDocumentRenderer` (`<= 190` linhas): Componente `@Component` responsável pela composição térmica 80mm do recibo POS.
