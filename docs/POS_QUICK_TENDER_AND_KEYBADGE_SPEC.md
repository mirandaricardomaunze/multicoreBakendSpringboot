# Especificação Técnica: Pagamento Rápido no POS (Quick Tender) e Badges de Teclado

## 1. Visão Geral e Objectivo
Esta especificação define o padrão para o **Ecrã de Pagamento Comercial Rápido (Quick Tender)** e os **Badges Visuais de Tecla de Atalho (KeyBadges)** no Multicore ERP. O objectivo é proporcionar ao operador de caixa um fluxo de checkout ultra-veloz, sem necessidade de calculadora, com feedback visual claro de troco em Meticais (MT) e suporte a métodos de pagamento moçambicanos (Numerário, Cartão/POS, M-Pesa, e-Mola, Transferência).

---

## 2. Princípios de Design e Usabilidade Comercial

1. **Visor de Troco de Alto Contraste:** O valor do troco deve ser o elemento visual de maior destaque após a digitação do valor recebido, visível a mais de 1 metro de distância (mínimo 24pt em negrito), utilizando cores semânticas imediatas (Verde Esmeralda para troco a devolver, Âmbar para valor em falta).
2. **Cédulas Nacionais Rápidas (Meticais):** O diálogo deve disponibilizar atalhos de 1 clique para as notas oficiais em circulação em Moçambique: **`Exacto`**, **`50 MT`**, **`100 MT`**, **`200 MT`**, **`500 MT`**, **`1000 MT`** e **`2000 MT`**.
3. **Selector Visual de Métodos:** Em vez de dropdowns ocultos, os métodos de pagamento (Numerário, Cartão, M-Pesa, e-Mola, Transferência) devem ser seleccionáveis directamente com botões ou chips temáticos identificados por ícones vetoriais.
4. **Badges de Tecla de Atalho (`KeyBadge`):** Teclas funcionais (`F2`, `F4`, `F6`, `F7`, `F9`, `Enter`, `Esc`, `Del`) devem ser renderizadas como "teclas físicas" em relevo no próprio botão, aumentando a intuitividade e a curva de aprendizagem do operador.
5. **Navegação 100% por Teclado:**
   - `Enter` no campo de valor entregue submete e valida o pagamento.
   - `Esc` descarta o modal e regressa ao carrinho sem alterar a venda.
   - Abertura modal centrada e sem bloqueio da Event Dispatch Thread (EDT).

---

## 3. Especificação dos Componentes

### 3.1 `PosPaymentDialog`
- **Largura / Altura Ideal:** 480px × 560px.
- **Display de Troco:** Painel estilizado com fundo escuro de contraste (`ModernPanel`), com texto "TROCO A DEVOLVER" em cinzento e montante monetário formatado (ex.: `350,00 MT`) em 24pt bold `UIHelper.APPROVED_GREEN`.
- **Falta de Valor:** Quando o valor entregue for inferior ao total, o visor exibe "FALTA RECEBER" em `UIHelper.PENDING_YELLOW`.
- **Campos Condicionais:** O campo de comprovativo/referência é activado automaticamente apenas para pagamentos electrónicos (M-Pesa, e-Mola, Cartão, Banco).

### 3.2 `KeyBadge`
- Componente de UI compacto (`JComponent`) que desenha uma tecla de teclado estilizada:
  - Fundo contrastante escuro com cantos arredondados (arc 6px).
  - Borda sutil de 1px com efeito de relevo/sombra.
  - Texto centrado em fonte monoespaçada ou negrito com padding horizontal calibrado (6px a 8px).

---

## 4. Regras de Negócio e Validações
1. Em pagamentos a dinheiro (**Numerário**), o valor entregue nunca pode ser inferior ao total da venda.
2. O troco é calculado como:
   $$\text{Troco} = \text{Valor Entregue} - \text{Total}$$
3. Em pagamentos electrónicos (**M-Pesa**, **e-Mola**, **Cartão**, **Transferência**), o valor entregue é considerado exactamente igual ao total da venda, sendo o troco sempre zero e o campo de referência disponibilizado para registo do ID de transacção.
