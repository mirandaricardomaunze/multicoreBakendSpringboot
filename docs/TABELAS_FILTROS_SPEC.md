# Filtros de tabela (pesquisa + tipo/estado)

**Última actualização:** 2026-07-07
**Estado:** feito.

## Objectivo

As tabelas de listagem devem ter **filtros**: uma **pesquisa livre** (em todas as colunas) e, conforme
a tabela, **dropdowns por tipo/estado**, para encontrar depressa o registo certo.

## Componente reutilizável — `mz.multicore.erp.gui.components.TableFilter`

- **`install(table, search, ColumnFilter...)`** e **`install(table, search, List<ColumnFilter>,
  List<PeriodFilter>)`** — instalam um `TableRowSorter` e ligam pesquisa + dropdowns de tipo/estado +
  dropdowns de **período (data)** a um `RowFilter` combinado (AND).
- **`rowMatches(cells, search, exactByColumn)`** — **lógica pura e testável**: a linha passa se o texto
  aparece em alguma célula (case-insensitive) **e** cada coluna filtrada bate exactamente o valor.
- **`matchesPeriod(date, opção, hoje)`** + **`parseCellDate(cell)`** — **lógica pura** do filtro por
  data (Hoje / Últimos 7 / Últimos 30 / Este mês); parseia `dd/MM/yyyy` (com ou sem hora).
- **`selectedModelRow(table)`** — converte o índice da selecção (vista→modelo). **Obrigatório** onde o
  código indexa a lista pela linha seleccionada (o sorter faz a vista divergir do modelo).
- **Aspecto profissional (FlatLaf):** `searchField` tem **ícone de lupa embutido** + **botão limpar**;
  `bar(...)` prefixa um **ícone de funil**; `label(text, iconCode)` põe ícone na etiqueta (ex.:
  calendário no período). Removido o emoji `🔍` dos campos antigos (Stock, Clientes).
- Fábricas: `searchField`, `combo`, `periodCombo`, `label(text[,icon])`, `bar`.

## Tabelas com filtro

| Painel | Tabela | Pesquisa | Dropdown(s) |
|--------|--------|----------|-------------|
| Plataforma | Empresas | nome/NUIT/email | Estado (ACTIVA/SUSPENSA) |
| Plataforma | Assinaturas | empresa | Estado · Plano |
| Plataforma | Utilizadores | utilizador/nome/empresa | Estado (ACTIVO/INATIVO/SUPERADMIN) |
| Plataforma | Assistência | empresa/assunto | Estado · Prioridade |
| Stock → Alertas | Esgotados | SKU/nome | — |
| Stock → Alertas | Validade | SKU/nome/lote | Estado (Expirado/A expirar) |
| CRM | Pedidos de Assistência | cliente/assunto/descrição | Estado · **Data (período)** |
| CRM | Folhas de Obra | cliente/técnico | Faturado (SIM/NÃO) |
| Tesouraria | Contas | conta/IBAN | — |
| Tesouraria | Fluxo de Caixa | conta/descrição | Tipo · **Data (período)** |
| Aprovações | Pendentes | documento/submissor | Perfil (ADMIN/MANAGER) |
| Aprovações | Histórico | documento/submissor | Estado final · **Data (período)** |
| Stock → Níveis de Stock | (filtro próprio) | SKU/nome | Armazém · Estado · **Categoria** |
| Stock → Movimentos | ✓ | artigo/lote/série/descrição | Tipo · **Data (período)** |
| Stock → Transferências | ✓ | guia/origem/destino/responsável | Estado · **Data (período)** |
| Vendas → Faturação | ✓ | nº fatura/cliente | Estado |
| Vendas → Recibos (RC) | ✓ | nº recibo/fatura/cliente | Método (CASH/BANK_TRANSFER/CARD) · Estado · **Data (período)** |
| Vendas → Encomendas (EC) | ✓ | nº encomenda/cliente | Estado (PENDING/BILLED/CANCELLED) |
| Vendas → Notas de Crédito (NC) | ✓ | nº/fatura/cliente | Motivo (RETURN/DISCOUNT/ERROR/CANCELLATION) · Estado · **Data (período)** |
| Vendas → Notas de Débito (ND) | ✓ | nº/fatura/cliente | Motivo (FREIGHT/SURCHARGE/CORRECTION/OTHER) · Estado · **Data (período)** |
| Vendas → Contas Correntes | ✓ | nº fatura/cliente/NUIT | Estado (APPROVED/PARTIALLY_PAID) · **Data (período)** |
| Vendas → Movimentos | ✓ | nº doc/cliente | **Data (período)** |
| Compras → Faturas de Compra | ✓ | nº doc/fornecedor/armazém | **Data (período)** |
| Compras → Fornecedores | ✓ | nome/NUIT | Estado (Activo/Inactivo) |
| Compras → Reposição | ✓ | produto/SKU | Estado (ESGOTADO/BAIXO) |
| Compras → Contas a Pagar | ✓ | nº compra/fornecedor | **Data (período)** |
| Compras → Encomendas a Fornecedor | ✓ | nº/fornecedor | Estado (ORDERED/PARTIALLY_RECEIVED/RECEIVED/CANCELLED) · **Data (período)** |
| Clientes | ✓ | nome/NUIT/email/endereço | — |
| Stock → Gestão de Armazéns | ✓ | nome/nº/localização/responsável | Tipo (Loja/Depósito/Central/Trânsito) · Estado |
| RH → Colaboradores | ✓ | nome/email/departamento/cargo | Estado (ACTIVE/SUSPENDED/TERMINATED) |
| RH → Recibos de Salário | ✓ | nº recibo/colaborador/período | Estado (DRAFT/PAID/CANCELLED) · **Data pag. (período)** |
| RH → Faltas | ✓ | colaborador/motivo | Tipo (JUSTIFIED/UNJUSTIFIED/SICK/…) · **Início (período)** |
| RH → Férias | ✓ | colaborador/decisor | Estado (PENDING/APPROVED/REJECTED/CANCELLED) · **Início (período)** |
| RH → Notas de Despesas | ✓ | colaborador/categoria/motivo | Estado (PENDING_APPROVAL/APPROVED/REJECTED) |
| Fiscal → Taxas Fiscais | ✓ | código/designação/tipo/base legal | Estado (ATIVA/INATIVA) |
| Fiscal → Retenções na Fonte | ✓ | beneficiário/NUIT/descrição/categoria | Estado (PENDING/DELIVERED/CANCELLED) · **Data (período)** |
| Config → Log de Auditoria | ✓ | utilizador/ação/detalhe | **Data (período)** |
| Config → Utilizadores | ✓ | username/nome | Perfil (ADMIN/MANAGER/EMPLOYEE) · Estado (ATIVO/INATIVO) |
| Config → Suporte à Plataforma | ✓ | assunto/prioridade/estado | — |
| POS → Histórico de Vendas | ✓ | nº venda/operador/cliente | Estado (PAID/APPROVED/PARTIALLY_PAID/CANCELLED) · **Data (período)** |
| Promoções | ✓ | nome/alcance/benefício | Tipo (Percentagem/Leve X, pague Y) · Estado (ACTIVA/INACTIVA) |

**Nota de aparência:** como o projecto usa o Look&Feel por omissão (sem FlatLaf), o campo de pesquisa é
o componente `SearchField`, que **desenha** a lupa e o texto-dica (os client-properties do FlatLaf não
renderizam). Os campos de pesquisa antigos (Stock, Clientes) foram migrados para `SearchField`.

## Notas

- Os dropdowns filtram pelo **texto exacto** da célula (o mesmo que é apresentado), por isso as opções
  correspondem às etiquetas mostradas.
- Onde havia realce por linha (renderer) — assinaturas e validades — o renderer passou a **converter o
  índice** vista→modelo, para colorir a linha certa quando filtrada/ordenada.
- **Vendas (ComercialPanel):** as acções que indexam a linha seleccionada — Recibos (anular),
  Encomendas (faturar/detalhes/imprimir), Notas de Crédito, Notas de Débito e Contas Correntes
  (receber pagamento) — passaram todas por `TableFilter.selectedModelRow(...)`, para agir sobre a
  linha **correcta** com filtro/ordenação activos (o sorter faz a vista divergir do modelo/lista DTO).
- **Compras (ComprasPanel):** Fornecedores e Encomendas a Fornecedor tinham pesquisa *server-side*
  (recarregava a lista a cada tecla); migraram para o `TableFilter` **cliente** — `load*` passou a
  carregar tudo e o filtro (pesquisa + estado + data) aplica-se sobre a vista. As acções que indexam a
  selecção — Fornecedores (editar/activar), Contas a Pagar (pagar) e Encomendas (receber/cancelar) —
  passaram por `TableFilter.selectedModelRow(...)`.
- **Clientes (ClientesPanel):** tinha pesquisa própria (`refilter()` reconstruía o modelo); migrou para
  o `TableFilter` — carrega todos, filtra sobre a vista, colunas ordenáveis; `selectedClient` converte
  o índice.
- **RH / Fiscal / Config / POS / Promoções:** todas as tabelas de listagem ganharam barra de filtros e
  as acções que indexam a selecção passaram por `TableFilter.selectedModelRow(...)` (ou já usavam
  `convertRowIndexToModel`, caso de Colaboradores e Gestão de Armazéns). Tabelas **agregadas** (Apuramento
  IVA, IRPS&INSS, Declarações) e o **carrinho** do POS não levam filtro — não são listagens pesquisáveis.
- **Suporte (Config):** prioridade/estado usam etiquetas localizadas, por isso só tem **pesquisa livre**
  (sem dropdown, para não fixar valores traduzidos).
