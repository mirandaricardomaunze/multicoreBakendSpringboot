# Especificação Canónica: Atalhos Rápidos de Teclado no POS (F1 a F12)

**Identificador:** `SPEC-POS-KEYBOARD-001`  
**Estado:** APROVADO  
**Módulos Envolvidos:** `desktop` (`mz.multicore.erp.gui.pos`, `mz.multicore.erp.gui`)  
**Data:** 2026-09-27  

---

## 1. Visão Geral e Contexto de Negócio

Em ambientes de retalho intenso e supermercados, a velocidade e a ergonomia de caixa são factores críticos de satisfação dos clientes e produtividade dos operadores. Um operador de ponto de venda profissional não deve depender do rato para operações comuns de balcão (leitura de artigos, pesquisa de produto, alteração de quantidades, concessão de descontos autorizados, recebimento/pagamento, emissão de devoluções e fecho de turno).

Esta especificação define o **Mapa Canónico de Teclas de Função (F1 a F12) e Atalhos de Operação do POS Multicore ERP**, juntamente com uma **Barra de Atalhos Rápida no Rodapé (Shortcut Bar / Ribbon)** clicável para operadores iniciantes ou touchscreens, e um **Modal Executivo de Ajuda (`F1`)**.

---

## 2. Mapa Canónico de Teclas de Atalho

| Tecla | Acção Canónica | Comportamento Operacional |
|---|---|---|
| **F1** | **Ajuda de Teclado** | Abre o diálogo com a matriz completa de teclas de atalho do POS. |
| **F2** | **Pesquisa de Artigo** | Foca o campo de pesquisa de artigos no catálogo e selecciona o texto existente. |
| **F3** | **Código de Barras** | Foca o campo de introdução/leitura óptica de código de barras. |
| **F4** | **Pesquisa de Cliente** | Foca o campo de cliente/NUIT para identificação da factura. |
| **F5** | **Desconto** | Abre diálogo ágil para aplicar desconto percentual ou monetário no artigo seleccionado. |
| **F6** | **Alterar Quantidade** | Abre modal de digitação rápida de quantidade para a linha do carrinho seleccionada. |
| **F7** | **Cartão de Fidelidade** | Abre o diálogo de consulta e leitura de cartão de fidelização de cliente. |
| **F8** | **Devoluções & Vales** | Abre o diálogo de devoluções no POS e emissão de vale de compras (`PosReturnDialog`). |
| **F9** | **Movimentos de Caixa** | Abre o modal de Sangria, Suprimento e Fundo de Maneio de Caixa. |
| **F10** | **Finalizar / Pagamento** | Abre o diálogo de pagamento com cálculo de troco e métodos (`PosPaymentDialog`). *(F9 mantido como alias retrocompatível)* |
| **F11** | **Alternar Vista** | Comuta entre o ecrã de Venda Activa e o Histórico de Vendas recentes. |
| **F12** | **Fecho Cego de Caixa** | Inicia o procedimento de fecho cego com contagem física de denominações (`PosBlindCloseDialog`). |
| **ESC** | **Cancelar / Limpar** | Solicita confirmação para limpar o carrinho ou foca o leitor de código de barras. |
| **DELETE** | **Remover Artigo** | Remove a linha seleccionada do carrinho de compras. |
| **+ / -** | **Ajuste de Quantidade** | Incrementa (+1) ou decrementa (-1) a quantidade da linha seleccionada no carrinho. |
| **CTRL + N** | **Nova Venda** | Limpa a venda actual para atendimento imediato do próximo cliente. |

---

## 3. Requisitos de Interface e Arquitectura

1. **Barra de Atalhos no Rodapé (`PosShortcutBar`):**
   - Inserida na zona inferior do `POSPanel` (`BorderLayout.SOUTH`).
   - Apresenta chips/badges compactos de cada função com a indicação da tecla (ex: `F1`, `F2`, etc.) e rótulo claro.
   - Totalmente interactiva: clicar na badge executa a acção correspondente, permitindo utilização fluida em monitores touchscreen.
   - Estilização coerente com o tema dark/light do Multicore ERP (`UIHelper`, `ModernPanel`, ícones FontAwesome vetoriais).
   - Proibição estrita de emojis Unicode crus (`▯`).

2. **Desacoplamento e Limite de Linhas (< 1000):**
   - O `POSPanel.java` conta com 997 linhas. Toda a lógica de vinculação de teclas e barra de atalhos deve residir em classes decompostas em `mz.multicore.erp.gui.pos`:
     - `PosKeyboardShortcutsHandler.java`
     - `PosShortcutBar.java`
     - `PosShortcutHelpDialog.java`
   - O `POSPanel.java` apenas instancia e delega a configuração, reduzindo o seu número de linhas total.

3. **Segurança de Foco e Conflitos:**
   - Teclas de função operam em nível `JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT`.
   - Atalhos como `DELETE`, `+` e `-` operam apenas quando a tabela do carrinho tem o foco ou quando um campo de texto não está a ser editado, prevenindo a eliminação de texto digitado em inputs.
