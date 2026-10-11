# SPEC — Tradução e Padronização de Estados em Tabelas e Filtros (PT-MZ)

**Criado em:** 2026-10-11  
**Módulos:** `contracts`, `desktop` (`mz.multicore.erp.gui.components`, `mz.multicore.erp.gui`)  
**Contexto:** Erradicação de enums/constantes em inglês (`PENDING`, `ORDERED`, `BILLED`, etc.) visíveis nas tabelas e dropdowns de filtro, garantindo terminologia corporativa em Português de Moçambique.

---

## 1. Diagnóstico do Problema

1. **Constantes em inglês na UI:**
   - Em várias tabelas e cabeçalhos de filtro, estados de documentos eram apresentados com nomes técnicos de enums (ex: `PENDING`, `PENDING_APPROVAL`, `ORDERED`, `PARTIALLY_RECEIVED`, `AWAITING_SEPARATION`, `BILLED`, `COMPLETED`, `OPERATIONAL`, `MAINTENANCE`).
2. **Desconexão entre `OrderStatusLabel` e `UIHelper.humanStatus`:**
   - O contrato `OrderStatusLabel` definia traduções como `"Por facturar"` para `PENDING` e `"Pendente de aprovação"` para `PENDING_APPROVAL`, enquanto `UIHelper.humanStatus` traduzia ambos genericamente como `"Pendente"`, e caía no fallback para estados como `AWAITING_SEPARATION` ou `BILLED`.
3. **Filtros dropdown com texto técnico:**
   - Componentes como `CommercialOrdersView`, `CommercialInvoicesView` e `PurchaseOrdersPanel` instanciam `TableFilter.combo(...)` com strings em inglês (`"PENDING"`, `"BILLED"`, `"ORDERED"`), expondo termos técnicos ao utilizador final.
4. **Acoplamento do filtro à string bruta do modelo:**
   - O `TableFilter` realizava comparação estrita exata (`want.equalsIgnoreCase(cell)`). Se o combo tivesse `"Por facturar"` e o modelo tivesse `"PENDING"`, o filtro falhava se não houvesse resolução semântica canónica.

---

## 2. Regras e Arquitectura da Solução

### R1. Centralização da Tradução Universal em PT-MZ (`UIHelper.humanStatus`)
- Todos os estados do ciclo de vida documental devem ser traduzidos para Português de Moçambique:
  - **Encomendas e Vendas:**
    - `PENDING` / `POR_FACTURAR` → `"Por facturar"`
    - `PENDING_APPROVAL` → `"Pendente de aprovação"`
    - `AWAITING_SEPARATION` → `"Aguarda separação"`
    - `IN_SEPARATION` → `"Em separação"`
    - `SEPARATED` → `"Separado"`
    - `GUIDE_PENDING` → `"Guia por aprovar"`
    - `GUIDED` → `"Expedido por guia"`
    - `TRANSFER_PENDING` → `"Transferência por aprovar"`
    - `TRANSFERRED` → `"Transferido para a loja"`
    - `BILLED` / `INVOICED` → `"Facturado"`
    - `CANCELLED` → `"Cancelado"`
  - **Facturação e Notas:**
    - `DRAFT` → `"Rascunho"`
    - `PENDING_DISCOUNT_APPROVAL` → `"Aprovação de desconto"`
    - `APPROVED` → `"Aprovado"`
    - `PARTIALLY_PAID` → `"Parcialmente paga"`
    - `PAID` → `"Paga"`
    - `REJECTED` → `"Rejeitado"`
    - `OVERDUE` → `"Em atraso"`
  - **Compras e Recepção:**
    - `ORDERED` → `"Encomendado"`
    - `PARTIALLY_RECEIVED` → `"Parcialmente recebido"`
    - `RECEIVED` → `"Recebido"`
  - **Recibos e Caixa:**
    - `COMPLETED` → `"Concluído"`
  - **Contratos e Subscrições:**
    - `ACTIVE` → `"Activo"`
    - `SUSPENDED` → `"Suspenso"`
    - `EXPIRED` → `"Expirado"`
  - **Gestão de Frotas:**
    - `OPERATIONAL` → `"Operacional"`
    - `MAINTENANCE` → `"Manutenção"`
    - `INSPECTION` → `"Inspecção"`
    - `OUT_OF_SERVICE` → `"Fora de serviço"`
    - `SOLD` → `"Vendido"`
  - **Ordens de Produção:**
    - `PLANEADA` → `"Planeada"`
    - `EM_PRODUCAO` → `"Em produção"`
    - `CONCLUIDA` → `"Concluída"`
    - `CANCELADA` → `"Cancelada"`
  - **Guias de Remessa e Entrega:**
    - `ISSUED` → `"Emitida"`
    - `SHIPPED` → `"Em trânsito"`
    - `DELIVERED` → `"Entregue"`
  - **Movimentos de Tesouraria e Banco:**
    - `MATCHED` → `"Conciliado"`
    - `UNMATCHED` → `"Por conciliar"`

### R2. Inteligência Bidirecional no `TableFilter`
- O método de filtragem por coluna `cellMatches(cell, want)` aceita:
  1. Correspondência exata case-insensitive;
  2. `want` traduzido (ex: `"Por facturar"`) vs `cell` enum técnico (ex: `"PENDING"`);
  3. `want` enum técnico vs `cell` traduzido;
  4. Ambas as representações normalizadas via `UIHelper.humanStatus`.
- A pesquisa textual livre (`rowMatches`) pesquisa tanto o valor bruto da célula quanto a sua versão humanizada em português.

### R3. Cores Semânticas de Badges (`TableCellRenderers.StatusRenderer`)
- Badges preservam paleta harmónica (verde, amarelo, vermelho, azul, cinzento) reconhecendo tanto chaves técnicas em inglês quanto rótulos humanizados em português:
  - **Verde (`isGreen`):** `ACTIVE`, `ACTIVA`, `APPROVED`, `PAID`, `OPEN`, `RESOLVED`, `MATCHED`, `CONCILIADO`, `FACTURADO`, `FATURADA`, `BILLED`, `INVOICED`, `OPERATIONAL`, `OPERACIONAL`, `COMPLETED`, `CONCLUIDO`, `CONCLUIDA`, `RECEIVED`, `RECEBIDO`, `DELIVERED`, `ENTREGUE`.
  - **Amarelo (`isYellow`):** `PENDING`, `PENDING_APPROVAL`, `PENDING_DISCOUNT_APPROVAL`, `PARTIALLY_PAID`, `PENDENTE`, `POR FACTURAR`, `POR FATURAR`, `PENDENTE DE APROVAÇÃO`, `AGUARDA SEPARAÇÃO`, `AWAITING_SEPARATION`, `EM SEPARAÇÃO`, `IN_SEPARATION`, `ORDERED`, `ENCOMENDADO`, `PARTIALLY_RECEIVED`, `PARCIALMENTE RECEBIDO`, `GUIA POR APROVAR`, `GUIDE_PENDING`, `TRANSFERÊNCIA POR APROVAR`, `TRANSFER_PENDING`, `IN_PROGRESS`, `EM CURSO`, `UNMATCHED`, `POR CONCILIAR`, `CRÍTICO`, `MAINTENANCE`, `MANUTENÇÃO`, `INSPECTION`, `INSPECÇÃO`.
  - **Vermelho (`isRed`):** `INACTIVE`, `INACTIVA`, `REJECTED`, `CANCELLED`, `OVERDUE`, `REJEITADO`, `ANULADO`, `ANULADA`, `CANCELADO`, `CANCELADA`, `EM ATRASO`, `IGNORED`, `ESGOTADO`, `OUT_OF_SERVICE`, `FORA DE SERVIÇO`, `SOLD`, `VENDIDO`, `EXPIRED`, `EXPIRADO`.
  - **Azul (`isBlue`):** `PROCESSING`, `EM PROCESSAMENTO`, `SHIPPED`, `ENVIADO`, `EM TRÂNSITO`, `SCHEDULED`, `AGENDADO`, `BAIXO`, `DRAFT`, `RASCUNHO`, `SEPARATED`, `SEPARADO`, `GUIDED`, `EXPEDIDO POR GUIA`, `TRANSFERRED`, `TRANSFERIDO`, `PLANEADA`, `EM PRODUÇÃO`, `EM_PRODUCAO`, `SUSPENDED`, `SUSPENSO`.

### R4. Humanização de Todos os Menus Dropdown de Filtro
- Todos os `TableFilter.combo(...)` passam a conter rótulos legíveis em português de Moçambique, eliminando de vez strings como `"PENDING"` dos seletores.

---

## 3. Critérios de Sucesso e Validação

1. Todas as células com `TableCellRenderers.status()` exibem texto em português.
2. Nenhum filtro dropdown apresenta enums em inglês.
3. Testes do harness `TableStatusPtMzHarnessTest` cobrem 100% dos fluxos e passam sem falhas.
