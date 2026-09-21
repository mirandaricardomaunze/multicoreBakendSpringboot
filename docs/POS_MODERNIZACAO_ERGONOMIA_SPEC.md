# SPEC — Modernização e Otimização Ergonómica da UI do POS

**Criado em:** 2026-09-13  
**Camada:** cliente desktop Swing (`mz.multicore.erp.gui`, `mz.multicore.erp.gui.components`)  
**Backend:** contratos e regras de negócio de venda permanecem inalterados.

---

## 1. Objectivo

Maximizar a produtividade e ergonomia no balcão de atendimento (POS — Caixa), eliminando elementos intrusivos no fluxo de checkout ativo, aumentando a densidade visual do catálogo de produtos e proporcionando uma hierarquia visual de alto impacto para valores totais e atalhos de teclado.

---

## 2. Princípios de Desenho e Ergonomia

1. **Maximização da Altura Operacional no Checkout**:
   - A área de atendimento direto ("Venda POS") não deve conter cartões de resumo histórico/diário redundantes.
   - A remoção dos 3 cartões grandes recupera ~95px de altura útil, permitindo que a tabela do carrinho acomode mais linhas sem rolagem e que o catálogo de produtos exiba mais artigos em simultâneo.

2. **Densidade e Visibilidade do Catálogo de Produtos (`PosCatalogController`)**:
   - Dimensões calibradas para cartões de produto:
     - Imagem: `80 × 42 px` (quando presente) ou marcador compacto (`20 px`).
     - Padding interno: `5 px`, intervalo entre elementos: `2 px`.
   - Permite acomodar **6 a 8 produtos** por ecrã (grelha 2x3 ou 2x4) sem esforço visual.
   - Destaque claro de preço (`unitPrice`) em cor de contraste (`ACCENT_BLUE`) e etiqueta visual vermelha quando esgotado.

3. **Cabeçalho Operacional e Estado de Caixa**:
   - A barra superior une o seletor de abas (*Venda POS* / *Histórico de Vendas*) com as ações de caixa (*Abrir*, *Sangria/Suprimento*, *Fechar*) e a indicação compacta do estado do caixa.
   - A barra de seleção de cliente, armazém, conta e código de barras mantém proporções ergonómicas (`PosLayout.HEADER_FIELD_WEIGHTS`).

4. **Hierarquia Visual no Carrinho e Finalização**:
   - O valor **TOTAL A PAGAR** recebe destaque com tipografia ampliada (`22-24 px`, negrito) e fundo contrastante.
   - Os atalhos rápidos de operação permanecem garantidos:
     - `F2`: Foco na pesquisa de produto.
     - `F4`: Foco na pesquisa de cliente.
     - `F6`: Edição da quantidade do artigo selecionado.
     - `F9`: Finalizar venda (checkout).
     - `Delete`: Remover linha selecionada do carrinho.

5. **Rigor Arquitetural e Limite de Decomposição**:
   - `POSPanel.java` deve manter-se estritamente abaixo do teto de 1000 linhas de código, respeitando `UiPanelDecompositionTest`.
   - Zero `new Color(...)` fora das classes permitidas, em total conformidade com `FinalUiUniformityHarnessTest`.
