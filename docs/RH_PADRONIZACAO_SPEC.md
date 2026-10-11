# Especificação Técnica de Padronização — Fase 6: Módulo de Recursos Humanos (RH)

## 1. Contexto e Diagnóstico

O módulo de Recursos Humanos (RH) do Multicore ERP gere o ciclo de vida completo do colaborador, desde a contratação, assiduidade, férias, despesas e deduções até ao processamento salarial e rescisão.

### Diagnóstico de Desvios
1. **Ausência de Calibração `ensureHeadersFit`:** As tabelas de colaboradores, contratos, férias, assiduidade, recibos de salário, despesas, passivos e cessações não executavam a calibração automática de largura de colunas, causando truncagem de títulos e valores.
2. **Ausência de Gaveta «Quick Peek» Silencioso:** O utilizador precisava abrir perfis ou diálogos pesados para consultar rapidamente informações essenciais de um colaborador, contrato ou pedido de férias.

---

## 2. Requisitos Canónicos da Fase 6

1. **Card Canónico:** Manutenção de `ModernPanel(16)` via `UIHelper.tableCardTop(...)`.
2. **Calibração de Colunas:** Invocação de `UIHelper.ensureHeadersFit(table)` na montagem e após carregamento de dados em todos os painéis.
3. **Quick Peek Silencioso com Tecla `Espaço`:** Instalação de `TableQuickPeekController` com drawer lateral (`QuickPeekPanel` de 340px) exibindo metadados operacionais:
   - **Colaborador:** Nome, Cargo, Departamento, Telefone, Email, NUIT, Salário Base, Estado.
   - **Contratos:** Colaborador, Tipo de Contrato, Data Início, Data Fim, Remuneração, Estado.
   - **Férias:** Colaborador, Dias Solicitados, Data Início, Data Fim, Estado de Aprovação.
   - **Assiduidade / Folha de Ponto:** Colaborador, Data, Horas Normais, Horas Extras, Atrasos, Estado.
   - **Descontos & Adiantamentos:** Colaborador, Tipo de Dedução, Valor Total, Parcelas, Saldo Restante.
   - **Despesas:** Colaborador, Categoria, Data, Valor Reembolsável, Estado de Aprovação.
   - **Retenções & Obrigações:** Entidade/Imposto, Período, Valor Retido, Estado de Pagamento.
   - **Cessações:** Colaborador, Motivo de Saída, Último Dia de Trabalho, Valor da Indemnização, Estado.
4. **Preservação de Integridade:** Testes `HRUnifiedHarnessTest`, `DesktopThinContextTest` e `UiPanelDecompositionTest` 100% verdes.

---

## 3. Matriz de Ficheiros

| Painel | Ficheiro | Ações Canónicas |
| :--- | :--- | :--- |
| **Colaboradores & Folha** | `HRPanel.java` | `ensureHeadersFit` e Quick Peek em `employeesTable`, `payslipsTable`, `absencesTable`. |
| **Contratos** | `HRContractsPanel.java` | `ensureHeadersFit` e Quick Peek na tabela de contratos. |
| **Férias** | `HRVacationsPanel.java` | `ensureHeadersFit` e Quick Peek na tabela de férias. |
| **Folha de Ponto** | `HRTimeSheetPanel.java` | `ensureHeadersFit` e Quick Peek na tabela de assiduidade. |
| **Descontos** | `HRDeductionsPanel.java` | `ensureHeadersFit` e Quick Peek na tabela de deduções. |
| **Despesas** | `HRExpensesPanel.java` | `ensureHeadersFit` e Quick Peek na tabela de despesas. |
| **Retenções** | `HRLiabilitiesPanel.java` | `ensureHeadersFit` e Quick Peek na tabela de passivos. |
| **Cessações** | `HRTerminationsPanel.java` | `ensureHeadersFit` e Quick Peek na tabela de rescisões. |
