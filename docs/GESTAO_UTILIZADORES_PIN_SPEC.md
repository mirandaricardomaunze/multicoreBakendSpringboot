# Especificação Técnica: Gestão Visual de Utilizadores, Matriz de Permissões & PIN de Gestor (`SPEC-GUP-001`)

## 1. Âmbito & Objetivos
Esta especificação define o motor de **Gestão Visual de Utilizadores**, a **Matriz de Perfis/Permissões** e a infraestrutura de **PIN de Autorização de Gestor (Manager PIN)** para o Multicore ERP.

O objetivo é permitir que os administradores gerenciem os operadores da empresa, atribuam papéis (`ADMIN`, `MANAGER`, `SELLER`, `ACCOUNTANT`, `HR_MANAGER`), controlem a ativação/desativação de contas e utilizem um PIN de 4 dígitos para autorização presencial em tempo real de ações sensíveis (ex: descontos no POS, anulação de documentos, aprovação de quebras de stock).

---

## 2. Perfis & Matriz de Autorização (RBAC)

| Perfil (`Role`) | Descrição | Permissões Principais |
| :--- | :--- | :--- |
| `ADMIN` | Administrador de Empresa | Acesso total a todas as funcionalidades, configurações, backups, diagnósticos e gestão de utilizadores. |
| `MANAGER` | Gerente de Loja / Operacional | Acesso a relatórios de gestão, auditoria forense, fluxo de caixa, autorização de descontos e aprovação de quebras. |
| `SELLER` | Vendedor / Operador POS | Registo de vendas no POS, emissão de cotações/encomendas e consulta de stock. |
| `ACCOUNTANT` | Contabilista / Técnico | Diário contabilístico, plano de contas, apuramento de IVA e mapas fiscais SAFT. |
| `HR_MANAGER` | Gestor de Recursos Humanos | Processamento salarial, contratos, faltas, férias e saúde ocupacional. |

---

## 3. Protocolo de Autorização por PIN de Gestor (4 Dígitos)

1. **Definição de PIN:** Apenas um `MANAGER` ou `ADMIN` pode ter um PIN de 4 dígitos atribuído. O PIN é encriptado com SHA-256 no backend (`managerPinHash`).
2. **Desencadeamento na UI:** Quando um operador (`SELLER`) tenta executar uma ação que requer privilégio gerencial (ex: conceder desconto >10% no POS ou anular fatura), a UI abre o diálogo compacto `ManagerPinDialog`.
3. **Validação Backend:** O cliente envia `POST /api/users/verify-pin` com o PIN de 4 dígitos. O backend verifica se o PIN pertence a um `MANAGER` ou `ADMIN` ativo da empresa. Se for válido, devolve `approved: true` e regista o evento de auditoria com o nome do gerente autorizador.

---

## 4. Contratos de API REST (`backend`)

- `GET /api/users`: Lista os utilizadores da empresa ativa (`AppUserDTO`).
- `POST /api/users`: Cria um novo utilizador (`CreateUserRequest`).
- `POST /api/users/{username}/pin`: Atribui/atualiza o PIN de gestor de 4 dígitos (`SetManagerPinRequest`).
- `POST /api/users/verify-pin`: Valida o PIN de gestor (`VerifyManagerPinRequest` -> `VerifyManagerPinResponse`).
- `POST /api/users/{username}/reset-password`: Redefine a senha de acesso (`ResetUserPasswordRequest`).
- `PATCH /api/users/{username}/status`: Alterna o estado ativo/inativo (`ToggleUserStatusRequest`).

---

## 5. Interface Desktop (`desktop`)

1. **`UserManagementPanel.java`:** Painel zebrado com grelha de utilizadores, cartões métricos superiores (Total, Gestores, Operadores Ativos, PINs Configurados) e barra de ações.
2. **`UserEditorDialog.java`:** Formulário modal (`ModernFormDialog`) para introdução/edição de utilizadores.
3. **`ManagerPinDialog.java`:** Modal compacto com teclado de PIN e feedback visual imediato.
