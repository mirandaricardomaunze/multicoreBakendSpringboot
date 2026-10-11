# Especificação Canónica: Módulo de Gestão de Negócios e Filtro de Linhas de Distribuição

**Ficheiro:** `docs/GESTAO_DE_NEGOCIOS_SPEC.md`  
**Data:** 11 de Outubro de 2026  
**Domínio:** Compras / Comercial / Catálogo (`mz.multicore.erp.modules.purchases` & `mz.multicore.erp.modules.comercial`)  
**Arquitectura:** Reactor Maven (`contracts`, `backend`, `desktop`)

---

## 1. Contexto e Motivação Operacional

Empresas de distribuição grossista e representação comercial em Moçambique gerenciam múltiplos portfólios de fabricantes multinacionais e linhas estratégicas de produtos (ex.: **Unilever**, **BAT - British American Tobacco**, **Food & Commodities**, etc.).

Estes parceiros funcionam como fornecedores principais e exigem:
1. **Catalogação dedicada:** Cada artigo pode pertencer a uma **Unidade de Negócio / Linha de Distribuição**.
2. **Filtragem transversal no ERP:** Capacidade de filtrar produtos, stocks, compras e encomendas com base na unidade de negócio selecionada.
3. **Faturação flexível com restrição opcional:**
   - Por omissão (sem filtro de negócio selecionado no cabeçalho), a fatura aceita **produtos mistos** de diferentes negócios.
   - Quando o operador seleciona um **Negócio específico** (ex.: emissão exclusiva Unilever), o sistema **bloqueia estritamente** a inserção de produtos pertencentes a outros negócios, garantindo a integridade documental e acordos de canal.

---

## 2. Arquitetura e Modelo de Dados

### 2.1. Entidade `BusinessUnit` (Tabela `business_units`)
- `id`: BIGINT (PK)
- `company_id`: BIGINT (FK empresas, multi-tenant)
- `code`: VARCHAR(30) (Código corporativo único por empresa, ex: `UNL`, `BAT`, `FOOD`)
- `name`: VARCHAR(120) (Nome comercial, ex: `Unilever Moçambique`, `BAT`, `Food & Commodities`)
- `supplier_id`: BIGINT (FK opcional para `suppliers`, vinculando o fornecedor fabricante dono da marca)
- `description`: VARCHAR(500)
- `active`: BOOLEAN NOT NULL DEFAULT TRUE
- `created_at`, `updated_at`, `version`

### 2.2. Associação no Artigo (`products`)
- `business_unit_id`: BIGINT (FK opcional para `business_units`)

### 2.3. Contratos DTOs (`contracts`)
- `BusinessUnitDTO(Long id, String code, String name, Long supplierId, String supplierName, String description, boolean active, long productCount)`
- `CreateBusinessUnitRequest(String code, String name, Long supplierId, String description)`
- `UpdateBusinessUnitRequest(String code, String name, Long supplierId, String description, Boolean active)`
- Retrocompatibilidade estrita mantida em `ProductDTO`, `CreateProductRequest` e `UpdateProductRequest`.

---

## 3. Regras de Negócio Invioláveis (R1 a R5)

### R1 — Unicidade de Código por Empresa
Não podem existir duas unidades de negócio ativas com o mesmo código dentro do mesmo tenant.

### R2 — Associação Opcional e Flexível com Fornecedor Master
Cada unidade de negócio pode estar associada ao seu fornecedor fabricante principal (ex.: Fornecedor "Unilever Moçambique Lda"), simplificando a criação de ordens de compra e relatórios de fornecimento.

### R3 — Regra de Ouro da Faturação Mista vs. Exclusiva
- **Modo Misto (Padrão):** Quando a fatura/encomenda não define `businessUnitId` (nulo), o cliente pode comprar artigos de quaisquer negócios na mesma fatura.
- **Modo Exclusivo (Restrito):** Quando a fatura/encomenda define `businessUnitId`, **todos** os artigos faturados devem pertencer rigorosamente a essa unidade de negócio. Se um artigo de outro negócio for incluído, o sistema lança `BusinessRuleException`:
  > *"A fatura está restrita à linha de negócio '{nome}' e não aceita o produto '{produto}' pertencente a outro negócio."*

### R4 — Localização no Desktop: Menu Compras
A interface de **Gestão de Negócios** fica integrada como aba corporativa em **Compras** (`ComprasPanel`), mantendo o design corporativo com KPIs de topo (`KpiCard`), tabela `ModernPanel(16)` com paginação e formulário modal `ModernFormDialog` com validação no `setOnSave`.

### R5 — Filtros e Colunas Transversais nos Painéis do ERP
Todas as tabelas do sistema onde circulam artigos, compras, fornecedores, propostas e movimentações incluem coluna dedicada "Negócio" e seletor `TableFilter.combo("Todos os negócios")`:
1. **Catálogo de Artigos / Produtos** (`ProdutosPanel`): Coluna e filtro por linha de negócio.
2. **Níveis de Stock / Inventário** (`StockPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
3. **Lotes e Validades de Stock** (`StockBatchesPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
4. **Faturas Recentes de Clientes** (`CommercialInvoicesView` / `ComercialPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
5. **Encomendas de Clientes** (`CommercialOrdersView` / `ComercialPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
6. **Cotações / Propostas Comerciais** (`QuotationsPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
7. **Faturas de Compra / Entrada de Stock** (`ComprasPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
8. **Sugestão de Reposição de Compras** (`PurchaseReorderPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
9. **Encomendas a Fornecedor** (`PurchaseOrdersPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
10. **Histórico de Vendas POS** (`PosSalesHistoryPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
11. **Alertas de Stock - Lotes Expirados** (`StockAlertsPanel`): Coluna "Negócio" e filtro dinâmico.
12. **Alertas de Stock - Produtos Esgotados** (`StockAlertsPanel`): Coluna "Negócio" e filtro dinâmico.
13. **Movimentos & Rastreabilidade de Stock** (`StockPanel`): Coluna "Negócio", filtro dinâmico e exportação.
14. **Transferências entre Armazéns** (`StockPanel`): Coluna "Negócio" e filtro dinâmico.
15. **Guias de Remessa** (`DeliveryGuidesPanel`): Coluna "Negócio", filtro dinâmico e Quick-Peek.
16. **Quebras & Desperdício** (`StockWastePanel`): Coluna "Negócio" e filtro dinâmico.
17. **Notas de Crédito e Débito** (`CommercialNotesPanel`): Coluna "Negócio" e filtro dinâmico em Crédito e Débito.
18. **Promoções de Loja** (`PromotionsPanel`): Coluna "Negócio", filtro dinâmico por linha de negócio e Quick-Peek.
19. **Cabeçalho de Faturação/Vendas**: Seletor comutador "Todos os negócios (Misto)" vs Linha Exclusiva com validação estrita.

---

## 4. Testes de Harness Obrigatórios
Criar `BusinessUnitHarnessTest.java` cobrindo:
1. Criação e validação de Unidade de Negócio com código único e fornecedor.
2. Associação de produtos à unidade de negócio.
3. Faturação em modo misto (sucesso com produtos de negócios diferentes).
4. Faturação em modo restrito por negócio (bloqueio de produto divergente).
5. Faturação em modo restrito por negócio (sucesso quando todos os produtos pertencem ao negócio).
