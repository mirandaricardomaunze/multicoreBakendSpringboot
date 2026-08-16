# SPEC — Fecho profissional da UI de Stock e Comercial

**Criado em:** 2026-08-15  
**Camada:** cliente desktop Swing  
**Âmbito:** Stock, Comercial e atalhos canónicos de formulários/documentos.

## 1. Objectivo

Fechar a profissionalização visual restante nos dois módulos operacionais mais densos, preservando
todos os fluxos, permissões e contratos. A interface deve privilegiar acções frequentes, agrupar
operações auxiliares e manter decisões críticas explícitas.

## 2. Barras de acções

- Faturas: Liquidar e Anular permanecem visíveis; impressão, guia, exportação e actualização em
  “Mais acções”.
- Encomendas: Faturar, Converter em Guia e Cancelar permanecem visíveis; detalhes, impressão,
  exportação e actualização em “Mais acções”.
- Notas e guias: Aprovar/Rejeitar/Cancelar permanecem visíveis; imprimir/actualizar são auxiliares.
- Stock global: Cadastrar Produto, Inventário Físico e Trancar Stock permanecem visíveis; editar,
  criar armazém e etiquetas ficam em “Mais acções”.
- Categorias e armazéns: Novo e Editar visíveis; activar/desactivar e actualizar agrupados.
- Nenhum menu excede cinco entradas e não existem menus aninhados.

## 3. Formulários e controlos

- Selects de filtros usam `UIHelper.FORM_CONTROL_HEIGHT`; não usar altura local 35.
- Campos tipados existentes (`MoneyField`, `QuantityField`, `DateField`) continuam canónicos.
- Documentos multi-linha permanecem no `DocumentEditorHost`, com scroll vertical e barra fixa.
- Modais permanecem limitados a 94% da área principal e com scroll interno.

## 4. Tabelas

- Stock, faturas, encomendas, notas, guias e contas mantêm renderers canónicos para dinheiro,
  quantidade e estado.
- Acções agrupadas actuam sobre a mesma selecção da tabela e invocam os mesmos métodos anteriores.
- Não alterar paginação, filtros de API ou contratos nesta fase.

## 5. Teclado e acessibilidade

- `DocumentEditorHost`: `Ctrl+S` guarda e `Esc` solicita voltar, respeitando alterações por gravar.
- `ModernFormDialog`: `Ctrl+S` confirma e `Esc` fecha; inspector read-only não grava.
- Menus e itens icon-only mantêm tooltip, nome acessível, foco e operação por teclado.
- Atalhos POS existentes permanecem inalterados.

## 6. Fora de âmbito

- Alterar regras de stock, facturação, aprovação, pagamentos ou documentos fiscais.
- Alterar a ordem dos estados ou permissões.
- Reescrever Swing ou introduzir dependências visuais novas.

## 7. Definition of done

- Harness automático SCUI-01..12 verde.
- `mvn clean compile` e suite completa verdes.
- Nenhuma altura 35 nos filtros cobertos.
- Acções críticas auditadas como visíveis.
- SCUI-20..24 executados visualmente quando a aplicação e os dados estiverem disponíveis.

