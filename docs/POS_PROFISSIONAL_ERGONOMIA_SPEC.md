# SPEC — Modernização Visual e Ergonomia Profissional do POS

**Criado em:** 2026-09-15  
**Camada:** cliente desktop Swing (`mz.multicore.erp.gui`, `mz.multicore.erp.gui.components`)  
**Backend:** contratos e regras de negócio de checkout permanecem inalterados.

---

## 1. Objectivo

Elevar a experiência visual e a ergonomia de operação do ecrã **POS — Caixa** ao patamar dos principais softwares de faturação do mercado (*Square, Odoo, Toast, Primavera BSS*), garantindo:
1. **Diferenciação visual ágil dos artigos** no catálogo através de iconografia e tons contextuais por categoria de produto, reduzindo a fadiga cognitiva do operador.
2. **Sinalização do estado do caixa** mediante um banner temático integrado e elegante (alerta suave de aviso para caixa fechado e badge de sucesso para turno aberto).
3. **Controlos de quantidade precisos e confortáveis**, sem sobreposições visuais (botões de passo `−` e `+` com símbolo único e atalho `F6`).
4. **Display financeiro de alto impacto**, com o **Total a Pagar** destacado em primeiro plano para leitura instantânea pelo operador e cliente.
5. **Linguagem comercial formal e consistente**, adotando a designação institucional *"Venda a Crédito (Conta Corrente)"*.

---

## 2. Padrões de Design e Interface

### 2.1 Banner de Sessão de Caixa
- Em vez de uma etiqueta solta (`JLabel`), o estado da sessão é exibido num cartão moderno (`ModernPanel`) com preenchimento interno, cantos arredondados (`RADIUS_MD`), ícone vectorial e texto com peso visual adequado.
- **Caixa Aberto:**
  - Fundo suave de sucesso (`UIHelper.KPI_SUCCESS_SOFT` no tema claro / tom verde escuro integrado no tema escuro).
  - Ícone `fas-lock-open` em `UIHelper.APPROVED_GREEN`.
  - Informação visível: Operador de caixa, hora de abertura e valor do fundo de caixa inicial.
- **Caixa Fechado:**
  - Fundo suave de aviso (`UIHelper.KPI_WARNING_SOFT` / tom amber integrado).
  - Ícone `fas-lock` em `UIHelper.PENDING_YELLOW`.
  - Mensagem clara instruindo o início de turno para habilitar o checkout.

### 2.2 Cartões de Artigo no Catálogo (`PosCatalogController`)
- Para produtos sem fotografia cadastrada, o cartão abandona o ícone genérico monótono e passa a atribuir iconografia vectorial e cores harmónicas derivadas da categoria:
  - **Alimentação / Mercearia:** `fas-shopping-basket`, cor `UIHelper.PENDING_YELLOW`.
  - **Bebidas:** `fas-glass-martini-alt`, cor `UIHelper.ACCENT_BLUE`.
  - **Limpeza / Higiene:** `fas-pump-soap`, cor `UIHelper.APPROVED_GREEN`.
  - **Padaria / Pastelaria:** `fas-bread-slice`, cor `UIHelper.PENDING_YELLOW`.
  - **Outros / Gerais:** `fas-box`, cor `UIHelper.ACCENT_BLUE`.
- **Tipografia do Preço:**
  - O valor unitário é exibido em `14px Font.BOLD` em `UIHelper.ACCENT_BLUE`, com espaçamento equilibrado e indicação clara de moeda (`MT`).
  - Artigos esgotados continuam claramente identificados com a etiqueta `ESGOTADO` em `UIHelper.REJECTED_RED`.

### 2.3 Botões de Quantidade do Carrinho
- O botão de decremento apresenta estritamente o símbolo `−` (sem duplicar texto com ícone), e o botão de incremento apresenta o símbolo `+`.
- Ambos possuem dimensões confortáveis (`36 × 32 px` ou similar) para clique rápido ou utilização em telas tácteis.
- O botão central mantém a ação principal `"Quantidade (F6)"` com o ícone `fas-sort-numeric-up`.

### 2.4 Display de Totais do Carrinho
- O bloco de totais no rodapé do carrinho apresenta uma separação nítida:
  - À esquerda: discriminação contábil de **Subtotal s/ IVA** e **IVA** em grelha alinhada.
  - À direita: bloco destacado de **TOTAL A PAGAR** com tipografia ampliada (`24px Font.BOLD`), assegurando excelente visibilidade.
- O botão **Finalizar Venda (F9)** atua como o *Call to Action* (CTA) dominante no rodapé, acompanhado da opção de venda a crédito com tooltip informativo.

---

## 3. Conformidade Arquitetural

1. `POSPanel.java` deve manter-se estritamente abaixo do teto de **1000 linhas de código** (`UiPanelDecompositionTest`).
2. Nenhum literal `new Color(...)` fora das constantes canónicas de `UIHelper` (`FinalUiUniformityHarnessTest`).
3. Todos os atalhos de teclado operacionais (`F2`, `F3`, `F4`, `F6`, `F9`) permanecem garantidos.
