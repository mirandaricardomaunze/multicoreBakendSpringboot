# Harness — Filtros de tabela

> Cenários para [TABELAS_FILTROS_SPEC.md](TABELAS_FILTROS_SPEC.md).
> FT-01..07 automáticos (`TableFilterTest`); FT-50..55 manuais.

## Automático (`TableFilterTest` — lógica pura `rowMatches`)

| ID    | Cenário | Esperado |
|-------|---------|----------|
| FT-01 | Pesquisa por texto presente/ausente numa célula (case-insensitive). | Passa quando alguma célula contém; falha caso contrário. |
| FT-02 | Pesquisa vazia ou null. | Passa (sem filtro de texto). |
| FT-03 | Filtro de coluna exacto, ignorando maiúsculas. | Passa só quando a célula da coluna é igual ao valor. |
| FT-04 | Pesquisa e filtro de coluna combinados (AND). | Passa só quando ambos batem. |
| FT-05 | Valor de coluna vazio. | Não filtra por essa coluna. |
| FT-06 | `parseCellDate` com `dd/MM/yyyy`, com hora, e texto inválido/null. | Data correcta; null quando não parseia. |
| FT-07 | `matchesPeriod` para Hoje / Últimos 7 / Este mês; sem data com filtro activo. | Inclui/exclui conforme o período; sem data ⇒ excluída. |

## Manuais

| ID    | Passos | Esperado |
|-------|--------|----------|
| FT-50 | Numa tabela com filtro (ex.: Plataforma → Assinaturas), escrever na pesquisa. | A lista reduz às linhas que contêm o texto. |
| FT-51 | Escolher um valor no dropdown (ex.: Estado = Suspensa). | Só as linhas desse estado; combinar com a pesquisa faz AND. |
| FT-52 | Com um filtro activo, **seleccionar** uma linha e usar uma acção (Editar / Definir / Abrir). | Age sobre a linha **correcta** (índice convertido vista→modelo). |
| FT-53 | Filtrar Assinaturas/Validades onde há realce de cor. | A cor acompanha a linha certa após filtrar. |
| FT-54 | Campos de pesquisa (todas as tabelas). | Têm ícone de lupa embutido + botão limpar; a barra tem ícone de funil. |
| FT-55 | Filtrar por **Data** (ex.: Tesouraria → Fluxo → Últimos 7 dias) e por **Categoria** (Stock → Níveis). | Lista reduz ao período / à categoria escolhida. |
| FT-56 | Vendas → Notas de Crédito / Notas de Débito: pesquisar por nº/fatura/cliente e filtrar por **Motivo**, **Estado** e **Data**. | A lista reduz; combinar filtros faz AND. |
| FT-57 | Vendas → NC/ND/Recibos/Encomendas/Contas Correntes: com filtro activo, **seleccionar** uma linha e usar a acção (Aprovar/Rejeitar/Imprimir · Anular · Faturar/Detalhes · Receber Pagamento). | Age sobre a linha **correcta** (índice convertido vista→modelo). |
| FT-58 | Vendas → Recibos (Método) e Contas Correntes (Estado + Data). | Só as linhas do método/estado/período escolhido. |
| FT-59 | Compras → todas as tabelas de listagem (Faturas de Compra, Fornecedores, Reposição, Contas a Pagar, Encomendas a Fornecedor): pesquisar e filtrar por Estado/Data conforme a tabela. | A lista reduz; combinar faz AND. |
| FT-60 | Compras → Fornecedores/Encomendas (antes com pesquisa server-side): escrever na pesquisa. | Filtra sem recarregar do servidor; o botão Atualizar limpa a pesquisa. |
| FT-61 | Compras → com filtro activo, seleccionar e agir (Editar/Activar fornecedor · Registar pagamento · Receber/Cancelar encomenda). | Age sobre a linha **correcta** (índice convertido vista→modelo). |
| FT-62 | Clientes: pesquisar por nome/NUIT/email; ordenar por coluna; editar/eliminar com filtro activo. | Filtra e ordena; a acção age sobre a linha correcta. |
| FT-63 | Stock → Gestão de Armazéns: filtrar por Tipo e Estado; editar/activar. | Reduz a lista; a acção age no armazém certo. |
| FT-64 | RH → cada tab (Colaboradores/Recibos/Faltas/Férias/Despesas): pesquisar e filtrar por Estado/Tipo/Data. | Reduz a lista; combinar faz AND; acções agem na linha certa. |
| FT-65 | Fiscal → Taxas (Estado) e Retenções (Estado + Data). | Só as linhas do estado/período; selecção correcta. |
| FT-66 | Config → Auditoria (Data), Utilizadores (Perfil+Estado), Suporte (pesquisa). | Reduz a lista; alterar perfil/estado age no utilizador certo. |
| FT-67 | POS → Histórico de Vendas: filtrar por Estado e Data; reimprimir/devolver com filtro activo. | Reimprime/devolve a venda **correcta** (índice convertido). |
| FT-68 | Promoções: filtrar por Tipo e Estado; activar/desactivar com filtro activo. | Age sobre a promoção correcta. |

## Verificação

- `mvn -o test -Dtest=TableFilterTest` → verde (FT-01..05).
