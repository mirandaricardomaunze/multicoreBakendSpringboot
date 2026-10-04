# SPEC-PCM-001: Gestão Canónica de Sangrias, Suprimentos e Validação de Caixa no POS

## 1. Princípio e Enquadramento Operacional

No ponto de venda (POS), o controlo de numerário na gaveta é crítico para prevenção de quebras e fraudes:
- **Suprimento (Entrada):** Injeção de numerário na gaveta a meio do turno para reforço de trocos.
- **Sangria (Saída):** Retirada de numerário em excesso para o cofre ou pagamento de pequenas despesas autorizadas de loja.

Nenhuma operação deve ser realizada por caixas de diálogo genéricas (`JOptionPane`) nem sem validação do saldo em gaveta e motivo descritivo.

## 2. Diálogo Canónico de Movimentação (`PosCashMovementDialog`)

O diálogo apresenta:
1. **Cabeçalho de Caixa:**
   - Número da sessão activa, operador responsável e hora.
2. **Seletor de Tipo com Distinção Visual Semântica:**
   - `SUPRIMENTO`: Verde de aprovação (`APPROVED_GREEN`), entrada de troco.
   - `SANGRIA`: Amarelo de cautela / alerta (`PENDING_YELLOW` ou `REJECTED_RED`), retirada para cofre.
3. **Campos Obrigatórios:**
   - `Valor (MT)`: Campo `MoneyField` formatado com 2 casas decimais (`#,##0.00 MT`), valor > 0.
   - `Motivo / Justificação`: Texto obrigatório que fica registado na auditoria e no relatório Z de fecho de turno.
4. **Validações e Regras de Segurança:**
   - O valor tem de ser estritamente positivo (> 0).
   - O motivo é obrigatório para efeitos de prestação de contas.
   - Na sangria, o sistema alerta caso o valor solicitado seja superior ao numerário em caixa.

## 3. Comprovativo e Rastreabilidade

- A operação gera um registo em `till_movements`.
- No fecho de turno (`Relatório Z`), os suprimentos somam ao saldo esperado e as sangrias são subtraídas de forma discriminada.
- O terminal POS atualiza imediatamente o resumo de caixa e os botões de estado.

## 4. Harness e Verificação

- `PosCashMovementsHarnessTest` verifica:
  1. Instanciação do diálogo e validações de input (valor positivo e motivo).
  2. Integração com `PosCashSessionActions.java`.
  3. Ausência de emojis e conformidade com os tokens de design do ERP.
