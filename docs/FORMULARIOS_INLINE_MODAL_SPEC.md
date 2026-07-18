# Spec — Formulários inline → modal profissional (CRM, Financeiro, Config)

> Converte os 3 formulários **sempre presentes no ecrã** (inline) que faltavam para o modal canónico
> [ModernFormDialog](../src/main/java/com/phcpro/gui/components/ModernFormDialog.java). **Só
> apresentação/UX** — serviços e regras (`CRMService`, `FinanceService`, `AppUserService`) intactos.

**Última actualização:** 2026-06-30

## Problema

Três áreas ainda usavam um formulário embutido (`GridBagLayout`) a ocupar metade do ecrã, em vez de
abrir num modal por botão — destoava do resto do ERP (já em `ModernFormDialog`):

1. **CRM** — "Registar Folha de Obra (Fecho de Ticket)": form inline ao lado da lista.
2. **Financeiro** — "Registar Recebimento": form inline ao lado do histórico de fluxo de caixa.
3. **Configurações › Utilizadores** — "Criar Novo Utilizador": form inline ao lado da lista; o botão
   "Alterar Perfil" reutilizava (incorrectamente) o combo do form de criação.

## Decisões

- **Lista a largura total** em cada uma das 3 áreas, com **barra de acções** no topo (botão de
  criação + acções existentes). Removidos os `GridLayout(1,2)`/`formCard` inline.
- **Criação/edição em `ModernFormDialog`** (cabeçalho premium, contido na janela, scroll):
  - CRM: **Registar Folha de Obra** (`fas-tools`) — Ticket, Técnico, Horas, Descrição, Peças, Custo.
  - Financeiro: **Registar Recebimento** (`fas-money-bill-wave`) — Fatura aprovada + Conta.
  - Config: **Criar Novo Utilizador** (`fas-user-plus`) e **Alterar Perfil** (`fas-user-shield`,
    combo de role próprio, pré-seleccionado com o perfil actual) — corrige a dependência do combo
    partilhado.
- **Validação no `setOnSave`**: lança `IllegalArgumentException`/erro de negócio → o modal
  **mantém-se aberto** com a mensagem; sucesso fecha e recarrega a lista.
- Campos construídos localmente (sem campos de instância) e estilizados por `createDialogForm`.

## Não-objetivos

- Não alterar lógica/validação de negócio nos serviços.
- Não tocar nas outras abas (Auditoria, Backups) nem nas confirmações Sim/Não.
- Não introduzir nova biblioteca.
