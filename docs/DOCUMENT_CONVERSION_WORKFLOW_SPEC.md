# SPEC-DCW-001: Fluxo Canónico de Conversão Directa entre Documentos Comerciais

## 1. Princípio de Negócio e Enquadramento

No ciclo comercial do Multicore ERP, a transição entre estados operacionais deve ser contínua e sem atrito:
```
[ Cotação (QT) ] ──┬──> [ Encomenda de Cliente (EC) ] ──┬──> [ Guia de Remessa / Transporte (GR/GT) ]
                   │                                     └──> [ Factura Comercial (FT) ]
                   └────────────────────────────────────────> [ Factura Comercial Directa (FT) ]
```

Nenhum operador deve redigitar artigos, quantidades, preços unitários, descontos ou lotes ao progredir um negócio da fase de proposta para a fase de separação, transporte ou faturamento definitivo.

## 2. Regras de Conversão e Rastreabilidade

1. **Cotação para Encomenda de Cliente:**
   - Transfere cliente, armazém, linhas de produtos, quantidades e descontos acordados.
   - Estado da cotação passa a `CONVERTED` ou `ACCEPTED`.
   - A Encomenda criada herda a referência da cotação de origem.

2. **Cotação para Factura Comercial:**
   - Conversão directa quando o cliente aprova e liquida de imediato sem separação diferida.
   - Gera fatura no estado emitido, baixa de stock no armazém e lançamento em contas a receber.

3. **Encomenda de Cliente para Factura Comercial:**
   - Pode ser acionada tanto na listagem de encomendas (`Central de Pedidos e Separação`) quanto a partir do editor da encomenda.
   - Gera fatura com as linhas encomendadas e sincroniza o estado da encomenda para `INVOICED` / `FATURADA`.

4. **Encomenda de Cliente para Guia de Remessa / Transporte:**
   - Gera documento de circulação com motorista, viatura e armazém de destino para transporte de mercadorias.

## 3. Ergonomia e Interface (Desktop Swing)

- No painel de encomendas (`CommercialOrdersView` e `ComercialPanel`), as opções de conversão estão organizadas no menu de acções `Processo` e no editor documental.
- No painel de cotações (`QuotationsPanel` e `QuotationEditorForm`), acções visíveis e atalhos permitem a conversão imediata com diálogo de confirmação claro em português de Moçambique.
- Se o documento já estiver faturado ou cancelado, as acções de conversão ficam desabilitadas de forma segura.

## 4. Harness e Verificação Automatizada

- A classe `DocumentConversionWorkflowHarnessTest` verifica:
  1. Disponibilidade dos métodos de conversão no painel de cotações e encomendas.
  2. Proteção contra conversões duplicadas.
  3. Preservação de linhas e integridade dos cálculos a 2 casas decimais.
