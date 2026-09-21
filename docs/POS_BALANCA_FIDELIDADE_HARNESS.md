# Matriz de Testes & Validação: Balança USB/Serial & Cartão de Fidelidade POS

**Última actualização:** 2026-09-19
**Especificação associada:** `docs/POS_BALANCA_FIDELIDADE_SPEC.md`

## Critérios de Aceitação & Cobertura de Testes

| ID | Área | Descrição do Teste | Estado Esperado |
|---|---|---|---|
| **SCL-01** | Balança Serial | Parsing de tramas NCI / Toledo `<STX>001.450kg<CR>` extraindo 1.450 kg | Passa (`SerialScaleReaderTest`) |
| **SCL-02** | Balança Serial | Suporte a tara de recipiente e zeragem do indicador de peso | Passa (`SerialScaleReaderTest`) |
| **SCL-03** | Balança Serial | Deteção de peso instável (`UNSTABLE`) e bloqueio de leitura no prato | Passa (`SerialScaleReaderTest`) |
| **SCL-04** | Balança UI | Atualização do `PosScaleLiveWidget` refletindo o peso vivo em tempo real | Passa (`PosScaleWidgetTest`) |
| **SCL-05** | POS Checkout | Atribuição automática da quantidade capturada da balança ao adicionar artigo `WEIGHT` | Passa (`PosScaleLoyaltyHarnessTest`) |
| **LYT-01** | Fidelização | Identificação rápida de cliente por Cartão de Fidelidade `LOY-1001` via scanner / `F6` | Passa (`PosScaleLoyaltyHarnessTest`) |
| **LYT-02** | Fidelização | Cálculo de pontos ganhos na venda (`LoyaltyEngine.calculateEarnedPoints`) | Passa (`PosScaleLoyaltyHarnessTest`) |
| **LYT-03** | Fidelização | Validação de resgate máximo de pontos respeitando o saldo e total da compra | Passa (`PosScaleLoyaltyHarnessTest`) |
| **LYT-04** | Fidelização | Abatimento do desconto de pontos no total a pagar e atualização do saldo | Passa (`PosScaleLoyaltyHarnessTest`) |
| **SYS-01** | Decomposição | `POSPanel.java` mantido $\le 1000$ linhas | Passa (`UiPanelDecompositionTest`) |
