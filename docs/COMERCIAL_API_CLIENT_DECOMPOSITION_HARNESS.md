# Plano de Testes & Harness - ComercialApiClient (Fase 93)

## 1. Objectivos de Teste

Validar a integridade estrutural, desacoplamento e limites físicos da decomposição de `ComercialApiClient`.

## 2. Casos de Teste (CACD)

- **CACD-01: Limites físicos estritos de linhas de código:**
  - `ComercialApiClient.java` $\le 225$ linhas.
  - `CommercialClientApiClient.java` $\le 60$ linhas.
  - `CommercialProductApiClient.java` $\le 140$ linhas.
  - `CommercialOrderApiClient.java` $\le 150$ linhas.
  - `CommercialQuotationApiClient.java` $\le 105$ linhas.
  - `CommercialDeliveryGuideApiClient.java` $\le 75$ linhas.
  - `CommercialInvoiceApiClient.java` $\le 185$ linhas.
- **CACD-02: Zero diálogos legados JOptionPane:**
  - Nenhum ficheiro do pacote `desktop/client` pode conter chamadas a `JOptionPane`.
- **CACD-03: Delegação canónica para clientes especializados:**
  - `ComercialApiClient` deve delegar para cada um dos 6 sub-clientes especializados.
- **CACD-04: Contratos e métodos de api preservados:**
  - Verificação de métodos centrais como `getClients`, `getProductsPage`, `getOrdersByCompany`, `createQuotation`, `createDeliveryGuide`, `createInvoice`, `getReceivablesAging`.
