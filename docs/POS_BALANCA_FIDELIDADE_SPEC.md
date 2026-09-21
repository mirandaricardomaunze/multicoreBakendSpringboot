# Integração Directa com Balança USB/Serial no POS & Cartão de Fidelidade

**Última actualização:** 2026-09-19
**Estado:** Especificado e em implementação (Fase 13).

## 1. Balança USB/Serial em Tempo Real (`SerialScaleReader` / `PosScaleLiveWidget`)

### Objectivo
Permitir que o operador no balcão de vendas (POS) veja o peso instantâneo capturado directamente da balança física conectada por porta Serial / USB COM (ex: Toledo, NCI, CAS, Avery Berkel), sem depender exclusivamente da impressão de etiquetas de código de barras.

### Funcionamento e Protocolo
1. **Modos de Operação**:
   - **Continuous Data Stream (NCI / Toledo Standard)**: A balança transmite periodicamente tramas ASCII no formato `<STX><Status><Sign><Weight5><Units><CR>`.
   - **Poll / Demand Mode (`[ ⚖️ Capturar ]`)**: Envio do comando de pedido de peso `W<CR>` ou `P<CR>` via RS-232 e recepção da resposta.
   - **Modo de Simulação (`SimulatedSerialScale`)**: Ativado automaticamente em ambientes sem porta COM física ou para testes.
2. **Estados do Peso**:
   - `STABLE`: Peso estabilizado no prato da balança (pronto para captura automática ou com 1 clique).
   - `UNSTABLE`: Balança em movimento ou alteração de carga.
   - `ZERO` / `OVERLOAD`: Prato vazio ou capacidade excedida.
3. **Integração no Checkout de Peso**:
   - Ao seleccionar um artigo de tipo `ProductSaleType.WEIGHT`, o POS atribui automaticamente a quantidade em kg capturada da balança.

## 2. Cartão de Fidelidade do Cliente no POS (`PosLoyaltyController` & `LoyaltyEngine`)

### Objectivo
Identificar o cliente da compra através de Cartão de Fidelidade (código de barras scanner), NUIT ou número de telemóvel no atalho `F6`, permitindo a acumulação de pontos (`1 ponto por cada 100 MT`) e o resgate no checkout.

### Regras de Negócio de Resgate
- **Pontos Disponíveis**: Consultados no saldo do cliente (`ClientDTO.loyaltyPoints`).
- **Equivalência Monetária**: `1 Ponto = 1.00 MT` de desconto.
- **Limite por Venda**: O total de pontos resgatados não pode exceder a quantidade correspondente ao valor total da venda (`RedemptionResult`).
- **Processamento de Checkout**: O valor em pontos resgatados é deduzido como desconto da venda e o saldo final de pontos é atualizado.

## 3. Matriz de Componentes

| Componente | Módulo | Função |
|---|---|---|
| `SerialScaleReader.java` | `desktop` | Driver de leitura da porta Serial COM com suporte NCI/Toledo/Simulado |
| `PosScaleLiveWidget.java` | `desktop` | Widget de UI compacto com indicador de peso vivo, tara e botão de captura |
| `PosLoyaltyController.java` | `desktop` | Controller desacoplado para pesquisa de fidelidade, acumulação e resgate |
| `POSPanel.java` | `desktop` | Painel principal POS estritamente $\le 1000$ linhas |
