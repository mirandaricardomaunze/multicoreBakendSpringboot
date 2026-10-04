# Matriz de Conformidade Automatizada: Gestão de Utilizadores & PIN de Gestor (`HARNESS-GUP-001`)

Esta matriz estabelece os critérios de validação e testes automatizados para a suíte de Gestão de Utilizadores e PIN de Autorização de Gestor.

---

## 1. Matriz de Testes Automatizados (GUP-01 a GUP-08)

| ID | Descrição do Teste | Módulo | Classe de Teste | Resultado Esperado |
| :--- | :--- | :--- | :--- | :--- |
| **GUP-01** | Listagem de utilizadores restrita por tenant de empresa | `backend` | `UserManagementHarnessTest` | Devolve apenas os utilizadores pertencentes à empresa do `CurrentUserContext`. |
| **GUP-02** | Criação de utilizador com papel `SELLER` ou `MANAGER` | `backend` | `UserManagementHarnessTest` | Cria utilizador e devolve DTO sem expor senha. |
| **GUP-03** | Atribuição de PIN de gestor de 4 dígitos | `backend` | `UserManagementHarnessTest` | Encripta e persiste o `managerPinHash` com sucesso. |
| **GUP-04** | Verificação com sucesso de PIN de gestor válido | `backend` | `UserManagementHarnessTest` | Devolve `approved: true` e o nome do gerente autorizador. |
| **GUP-05** | Recusa de PIN incorreto ou de utilizador inativo/sem permissão | `backend` | `UserManagementHarnessTest` | Devolve `approved: false` ou lança `BusinessRuleException`. |
| **GUP-06** | Ativação/Desativação de utilizador | `backend` | `UserManagementHarnessTest` | Alterna o campo `active` e impede login de utilizadores desativados. |
| **GUP-07** | Instanciação sem erros do `UserManagementPanel` em headless | `desktop` | `UserManagementPanelHarnessTest` | Monta a tabela e cartões KPI sem excepções AWT. |
| **GUP-08** | Validação do modal `ManagerPinDialog` | `desktop` | `UserManagementPanelHarnessTest` | Valida introdução de PIN de 4 dígitos e callback de aprovação. |
