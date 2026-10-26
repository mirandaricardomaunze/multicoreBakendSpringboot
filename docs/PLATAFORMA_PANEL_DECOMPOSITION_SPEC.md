# Especificação — Decomposição Canónica do Painel da Plataforma (Superadmin)
**Código:** `SPEC-PLATAFORMA-PANEL-DECOMPOSITION-001`  
**Módulo:** `desktop` (`mz.multicore.erp.gui.platform` / `mz.multicore.erp.gui`)  
**Data:** 2026-10-22  

---

## 1. Contexto e Diagnóstico

O painel `PlataformaPanel.java` atingiu **1.298 linhas**, sendo o ficheiro mais volumoso do desktop e violando frontalmente a restrição do sistema (`MAX_PANEL_LINES = 1000`).

O ficheiro concentra 6 abas de alta complexidade do Superadmin:
1. **Empresas:** Listagem, pesquisa, ativação/desativação, bloqueio, visualização de NUIT e detalhes da subscrição.
2. **Assinaturas & Pagamentos:** KPIs de MRR/ARR, taxa de renovação, alertas de risco de expiração, gráfico de distribuição de planos e histórico de pagamentos.
3. **Utilizadores da Plataforma:** Listagem global de contas em todas as empresas clientes, alteração de palavra-passe e permissões.
4. **Assistência & Suporte:** Abertura e acompanhamento de tickets, categorização de criticidade e encerramento.
5. **Versões dos Clientes:** Auditoria de versões instaladas nos postos desktop e deteção de versões desatualizadas.
6. **Saúde & Diagnóstico:** Telemetria de CPU/memória, tempo de resposta, latência de base de dados e integridade dos nós.

---

## 2. Metas de Linhas e Arquitetura Canónica

| Componente | Ficheiro | Responsabilidade | Teto de Linhas |
| :--- | :--- | :--- | :--- |
| **Orquestrador de Abas** | `PlataformaPanel.java` | Cabeçalho executivo, injeção de clientes e coordenação de `onPanelSelected()` | <= 120 linhas |
| **Aba 1: Empresas** | `PlatformCompaniesView.java` | Tabela de empresas clientes, ações de ativação/bloqueio e detalhe | <= 280 linhas |
| **Aba 2: Assinaturas** | `PlatformSubscriptionsView.java` | KPIs de receita recorrente (MRR), pagamentos, gráficos de planos e renovações | <= 280 linhas |
| **Aba 3: Utilizadores** | `PlatformUsersView.java` | Tabela global de utilizadores e redefinição de acessos | <= 220 linhas |
| **Aba 4: Assistência** | `PlatformSupportTicketsView.java` | Gestão e resolução de tickets de suporte | <= 220 linhas |
| **Aba 5: Versões** | `PlatformClientVersionsView.java` | Auditoria de compatibilidade de versões dos clientes | <= 180 linhas |
| **Aba 6: Monitorização** | `PlatformSystemMonitoringView.java` | Painel de telemetria, integridade de microsserviços e PostgreSQL | <= 220 linhas |

---

## 3. Regras Arquiteturais Não-Negociáveis

1. **Retrocompatibilidade Rigorosa de Construtores:**
   `PlataformaPanel` deve manter exatamente os dois construtores públicos:
   * `public PlataformaPanel(PlatformApiClient platformApiClient)`
   * `public PlataformaPanel(PlatformApiClient platformApiClient, SystemMonitoringApiClient monitoringApiClient)`
   * `public void onPanelSelected()` delegando para o carregamento preguiçoso de cada aba ativa.
2. **Contenção e Design System:**
   * Todas as tabelas continuam contidas em `ModernPanel(16)` via `tableCardTop` e `TableFilter.bar`.
   * Botões de ação em cabeçalhos respeitam o teto de no máximo 2-3 botões abertos, agrupando ações em `ActionMenuButton`.
   * Proibido o uso de emojis em abas, botões ou cabeçalhos.
3. **Fronteiras Thin Client:**
   * Todos os sub-painéis comunicam exclusivamente via `PlatformApiClient` / `SystemMonitoringApiClient`.

---

## 4. Critérios de Aceitação

- [x] `PlataformaPanel.java` reduzido para menos de 130 linhas.
- [x] 6 sub-views especializadas no pacote `mz.multicore.erp.gui.platform`.
- [x] `mvn compile` sem avisos ou erros.
- [x] Testes `PlataformaPanelDecompositionHarnessTest`, `TableCardContainmentAuditTest` e `DesktopThinContextTest` 100% verdes.
