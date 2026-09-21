# Especificação Técnica — Pacote de Produtividade, Gestão e Rigor Operacional

Este documento define a especificação técnica para as 5 extensões de funcionalidade do Multicore ERP:
1. **Filtro Dinâmico de Período e Ranking Top 5 no Dashboard**
2. **Fecho Cego e Rigor de Caixa no POS**
3. **Gerador de Etiquetas de Prateleira e Código de Barras no Stock**
4. **Feed de Atividade Recente (Activity Stream)**
5. **Exportação Universal de Tabelas para CSV/Excel**

---

## 1. Dashboard: Filtro Dinâmico de Período e Ranking Top 5

### 1.1 Seletor de Período
- Chips de período: `HOJE` (dia corrente), `ESTA_SEMANA` (últimos 7 dias), `ESTE_MES` (mês corrente), `ESTE_ANO` (ano corrente), `TODOS` (histórico total).
- Ao alternar o chip, o `DashboardPanel` recalcula ou refiltra as faturas, vendas POS, compras e KPIs de forma assíncrona fora do EDT, atualizando os cartões KPI e os gráficos de barra e pizza (`SimpleBarChart`, `SimplePieChart`).

### 1.2 Widget "Top 5 Produtos Mais Vendidos"
- Agrega as linhas de faturas aprovadas/pagas e vendas POS para calcular os 5 produtos com maior faturação e volume de vendas.
- Renderização visual: Barras horizontais proporcionais com gradiente semântico, código/nome do artigo, percentagem do total e valor em Meticais (`MT`).

---

## 2. POS: Fecho Cego de Caixa e Rigor Operacional

### 2.1 Fluxo de Fecho Cego (`Blind Close`)
1. O operador clica em "Fechar Caixa".
2. Um diálogo modal solicita que o operador conte e introduza o valor físico presente na gaveta (`Saldo Físico Contado`), **sem exibir o saldo esperado pelo sistema**.
3. Opcionalmente, permite selecionar a conta de tesouraria para depósito do numerário.
4. O backend calcula: `Diferença = Saldo Contado − Saldo Esperado`.
5. Um resumo visual exibe:
   - Saldo Esperado
   - Saldo Contado
   - Diferença (Verde para zero/sobra positiva, Vermelho para quebra/diferença negativa)
6. Disponibiliza a geração e impressão do Fecho Z (`POSZReportPrintService` / `PrintPreviewDialog`).

---

## 3. Stock: Gerador de Etiquetas de Prateleira (`ShelfLabelsDialog`)

### 3.1 Geometria e Formato
- Formato de etiqueta de prateleira padrão de retalho:
  - Cabeçalho: Nome da Empresa / Logótipo
  - Centro: Nome do Artigo em destaque (negrito) e Categoria / SKU
  - Código de Barras: Representação gráfica do código de barras (vetorial Java 2D ou representação Code-128 legível)
  - Rodapé: Preço de Venda em destaque (`1.250,00 MT`) com indicação de IVA e data de impressão.
- Modos de Impressão:
  - **Etiqueta Individual / Bobina Térmica (80mm)**
  - **Grelha de Etiquetas (Folha A4, 3 colunas x 8 linhas = 24 etiquetas por folha)**

---

## 4. Feed de Atividade Recente (`RecentActivityWidget`)

### 4.1 Modelo de Dados (`ActivityEntry`)
- `id`: Identificador da transação
- `title`: Título da ação (ex.: "Fatura FT-2026/18 emitida", "Venda POS #64 finalizada", "Recepção de Compra PO-102")
- `detail`: Detalhe monetário ou de cliente/fornecedor (ex.: "Valor: 4.500,00 MT · Cliente: Supermercado Central")
- `timestamp`: Data e hora da operação (`Instant` / `LocalDateTime`)
- `iconCode`: Ícone FontIcon correspondente (`fas-file-invoice-dollar`, `fas-cash-register`, `fas-shopping-cart`, `fas-boxes`)
- `accentColor`: Cor semântica da operação

---

## 5. Exportação Universal para CSV / Excel (`TableCsvExporter`)

### 5.1 Requisitos de Formato
- Codificação: `UTF-8` com BOM opcional para abertura direta no Microsoft Excel sem corrupção de caracteres acentuados.
- Delimitador: Ponto e vírgula (`;`) ou vírgula (`,`) com escape correto de aspas duplas (`""`).
- Suporte a tabelas filtradas: Exporta exatamente as linhas visíveis após aplicação de filtros de pesquisa e paginação.
- Integração: Acessível via clique com o botão direito (`TableContextMenu` -> "Exportar para CSV") e atalho de teclado `Ctrl+E`.
