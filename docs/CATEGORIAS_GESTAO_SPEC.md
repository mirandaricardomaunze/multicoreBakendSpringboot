# Spec — Gestão de categorias de produto

> Ecrã profissional para gerir as categorias da loja (criar, editar, activar/desactivar), com cor
> visível, contagem de produtos e pesquisa. Vive na tab "Categorias" do Stock.

**Última actualização:** 2026-06-28

## Problema

A gestão de categorias existia mas era básica: tabela só com texto (código/nome/hex/estado), cor em
hexadecimal cru (difícil de ler/escolher) e sem noção de quantos produtos usam cada categoria nem
forma de procurar.

## Decisão

- **Backend reutilizado:** `ProductCategoryService` (create/update/setActive/getAll/getActive) já cobre
  o domínio, com guarda multi-empresa e `BusinessRuleException`. Não foi alterado.
- **Tabela profissional** (tab "Categorias" do `StockPanel`): colunas **Código · Nome · Cor · Produtos
  · Estado**.
  - **Cor visível:** renderizador próprio (`ColorCellRenderer`) pinta uma **amostra** da cor + o hex.
  - **Produtos:** contagem de produtos por categoria (a partir de `ComercialService.getAllProducts()`),
    para o gestor ver o que está em uso antes de desactivar.
  - **Pesquisa** por código/nome (filtra a lista em memória).
- **Diálogo premium** (`ModernFormDialog`, ícone `fas-tags`, subtítulo): **seletor de cor** via
  `JColorChooser` com **amostra** ao vivo + botões "Escolher…" e "Limpar" (em vez do hex à mão). Código
  e nome obrigatórios (erro mantém o modal aberto).
- **Activar/Desactivar** em vez de apagar — preserva integridade (produtos podem referenciar a
  categoria); soft-state já suportado pelo service.

## Não-objetivos

- Não apagar categorias fisicamente (usa-se activar/desactivar).
- Não reorganizar produtos em massa nem hierarquia de subcategorias.
- Não alterar o domínio do `ProductCategoryService`.

## Notas técnicas

- A contagem de produtos é recalculada em `loadCategories()` (chamado em `onPanelSelected`).
- A cor é guardada como hex `#RRGGBB`; amostras inválidas/ausentes mostram contorno cinza / "—".
