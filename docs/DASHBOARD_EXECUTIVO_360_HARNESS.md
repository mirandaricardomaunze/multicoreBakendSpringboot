# Matriz de Testes & Harness de Conformidade: Dashboard Executivo 360°
**Código:** HARNESS-D360-001  
**Referência:** `docs/DASHBOARD_EXECUTIVO_360_SPEC.md` (SPEC-D360-001)  
**Data:** 2026-09-17  

---

## 1. Objectivo
Garantir a verificação automatizada e a robustez do componente `StrategicPulseWidget` e da sua integração em `DashboardPanel.java`, assegurando a agregação dos 4 pilares de governação estratégica e navegação fluida em 1-clique sem regressões de UI ou violações arquiteturais.

---

## 2. Critérios de Avaliação Automatizada

| ID | Área | Descrição do Teste | Critério de Aceitação |
|---|---|---|---|
| **D360-01** | Instanciação & Fallback | `StrategicPulseWidget` inicializa em ambiente headless sem dependências nulas | Não lança NPE mesmo com clientes nulos (estado neutro elegante) |
| **D360-02** | Pilar 1: Forense | Apresentação de dados de conformidade | Score formatado (ex.: "98.5%"), subtexto com total de anomalias críticas |
| **D360-03** | Pilar 2: Liquidez | Apresentação de projeção previsional | Saldo projetado a 30 dias exibido com formatação monetária (MT) |
| **D360-04** | Pilar 3: Risco de Crédito | Apresentação de exposição em mora | Total vencido em MT e contagem de clientes em risco crítico |
| **D360-05** | Pilar 4: Metas Comerciais | Apresentação de desempenho de vendas | % de meta concretizada e valores realizado vs planeado |
| **D360-06** | Ações de 1-Clique | Disparo de callbacks de navegação | O clique em cada um dos 4 botões invoca o callback com o cartão correto (`auditoria_forense`, `previsao_tesouraria`, `risco_credito`, `desempenho`) |
| **D360-07** | Arquitetura & Budget | Conformidade com guardas do repositório | `DashboardPanel.java` $\le 700$ linhas ($\le 1000$), zero dependências de BD no desktop, zero violações em `MultiModuleArchitectureHarnessTest` |

---

## 3. Testes Automatizados no Desktop
- Classe de Teste: `desktop/src/test/java/mz/multicore/erp/gui/StrategicPulseWidgetTest.java`
- Verificação de Uniformidade UI: `FinalUiUniformityHarnessTest.java`
- Verificação de Arquitetura Multi-Módulo: `MultiModuleArchitectureHarnessTest.java`
