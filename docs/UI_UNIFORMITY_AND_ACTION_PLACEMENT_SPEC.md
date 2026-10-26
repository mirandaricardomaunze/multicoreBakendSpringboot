# UI Uniformity and Action Placement Specification

## 1. Contexto e Diagnóstico
A suíte de testes de conformidade visual e integridade de UI (`EnterpriseModulesUiUniformityHarnessTest`, `FinalUiUniformityHarnessTest`, `TableActionPlacementHarnessTest` e `PhcInlineDocumentGridHarnessTest`) reprovou por três desvios pontuais em relação ao design system canónico:
1. `CommercialOrdersView.java` instanciou uma cor local `new Color(220, 38, 38)` para o aviso de crédito em vez de utilizar a constante corporativa canónica `UIHelper.REJECTED_RED`.
2. `CommercialInvoicesView.java` estilizou a adição do cabeçalho de ações com condicional ternária inline no método `cardHeader.add(...)`, quebrando a assinatura e ordem canónica de botões declarativa esperada pelo teste de colocação de ações.
3. `CommercialInvoicesView.java` omitiu o lembrete explícito de regra de negócio `"depois de emitida a fatura é imutável"` na grelha de linhas do editor de rascunhos.

## 2. Regras e Invariantes
- **Cores Estritamente Centralizadas:** Qualquer elemento visual de aviso ou estado de erro deve utilizar `UIHelper.REJECTED_RED`, sendo proibida a instanciação avulsa de `new Color(...)` nos painéis corporativos.
- **Topos de Tabela e Colocação de Ações:** O cabeçalho do card de tabela deve invocar explicitamente `cardHeader.add(UIHelper.actionsBar(refreshBtn, moreBtn, issueBtn), BorderLayout.EAST)` no padrão canónico. A gestão de permissões de emissão deve ocultar ou desativar o botão `issueBtn` sem fragmentar o layout em blocos condicionais divergentes.
- **Imutabilidade Documental:** A documentação de UI e tooltip da tabela de faturação deve conter o princípio fiscal de Moçambique: "depois de emitida a fatura é imutável".

## 3. Harness de Validação
- `EnterpriseModulesUiUniformityHarnessTest`
- `FinalUiUniformityHarnessTest`
- `TableActionPlacementHarnessTest`
- `PhcInlineDocumentGridHarnessTest`
