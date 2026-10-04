# Matriz de Testes (Harness): Pagamento Rápido no POS e Badges de Teclado

## 1. Objectivo
Garantir que a experiência de finalização de venda no POS, o cálculo instantâneo de troco com notas em Meticais e a apresentação dos atalhos de teclado funcionam com 100% de precisão e sem regressões visuais ou bloqueio de interface.

---

## 2. Casos de Teste Automatizados

| ID | Cenário | Entrada | Resultado Esperado |
| :--- | :--- | :--- | :--- |
| **QTH-01** | Cálculo de troco exacto | Total: `1250.00 MT`<br>Entregue: `1250.00 MT` | Troco = `0,00 MT` em verde. |
| **QTH-02** | Cálculo de troco com nota superior | Total: `1250.00 MT`<br>Entregue: `1500.00 MT` | Troco = `250,00 MT` em verde. |
| **QTH-03** | Detecção de valor insuficiente | Total: `1250.00 MT`<br>Entregue: `1000.00 MT` | Falta = `250,00 MT` em amarelo/âmbar; rejeição ao confirmar. |
| **QTH-04** | Atribuição de Cédula Moçambicana | Clica em `2000 MT` com Total `1450.00 MT` | Campo entregue assume `2000`; Troco = `550,00 MT`. |
| **QTH-05** | Comutação para método electrónico (M-Pesa/Cartão) | Selecciona `M-Pesa` | Cédulas desativadas, troco oculto/nulo, campo de referência habilitado. |
| **QTH-06** | Componente `KeyBadge` | Tecla `"F9"` | Desenha retângulo com borda, cantos arredondados e texto de alto contraste. |
| **QTH-07** | Decomposição de Linhas | Verificação de ficheiros modificados | `POSPanel.java <= 1000 linhas`, `PosPaymentDialog.java <= 1000 linhas`. |
