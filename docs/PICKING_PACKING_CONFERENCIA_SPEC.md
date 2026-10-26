# SPEC: Separação de Encomendas & Conferência Óptica de Embalagens (Picking & Packing)

## 1. Contexto e Motivação
Com a introdução do padrão multinível de embalagens (**Caixa → Embalagem → Unidade**) nas encomendas e faturas, tornou-se essencial fechar o ciclo físico de armazém. Quando um cliente encomenda produtos, os operadores de armazém precisam de:
1. Uma **Folha de Separação A4** profissional e clara, contendo referências, códigos de barras, armazém, lotes recomendados (FEFO) e a discriminação precisa de quantas caixas completas (`Cx.`) e embalagens avulsas (`Emb.`) devem ser recolhidas das prateleiras.
2. Uma ferramenta de **Conferência Óptica** no desktop (`OrderPickingDialog`), permitindo ao operador de expedição bipar os artigos com leitor de código de barras USB/Bluetooth para garantir que o que foi colocado na palete/carrinha coincide exatamente com o solicitado, eliminando faltas ou erros de expedição antes da faturação.

## 2. Invariantes e Regras de Negócio
- **R1 - Cálculo Canónico de Embalagens no Picking:**
  A folha de separação utiliza rigorosamente a mesma formulação canónica do `LineItemsTableRenderer`:
  - `Qtd.`: Total em unidades base.
  - `Emb.`: Embalagens internas por caixa.
  - `Cx.`: Número de caixas inteiras ou fração decimal (ex.: `0.75` para 3 de 4 embalagens; `0.00` para artigos sem caixa configurada).
- **R2 - Suporte Dual de Emissão de Separação:**
  O sistema suporta tanto a emissão em **Folha A4 Executiva** (`OrderPickingSheetPrintService`) para armazéns centrais e frotas de distribuição, como o **Talão Térmico 80mm** (`OrderPickingPrintService`) para separação rápida em balcão/loja.
- **R3 - Conferência Óptica Guiada:**
  O modal `OrderPickingDialog` valida:
  - Bipagem de Código de Barras (EAN-13, EAN-8, SKU ou Código interno).
  - Suporte a bipar caixa inteira ou unidade individual.
  - Alerta sonoro/visual caso seja bipado um artigo que não pertence à encomenda ou se a quantidade exceder o pedido.
  - Opção de confirmação direta `[Conferir Tudo]` para gerentes autorizados.
- **R4 - Transição de Estado Segura:**
  - A encomenda passa de `PENDING` / `AWAITING_SEPARATION` para `IN_SEPARATION` ao emitir a folha de separação.
  - Passa a `SEPARATED` quando todas as linhas forem conferidas com sucesso.
  - Faturação (`billOrder`) só permitida quando o pedido estiver devidamente separado ou aprovado.

## 3. Contratos e DTOs (`contracts`)
- `OrderPickingLineDTO`:
  - `Long lineId`
  - `Long productId`
  - `String productReference`
  - `String barcode`
  - `String productName`
  - `BigDecimal requestedQuantity`
  - `BigDecimal confirmedQuantity`
  - `Integer packagesPerBox`
  - `BigDecimal boxes`
  - `String batchNumber`
  - `LocalDate expiryDate`
- `CompleteOrderPickingRequest`:
  - `Long orderId`
  - `String terminalName`
  - `String operatorNotes`
  - `List<OrderPickingLineConfirmationDTO> confirmedLines`

## 4. Arquitetura e Módulos
- **`contracts`**: Novos DTOs de picking e conferência.
- **`backend`**:
  - `OrderPickingSheetPrintService`: renderizador PDF A4 executivo com tabela corporativa, código de barras e caixas de visto de conferência.
  - `CustomerOrderFulfillmentService`: enriquecimento do método de separação com conferência física detalhada (`completeSeparationWithConfirmation`).
- **`desktop`**:
  - `OrderPickingDialog`: diálogo modal corporativo moderno (`ModernFormDialog`), com tabela protegida contra compressão, campo de leitor de código de barras com auto-foco, resumo de caixas/unidades e validação atómica.
  - `CustomerOrderFulfillmentActions`: integração do novo diálogo no menu de ações de encomendas de `ComercialPanel`.

## 5. Plano de Testes (Harness)
- `OrderPickingSheetPrintServiceTest`: valida renderização da folha de picking A4 com códigos de barras e cálculo de caixas (`0.00` fallback).
- `OrderPickingConferenciaHarnessTest`: valida ciclo de conferência óptica no backend (bipagens parciais, completas, rejeição de produtos estranhos).
- `OrderPickingUiHarnessTest`: valida componentes visuais do diálogo de conferência e atalhos de leitor óptico.
- `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`: garantia de isolamento arquitetural e integridade do Swing.
