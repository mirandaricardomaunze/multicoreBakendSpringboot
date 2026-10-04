# SPEC-GOB-001: Barra de Pesquisa Global Rápida — Omnibar (`Ctrl+K`)

## 1. Princípio de Ergonomia e Acessibilidade

No ERP moderno, a navegação não deve depender exclusivamente de cliques em árvores ou menus laterais. A barra **Omnibar (`Ctrl+K`)** proporciona acesso instantâneo a qualquer módulo, documento ou acção rápida a partir de qualquer ponto do sistema:

- **Atalho Universal:** `Ctrl+K` (ou `Cmd+K` em macOS) em qualquer ecrã abre o Spotlight/Omnibar.
- **Teclado:**
  - `Seta Cima / Seta Baixo`: navegação entre resultados.
  - `ENTER`: executa a acção ou salta para o módulo pretendido.
  - `ESC`: fecha o diálogo sem executar acção.
- **Pesquisa Instantânea:** filtro em tempo real à medida que o utilizador digita.

## 2. Categorias de Resultados

1. **Navegação de Ecrãs:**
   - Acesso directo a todos os módulos do ERP: POS, Comercial/Vendas, Compras, Stock/Armazéns, Financeiro, Fiscal, Recursos Humanos, CRM e Configurações.
2. **Atalhos Operacionais Rápidos:**
   - Emissão de nova fatura, nova encomenda, nova cotação, nova transferência de stock, consulta de saldo de caixa.
3. **Estilo Visual:**
   - Fundo compatível com tema claro/escuro (`FlatLaf`).
   - Ícones semânticos vetoriais FontAwesome (proibido emojis Unicode).
   - Destaque nítido do item selecionado (`UIHelper.SELECTION_BG`).

## 3. Harness e Verificação

- `GlobalOmnibarHarnessTest` verifica:
  1. Instalação do atalho `Ctrl+K` no `MainFrame`.
  2. Filtragem correcta de termos por texto parcial (case-insensitive).
  3. Execução segura da acção associada ao pressionar `Enter`.
